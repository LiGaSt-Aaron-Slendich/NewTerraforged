package com.terraforged.mod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.page.PresetsPage;
import com.terraforged.mod.client.gui.screen.page.ScrollPage;
import com.terraforged.mod.client.gui.screen.page.SettingsSectionPage;
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
 * TerraForged-style Generator Settings screen (1.18.2 official mappings).
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
        Runnable refresh = this.previewPage::refresh;
        this.pages = new Page[]{
                new PresetsPage(this.draft, refresh),
                new SettingsSectionPage("newterraforged.gui.page.world", this.draft, "world", () -> this.draft.settings().world, refresh),
                new SettingsSectionPage("newterraforged.gui.page.climate", this.draft, "climate", () -> this.draft.settings().climate, refresh),
                new SettingsSectionPage("newterraforged.gui.page.terrain", this.draft, "terrain", () -> this.draft.settings().terrain, refresh),
                new SettingsSectionPage("newterraforged.gui.page.rivers", this.draft, "rivers", () -> this.draft.settings().rivers, refresh),
                new SettingsSectionPage("newterraforged.gui.page.filters", this.draft, "filters", () -> this.draft.settings().filters, refresh),
        };
    }

    public static void open(CreateWorldScreen parent) {
        Minecraft.getInstance().setScreen(new ConfigScreen(parent));
    }

    public SettingsDraft draft() {
        return this.draft;
    }

    /** Re-run {@link #init()} after draft resets (pages cannot call protected init). */
    public void reloadPages() {
        this.init();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Page page = this.pages[this.pageIndex];
        if (page instanceof ScrollPage scrollPage && scrollPage.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        GeneratorSettingsApplier.apply(this.parent, this.draft);
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

    private static EditBox findSeedBox(CreateWorldScreen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
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
