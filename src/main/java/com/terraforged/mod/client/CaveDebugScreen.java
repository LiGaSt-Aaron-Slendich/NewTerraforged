package com.terraforged.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public final class CaveDebugScreen extends Screen {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 8;
    private final List<String> lines;
    private int scroll;

    public CaveDebugScreen(List<String> lines) {
        super((Component)new TextComponent("NewTerraForged Cave Debug"));
        this.lines = lines;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new Button(this.width / 2 - 60, this.height - 28, 120, 20, (Component)new TextComponent("Close"), b -> this.onClose()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, this.lines.size() * LINE_HEIGHT - (this.height - 48));
        this.scroll = (int)Math.max(0.0, Math.min(maxScroll, this.scroll - delta * LINE_HEIGHT));
        return true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 6, 0xFFFFFF);
        int y = PADDING + 14 - this.scroll;
        for (String line : this.lines) {
            if (y > -LINE_HEIGHT && y < this.height - 36) {
                this.font.draw(poseStack, line, PADDING, y, 0xDDDDDD);
            }
            y += LINE_HEIGHT;
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
