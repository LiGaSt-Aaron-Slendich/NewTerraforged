package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button that draws a full-name tip slightly above itself on hover. */
public final class HoverTipButton extends Button {
    private final Supplier<Component> tip;

    public HoverTipButton(int x, int y, int w, int h, Component message, OnPress onPress, Supplier<Component> tip) {
        super(x, y, w, h, message, onPress);
        this.tip = tip;
    }

    @Override
    public void renderToolTip(PoseStack pose, int mouseX, int mouseY) {
        Component text = this.tip != null ? this.tip.get() : this.getMessage();
        if (text == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int tw = font.width(text);
        int tx = this.x + (this.width - tw) / 2;
        int ty = this.y - font.lineHeight - 3;
        if (ty < 2) {
            ty = this.y + this.height + 2;
        }
        fill(pose, tx - 3, ty - 2, tx + tw + 3, ty + font.lineHeight + 1, 0xC0101010);
        font.draw(pose, text, tx, ty, 0xFFFFFF);
    }
}
