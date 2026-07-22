package com.terraforged.mod.client.gui.screen.egf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.platform.forge.TFExperimentalGenerationConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/**
 * Experimental Generation Features menu — more toggles will land here over time.
 */
public final class ExperimentalGenerationFeaturesScreen extends Screen {
    private final Screen parent;

    public ExperimentalGenerationFeaturesScreen(Screen parent) {
        super(new TextComponent("Experimental Generation Features"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 48;
        this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Archipelago", TFExperimentalGenerationConfig.archipelagoEnabled()),
                b -> {
                    boolean next = !TFExperimentalGenerationConfig.archipelagoEnabled();
                    TFExperimentalGenerationConfig.setArchipelago(next);
                    b.setMessage(label("Archipelago", next));
                }
        ));
        y += 28;
        this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Scattered Archipelago", TFExperimentalGenerationConfig.scatteredArchipelagoEnabled()),
                b -> {
                    boolean next = !TFExperimentalGenerationConfig.scatteredArchipelagoEnabled();
                    TFExperimentalGenerationConfig.setScatteredArchipelago(next);
                    b.setMessage(label("Scattered Archipelago", next));
                }
        ));
        this.addRenderableWidget(new Button(cx - 60, this.height - 32, 120, 20, new TextComponent("Done"), b -> this.onClose()));
    }

    private static TextComponent label(String name, boolean on) {
        return new TextComponent(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 12, 0xFFE080);
        drawCenteredString(pose, this.font, "Unstable / unfinished worldgen. Default OFF for releases.", this.width / 2, 28, 0xFFAAAAAA);
        drawCenteredString(pose, this.font, "Saved to config/NewTerraForged/Critical Options/experimental-generation.toml", this.width / 2, this.height - 52, 0xFF888888);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
