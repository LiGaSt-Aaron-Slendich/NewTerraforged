package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.util.serialization.DataUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Port of TerraForged 0.2.x {@code PreviewPage}, adapted to NewTF {@link ConfigScreen} / {@link SettingsDraft}.
 */
public final class PreviewPage implements Page {
    private final SettingsDraft draft;
    private final Preview preview;

    public PreviewPage(SettingsDraft draft) {
        this.draft = draft;
        this.preview = new Preview(draft.seed());
        // Start more zoomed-in so MULTI continents read as landmasses, not island soup.
        this.preview.previewSettings().zoom = 78;
    }

    public Preview getPreviewWidget() {
        return this.preview;
    }

    public int getSeed() {
        return this.preview.getSeed();
    }

    @Override
    public Component title() {
        return new TranslatableComponent("newterraforged.gui.preview");
    }

    @Override
    public void init(ConfigScreen screen, int left, int top, int width, int height) {
        int gap = 2;
        int btnH = 20;
        int row2Y = top + btnH + gap;
        int controlsH = btnH * 2 + gap + 4;
        int legendPad = 36;
        int mapSize = Math.min(Preview.SIZE, Math.min(width, Math.max(96, height - controlsH - legendPad)));
        int mapLeft = left + Math.max(0, (width - mapSize) / 2);

        this.preview.x = mapLeft;
        this.preview.y = top + controlsH;
        this.preview.setWidth(mapSize);
        this.preview.setHeight(mapSize);

        // Two rows fully inside [left, left+width] — never overflow the right edge.
        int half = Math.max(40, (width - gap) / 2);
        int zoomHalf = Math.max(28, (width - gap) / 2);

        screen.addRenderableWidget(new Button(left, top, half, btnH, new TranslatableComponent("newterraforged.gui.preview.seed"), b -> {
            this.preview.regenerate();
            this.draft.setSeed(this.preview.getSeed());
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(left + half + gap, top, width - half - gap, btnH, new TextComponent(shortMode(this.preview.previewSettings().display)), b -> {
            this.preview.previewSettings().display = this.preview.previewSettings().display.next();
            b.setMessage(new TextComponent(shortMode(this.preview.previewSettings().display)));
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(left, row2Y, zoomHalf, btnH, new TextComponent("Zoom −"), b -> {
            this.preview.previewSettings().zoom = Math.max(1, this.preview.previewSettings().zoom - 8);
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(left + zoomHalf + gap, row2Y, width - zoomHalf - gap, btnH, new TextComponent("Zoom +"), b -> {
            this.preview.previewSettings().zoom = Math.min(100, this.preview.previewSettings().zoom + 8);
            this.refresh();
        }));

        screen.addRenderableWidget(this.preview);
        this.refresh();
    }

    private static String shortMode(RenderMode mode) {
        return switch (mode) {
            case BIOME_TYPE -> "Biomes";
            case TRANSITION_POINTS -> "Edges";
            case TEMPERATURE -> "Temp";
            case MOISTURE -> "Wet";
            case BIOME -> "BiomeId";
            case MACRO_NOISE -> "Macro";
            case TERRAIN_REGION -> "Terrain";
        };
    }

    public void refresh() {
        this.draft.applyToSettings();
        CompoundTag previewNbt = DataUtils.toCompactNBT(this.preview.previewSettings());
        this.preview.update(this.draft.settings(), previewNbt);
    }

    @Override
    public void save() {
        this.draft.applyToSettings();
        this.draft.setSeed(this.preview.getSeed());
    }

    @Override
    public void close() {
        this.preview.close();
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
    }
}
