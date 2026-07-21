package com.terraforged.mod.client.gui.screen.preset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.client.gui.screen.preview.Preview;
import com.terraforged.mod.client.gui.screen.preview.PreviewSettings;
import com.terraforged.mod.client.gui.screen.preview.RenderMode;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.IOException;
import java.util.List;
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
    private int previewSize;
    private int listLeft;
    private int listWidth;
    private int panelTop;
    private int panelBottom;
    private boolean previewClosed;

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
        this.panelTop = 54;
        this.panelBottom = this.height - 36;
        this.listLeft = 12;
        this.listWidth = Mth.clamp(this.width / 3, 190, 280);

        // Img on the right — as large as space allows.
        int rightPad = 16;
        int imgMax = this.panelBottom - this.panelTop - 40;
        int imgByWidth = this.width - (this.listLeft + this.listWidth + 24) - rightPad;
        this.previewSize = Mth.clamp(Math.min(imgMax, imgByWidth), 120, 220);

        this.preview = new Preview((int) this.draft.seed());
        this.preview.setShowLegend(false);
        this.preview.previewSettings().zoom = this.previewSettings.zoom;
        this.preview.previewSettings().display = this.previewSettings.display;
        this.preview.x = this.width - this.previewSize - rightPad;
        this.preview.y = this.panelTop;
        this.preview.setWidth(this.previewSize);
        this.preview.setHeight(this.previewSize);
        this.addRenderableWidget(this.preview);

        this.list = new PresetList(this.listWidth, this.panelTop, this.height - this.panelBottom);
        this.addWidget(this.list);
        this.reloadList();

        int tabY = 28;
        int tabH = 24;
        int tab1W = 120;
        int tab2W = 150;
        this.addRenderableWidget(new TabButton(this.listLeft, tabY, tab1W, tabH,
                new TranslatableComponent("newterraforged.gui.presets.tab.default"),
                PresetBrowserScreen.Tab.DEFAULT));
        this.addRenderableWidget(new TabButton(this.listLeft + tab1W + 2, tabY, tab2W, tabH,
                new TranslatableComponent("newterraforged.gui.presets.tab.user"),
                PresetBrowserScreen.Tab.USER));

        int cy = this.height - 28;
        this.importButton = this.addRenderableWidget(new Button(this.width / 2 - 155, cy, 150, 20,
                new TranslatableComponent("newterraforged.gui.presets.import"), b -> this.importSelected()));
        this.addRenderableWidget(new Button(this.width / 2 + 5, cy, 150, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
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
            SettingsDraft previewDraft = new SettingsDraft(this.draft.seed());
            previewDraft.loadGeneratorSettings(settings);
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
            this.onImported.run();
            this.closePreview();
            this.minecraft.setScreen(this.parent);
        } catch (IOException e) {
            this.minecraft.gui.getChat().addMessage(new TextComponent("NewTF: failed to import preset"));
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

        // Inf under Img.
        if (this.selected != null) {
            int infoX = this.preview.x;
            int infoY = this.preview.y + this.previewSize + 8;
            fill(pose, infoX - 4, infoY - 4, infoX + this.previewSize + 4, infoY + 34, 0x88000000);
            String name = this.font.plainSubstrByWidth(this.selected.displayName(), this.previewSize);
            drawString(pose, this.font, name, infoX, infoY, 0xFFFFFF);
            drawString(pose, this.font, new TranslatableComponent("newterraforged.gui.presets.author", this.authorText), infoX, infoY + 14, 0xC0C0C0);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private enum Tab {
        DEFAULT,
        USER
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
                int maxText = Math.max(8, width - 6);
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
