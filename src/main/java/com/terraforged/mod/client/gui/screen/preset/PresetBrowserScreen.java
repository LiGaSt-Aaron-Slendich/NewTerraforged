package com.terraforged.mod.client.gui.screen.preset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.client.gui.screen.preview.Preview;
import com.terraforged.mod.client.gui.screen.preview.PreviewSettings;
import com.terraforged.mod.client.gui.screen.preview.RenderMode;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

/**
 * Layout (from design sketch):
 * <pre>
 *  [Default Presets] [My / Imported]     title
 *  |roll| Names...                       | Img |
 *  |    |                                | Inf |
 *                 [Import] [Cancel]
 * </pre>
 */
public final class PresetBrowserScreen extends Screen {
    private static final int ROLL_W = 8;

    private final ConfigScreen parent;
    private final SettingsDraft draft;
    private final Runnable onImported;
    private PresetBrowserScreen.Tab tab = PresetBrowserScreen.Tab.DEFAULT;
    private PresetList list;
    private Preview preview;
    private final PreviewSettings previewSettings = new PreviewSettings();
    @Nullable
    private PresetEntry selected;
    private String authorText = "";
    private Button importButton;
    private ImportFileIconButton importFileButton;
    private int previewSize;
    private int listLeft;
    private int listWidth;
    private int panelTop;
    private int panelBottom;
    private boolean previewClosed;
    private final AtomicBoolean fileDialogOpen = new AtomicBoolean(false);

    public PresetBrowserScreen(ConfigScreen parent, SettingsDraft draft, Runnable onImported) {
        super(new TranslatableComponent("newterraforged.gui.presets.browser.title"));
        this.parent = parent;
        this.draft = draft;
        this.onImported = onImported;
        this.previewSettings.zoom = Preview.zoomSettingForArea(Preview.DEFAULT_AREA);
        this.previewSettings.display = RenderMode.BIOME_TYPE;
    }

    @Override
    protected void init() {
        this.previewClosed = false;
        // Relative layout so Img+Inf always sit above the bottom buttons.
        int bottomBar = Mth.clamp(this.height / 16, 28, 40);
        int topPad = Mth.clamp(this.height / 18, 24, 36);
        this.panelTop = topPad + Mth.clamp(this.height / 24, 22, 30);
        this.panelBottom = this.height - bottomBar - 4;
        this.listLeft = Mth.clamp(this.width / 64, 8, 16);

        int rightPad = Mth.clamp(this.width / 60, 10, 20);
        int gap = Mth.clamp(this.width / 60, 10, 20);
        int infoH = Mth.clamp(this.height / 16, 36, 48);
        int availH = Math.max(64, this.panelBottom - this.panelTop - infoH - 8);
        int availW = Math.max(96, this.width / 3);
        // Never force a size larger than remaining space (old min=160 caused Cancel overlap).
        this.previewSize = Math.max(64, Math.min(availH, Math.min(availW, this.height * 2 / 5)));

        this.preview = new Preview((int) this.draft.seed());
        this.preview.setShowLegend(false);
        this.preview.previewSettings().zoom = this.previewSettings.zoom;
        this.preview.previewSettings().display = this.previewSettings.display;
        this.preview.x = this.width - this.previewSize - rightPad;
        this.preview.y = this.panelTop;
        this.preview.setWidth(this.previewSize);
        this.preview.setHeight(this.previewSize);
        this.addRenderableWidget(this.preview);

        this.listWidth = Math.max(Mth.clamp(this.width / 4, 160, 320), this.preview.x - gap - this.listLeft);

        this.list = new PresetList(this.listWidth, this.panelTop, this.height - this.panelBottom);
        this.addWidget(this.list);
        this.reloadList();

        // Import-from-file icon at the right edge of the names panel (yellow square on sketch).
        int icon = 18;
        this.importFileButton = this.addRenderableWidget(new ImportFileIconButton(
                this.listLeft + this.listWidth - icon - 3,
                this.panelTop + 1,
                icon,
                icon,
                b -> this.openImportFromFileDialog()));

        int tabY = topPad;
        int tabH = Mth.clamp(this.height / 28, 20, 26);
        int tabGap = 2;
        int tab1W = (this.listWidth - tabGap) / 2;
        int tab2W = this.listWidth - tabGap - tab1W;
        this.addRenderableWidget(new TabButton(this.listLeft, tabY, tab1W, tabH,
                new TranslatableComponent("newterraforged.gui.presets.tab.default"),
                PresetBrowserScreen.Tab.DEFAULT));
        this.addRenderableWidget(new TabButton(this.listLeft + tab1W + tabGap, tabY, tab2W, tabH,
                new TranslatableComponent("newterraforged.gui.presets.tab.user"),
                PresetBrowserScreen.Tab.USER));

        int btnW = Mth.clamp(this.width / 5, 120, 160);
        int cy = this.height - bottomBar + 4;
        this.importButton = this.addRenderableWidget(new Button(this.width / 2 - btnW - 4, cy, btnW, 20,
                new TranslatableComponent("newterraforged.gui.presets.import"), b -> this.importSelected()));
        this.addRenderableWidget(new Button(this.width / 2 + 4, cy, btnW, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
        this.updateImportEnabled();
    }

    private void switchTab(PresetBrowserScreen.Tab tab) {
        if (this.tab == tab) {
            return;
        }
        this.tab = tab;
        this.selected = null;
        this.authorText = "";
        this.reloadList();
        this.updateImportEnabled();
    }

    private void reloadList() {
        List<PresetEntry> entries = this.tab == PresetBrowserScreen.Tab.DEFAULT
                ? PresetLibrary.listDefaultPresets()
                : PresetLibrary.listUserPresets();
        this.list.reload(entries);
    }

    private void onEntrySelected(@Nullable PresetEntry entry) {
        this.selected = entry;
        if (entry == null) {
            this.authorText = "";
            this.updateImportEnabled();
            return;
        }
        this.authorText = entry.displayAuthor();
        try {
            GeneratorSettings settings = entry.loadSettings();
            long previewSeed = settings.seed != -1L ? settings.seed : this.draft.seed();
            SettingsDraft previewDraft = new SettingsDraft(previewSeed);
            previewDraft.loadGeneratorSettings(settings);
            applyShipwreckedPresetHints(entry, previewDraft);
            this.preview.setSeed((int) previewSeed);
            this.preview.update(previewDraft.settings(), DataUtils.toCompactNBT(this.previewSettings));
        } catch (IOException e) {
            this.authorText = PresetFormat.UNKNOWN_CREATOR;
        }
        this.updateImportEnabled();
    }

    private void importSelected() {
        if (this.selected == null) {
            return;
        }
        try {
            this.draft.loadGeneratorSettings(this.selected.loadSettings());
            applyShipwreckedPresetHints(this.selected, this.draft);
            this.onImported.run();
            this.closePreview();
            this.minecraft.setScreen(this.parent);
        } catch (IOException e) {
            this.minecraft.gui.getChat().addMessage(new TextComponent("NewTF: failed to import preset"));
        }
    }

    private static void applyShipwreckedPresetHints(PresetEntry entry, SettingsDraft draft) {
        if (entry == null || draft == null || draft.settings() == null) {
            return;
        }
        String id = entry.id() == null ? "" : entry.id().toLowerCase(java.util.Locale.ROOT);
        String name = entry.displayName() == null ? "" : entry.displayName().toLowerCase(java.util.Locale.ROOT);
        if (!id.contains("shipwrecked") && !name.contains("shipwrecked")) {
            return;
        }
        var world = draft.settings().world;
        world.properties.worldStyle = com.terraforged.engine.settings.WorldSettings.WorldStyle.SHIPWRECKED;
        world.properties.spawnType = com.terraforged.engine.world.continent.SpawnType.WORLD_ORIGIN;
        world.continent.guaranteedContinentsEnabled = false;
        world.continent.continentSkipping = 1.0F;
        world.continent.continentScale = Math.min(world.continent.continentScale, 1000);
        world.islands.archipelago = true;
        world.islands.archipelagoChance = Math.max(world.islands.archipelagoChance, 0.55F);
        world.islands.scatteredArchipelago = true;
        world.islands.scatteredArchipelagoChance = Math.max(world.islands.scatteredArchipelagoChance, 0.65F);
        world.islands.volcanicIslandsChance = Math.max(world.islands.volcanicIslandsChance, 0.35F);
        world.islands.coastalIslandsChance = Math.max(world.islands.coastalIslandsChance, 0.45F);
        draft.refreshNbt();
    }

    private void openImportFromFileDialog() {
        if (!this.fileDialogOpen.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                FileDialog dialog = new FileDialog((Frame) null, "Import NewTF Preset", FileDialog.LOAD);
                dialog.setFilenameFilter((dir, name) -> {
                    String n = name == null ? "" : name.toLowerCase();
                    return n.endsWith(".json") || n.endsWith(".ntpreset");
                });
                dialog.setFile("*.json;*.ntpreset");
                dialog.setVisible(true);
                String dir = dialog.getDirectory();
                String file = dialog.getFile();
                if (dir == null || file == null) {
                    return;
                }
                Path src = Path.of(dir, file);
                Minecraft.getInstance().execute(() -> this.finishImportFromFile(src));
            } catch (Throwable e) {
                Minecraft.getInstance().execute(() ->
                        this.minecraft.gui.getChat().addMessage(new TextComponent("NewTF: file dialog failed")));
            } finally {
                this.fileDialogOpen.set(false);
            }
        }, "ntf-preset-file-import");
        t.setDaemon(true);
        t.start();
    }

    private void finishImportFromFile(Path src) {
        try {
            Path copied = PresetLibrary.importUserPresetFile(src);
            this.tab = PresetBrowserScreen.Tab.USER;
            this.reloadList();
            this.list.selectByFileName(copied.getFileName().toString());
            this.minecraft.gui.getChat().addMessage(new TextComponent("NewTF: imported " + copied.getFileName()));
        } catch (Exception e) {
            this.minecraft.gui.getChat().addMessage(new TextComponent(
                    "NewTF: import failed — " + (e.getMessage() == null ? "invalid preset" : e.getMessage())));
        }
    }

    private void updateImportEnabled() {
        if (this.importButton != null) {
            this.importButton.active = this.selected != null;
        }
    }

    private void closePreview() {
        if (!this.previewClosed && this.preview != null) {
            this.previewClosed = true;
            this.preview.close();
        }
    }

    @Override
    public void onClose() {
        this.closePreview();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.closePreview();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.list.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.importFileButton != null && this.importFileButton.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.importFileButton);
            return true;
        }
        // Prefer list hit-testing so presets are selectable by mouse, not only arrows.
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.list);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.list != null && this.list.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);

        // Names panel under tabs.
        fill(pose, this.listLeft, this.panelTop, this.listLeft + this.listWidth, this.panelBottom, 0x88000000);
        hLine(pose, this.listLeft, this.listLeft + this.listWidth - 1, this.panelTop, 0xFFB0B0B0);
        vLine(pose, this.listLeft, this.panelTop, this.panelBottom - 1, 0xFF707070);
        vLine(pose, this.listLeft + this.listWidth - 1, this.panelTop, this.panelBottom - 1, 0xFF707070);
        hLine(pose, this.listLeft, this.listLeft + this.listWidth - 1, this.panelBottom - 1, 0xFF707070);

        // Always-visible roll track on the left of Names.
        int rollX0 = this.listLeft + 2;
        int rollX1 = rollX0 + ROLL_W;
        fill(pose, rollX0, this.panelTop + 2, rollX1, this.panelBottom - 2, 0xFF101010);
        hLine(pose, rollX0, rollX1 - 1, this.panelTop + 2, 0xFF505050);
        hLine(pose, rollX0, rollX1 - 1, this.panelBottom - 3, 0xFF505050);

        this.list.render(pose, mouseX, mouseY, partialTick);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        // Red Inf zone under green Img: name + author (height relative; always above bottom bar).
        int infoX = this.preview.x;
        int infoY = this.preview.y + this.previewSize + Math.max(4, this.height / 80);
        int infoH = Mth.clamp(this.height / 16, 32, 44);
        int maxInfoBottom = this.height - Mth.clamp(this.height / 16, 28, 40) - 2;
        if (infoY + infoH > maxInfoBottom) {
            infoH = Math.max(24, maxInfoBottom - infoY);
        }
        fill(pose, infoX - 4, infoY - 4, infoX + this.previewSize + 4, infoY + infoH, 0x88000000);
        hLine(pose, infoX - 4, infoX + this.previewSize + 3, infoY - 4, 0xFFB05050);
        hLine(pose, infoX - 4, infoX + this.previewSize + 3, infoY + infoH - 1, 0xFFB05050);
        vLine(pose, infoX - 4, infoY - 4, infoY + infoH - 1, 0xFFB05050);
        vLine(pose, infoX + this.previewSize + 3, infoY - 4, infoY + infoH - 1, 0xFFB05050);
        if (this.selected != null && infoH >= 28) {
            String name = this.font.plainSubstrByWidth(this.selected.displayName(), this.previewSize);
            drawString(pose, this.font, name, infoX, infoY, 0xFFFFFF);
            drawString(pose, this.font, new TranslatableComponent("newterraforged.gui.presets.author", this.authorText), infoX, infoY + 14, 0xC0C0C0);
        }

        super.render(pose, mouseX, mouseY, partialTick);
        if (this.importFileButton != null && this.importFileButton.isHoveredOrFocused()) {
            this.renderTooltip(pose, new TranslatableComponent("newterraforged.gui.presets.import_file"), mouseX, mouseY);
        }
    }

    private enum Tab {
        DEFAULT,
        USER
    }

    /** Folder/arrow icon button — no text label. */
    private static final class ImportFileIconButton extends Button {
        ImportFileIconButton(int x, int y, int w, int h, OnPress onPress) {
            super(x, y, w, h, TextComponent.EMPTY, onPress);
        }

        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            int bg = this.isHoveredOrFocused() ? 0xFF5A5A5A : 0xFF3A3A3A;
            fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, bg);
            hLine(pose, this.x, this.x + this.width - 1, this.y, 0xFFC0C0C0);
            hLine(pose, this.x, this.x + this.width - 1, this.y + this.height - 1, 0xFF707070);
            vLine(pose, this.x, this.y, this.y + this.height - 1, 0xFFC0C0C0);
            vLine(pose, this.x + this.width - 1, this.y, this.y + this.height - 1, 0xFF707070);
            // Procedural folder + down arrow (works without texture assets).
            int cx = this.x + this.width / 2;
            int cy = this.y + this.height / 2;
            int folder = this.active ? 0xFFE8D090 : 0xFF888888;
            fill(pose, cx - 5, cy - 1, cx + 5, cy + 5, folder);
            fill(pose, cx - 5, cy - 3, cx - 1, cy - 1, folder);
            int arrow = this.active ? 0xFFFFFFFF : 0xFFAAAAAA;
            fill(pose, cx - 1, cy - 6, cx + 1, cy + 1, arrow);
            fill(pose, cx - 3, cy - 1, cx + 3, cy + 1, arrow);
            fill(pose, cx - 2, cy + 1, cx + 2, cy + 2, arrow);
            fill(pose, cx - 1, cy + 2, cx + 1, cy + 3, arrow);
        }
    }

    /** Folder/file-tab look: selected flush with panel, unselected recessed. */
    private final class TabButton extends AbstractWidget {
        private final PresetBrowserScreen.Tab target;

        TabButton(int x, int y, int width, int height, Component message, PresetBrowserScreen.Tab target) {
            super(x, y, width, height, message);
            this.target = target;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            PresetBrowserScreen.this.switchTab(this.target);
        }

        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            boolean selected = PresetBrowserScreen.this.tab == this.target;
            int y0 = selected ? this.y : this.y + 4;
            int y1 = this.y + this.height;
            // Trapezoid-ish tab body
            int bg = selected ? 0xFF454545 : (this.isHoveredOrFocused() ? 0xFF303030 : 0xFF222222);
            fill(pose, this.x, y0, this.x + this.width, y1, bg);
            // Folded top corners (2px steps)
            if (!selected) {
                fill(pose, this.x, y0, this.x + 3, y0 + 3, 0xFF151515);
                fill(pose, this.x + this.width - 3, y0, this.x + this.width, y0 + 3, 0xFF151515);
            }
            int edge = selected ? 0xFFE0E0E0 : 0xFF808080;
            hLine(pose, this.x + (selected ? 0 : 2), this.x + this.width - 1 - (selected ? 0 : 2), y0, edge);
            vLine(pose, this.x, y0, y1 - 1, edge);
            vLine(pose, this.x + this.width - 1, y0, y1 - 1, edge);
            if (selected) {
                // Connect to panel: erase bottom edge
                hLine(pose, this.x + 1, this.x + this.width - 2, y1 - 1, 0xFF454545);
            } else {
                hLine(pose, this.x, this.x + this.width - 1, y1 - 1, 0xFFB0B0B0);
            }
            Font font = Minecraft.getInstance().font;
            int textColor = selected ? 0xFFFFFF : 0xA8A8A8;
            String label = font.plainSubstrByWidth(this.getMessage().getString(), this.width - 8);
            drawCenteredString(pose, font, label, this.x + this.width / 2, y0 + (y1 - y0 - 8) / 2, textColor);
        }

        @Override
        public void updateNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    private final class PresetList extends ObjectSelectionList<PresetList.Entry> {
        PresetList(int width, int top, int bottomReserve) {
            super(PresetBrowserScreen.this.minecraft, width, PresetBrowserScreen.this.height, top, PresetBrowserScreen.this.height - bottomReserve, 20);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(PresetBrowserScreen.this.listLeft);
        }

        @Override
        public int getRowWidth() {
            // Leave room for left roll + padding.
            return Math.max(40, this.width - ROLL_W - 16);
        }

        @Override
        protected int getScrollbarPosition() {
            // Roll on the left of Names (design sketch).
            return this.x0 + 2;
        }

        @Override
        public int getRowLeft() {
            return this.x0 + ROLL_W + 8;
        }

        @Override
        protected boolean isFocused() {
            return PresetBrowserScreen.this.getFocused() == this;
        }

        @Override
        public void setSelected(@Nullable PresetList.Entry entry) {
            super.setSelected(entry);
            PresetBrowserScreen.this.onEntrySelected(entry == null ? null : entry.entry);
        }

        void reload(List<PresetEntry> entries) {
            this.clearEntries();
            for (PresetEntry entry : entries) {
                this.addEntry(new Entry(entry));
            }
            if (!entries.isEmpty()) {
                this.setSelected(this.children().get(0));
            } else {
                PresetBrowserScreen.this.onEntrySelected(null);
            }
        }

        void selectByFileName(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return;
            }
            String bare = fileName;
            int dot = fileName.lastIndexOf('.');
            if (dot > 0) {
                bare = fileName.substring(0, dot);
            }
            for (Entry entry : this.children()) {
                String id = entry.entry.id();
                String name = entry.entry.displayName();
                if (id.endsWith(fileName) || id.endsWith("/" + fileName) || id.endsWith("\\" + fileName)
                        || name.equalsIgnoreCase(bare)) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        private final class Entry extends ObjectSelectionList.Entry<PresetList.Entry> {
            private final PresetEntry entry;

            private Entry(PresetEntry entry) {
                this.entry = entry;
            }

            @Override
            public void render(PoseStack pose, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                Font font = PresetBrowserScreen.this.font;
                boolean selected = PresetList.this.getSelected() == this;
                if (selected) {
                    fill(pose, left - 2, top, left + width, top + height - 1, 0x66FFFFFF);
                } else if (hovered) {
                    fill(pose, left - 2, top, left + width, top + height - 1, 0x33FFFFFF);
                }
                int maxText = Math.max(8, width - 24);
                String label = font.plainSubstrByWidth(this.entry.displayName(), maxText);
                int color = selected || hovered ? 0xFFFFA0 : 0xFFFFFF;
                drawString(pose, font, label, left + 2, top + 6, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    PresetList.this.setSelected(this);
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return new TextComponent(this.entry.displayName());
            }
        }
    }
}
