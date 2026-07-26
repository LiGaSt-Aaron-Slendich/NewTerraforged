package com.terraforged.mod.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import net.minecraft.network.chat.Component;

/** One left-pane settings page inside {@link ConfigScreen}. */
public interface Page {
    Component title();

    void init(ConfigScreen screen, int left, int top, int width, int height);

    default void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
    }

    default void save() {
    }

    default void close() {
    }
}
