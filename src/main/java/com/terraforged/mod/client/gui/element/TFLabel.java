package com.terraforged.mod.client.gui.element;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/** Non-interactive section label. */
public class TFLabel extends AbstractWidget implements Element {
    private final int color;

    public TFLabel(String text) {
        this(text, 0xFFFFFF);
    }

    public TFLabel(String text, int color) {
        super(0, 0, 100, 20, Component.literal(text));
        this.color = color;
        this.visible = true;
        this.active = false;
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        font.draw(pose, this.getMessage(), this.x, this.y + (this.height - 8) / 2.0F, this.color);
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}
