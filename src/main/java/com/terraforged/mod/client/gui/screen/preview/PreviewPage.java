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
 * Layout: yellow control row → green Img → red Inf (legend).
 */
public final class PreviewPage implements Page {
    private static final int INFO_H = 52;

    private final SettingsDraft draft;
    private final Preview preview;
    private int infoX;
    private int infoY;
    private int infoW;

    public PreviewPage(SettingsDraft draft) {
        this.draft = draft;
        this.preview = new Preview((int) draft.seed());
        this.preview.previewSettings().zoom = Preview.zoomSettingForArea(Preview.DEFAULT_AREA);
        this.preview.setShowLegend(false);
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
        // Yellow zone: four controls in one row across the full preview column width.
        int controlsH = btnH + 4;
        int mapSize = Math.min(Preview.SIZE, Math.min(width, Math.max(96, height - controlsH - INFO_H - 4)));
        int mapLeft = left + Math.max(0, (width - mapSize) / 2);

        this.preview.x = mapLeft;
        this.preview.y = top + controlsH;
        this.preview.setWidth(mapSize);
        this.preview.setHeight(mapSize);

        this.infoX = mapLeft;
        this.infoY = this.preview.y + mapSize + 4;
        this.infoW = mapSize;

        int btnW = Math.max(28, (width - gap * 3) / 4);
        int x0 = left;
        int x1 = left + btnW + gap;
        int x2 = left + (btnW + gap) * 2;
        int x3 = left + (btnW + gap) * 3;
        int lastW = Math.max(28, width - (btnW + gap) * 3);

        screen.addRenderableWidget(new Button(x0, top, btnW, btnH, new TranslatableComponent("newterraforged.gui.preview.seed"), b -> {
            this.preview.regenerate();
            this.draft.setSeed(Integer.toUnsignedLong(this.preview.getSeed()));
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(x1, top, btnW, btnH, new TextComponent(shortMode(this.preview.previewSettings().display)), b -> {
            this.preview.previewSettings().display = this.preview.previewSettings().display.next();
            b.setMessage(new TextComponent(shortMode(this.preview.previewSettings().display)));
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(x2, top, btnW, btnH, new TextComponent("Zoom −"), b -> {
            this.preview.previewSettings().zoom = Math.max(1, this.preview.previewSettings().zoom - 8);
            this.refresh();
        }));
        screen.addRenderableWidget(new Button(x3, top, lastW, btnH, new TextComponent("Zoom +"), b -> {
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
        this.draft.setSeed(Integer.toUnsignedLong(this.preview.getSeed()));
    }

    @Override
    public void close() {
        this.preview.close();
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        // Red Inf zone under the green Img.
        if (this.infoW <= 0) {
            return;
        }
        int x0 = this.infoX - 2;
        int y0 = this.infoY - 2;
        int x1 = this.infoX + this.infoW + 2;
        int y1 = this.infoY + INFO_H;
        fill(pose, x0, y0, x1, y1, 0x88000000);
        fill(pose, x0, y0, x1, y0 + 1, 0xFFB05050);
        fill(pose, x0, y1 - 1, x1, y1, 0xFFB05050);
        fill(pose, x0, y0, x0 + 1, y1, 0xFFB05050);
        fill(pose, x1 - 1, y0, x1, y1, 0xFFB05050);

        this.preview.updateHoverLegend(mouseX, mouseY);
        this.preview.renderLegendAt(pose, this.infoX + 4, this.infoY + 4, this.infoW - 8, 0xFFFFFF);
    }

    private static void fill(PoseStack pose, int x0, int y0, int x1, int y1, int color) {
        net.minecraft.client.gui.GuiComponent.fill(pose, x0, y0, x1, y1, color);
    }
}
