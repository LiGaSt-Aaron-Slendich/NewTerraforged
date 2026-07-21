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

public final class PresetBrowserScreen extends Screen {
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
    private boolean previewClosed;

    public PresetBrowserScreen(ConfigScreen parent, SettingsDraft draft, Runnable onImported) {
        super(new TranslatableComponent("newterraforged.gui.presets.browser.title"));
        this.parent = parent;
        this.draft = draft;
        this.onImported = onImported;
        this.previewSettings.zoom = 78;
        this.previewSettings.display = RenderMode.BIOME_TYPE;
    }

    @Override
    protected void init() {
        this.previewClosed = false;
        int top = 56;
        int bottom = 36;
        this.listLeft = 12;
        this.listWidth = Mth.clamp(this.width / 3, 180, 260);
        this.previewSize = Math.min(128, Math.max(96, this.height - top - bottom - 48));

        this.preview = new Preview((int) this.draft.seed());
        this.preview.x = this.width - this.previewSize - 16;
        this.preview.y = top;
        this.preview.setWidth(this.previewSize);
        this.preview.setHeight(this.previewSize);
        this.addRenderableWidget(this.preview);

        this.list = new PresetList(this.listWidth, top, bottom);
        this.addWidget(this.list);
        this.reloadList();

        int tabY = 28;
        int tabH = 22;
        int tab1W = 118;
        int tab2W = 148;
        this.addRenderableWidget(new TabButton(this.listLeft, tabY, tab1W, tabH,
                new TranslatableComponent("newterraforged.gui.presets.tab.default"),
                PresetBrowserScreen.Tab.DEFAULT));
        this.addRenderableWidget(new TabButton(this.listLeft + tab1W, tabY, tab2W, tabH,
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
            // Preview-only: do not mutate the live draft until Import is pressed.
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
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        // Content panel under tabs (file-explorer style).
        int panelTop = 50;
        fill(pose, this.listLeft, panelTop, this.listLeft + this.listWidth, this.height - 34, 0x66000000);
        hLine(pose, this.listLeft, this.listLeft + this.listWidth - 1, panelTop, 0xFF8A8A8A);

        this.list.render(pose, mouseX, mouseY, partialTick);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.selected != null) {
            int infoX = this.preview.x;
            int infoY = this.preview.y + this.previewSize + 6;
            String name = this.font.plainSubstrByWidth(this.selected.displayName(), this.previewSize);
            drawString(pose, this.font, name, infoX, infoY, 0xFFFFFF);
            drawString(pose, this.font, new TranslatableComponent("newterraforged.gui.presets.author", this.authorText), infoX, infoY + 12, 0xC0C0C0);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private enum Tab {
        DEFAULT,
        USER
    }

    /** File-explorer style tab: selected sits flush with the panel edge. */
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
            int bg = selected ? 0xFF3A3A3A : (this.isHoveredOrFocused() ? 0xFF2A2A2A : 0xFF1E1E1E);
            int y0 = selected ? this.y : this.y + 3;
            int y1 = this.y + this.height;
            fill(pose, this.x, y0, this.x + this.width, y1, bg);
            // Top / side borders
            int edge = selected ? 0xFFC0C0C0 : 0xFF707070;
            hLine(pose, this.x, this.x + this.width - 1, y0, edge);
            vLine(pose, this.x, y0, y1 - 1, edge);
            vLine(pose, this.x + this.width - 1, y0, y1 - 1, edge);
            if (!selected) {
                hLine(pose, this.x, this.x + this.width - 1, y1 - 1, 0xFF8A8A8A);
            }
            Font font = Minecraft.getInstance().font;
            int textColor = selected ? 0xFFFFFF : 0xA0A0A0;
            drawCenteredString(pose, font, this.getMessage(), this.x + this.width / 2, y0 + (y1 - y0 - 8) / 2, textColor);
        }

        @Override
        public void updateNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    private final class PresetList extends ObjectSelectionList<PresetList.Entry> {
        PresetList(int width, int top, int bottom) {
            super(PresetBrowserScreen.this.minecraft, width, PresetBrowserScreen.this.height, top, PresetBrowserScreen.this.height - bottom, 20);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(PresetBrowserScreen.this.listLeft);
        }

        @Override
        public int getRowWidth() {
            return Math.max(40, this.width - 14);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
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
                int maxText = Math.max(8, width - 8);
                String label = font.plainSubstrByWidth(this.entry.displayName(), maxText);
                int color = hovered || PresetList.this.getSelected() == this ? 0xFFFFA0 : 0xFFFFFF;
                drawString(pose, font, label, left + 4, top + 6, color);
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
