package com.terraforged.mod.internal.probe.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.internal.probe.InspectorOverlayMode;
import com.terraforged.mod.internal.probe.TfOverlayColumn;
import com.terraforged.mod.internal.probe.TfProbeResult;
import com.terraforged.mod.platform.forge.ProbeNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

public final class InspectorClient {
    private static final int OVERLAY_RADIUS_CHUNKS = 6;
    private static final int OVERLAY_RADIUS = OVERLAY_RADIUS_CHUNKS * 16;
    private static final int OVERLAY_TICK_COOLDOWN = 30;
    private static final double MOVE_SPEED = 1.1;
    private static final double SPRINT_MULT = 3.5;
    private static final double OVERLAY_MOVE_THRESHOLD = 12.0;
    private static final State STATE = new State();
    private static boolean bootstrapped;
    private static int overlayCooldown;

    private InspectorClient() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        InspectorUiHider.register();
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onRenderTick);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onRenderWorld);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onCameraSetup);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onRenderHud);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onMouseInput);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onMouseScroll);
        MinecraftForge.EVENT_BUS.addListener(InspectorClient::onKeyInput);
        TerraForged.LOG.info("[probe] Inspector client ready");
    }

    public static void onSessionToggle(boolean active) {
        if (active) {
            InspectorClient.startLocal();
        } else {
            InspectorClient.stopLocal(false);
        }
    }

    public static void onProbeResult(TfProbeResult result) {
        STATE.pendingProbe = false;
        STATE.pickPos = new BlockPos(result.x(), result.y(), result.z());
        InspectorDetailPanel.INSTANCE.show(result);
    }

    public static void onOverlay(byte modeId, List<TfOverlayColumn> columns) {
        if (!STATE.active || STATE.mode.id != modeId) {
            return;
        }
        STATE.overlayColumns = columns;
    }

    public static boolean isActive() {
        return STATE.active;
    }

    public static InspectorCamera inspectorCamera() {
        return STATE.camera;
    }

    private static void startLocal() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        STATE.active = true;
        STATE.savedPos = player.position();
        STATE.savedYaw = player.getYRot();
        STATE.savedPitch = player.getXRot();
        STATE.camera.initFromEye(player.getEyePosition(1.0f), player.getYRot(), player.getXRot());
        STATE.mode = InspectorOverlayMode.BIOMES;
        STATE.overlayColumns = List.of();
        STATE.pickPos = null;
        STATE.zoneStart = null;
        STATE.zoneEnd = null;
        STATE.zoneDragging = false;
        STATE.pendingProbe = false;
        STATE.mmbDown = false;
        STATE.hoveredLabel = null;
        STATE.lastOverlayPos = null;
        STATE.savedGamma = mc.options.gamma;
        InspectorClient.applyGammaForMode(mc);
        InspectorDetailPanel.INSTANCE.clear();
        InspectorInputHelper.activateFreeCursor(mc);
        InspectorClient.setSpectator(mc, player, true);
        player.setInvisible(true);
        player.setDeltaMovement(Vec3.ZERO);
        STATE.savedFlying = player.getAbilities().flying;
        STATE.savedMayFly = player.getAbilities().mayfly;
        player.getAbilities().flying = true;
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        InspectorClient.requestOverlay();
    }

    private static void stopLocal(boolean notifyServer) {
        if (!STATE.active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        STATE.active = false;
        Player player = mc.player;
        if (player != null) {
            InspectorClient.setSpectator(mc, player, false);
            player.setInvisible(false);
            player.getAbilities().flying = STATE.savedFlying;
            player.getAbilities().mayfly = STATE.savedMayFly;
            player.onUpdateAbilities();
            if (STATE.savedPos != null) {
                player.setPos(STATE.savedPos.x, STATE.savedPos.y, STATE.savedPos.z);
                player.setYRot(STATE.savedYaw);
                player.setXRot(STATE.savedPitch);
            }
            player.setDeltaMovement(Vec3.ZERO);
        }
        mc.mouseHandler.grabMouse();
        mc.options.gamma = STATE.savedGamma;
        InspectorDetailPanel.INSTANCE.clear();
        STATE.overlayColumns = List.of();
        if (notifyServer) {
            ProbeNetwork.sendStopSession();
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !STATE.active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            InspectorClient.stopLocal(true);
            return;
        }
        InspectorInputHelper.ensureFreeCursor(mc);
        if (STATE.mode == InspectorOverlayMode.BIOMES) {
            long window = mc.getWindow().getWindow();
            STATE.hoveredLabel = InspectorOverlayHitTest.hoverLabel(STATE.camera, STATE.overlayColumns, window, mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), (float)mc.options.fov);
        } else {
            STATE.hoveredLabel = null;
        }
        Vec3 pos = STATE.camera.position();
        boolean moved = STATE.lastOverlayPos == null || pos.distanceToSqr(STATE.lastOverlayPos) >= OVERLAY_MOVE_THRESHOLD * OVERLAY_MOVE_THRESHOLD;
        if (moved && --overlayCooldown <= 0) {
            overlayCooldown = OVERLAY_TICK_COOLDOWN;
            STATE.lastOverlayPos = pos;
            InspectorClient.requestOverlay();
        }
    }

    private static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !STATE.active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || InspectorInputHelper.isTextInputActive(mc)) {
            return;
        }
        InspectorClient.trackViewportDrag(mc);
        InspectorClient.processFlight(mc);
        InspectorClient.syncPlayerToCamera(player);
        if (STATE.zoneDragging && STATE.zoneStart != null) {
            BlockPos look = InspectorInputHelper.rayPickHorizontalPlane(mc, STATE.camera, STATE.zoneStart.getY());
            if (look != null) {
                STATE.zoneEnd = look;
            }
        }
    }

    private static void processFlight(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        double speed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ? MOVE_SPEED * SPRINT_MULT : MOVE_SPEED;
        double forward = 0.0;
        double right = 0.0;
        double up = 0.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W)) forward += 1.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S)) forward -= 1.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D)) right += 1.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A)) right -= 1.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_SPACE)) up += 1.0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)) up -= 1.0;
        STATE.camera.fly(forward, right, up, speed);
        if (mc.player != null) {
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void setSpectator(Minecraft mc, Player player, boolean spectator) {
        if (mc.getSingleplayerServer() != null) {
            ServerPlayer serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer != null) {
                if (spectator) {
                    STATE.savedGameMode = serverPlayer.gameMode.getGameModeForPlayer();
                    serverPlayer.setGameMode(GameType.SPECTATOR);
                } else if (STATE.savedGameMode != null) {
                    serverPlayer.setGameMode(STATE.savedGameMode);
                }
            }
        }
        player.noPhysics = spectator;
    }

    private static void syncPlayerToCamera(Player player) {
        Vec3 eye = STATE.camera.eyePosition();
        float yaw = STATE.camera.yaw();
        float pitch = STATE.camera.pitch();
        player.absMoveTo(eye.x, eye.y, eye.z, yaw, pitch);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            ServerPlayer serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer != null) {
                serverPlayer.absMoveTo(eye.x, eye.y, eye.z, yaw, pitch);
                serverPlayer.setYRot(yaw);
                serverPlayer.setXRot(pitch);
                serverPlayer.yRotO = yaw;
                serverPlayer.xRotO = pitch;
                serverPlayer.yHeadRot = yaw;
                serverPlayer.yHeadRotO = yaw;
            }
        }
    }

    private static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!STATE.active) {
            return;
        }
        event.setYaw(STATE.camera.yaw());
        event.setPitch(STATE.camera.pitch());
        event.setRoll(0.0f);
    }

    private static void onRenderWorld(RenderLevelLastEvent event) {
        if (!STATE.active) {
            return;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InspectorWorldOverlay.render(event.getPoseStack(), cam, STATE.mode, STATE.overlayColumns, STATE.hoveredLabel, STATE.pickPos, STATE.zoneStart, STATE.zoneEnd);
    }

    private static void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (!STATE.active || event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        PoseStack poseStack = event.getMatrixStack();
        poseStack.pushPose();
        InspectorHud.render(poseStack, STATE);
        InspectorDetailPanel.INSTANCE.render(poseStack, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        poseStack.popPose();
    }

    private static void onMouseInput(InputEvent.MouseInputEvent event) {
        if (!STATE.active || InspectorInputHelper.isTextInputActive(Minecraft.getInstance())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            STATE.mmbDown = true;
            double[] pos = InspectorInputHelper.cursorPos(mc);
            STATE.lastMouseX = pos[0];
            STATE.lastMouseY = pos[1];
            STATE.hasLastMouse = true;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            STATE.mmbDown = false;
            STATE.hasLastMouse = false;
        }
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (InspectorDetailPanel.INSTANCE.tryCloseAt(mc)) {
                return;
            }
            if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
                STATE.zoneDragging = true;
                BlockPos start = InspectorClient.rayPickBlock(mc);
                if (start != null) {
                    STATE.zoneStart = start;
                    STATE.zoneEnd = start;
                }
            } else {
                InspectorClient.pick(mc);
            }
        }
        if (event.getAction() == GLFW.GLFW_RELEASE && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (STATE.zoneDragging) {
                STATE.zoneDragging = false;
                InspectorClient.exportZone(mc);
            }
        }
    }

    private static void trackViewportDrag(Minecraft mc) {
        if (!STATE.mmbDown || InspectorInputHelper.isTextInputActive(mc)) {
            STATE.hasLastMouse = false;
            return;
        }
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) != GLFW.GLFW_PRESS) {
            STATE.mmbDown = false;
            STATE.hasLastMouse = false;
            return;
        }
        double[] pos = InspectorInputHelper.cursorPos(mc);
        if (STATE.hasLastMouse) {
            double dx = pos[0] - STATE.lastMouseX;
            double dy = pos[1] - STATE.lastMouseY;
            if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
                STATE.camera.pan(dx, dy);
            } else {
                STATE.camera.orbit(dx, dy);
            }
        }
        STATE.lastMouseX = pos[0];
        STATE.lastMouseY = pos[1];
        STATE.hasLastMouse = true;
    }

    private static void onMouseScroll(InputEvent.MouseScrollEvent event) {
        if (!STATE.active || InspectorInputHelper.isTextInputActive(Minecraft.getInstance())) {
            return;
        }
        if (InspectorDetailPanel.INSTANCE.isMouseOver(Minecraft.getInstance())) {
            InspectorDetailPanel.INSTANCE.scroll(event.getScrollDelta());
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return;
        }
        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
            InspectorDetailPanel.INSTANCE.scroll(event.getScrollDelta());
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return;
        }
        STATE.camera.dolly(event.getScrollDelta());
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    private static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!STATE.active) {
            return;
        }
        if (InspectorInputHelper.isTextInputActive(Minecraft.getInstance()) && event.getKey() != GLFW.GLFW_KEY_ESCAPE) {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        int key = event.getKey();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (InspectorDetailPanel.INSTANCE.isOpen()) {
                InspectorDetailPanel.INSTANCE.clear();
            } else {
                InspectorClient.stopLocal(true);
            }
            return;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && InspectorDetailPanel.INSTANCE.isOpen()) {
            InspectorDetailPanel.INSTANCE.clear();
            return;
        }
        if (key == GLFW.GLFW_KEY_M) {
            STATE.mode = STATE.mode.next();
            STATE.overlayColumns = List.of();
            InspectorClient.applyGammaForMode(Minecraft.getInstance());
            InspectorClient.requestOverlay();
            return;
        }
        if (key == GLFW.GLFW_KEY_1) {
            InspectorClient.setMode(InspectorOverlayMode.BIOMES);
        } else if (key == GLFW.GLFW_KEY_2) {
            InspectorClient.setMode(InspectorOverlayMode.FEATURES);
        } else if (key == GLFW.GLFW_KEY_3) {
            InspectorClient.setMode(InspectorOverlayMode.TERRAIN);
        }
        if (key == GLFW.GLFW_KEY_F && STATE.mode == InspectorOverlayMode.BIOMES) {
            BlockPos hit = InspectorClient.rayPickBlock(Minecraft.getInstance());
            if (hit != null) {
                STATE.camera.setFocus(Vec3.atCenterOf(hit));
            }
        }
    }

    private static void setMode(InspectorOverlayMode mode) {
        STATE.mode = mode;
        STATE.overlayColumns = List.of();
        InspectorClient.applyGammaForMode(Minecraft.getInstance());
        InspectorClient.requestOverlay();
    }

    private static void applyGammaForMode(Minecraft mc) {
        if (!STATE.active || mc == null) {
            return;
        }
        mc.options.gamma = STATE.mode == InspectorOverlayMode.BIOMES ? 1.0 : 3.5;
    }

    private static void pick(Minecraft mc) {
        BlockPos pos = InspectorClient.rayPickBlock(mc);
        if (pos == null) {
            return;
        }
        STATE.pendingProbe = true;
        STATE.camera.setFocus(Vec3.atCenterOf(pos));
        ProbeNetwork.sendProbeRequest(pos);
    }

    private static BlockPos rayPickBlock(Minecraft mc) {
        BlockHitResult hit = InspectorInputHelper.rayPick(mc, STATE.camera);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return hit.getBlockPos();
    }

    private static void requestOverlay() {
        if (!STATE.active) {
            return;
        }
        Vec3 pos = STATE.camera.position();
        BlockPos center = new BlockPos(pos.x, pos.y, pos.z);
        ProbeNetwork.sendOverlayRequest(STATE.mode, center, OVERLAY_RADIUS);
    }

    private static void exportZone(Minecraft mc) {
        if (STATE.zoneStart == null || STATE.zoneEnd == null || mc.player == null) {
            return;
        }
        int minX = Math.min(STATE.zoneStart.getX(), STATE.zoneEnd.getX());
        int maxX = Math.max(STATE.zoneStart.getX(), STATE.zoneEnd.getX());
        int minZ = Math.min(STATE.zoneStart.getZ(), STATE.zoneEnd.getZ());
        int maxZ = Math.max(STATE.zoneStart.getZ(), STATE.zoneEnd.getZ());
        int y = Math.min(STATE.zoneStart.getY(), STATE.zoneEnd.getY());
        ArrayList<String> lines = new ArrayList<>();
        lines.add("=== TF Inspector Zone ===");
        lines.add(String.format(Locale.ROOT, "Bounds: %d..%d, y=%d, %d..%d", minX, maxX, y, minZ, maxZ));
        int cx = (minX + maxX) / 2;
        int cz = (minZ + maxZ) / 2;
        ProbeNetwork.sendProbeRequest(new BlockPos(cx, y, cz));
        STATE.pendingProbe = true;
        InspectorDetailPanel.INSTANCE.show(new TfProbeResult(cx, y, cz, lines));
    }

    static final class State {
        boolean active;
        final InspectorCamera camera = new InspectorCamera();
        InspectorOverlayMode mode = InspectorOverlayMode.BIOMES;
        List<TfOverlayColumn> overlayColumns = List.of();
        String hoveredLabel;
        BlockPos pickPos;
        BlockPos zoneStart;
        BlockPos zoneEnd;
        boolean zoneDragging;
        boolean pendingProbe;
        boolean mmbDown;
        boolean hasLastMouse;
        double lastMouseX;
        double lastMouseY;
        Vec3 savedPos;
        float savedYaw;
        float savedPitch;
        boolean savedFlying;
        boolean savedMayFly;
        double savedGamma = 1.0;
        GameType savedGameMode;
        Vec3 lastOverlayPos;
    }
}
