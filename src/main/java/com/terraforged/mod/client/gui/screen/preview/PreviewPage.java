package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.util.serialization.DataUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

/**
 * Layout: yellow control row → green Img → red Inf (legend). Sizes are relative to the column.
 */
public final class PreviewPage implements Page {
    private final SettingsDraft draft;
    private final Preview preview;
    private final List<AbstractWidget> tipButtons = new ArrayList<>();
    private int infoX;
    private int infoY;
    private int infoW;
    private int infoH;

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
        this.tipButtons.clear();
        int gap = Math.max(1, width / 120);
        int btnH = Mth.clamp(height / 22, 16, 20);
        int controlsH = btnH + Math.max(2, height / 80);
        this.infoH = Mth.clamp(height / 12, 40, 56);
        int mapPad = Math.max(2, height / 100);
        int mapSize = Math.min(Preview.SIZE, Math.min(width, Math.max(64, height - controlsH - this.infoH - mapPad)));
        int mapLeft = left + Math.max(0, (width - mapSize) / 2);

        this.preview.x = mapLeft;
        this.preview.y = top + controlsH;
        this.preview.setWidth(mapSize);
        this.preview.setHeight(mapSize);

        this.infoX = mapLeft;
        this.infoY = this.preview.y + mapSize + mapPad;
        this.infoW = mapSize;

        int btnW = Math.max(20, (width - gap * 4) / 5);
        int x0 = left;
        int x1 = left + btnW + gap;
        int x2 = left + (btnW + gap) * 2;
        int x3 = left + (btnW + gap) * 3;
        int x4 = left + (btnW + gap) * 4;
        int lastW = Math.max(20, width - (btnW + gap) * 4);

        this.tipButtons.add(screen.addRenderableWidget(new HoverTipButton(
                x0, top, btnW, btnH,
                new TranslatableComponent("newterraforged.gui.preview.seed"),
                b -> {
                    this.preview.regenerate();
                    this.draft.setSeed(Integer.toUnsignedLong(this.preview.getSeed()));
                    this.refresh();
                },
                () -> new TranslatableComponent("newterraforged.gui.preview.seed"))));

        this.tipButtons.add(screen.addRenderableWidget(new HoverTipButton(
                x1, top, btnW, btnH,
                new TextComponent(shortMode(this.preview.previewSettings().display)),
                b -> {
                    this.preview.previewSettings().display = this.preview.previewSettings().display.next();
                    b.setMessage(new TextComponent(shortMode(this.preview.previewSettings().display)));
                    this.refresh();
                },
                () -> new TextComponent(fullMode(this.preview.previewSettings().display)))));

        this.tipButtons.add(screen.addRenderableWidget(new HoverTipButton(
                x2, top, btnW, btnH,
                new TextComponent(shortFilter(this.preview.previewSettings().terrainFilter)),
                b -> {
                    this.preview.previewSettings().terrainFilter = nextTerrainFilter(this.preview.previewSettings().terrainFilter);
                    b.setMessage(new TextComponent(shortFilter(this.preview.previewSettings().terrainFilter)));
                    this.refresh();
                },
                () -> new TextComponent(fullFilter(this.preview.previewSettings().terrainFilter)))));

        this.tipButtons.add(screen.addRenderableWidget(new HoverTipButton(
                x3, top, btnW, btnH, new TextComponent("Zoom -"),
                b -> {
                    this.preview.previewSettings().zoom = Math.max(1, this.preview.previewSettings().zoom - 8);
                    this.refresh();
                },
                () -> new TextComponent("Zoom out"))));

        this.tipButtons.add(screen.addRenderableWidget(new HoverTipButton(
                x4, top, lastW, btnH, new TextComponent("Zoom +"),
                b -> {
                    this.preview.previewSettings().zoom = Math.min(100, this.preview.previewSettings().zoom + 8);
                    this.refresh();
                },
                () -> new TextComponent("Zoom in"))));

        screen.addRenderableWidget(this.preview);
        this.refresh();
    }

    private static final String[] TERRAIN_FILTERS = {
            "",
            "hills",
            "plateau",
            "mountains",
            "flats",
            "volcano",
            "volcano_pipe",
            "laguna",
            "dolomites"
    };

    private static String nextTerrainFilter(String current) {
        String cur = current == null ? "" : current;
        for (int i = 0; i < TERRAIN_FILTERS.length; i++) {
            if (TERRAIN_FILTERS[i].equalsIgnoreCase(cur)) {
                return TERRAIN_FILTERS[(i + 1) % TERRAIN_FILTERS.length];
            }
        }
        return TERRAIN_FILTERS[0];
    }

    private static String shortFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "Find:Off";
        }
        String f = filter.length() > 8 ? filter.substring(0, 8) : filter;
        return "F:" + f;
    }

    private static String fullFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "Find: Off";
        }
        return "Find: " + filter;
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
            case HEIGHT -> "Height";
        };
    }

    private static String fullMode(RenderMode mode) {
        return switch (mode) {
            case BIOME_TYPE -> "Display: Biomes";
            case TRANSITION_POINTS -> "Display: Transition edges";
            case TEMPERATURE -> "Display: Temperature";
            case MOISTURE -> "Display: Moisture";
            case BIOME -> "Display: Biome id";
            case MACRO_NOISE -> "Display: Macro noise";
            case TERRAIN_REGION -> "Display: Terrain region";
            case HEIGHT -> "Display: Height (land + seafloor)";
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
        if (this.infoW <= 0 || this.infoH <= 0) {
            return;
        }
        int x0 = this.infoX - 2;
        int y0 = this.infoY - 2;
        int x1 = this.infoX + this.infoW + 2;
        int y1 = this.infoY + this.infoH;
        fill(pose, x0, y0, x1, y1, 0x88000000);
        fill(pose, x0, y0, x1, y0 + 1, 0xFFB05050);
        fill(pose, x0, y1 - 1, x1, y1, 0xFFB05050);
        fill(pose, x0, y0, x0 + 1, y1, 0xFFB05050);
        fill(pose, x1 - 1, y0, x1, y1, 0xFFB05050);

        this.preview.updateHoverLegend(mouseX, mouseY);
        this.preview.renderLegendAt(pose, this.infoX + 4, this.infoY + 4, this.infoW - 8, 0xFFFFFF);

        for (AbstractWidget widget : this.tipButtons) {
            if (widget.isHoveredOrFocused()) {
                widget.renderToolTip(pose, mouseX, mouseY);
            }
        }
    }

    private static void fill(PoseStack pose, int x0, int y0, int x1, int y1, int color) {
        net.minecraft.client.gui.GuiComponent.fill(pose, x0, y0, x1, y1, color);
    }
}
