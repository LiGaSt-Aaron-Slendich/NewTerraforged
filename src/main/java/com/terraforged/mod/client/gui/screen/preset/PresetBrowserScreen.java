package com.terraforged.mod.client.gui.screen.preset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.gui.screen.SettingsDraft;
import com.terraforged.mod.client.gui.screen.preview.Preview;
import com.terraforged.mod.client.gui.screen.preview.PreviewSettings;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

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

    public PresetBrowserScreen(ConfigScreen parent, SettingsDraft draft, Runnable onImported) {
        super(new TranslatableComponent("newterraforged.gui.presets.browser.title"));
        this.parent = parent;
        this.draft = draft;
        this.onImported = onImported;
        this.previewSettings.zoom = 78;
        this.previewSettings.display = com.terraforged.mod.client.gui.screen.preview.RenderMode.BIOME_TYPE;
    }

    @Override
    protected void init() {
        int top = 52;
        int bottom = 32;
        int listWidth = Math.max(160, this.width / 3);
        int previewSize = Math.min(128, Math.max(96, this.height - top - bottom - 40));
        this.previewSize = previewSize;

        this.preview = new Preview((int) this.draft.seed());
        this.preview.x = this.width - previewSize - 16;
        this.preview.y = top;
        this.preview.setWidth(previewSize);
        this.preview.setHeight(previewSize);
        this.addRenderableWidget(this.preview);

        this.list = new PresetList(listWidth, top, bottom);
        this.addWidget(this.list);
        this.reloadList();

        int tabY = 28;
        int tabW = 120;
        int tabX = 8;
        this.addRenderableWidget(new Button(tabX, tabY, tabW, 20, new TranslatableComponent("newterraforged.gui.presets.tab.default"), b -> this.switchTab(PresetBrowserScreen.Tab.DEFAULT)));
        this.addRenderableWidget(new Button(tabX + tabW + 4, tabY, tabW + 20, 20, new TranslatableComponent("newterraforged.gui.presets.tab.user"), b -> this.switchTab(PresetBrowserScreen.Tab.USER)));

        int cy = this.height - 28;
        this.importButton = this.addRenderableWidget(new Button(this.width / 2 - 155, cy, 150, 20, new TranslatableComponent("newterraforged.gui.presets.import"), b -> this.importSelected()));
        this.addRenderableWidget(new Button(this.width / 2 + 5, cy, 150, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
        this.updateImportEnabled();
    }

    private void switchTab(PresetBrowserScreen.Tab tab) {
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
            this.draft.loadGeneratorSettings(settings);
            this.preview.update(this.draft.settings(), DataUtils.toCompactNBT(this.previewSettings));
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

    @Override
    public void onClose() {
        if (this.preview != null) {
            this.preview.close();
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        this.list.render(pose, mouseX, mouseY, partialTick);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.selected != null) {
            int infoX = this.preview.x;
            int infoY = this.preview.y + this.previewSize + 6;
            drawString(pose, this.font, this.selected.displayName(), infoX, infoY, 0xFFFFFF);
            drawString(pose, this.font, new TranslatableComponent("newterraforged.gui.presets.author", this.authorText), infoX, infoY + 12, 0xC0C0C0);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        if (this.preview != null) {
            this.preview.close();
        }
    }

    private enum Tab {
        DEFAULT,
        USER
    }

    private final class PresetList extends ObjectSelectionList<PresetList.Entry> {
        PresetList(int width, int top, int bottom) {
            super(PresetBrowserScreen.this.minecraft, width, PresetBrowserScreen.this.height, top, PresetBrowserScreen.this.height - bottom, 18);
            this.setLeftPos(8);
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
                drawString(pose, PresetBrowserScreen.this.font, this.entry.displayName(), left + 4, top + 4, hovered ? 0xFFFFA0 : 0xFFFFFF);
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
