package com.terraforged.mod.internal.probe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

final class ScreenFill {
    private ScreenFill() {
    }

    static void fill(PoseStack poseStack, int left, int top, int right, int bottom, int color) {
        if (left < right && top < bottom) {
            Screen.fill(poseStack, left, top, right, bottom, color);
        }
    }
}
