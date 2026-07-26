package com.terraforged.mod.client.gui.screen;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.client.gui.Page;
import com.terraforged.mod.client.gui.screen.page.PresetsPage;
import com.terraforged.mod.client.gui.screen.page.ScrollPage;
import com.terraforged.mod.client.gui.screen.page.SettingsSectionPage;
import com.terraforged.mod.client.gui.screen.preview.PreviewPage;
import com.terraforged.mod.client.screen.ScreenUtil;
import com.terraforged.mod.worldgen.settings.ContinentShapeWiring;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import javax.annotation.Nullable;

/**
 * TerraForged-style Generator Settings screen (1.18.2 official mappings).
 */
public final class ConfigScreen extends Screen {
    private static final int BOTTOM_BAR = 28;
    private static final int BOTTOM_GAP = 10;

    private final CreateWorldScreen parent;
    private final SettingsDraft draft;
    private final PreviewPage previewPage;
    private final Page[] pages;
    private final boolean shipwreckedMode;
    private int pageIndex;

    public ConfigScreen(CreateWorldScreen parent) {
        this(parent, null);
    }

    /**
     * @param existingSettings Pass the current {@link WorldGenSettings} so we restore any
     *                         previously applied Customize settings rather than starting fresh.
     */
    public ConfigScreen(CreateWorldScreen parent, @Nullable WorldGenSettings existingSettings) {
        super(Component.translatable(ScreenUtil.isShipwreckedWorldType(parent)
                ? "newterraforged.gui.config.title.shipwrecked"
                : "newterraforged.gui.config.title"));
        this.parent = parent;
        long seed = readSeed(parent);
        this.draft = SettingsDraft.fromWorldSettings(seed, existingSettings != null
                ? existingSettings
                : safeCurrentSettings(parent));
        this.shipwreckedMode = ScreenUtil.isShipwreckedWorldType(parent)
                || this.draft.settings().world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED;
        if (this.shipwreckedMode) {
            this.draft.settings().world.properties.worldStyle = WorldSettings.WorldStyle.SHIPWRECKED;
            this.draft.settings().world.properties.spawnType = com.terraforged.engine.world.continent.SpawnType.WORLD_ORIGIN;
            ContinentShapeWiring.bakeIslandsIntoEngine(this.draft.settings());
            this.draft.refreshNbt();
        }
        this.previewPage = new PreviewPage(this.draft);
        Runnable refresh = this.previewPage::refresh;
        // Hide nested oceanLandscape here — it has its own page when EGF is on.
        // Guaranteed Continents knobs stay on World but are Feature Blocked until EGF Untested is on.
        Set<String> worldSkip = this.shipwreckedMode
                ? Set.of("continent", "worldStyle", "oceanLandscape")
                : Set.of("worldStyle", "oceanLandscape");
        java.util.ArrayList<Page> pageList = new java.util.ArrayList<>();
        pageList.add(new PresetsPage(this.draft, refresh));
        pageList.add(new SettingsSectionPage(
                this.shipwreckedMode
                        ? "newterraforged.gui.page.world.shipwrecked"
                        : "newterraforged.gui.page.world",
                this.draft,
                "world",
                () -> this.draft.settings().world,
                refresh,
                worldSkip));
        // Separate page — only navigable when EGF Ocean Landscape is ON.
        pageList.add(new SettingsSectionPage(
                "newterraforged.gui.page.ocean_landscape",
                this.draft,
                "world",
                "oceanLandscape",
                () -> this.draft.settings().world.oceanLandscape,
                refresh));
        pageList.add(new SettingsSectionPage("newterraforged.gui.page.climate", this.draft, "climate", () -> this.draft.settings().climate, refresh));
        pageList.add(new SettingsSectionPage("newterraforged.gui.page.terrain", this.draft, "terrain", () -> this.draft.settings().terrain, refresh));
        pageList.add(new SettingsSectionPage("newterraforged.gui.page.rivers", this.draft, "rivers", () -> this.draft.settings().rivers, refresh));
        pageList.add(new SettingsSectionPage("newterraforged.gui.page.filters", this.draft, "filters", () -> this.draft.settings().filters, refresh));
        this.pages = pageList.toArray(Page[]::new);
    }

    public boolean shipwreckedMode() {
        return this.shipwreckedMode;
    }

    public static void open(CreateWorldScreen parent) {
        Minecraft.getInstance().setScreen(new ConfigScreen(parent, safeCurrentSettings(parent)));
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
        int pad = 8;
        int bottomReserve = BOTTOM_BAR + BOTTOM_GAP;
        int contentTop = 28;
        int contentHeight = Math.max(80, this.height - contentTop - bottomReserve);
        // Preview control row sits slightly above the settings column top.
        int previewTop = Math.max(pad + 2, contentTop - 10);

        // Left ~38% of width, right preview gets the rest — scales with window size.
        int leftWidth = Mth.clamp(this.width * 38 / 100, 140, Math.max(140, this.width / 2 - pad * 2));
        int rightLeft = leftWidth + pad * 2;
        int rightWidth = Math.max(100, this.width - rightLeft - pad);

        Page page = this.pages[this.pageIndex];
        if (!this.pageAllowed(this.pageIndex)) {
            this.pageIndex = this.nextReachablePage(-1);
            page = this.pages[this.pageIndex];
        }
        page.init(this, pad, contentTop, leftWidth, contentHeight);
        if (this.pageIndex > 0) {
            this.previewPage.init(this, rightLeft, previewTop, rightWidth, contentHeight + (contentTop - previewTop));
        }

        int cy = this.height - BOTTOM_BAR;
        int bw = 50;
        int mid = this.width / 2;
        this.addRenderableWidget(new Button(mid - bw * 2 - 4, cy, bw, 20, Component.literal("<<"), b -> {
            int next = this.prevReachablePage(this.pageIndex);
            if (next != this.pageIndex) {
                this.pageIndex = next;
                this.init();
            }
        }));
        this.addRenderableWidget(new Button(mid - bw - 2, cy, bw, 20, CommonComponents.GUI_CANCEL, b -> this.onClose()));
        this.addRenderableWidget(new Button(mid + 2, cy, bw, 20, CommonComponents.GUI_DONE, b -> this.applyAndClose()));
        this.addRenderableWidget(new Button(mid + bw + 4, cy, bw, 20, Component.literal(">>"), b -> {
            int next = this.nextReachablePage(this.pageIndex);
            if (next != this.pageIndex) {
                this.pageIndex = next;
                this.init();
            }
        }));
    }

    /** Ocean Landscape page is index 2 — skip when EGF flag is off. */
    private boolean pageAllowed(int index) {
        if (index < 0 || index >= this.pages.length) {
            return false;
        }
        if (index == 2 && !com.terraforged.mod.platform.forge.TFNoiseVariantFlags.oceanLandscapeEnabled()) {
            return false;
        }
        return true;
    }

    private int nextReachablePage(int from) {
        for (int i = from + 1; i < this.pages.length; i++) {
            if (this.pageAllowed(i)) {
                return i;
            }
        }
        return from;
    }

    private int prevReachablePage(int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (this.pageAllowed(i)) {
                return i;
            }
        }
        return from;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        drawString(pose, this.font, this.pages[this.pageIndex].title(), 8, 16, 0xE0E0E0);
        this.pages[this.pageIndex].render(pose, mouseX, mouseY, partialTick);
        if (this.pageIndex > 0) {
            this.previewPage.render(pose, mouseX, mouseY, partialTick);
        }
        super.render(pose, mouseX, mouseY, partialTick);
        if (this.pages[this.pageIndex] instanceof ScrollPage scrollPage) {
            scrollPage.renderHoveredTooltip(pose, mouseX, mouseY, this);
        }
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

    public void flushPreviewSeed() {
        this.previewPage.save();
    }

    private void applyAndClose() {
        try {
            for (Page page : this.pages) {
                page.save();
            }
            this.previewPage.save();
            if (this.shipwreckedMode) {
                this.draft.settings().world.properties.worldStyle = WorldSettings.WorldStyle.SHIPWRECKED;
                ContinentShapeWiring.bakeIslandsIntoEngine(this.draft.settings());
                this.draft.refreshNbt();
            }
            GeneratorSettingsApplier.apply(this.parent, this.draft);
            this.onClose();
        } catch (Throwable t) {
            TerraForged.LOG.error("Failed to apply NewTF generator settings", t);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("NewTF: failed to apply settings — see log"));
        }
    }

    /** Safely read current WorldGenSettings without throwing (screen may not be fully inited). */
    @Nullable
    private static WorldGenSettings safeCurrentSettings(CreateWorldScreen screen) {
        try {
            return screen.worldGenSettingsComponent.createFinalSettings(screen.hardCore).worldGenSettings();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long readSeed(CreateWorldScreen screen) {
        EditBox box = findSeedBox(screen);
        if (box == null || box.getValue().isEmpty()) {
            try {
                WorldGenSettings settings = screen.worldGenSettingsComponent.createFinalSettings(screen.hardCore).worldGenSettings();
                return settings.seed();
            } catch (Throwable ignored) {
                return -1;
            }
        }
        try {
            return Long.parseLong(box.getValue());
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
