package com.terraforged.mod.internal.probe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.internal.probe.InspectorOverlayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;

final class InspectorHud {
    private InspectorHud() {
    }

    static void render(PoseStack poseStack, InspectorClient.State state) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int x = 8;
        int y = 8;
        int color = 0xE0FFE0;
        font.draw(poseStack, "TF Inspector", x, y, 0x55FF55);
        y += 11;
        font.draw(poseStack, "Mode: " + state.mode.name() + "  [1/2/3]  M cycle", x, y, color);
        y += 10;
        if (state.mode == InspectorOverlayMode.BIOMES) {
            font.draw(poseStack, "3D cave fill + 2D surface cover | hover = highlight", x, y, 0xAAAAAA);
            y += 10;
            if (state.hoveredLabel != null) {
                font.draw(poseStack, "Hover: " + state.hoveredLabel, x, y, 0xFFFF88);
                y += 10;
            }
        }
        Vec3 pos = state.camera.position();
        font.draw(poseStack, String.format("Cam %.1f %.1f %.1f | zone 6x6 chunks", pos.x, pos.y, pos.z), x, y, color);
        y += 10;
        font.draw(poseStack, "MMB orbit | Shift+MMB pan | Scroll zoom | WASD fly", x, y, 0xCCCCCC);
        y += 10;
        font.draw(poseStack, "LMB pick | Shift+LMB zone | F focus | Esc stop", x, y, 0xCCCCCC);
        y += 10;
        font.draw(poseStack, "Probe panel: wheel over panel to scroll", x, y, 0xAAAAAA);
        if (state.zoneDragging || state.zoneStart != null) {
            y += 10;
            font.draw(poseStack, "Zone select active", x, y, 0xFFAA00);
        }
        if (state.pendingProbe) {
            y += 10;
            font.draw(poseStack, "Probing...", x, y, 0xAAAAFF);
        }
    }
}
