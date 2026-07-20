package com.terraforged.mod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.page.StubPage;
import com.terraforged.mod.client.gui.screen.preview.PreviewPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.levelgen.WorldGenSettings;

/**
 * Scaffold of TerraForged 0.2.x Generator Settings ({@code ConfigScreen}).
 * Live climate preview works; TerraSettings sliders / Done→WorldGenSettings apply come next.
 */
public final class ConfigScreen extends Screen {
    private final CreateWorldScreen parent;
    private final SettingsDraft draft;
    private final PreviewPage previewPage;
    private final Page[] pages;
    private int pageIndex;

    public ConfigScreen(CreateWorldScreen parent) {
        super(new TranslatableComponent("newterraforged.gui.config.title"));
        this.parent = parent;
        this.draft = new SettingsDraft(readSeed(parent));
        this.previewPage = new PreviewPage(this.draft);
        this.pages = new Page[]{
                new StubPage("newterraforged.gui.page.presets", "newterraforged.gui.page.presets.body"),
                new StubPage("newterraforged.gui.page.world", "newterraforged.gui.page.world.body"),
                new StubPage("newterraforged.gui.page.climate", "newterraforged.gui.page.climate.body"),
                new StubPage("newterraforged.gui.page.terrain", "newterraforged.gui.page.terrain.body"),
                new StubPage("newterraforged.gui.page.rivers", "newterraforged.gui.page.rivers.body"),
                new StubPage("newterraforged.gui.page.filters", "newterraforged.gui.page.filters.body"),
        };
    }

    public static void open(CreateWorldScreen parent) {
        Minecraft.getInstance().setScreen(new ConfigScreen(parent));
    }

    /** Expose addRenderableWidget to pages in this package tree. */
    public <T extends net.minecraft.client.gui.components.AbstractWidget> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int pad = 12;
        int leftWidth = Math.min(220, this.width / 2 - 24);
        int rightLeft = leftWidth + pad * 2;
        int rightWidth = this.width - rightLeft - pad;
        int contentTop = 32;
        int contentHeight = this.height - 70;

        Page page = this.pages[this.pageIndex];
        page.init(this, pad, contentTop, leftWidth, contentHeight);
        if (this.pageIndex > 0) {
            this.previewPage.init(this, rightLeft, contentTop, rightWidth, contentHeight);
        }

        int cy = this.height - 28;
        int bw = 50;
        int mid = this.width / 2;
        this.addRenderableWidget(new Button(mid - bw * 2 - 4, cy, bw, 20, new TextComponent("<<"), b -> {
            if (this.pageIndex > 0) {
                this.pageIndex--;
                this.init();
            }
        }));
        this.addRenderableWidget(new Button(mid - bw - 2, cy, bw, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
        this.addRenderableWidget(new Button(mid + 2, cy, bw, 20, CommonComponents.GUI_DONE, b -> this.applyAndClose()));
        this.addRenderableWidget(new Button(mid + bw + 4, cy, bw, 20, new TextComponent(">>"), b -> {
            if (this.pageIndex + 1 < this.pages.length) {
                this.pageIndex++;
                this.init();
            }
        }));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        drawString(pose, this.font, this.pages[this.pageIndex].title(), 12, 18, 0xE0E0E0);
        this.pages[this.pageIndex].render(pose, mouseX, mouseY, partialTick);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        for (Page page : this.pages) {
            page.close();
        }
        this.previewPage.close();
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void applyAndClose() {
        for (Page page : this.pages) {
            page.save();
        }
        // Scaffold: sync seed into Create World seed box when present. Full WorldGenSettings apply later.
        writeSeed(this.parent, this.draft.seed());
        this.onClose();
    }

    private static int readSeed(CreateWorldScreen screen) {
        EditBox box = findSeedBox(screen);
        if (box == null || box.getValue().isEmpty()) {
            try {
                WorldGenSettings settings = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
                return (int)settings.seed();
            } catch (Throwable ignored) {
                return -1;
            }
        }
        try {
            return (int)Long.parseLong(box.getValue());
        } catch (NumberFormatException e) {
            return box.getValue().hashCode();
        }
    }

    private static void writeSeed(CreateWorldScreen screen, int seed) {
        EditBox box = findSeedBox(screen);
        if (box != null) {
            box.setValue(String.valueOf(seed));
        }
    }

    private static EditBox findSeedBox(CreateWorldScreen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
                // Seed field is the EditBox under more-options; prefer any numeric-looking empty/long field.
                String value = box.getValue();
                if (value.isEmpty()) {
                    return box;
                }
                try {
                    Long.parseLong(value);
                    return box;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
