package com.terraforged.mod.client.gui.screen.nv;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/** Minimal command strip over the title screen — no chrome, no help text. */
public final class NvCmdOverlay extends Screen {
    private final Screen parent;
    private EditBox input;

    public NvCmdOverlay(Screen parent) {
        super(TextComponent.EMPTY);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.input = new EditBox(this.font, 8, this.height - 28, this.width - 16, 20, new TextComponent(""));
        this.input.setMaxLength(256);
        this.input.setEditable(true);
        this.input.setBordered(true);
        this.input.setVisible(true);
        this.input.setCanLoseFocus(true);
        this.addRenderableWidget(this.input);
        this.setInitialFocus(this.input);
        this.input.setFocus(true);
    }

    @Override
    public void tick() {
        this.input.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.input.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.input);
            this.input.setFocus(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.submit();
            return true;
        }
        if (this.input.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.input.isFocused() && this.input.charTyped(codePoint, modifiers)) {
            return true;
        }
        this.setFocused(this.input);
        this.input.setFocus(true);
        return this.input.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    private void submit() {
        String raw = this.input.getValue() == null ? "" : this.input.getValue().trim();
        this.input.setValue("");
        this.setFocused(this.input);
        this.input.setFocus(true);
        if (raw.isEmpty()) {
            return;
        }
        this.handle(raw);
    }

    private void handle(String raw) {
        String cmd = raw.startsWith("/") ? raw.substring(1).trim() : raw.trim();
        String lower = cmd.toLowerCase(Locale.ROOT);
        if (lower.equals("exit") || lower.equals("quit") || lower.equals("close")) {
            this.onClose();
            return;
        }
        if (lower.equals("egf open") || lower.equals("egf")) {
            // Close the console overlay — EGF's parent is the underlying title/menu screen.
            Minecraft.getInstance().setScreen(new NvFlagPanel(this.parent));
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null) {
            this.parent.render(pose, 0, 0, partialTick);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
