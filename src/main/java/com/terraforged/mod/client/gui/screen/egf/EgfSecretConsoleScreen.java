package com.terraforged.mod.client.gui.screen.egf;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/**
 * Secret title-screen console (open with U→I→0→1). Commands like {@code /EGF open}.
 */
public final class EgfSecretConsoleScreen extends Screen {
    private static final int MAX_LOG = 80;
    private final Screen parent;
    private final List<String> log = new ArrayList<>();
    private EditBox input;
    private int scroll;

    public EgfSecretConsoleScreen(Screen parent) {
        super(new TextComponent("NewTerraForged Console"));
        this.parent = parent;
        this.log.add("NewTerraForged secret console");
        this.log.add("Type /EGF open  — Experimental Generation Features");
        this.log.add("Type help      — commands");
        this.log.add("Type exit      — close");
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.input = new EditBox(this.font, 8, this.height - 28, this.width - 16, 20, new TextComponent("cmd"));
        this.input.setMaxLength(256);
        this.input.setEditable(true);
        this.input.setBordered(true);
        this.input.setVisible(true);
        this.input.setCanLoseFocus(true);
        // addRenderableWidget: receives mouse + keyboard + draws the box
        this.addRenderableWidget(this.input);
        this.addRenderableWidget(new Button(this.width - 88, 6, 80, 20, new TextComponent("Close"), b -> this.onClose()));
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
        if (keyCode == 256) { // Escape
            this.onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter / KP Enter
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
        // If somehow unfocused, refocus and accept the char.
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
        this.append("> " + raw);
        this.handle(raw);
    }

    private void handle(String raw) {
        String cmd = raw.startsWith("/") ? raw.substring(1).trim() : raw.trim();
        String lower = cmd.toLowerCase(Locale.ROOT);
        if (lower.equals("exit") || lower.equals("quit") || lower.equals("close")) {
            this.onClose();
            return;
        }
        if (lower.equals("help") || lower.equals("?")) {
            this.append("Commands:");
            this.append("  /EGF open   — Experimental Generation Features menu");
            this.append("  help        — this list");
            this.append("  exit        — close console");
            return;
        }
        if (lower.equals("egf open") || lower.equals("egf")) {
            Minecraft.getInstance().setScreen(new ExperimentalGenerationFeaturesScreen(this));
            return;
        }
        if (lower.startsWith("egf ")) {
            this.append("Unknown EGF subcommand. Try: /EGF open");
            return;
        }
        this.append("Unknown command. Type help");
    }

    private void append(String line) {
        this.log.add(line);
        while (this.log.size() > MAX_LOG) {
            this.log.remove(0);
        }
        this.scroll = Math.max(0, this.log.size() * 10 - (this.height - 56));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, this.log.size() * 10 - (this.height - 56));
        this.scroll = (int) Math.max(0.0, Math.min(maxScroll, this.scroll - delta * 10.0));
        return true;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        fill(pose, 4, 4, this.width - 4, this.height - 36, 0xC0101010);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 10, 0xFFE080);
        int y = 28 - this.scroll;
        for (String line : this.log) {
            if (y > 20 && y < this.height - 40) {
                this.font.draw(pose, line, 10, y, 0xFFDDDDDD);
            }
            y += 10;
        }
        // Widgets (EditBox + Close) render via super
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
