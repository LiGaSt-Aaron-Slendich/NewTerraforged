package com.terraforged.mod.internal.probe.client;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

final class InspectorInputHelper {
    private static Field mouseGrabbedField;

    private InspectorInputHelper() {
    }

    static boolean isTextInputActive(Minecraft mc) {
        return mc.screen != null;
    }

    /** Unlock OS cursor and clear Minecraft grab flag (without warping every tick). */
    static void ensureFreeCursor(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        InspectorInputHelper.clearMouseGrabbed(mc.mouseHandler);
    }

    /** One-time transition from FPS grab to free cursor when inspector starts. */
    static void activateFreeCursor(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        double[] pos = InspectorInputHelper.cursorPos(mc);
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
            GLFW.glfwSetCursorPos(window, pos[0], pos[1]);
        }
        InspectorInputHelper.ensureFreeCursor(mc);
    }

    static BlockHitResult rayPick(Minecraft mc, InspectorCamera camera) {
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return null;
        }
        double[] pos = InspectorInputHelper.cursorPos(mc);
        Vec3 start = camera.eyePosition();
        Vec3 look = InspectorInputHelper.lookFromScreen(pos[0], pos[1], mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), camera.yaw(), camera.pitch(), (float)mc.options.fov);
        Vec3 end = start.add(look.scale(160.0));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    static BlockPos rayPickHorizontalPlane(Minecraft mc, InspectorCamera camera, int planeY) {
        double[] pos = InspectorInputHelper.cursorPos(mc);
        Vec3 start = camera.eyePosition();
        Vec3 look = InspectorInputHelper.lookFromScreen(pos[0], pos[1], mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), camera.yaw(), camera.pitch(), (float)mc.options.fov);
        if (Math.abs(look.y) < 1.0E-4) {
            return null;
        }
        double t = ((double)planeY + 0.5 - start.y) / look.y;
        if (t < 0.0) {
            return null;
        }
        Vec3 hit = start.add(look.scale(t));
        return new BlockPos(Mth.floor(hit.x), planeY, Mth.floor(hit.z));
    }

    static Vec3 lookFromScreen(double mx, double my, int screenW, int screenH, float yaw, float pitch, float fov) {
        float[] aim = InspectorInputHelper.mouseAimOffset(mx, my, screenW, screenH, fov);
        float aimYaw = yaw + aim[0];
        float aimPitch = Mth.clamp(pitch + aim[1], -89.0f, 89.0f);
        return InspectorInputHelper.lookFromAngles(aimYaw, aimPitch);
    }

    static Vec3 lookFromAngles(float yaw, float pitch) {
        float pitchRad = (float)Math.toRadians(pitch);
        float yawRad = (float)Math.toRadians(yaw);
        float cosPitch = Mth.cos(pitchRad);
        return new Vec3(-Mth.sin(yawRad) * cosPitch, -Mth.sin(pitchRad), Mth.cos(yawRad) * cosPitch);
    }

    private static float[] mouseAimOffset(double mx, double my, int screenW, int screenH, float fov) {
        int w = Math.max(1, screenW);
        int h = Math.max(1, screenH);
        float aspect = (float)w / (float)h;
        float dx = (float)((0.5 - mx / (double)w) * fov * aspect * 0.55);
        float dy = (float)((0.5 - my / (double)h) * fov * 0.55);
        return new float[]{dx, dy};
    }

    static double[] cursorPos(Minecraft mc) {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), x, y);
        return new double[]{x[0], y[0]};
    }

    private static void clearMouseGrabbed(MouseHandler handler) {
        Field field = InspectorInputHelper.mouseGrabbedField();
        if (field == null) {
            return;
        }
        try {
            field.setBoolean(handler, false);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field mouseGrabbedField() {
        if (mouseGrabbedField != null) {
            return mouseGrabbedField;
        }
        for (String name : new String[]{"mouseGrabbed", "isMouseGrabbed"}) {
            try {
                Field field = MouseHandler.class.getDeclaredField(name);
                field.setAccessible(true);
                mouseGrabbedField = field;
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
