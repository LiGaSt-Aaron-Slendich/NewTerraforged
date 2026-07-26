package com.terraforged.mod.client.gui.screen.page;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Placeholder settings page until TerraSettings / TOML widgets are ported. */
public final class StubPage implements Page {
    private final Component title;
    private final String bodyKey;
    private int left;
    private int top;

    public StubPage(String titleKey, String bodyKey) {
        this.title = Component.translatable(titleKey);
        this.bodyKey = bodyKey;
    }

    @Override
    public Component title() {
        return this.title;
    }

    @Override
    public void init(ConfigScreen screen, int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Minecraft.getInstance().font.draw(pose, this.title, this.left, this.top, 0xFFFFFF);
        Minecraft.getInstance().font.drawWordWrap(
                Component.translatable(this.bodyKey),
                this.left,
                this.top + 18,
                200,
                0xA0A0A0
        );
    }
}
