package com.terraforged.mod.client.gui.screen.nv;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleDefaults;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleIO;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeCategory;
import com.terraforged.mod.worldgen.cave.CaveBiomeRule;
import com.terraforged.mod.worldgen.cave.CaveBiomeRuleRegistry;
import com.terraforged.mod.worldgen.cave.CaveClimateType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Experimental Generation Features:
 * side tabs, token search, icon rows, full edit panel with chance popup / add / slope checkbox.
 */
public final class NvFlagPanel extends Screen {
    private enum Tab { UNTESTED, SURFACE_BIOMES, CAVE_BIOMES }

    private enum Popup {
        NONE,
        CHANCE,
        ADD_TERRAIN,
        ADD_SUBTERRAIN,
        ADD_CLIMATE,
        ADD_ZONE,
        ADD_CAVE_CLIMATE,
        ADD_CAVE_SYSTEM,
        ADD_GENERATION
    }

    private static final int TAB_W = 28;
    private static final int TAB_H = 108;
    private static final int TAB_ICON_H = 40;
    private static final int TAB_TEX = 32;
    private static final int ICON = BiomeRuleIcons.SIZE;
    private static final int ICON_GAP = 2;
    private static final int BTN_ICON = 20;

    private static final List<String> ADDABLE_TERRAINS = List.of(
            "plains", "steppe", "dales", "river", "hills_1", "hills_2", "plateau", "badlands", "beach",
            "mountains_1", "mountains_2", "mountains_3", "mountains_ridge_1", "mountains_ridge_2",
            "dolomites", "torridonian", "volcano", "volcano_pipe", "island_flats", "island_hills",
            "island_plateau", "island_mountains", "island_volcano", "laguna"
    );
    private static final List<String> ADDABLE_SUBS = List.of(
            "river_bank", "canyon", "desert_canyon", "ocean_beach", "sea_beach", "volcanic_beach",
            "mountain_peak", "bare_mountain_peak", "mountain_body", "mountain_foothill", "bare_mountain"
    );
    private static final List<String> ADDABLE_CLIMATES = List.of(
            "temperate", "warm", "hot", "cold", "snowy", "wet", "desert", "savanna", "taiga", "tundra",
            "jungle", "alpine", "volcanic", "mesa"
    );
    private static final List<String> ADDABLE_ZONES = List.of(
            BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO,
            BiomeRule.ZONE_NEAR_DORMANT_VOLCANO
    );

    private final Screen parent;
    private Tab tab = Tab.UNTESTED;
    private boolean editing;
    private Popup popup = Popup.NONE;
    private boolean popupSub;
    private String popupKey = "";
    private EditBox chanceBox;
    private int addScroll;

    private EditBox biomeFilter;
    private IconButton reloadButton;
    private IconButton editButton;

    private final List<ResourceLocation> biomeIds = new ArrayList<>();
    private final List<ResourceLocation> filteredIds = new ArrayList<>();
    private int listScroll;
    private int selectedIndex = -1;
    private BiomeRule selectedRule;
    private CaveBiomeRule selectedCaveRule;
    private String status = "";

    private final List<IconHit> iconHits = new ArrayList<>();
    private final List<ClickHit> clickHits = new ArrayList<>();

    private record IconHit(int x, int y, int w, int h, List<String> tip) {}
    /** button: 0 = LMB, 1 = RMB, -1 = any */
    private record ClickHit(int x, int y, int w, int h, int button, Runnable action) {}

    public NvFlagPanel(Screen parent) {
        super(new TextComponent("Experimental Generation Features"));
        this.parent = parent;
    }

    private int contentLeft() {
        return TAB_W + 8;
    }

    private boolean isRulesTab() {
        return this.tab == Tab.SURFACE_BIOMES || this.tab == Tab.CAVE_BIOMES;
    }

    private boolean isCaveTab() {
        return this.tab == Tab.CAVE_BIOMES;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.iconHits.clear();
        this.clickHits.clear();
        this.popup = Popup.NONE;

        // Global Done only outside biome-rule edit mode (edit uses Back/Save at bottom).
        if (!(this.isRulesTab() && this.editing)) {
            this.addRenderableWidget(new Button(this.width / 2 - 60, this.height - 26, 120, 20, new TextComponent("Done"), b -> this.onClose()));
        }

        if (this.tab == Tab.UNTESTED) {
            this.editing = false;
            this.initUntestedTab();
        } else if (this.editing) {
            this.initEditMode();
        } else {
            this.initBrowseMode();
        }
    }

    private void setTab(Tab next) {
        this.tab = next;
        this.editing = false;
        this.popup = Popup.NONE;
        this.status = "";
        this.selectedIndex = -1;
        this.selectedRule = null;
        this.selectedCaveRule = null;
        this.listScroll = 0;
        this.init();
    }

    private void initUntestedTab() {
        int cx = (this.contentLeft() + this.width) / 2;
        int y = 48;
        this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Archipelago", TFNoiseVariantFlags.archipelagoEnabled()),
                b -> {
                    boolean next = !TFNoiseVariantFlags.archipelagoEnabled();
                    TFNoiseVariantFlags.setArchipelago(next);
                    b.setMessage(label("Archipelago", next));
                }
        ));
        y += 28;
        this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Scattered Archipelago", TFNoiseVariantFlags.scatteredArchipelagoEnabled()),
                b -> {
                    boolean next = !TFNoiseVariantFlags.scatteredArchipelagoEnabled();
                    TFNoiseVariantFlags.setScatteredArchipelago(next);
                    b.setMessage(label("Scattered Archipelago", next));
                }
        ));
        y += 28;
        this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Islands", TFNoiseVariantFlags.islandsEnabled()),
                b -> {
                    boolean next = !TFNoiseVariantFlags.islandsEnabled();
                    TFNoiseVariantFlags.setIslands(next);
                    b.setMessage(label("Islands", next));
                }
        ));
    }

    private void initBrowseMode() {
        this.reloadBiomeIds();
        int left = this.contentLeft();

        this.biomeFilter = new EditBox(this.font, left + 4, 28, 168, 18, new TextComponent("filter"));
        this.biomeFilter.setMaxLength(64);
        this.biomeFilter.setResponder(s -> {
            this.applyFilter();
            this.listScroll = 0;
        });
        this.addRenderableWidget(this.biomeFilter);

        this.reloadButton = this.addRenderableWidget(new IconButton(
                left + 176, 27, BTN_ICON, BTN_ICON,
                BiomeRuleIcons.ui("icon_reload"),
                "Reload",
                b -> {
                    if (this.isCaveTab()) {
                        if (TFCaveBiomeConfig.INSTANCE == null) {
                            TFCaveBiomeConfig.load();
                        }
                        CaveBiomeRuleRegistry.reload();
                    } else {
                        BiomeRuleRegistry.syncAtGameLaunch();
                    }
                    this.reloadBiomeIds();
                    this.status = "Reloaded (" + this.biomeIds.size() + ")";
                    this.selectIndex(this.selectedIndex);
                }
        ));
        this.editButton = this.addRenderableWidget(new IconButton(
                left + 200, 27, BTN_ICON, BTN_ICON,
                BiomeRuleIcons.ui("icon_edit"),
                "Edit",
                b -> this.openEdit()
        ));
        boolean hasSel = this.isCaveTab() ? this.selectedCaveRule != null : this.selectedRule != null;
        this.editButton.visible = hasSel;
        this.editButton.active = hasSel;

        this.applyFilter();
        if (!this.filteredIds.isEmpty() && this.selectedIndex < 0) {
            this.selectIndex(0);
        }
        this.refreshEditVisibility();
    }

    private void initEditMode() {
        int left = this.contentLeft();
        int btnY = this.height - 26;
        int mid = (left + this.width) / 2;
        this.addRenderableWidget(new Button(mid - 110, btnY, 100, 20, new TextComponent("Back"), b -> this.exitEdit()));
        this.addRenderableWidget(new Button(mid + 10, btnY, 100, 20, new TextComponent("Save rule"), b -> this.saveSelected()));
    }

    private void exitEdit() {
        this.editing = false;
        this.popup = Popup.NONE;
        this.status = "";
        // Discard unsaved in-memory edits by reloading the rule from the registry.
        if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredIds.size()) {
            ResourceLocation id = this.filteredIds.get(this.selectedIndex);
            if (this.isCaveTab()) {
                this.selectedCaveRule = CaveBiomeRuleRegistry.get(id);
                this.selectedRule = null;
            } else {
                this.selectedRule = BiomeRuleRegistry.get(id);
                this.selectedCaveRule = null;
            }
        }
        this.init();
    }

    private void openEdit() {
        if (this.isCaveTab()) {
            if (this.selectedCaveRule == null) {
                this.status = "Select a biome first";
                return;
            }
        } else if (this.selectedRule == null) {
            this.status = "Select a biome first";
            return;
        }
        this.status = "";
        this.editing = true;
        this.popup = Popup.NONE;
        this.init();
    }

    private void refreshEditVisibility() {
        if (this.editButton != null) {
            boolean on = this.isCaveTab() ? this.selectedCaveRule != null : this.selectedRule != null;
            this.editButton.visible = on;
            this.editButton.active = on;
        }
    }

    private void reloadBiomeIds() {
        this.biomeIds.clear();
        if (this.isCaveTab()) {
            this.biomeIds.addAll(CaveBiomeRuleRegistry.idsSorted());
        } else {
            this.biomeIds.addAll(BiomeRuleRegistry.snapshot().keySet());
            this.biomeIds.sort(Comparator.comparing(ResourceLocation::toString));
        }
        this.applyFilter();
    }

    /** Match query against namespace, path, and underscore / hyphen tokens. */
    private static boolean matchesTokenSearch(ResourceLocation id, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        String full = id.toString().toLowerCase(Locale.ROOT);
        if (full.contains(q)) {
            return true;
        }
        if (id.getNamespace().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String token : id.getPath().toLowerCase(Locale.ROOT).split("[_/\\-.]+")) {
            if (token.contains(q) || q.contains(token) && token.length() >= 3) {
                return true;
            }
        }
        return false;
    }

    private void applyFilter() {
        String q = this.biomeFilter == null ? "" : this.biomeFilter.getValue();
        this.filteredIds.clear();
        for (ResourceLocation id : this.biomeIds) {
            if (matchesTokenSearch(id, q)) {
                this.filteredIds.add(id);
            }
        }
        if (this.selectedIndex >= this.filteredIds.size()) {
            this.selectedIndex = this.filteredIds.isEmpty() ? -1 : 0;
        }
        this.selectIndex(this.selectedIndex);
    }

    private void selectIndex(int idx) {
        int prev = this.selectedIndex;
        this.selectedIndex = idx;
        this.selectedRule = null;
        this.selectedCaveRule = null;
        if (idx >= 0 && idx < this.filteredIds.size()) {
            ResourceLocation id = this.filteredIds.get(idx);
            if (this.isCaveTab()) {
                this.selectedCaveRule = CaveBiomeRuleRegistry.get(id);
            } else {
                this.selectedRule = BiomeRuleRegistry.get(id);
            }
        }
        // Clear stale edit messages when browsing another biome (unsaved edits are not shown).
        if (prev != idx && !this.editing) {
            this.status = "";
        }
        this.refreshEditVisibility();
    }

    private void mutateRule(BiomeRule next) {
        this.selectedRule = next;
    }

    private void mutateCaveRule(CaveBiomeRule next) {
        this.selectedCaveRule = next;
    }

    private CaveBiomeRule copyCave(
            CaveBiomeCategory category,
            Set<CaveClimateType> climates,
            Set<String> systems
    ) {
        CaveBiomeRule r = this.selectedCaveRule;
        return new CaveBiomeRule(
                r.biome,
                category,
                r.placementType,
                climates,
                systems,
                r.temperature,
                r.vegetationDensity,
                r.weight,
                r.ceilingPatchMin,
                r.ceilingPatchMax,
                r.islandMaxRadius,
                r.stats,
                r.statGenerator,
                false
        );
    }

    private void toggleSlope() {
        if (this.selectedRule == null) {
            return;
        }
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                !this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                this.selectedRule.zoneFlags,
                false
        ));
    }

    private void addClimate(String tag) {
        if (this.selectedRule == null || tag == null || tag.isBlank()) {
            return;
        }
        List<String> tags = new ArrayList<>(this.selectedRule.climateTags);
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                tags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                this.selectedRule.zoneFlags,
                false
        ));
        this.popup = Popup.NONE;
        this.status = "Added climate " + tag;
        this.init();
    }

    private void removeClimate(String tag) {
        if (this.selectedRule == null || tag == null) {
            return;
        }
        List<String> tags = new ArrayList<>(this.selectedRule.climateTags);
        tags.remove(tag);
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                tags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                this.selectedRule.zoneFlags,
                false
        ));
        this.status = "Removed climate " + tag;
    }

    private void addZone(String key) {
        if (this.selectedRule == null || key == null) {
            return;
        }
        Map<String, BiomeRule.ZoneFlag> zones = new LinkedHashMap<>(this.selectedRule.zoneFlags);
        zones.putIfAbsent(key, BiomeRule.ZoneFlag.enabled(640.0F, 1.0F));
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                zones,
                false
        ));
        this.popup = Popup.NONE;
        this.status = "Added zone " + key;
        this.init();
    }

    private void removeZone(String key) {
        if (this.selectedRule == null || key == null) {
            return;
        }
        Map<String, BiomeRule.ZoneFlag> zones = new LinkedHashMap<>(this.selectedRule.zoneFlags);
        zones.remove(key);
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                zones,
                false
        ));
        this.status = "Removed zone " + key;
    }

    private void addCaveClimate(String alias) {
        if (this.selectedCaveRule == null || alias == null || alias.isBlank()) {
            return;
        }
        EnumSet<CaveClimateType> climates = this.selectedCaveRule.climates.isEmpty()
                ? EnumSet.noneOf(CaveClimateType.class)
                : EnumSet.copyOf(this.selectedCaveRule.climates);
        climates.add(CaveClimateType.fromAlias(alias));
        this.mutateCaveRule(this.copyCave(this.selectedCaveRule.category, climates, this.selectedCaveRule.systems));
        this.popup = Popup.NONE;
        this.status = "Added climate " + alias;
        this.init();
    }

    private void removeCaveClimate(String alias) {
        if (this.selectedCaveRule == null || alias == null) {
            return;
        }
        EnumSet<CaveClimateType> climates = this.selectedCaveRule.climates.isEmpty()
                ? EnumSet.noneOf(CaveClimateType.class)
                : EnumSet.copyOf(this.selectedCaveRule.climates);
        climates.remove(CaveClimateType.fromAlias(alias));
        this.mutateCaveRule(this.copyCave(this.selectedCaveRule.category, climates, this.selectedCaveRule.systems));
        this.status = "Removed climate " + alias;
    }

    private void addCaveSystem(String system) {
        if (this.selectedCaveRule == null || system == null || system.isBlank()) {
            return;
        }
        LinkedHashSet<String> systems = new LinkedHashSet<>(this.selectedCaveRule.systems);
        systems.add(system.toLowerCase(Locale.ROOT));
        this.mutateCaveRule(this.copyCave(this.selectedCaveRule.category, this.selectedCaveRule.climates, systems));
        this.popup = Popup.NONE;
        this.status = "Added system " + system;
        this.init();
    }

    private void removeCaveSystem(String system) {
        if (this.selectedCaveRule == null || system == null) {
            return;
        }
        LinkedHashSet<String> systems = new LinkedHashSet<>(this.selectedCaveRule.systems);
        systems.remove(system.toLowerCase(Locale.ROOT));
        this.mutateCaveRule(this.copyCave(this.selectedCaveRule.category, this.selectedCaveRule.climates, systems));
        this.status = "Removed system " + system;
    }

    private void setCaveGeneration(String gen) {
        if (this.selectedCaveRule == null || gen == null) {
            return;
        }
        CaveBiomeCategory cat = CaveBiomeRule.categoryFromGeneration(gen);
        this.mutateCaveRule(this.copyCave(cat, this.selectedCaveRule.climates, this.selectedCaveRule.systems));
        this.popup = Popup.NONE;
        this.status = "Generation: " + CaveBiomeRule.generationAlias(cat);
        this.init();
    }

    private void setChance(String key, boolean sub, float chance) {
        if (this.selectedRule == null || key == null || key.isBlank()) {
            return;
        }
        Map<String, Float> terrains = new LinkedHashMap<>(this.selectedRule.terrains);
        Map<String, Float> subs = new LinkedHashMap<>(this.selectedRule.subterrains);
        if (sub) {
            if (chance <= 0.0F) {
                subs.remove(key);
            } else {
                subs.put(key, chance);
            }
        } else {
            if (chance <= 0.0F) {
                terrains.remove(key);
            } else {
                terrains.put(key, chance);
            }
            if (terrains.isEmpty()) {
                this.status = "Keep at least one terrain";
                return;
            }
        }
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                terrains,
                subs,
                this.selectedRule.zoneFlags,
                false
        ));
    }

    private void addNamed(String key, boolean sub) {
        if (this.selectedRule == null || key == null) {
            return;
        }
        Map<String, Float> terrains = new LinkedHashMap<>(this.selectedRule.terrains);
        Map<String, Float> subs = new LinkedHashMap<>(this.selectedRule.subterrains);
        if (sub) {
            subs.putIfAbsent(key, 1.0F);
        } else {
            terrains.putIfAbsent(key, 1.0F);
        }
        this.mutateRule(new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                terrains,
                subs,
                this.selectedRule.zoneFlags,
                false
        ));
        this.popup = Popup.NONE;
        this.status = "Added " + key;
        this.init();
    }

    private void openChancePopup(String key, boolean sub) {
        this.popup = Popup.CHANCE;
        this.popupSub = sub;
        this.popupKey = key;
        float cur = 1.0F;
        if (this.selectedRule != null) {
            Float v = sub ? this.selectedRule.subterrains.get(key) : this.selectedRule.terrains.get(key);
            if (v != null) {
                cur = v;
            }
        }
        this.clearWidgetsKeepEditChrome();
        int px = this.width / 2 - 54;
        int py = this.height / 2 - 22;
        this.chanceBox = new EditBox(this.font, px + 8, py + 18, 92, 16, new TextComponent("chance"));
        this.chanceBox.setMaxLength(8);
        this.chanceBox.setValue(fmt(cur));
        this.chanceBox.setBordered(true);
        this.addRenderableWidget(this.chanceBox);
        this.setInitialFocus(this.chanceBox);
        this.chanceBox.setFocus(true);
    }

    private void commitChancePopup() {
        if (this.chanceBox == null) {
            this.popup = Popup.NONE;
            this.init();
            return;
        }
        try {
            float v = Float.parseFloat(this.chanceBox.getValue().trim());
            this.setChance(this.popupKey, this.popupSub, v);
            this.popup = Popup.NONE;
            this.init();
        } catch (NumberFormatException e) {
            this.status = "Bad chance value";
        }
    }

    private void openAddPopup(Popup kind) {
        this.popup = kind;
        this.popupSub = kind == Popup.ADD_SUBTERRAIN;
        this.addScroll = 0;
        this.clearWidgetsKeepEditChrome();
    }

    /** Keep Back/Save (edit) or Done (browse) while showing a popup overlay. */
    private void clearWidgetsKeepEditChrome() {
        this.clearWidgets();
        if (this.editing) {
            this.initEditMode();
        } else {
            this.addRenderableWidget(new Button(this.width / 2 - 60, this.height - 26, 120, 20, new TextComponent("Done"), b -> this.onClose()));
        }
    }

    private void saveSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredIds.size()) {
            this.status = "Nothing to save";
            return;
        }
        ResourceLocation id = this.filteredIds.get(this.selectedIndex);
        if (this.isCaveTab()) {
            if (this.selectedCaveRule == null) {
                this.status = "Nothing to save";
                return;
            }
            CaveBiomeRule toSave = this.copyCave(
                    this.selectedCaveRule.category,
                    this.selectedCaveRule.climates,
                    this.selectedCaveRule.systems
            );
            try {
                CaveBiomeRuleRegistry.putAndSave(id, toSave);
                this.reloadBiomeIds();
                for (int i = 0; i < this.filteredIds.size(); i++) {
                    if (this.filteredIds.get(i).equals(id)) {
                        this.selectIndex(i);
                        break;
                    }
                }
                this.status = "Saved " + id;
            } catch (Exception e) {
                this.status = "Save failed: " + e.getMessage();
                TerraForged.LOG.error("[CaveBiomeRules] save failed {}", id, e);
            }
            return;
        }
        if (this.selectedRule == null) {
            this.status = "Nothing to save";
            return;
        }
        // Force non-auto so future sync keeps player/default edits.
        BiomeRule toSave = new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                this.selectedRule.zoneFlags,
                false
        );
        Path file = BiomeRuleRegistry.biomesRoot().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        try {
            Files.createDirectories(file.getParent());
            BiomeRuleIO.write(file, toSave);
            if (BiomeRuleDefaults.isEnabled()) {
                BiomeRuleDefaults.savePlayerDefault(id, toSave);
            }
            BiomeRuleRegistry.syncAtGameLaunch();
            this.reloadBiomeIds();
            for (int i = 0; i < this.filteredIds.size(); i++) {
                if (this.filteredIds.get(i).equals(id)) {
                    this.selectIndex(i);
                    break;
                }
            }
            this.status = "Saved " + id + (BiomeRuleDefaults.isEnabled() ? " (+ default)" : "");
        } catch (Exception e) {
            this.status = "Save failed: " + e.getMessage();
            TerraForged.LOG.error("[BiomeRules] save failed {}", id, e);
        }
    }

    private static TextComponent label(String name, boolean on) {
        return new TextComponent(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        this.iconHits.clear();
        this.clickHits.clear();
        this.renderSideTabs(pose, mouseX, mouseY);

        if (this.tab == Tab.UNTESTED) {
            int mid = (this.contentLeft() + this.width) / 2;
            drawCenteredString(pose, this.font, this.title, mid, 14, 0xFFE080);
            drawCenteredString(pose, this.font, "Unstable / unfinished worldgen. Default OFF for releases.", mid, 28, 0xFFAAAAAA);
        } else if (this.editing) {
            this.renderEditPanel(pose, mouseX, mouseY);
        } else {
            this.renderBrowsePanel(pose, mouseX, mouseY);
        }

        super.render(pose, mouseX, mouseY, partialTick);

        if (this.popup != Popup.NONE) {
            this.renderPopup(pose, mouseX, mouseY);
        }
        this.renderHoveredIconTooltip(pose, mouseX, mouseY);
        if (this.reloadButton != null && this.reloadButton.isHoveredOrFocused()) {
            this.renderTooltip(pose, new TextComponent("Reload"), mouseX, mouseY);
        } else if (this.editButton != null && this.editButton.visible && this.editButton.isHoveredOrFocused()) {
            this.renderTooltip(pose, new TextComponent("Edit"), mouseX, mouseY);
        }
    }

    private void renderSideTabs(PoseStack pose, int mouseX, int mouseY) {
        int y1 = 28;
        int y2 = y1 + TAB_H + 6;
        int y3 = y2 + TAB_ICON_H + 4;
        this.drawSideTab(pose, 0, y1, Tab.UNTESTED, "1", "Untested", mouseX, mouseY);
        this.drawSideIconTab(pose, 0, y2, Tab.SURFACE_BIOMES, BiomeRuleIcons.tabSurface(), "Surface Biomes", mouseX, mouseY);
        this.drawSideIconTab(pose, 0, y3, Tab.CAVE_BIOMES, BiomeRuleIcons.tabCave(), "Cave Biomes", mouseX, mouseY);
    }

    private void drawSideTab(PoseStack pose, int x, int y, Tab which, String num, String title, int mouseX, int mouseY) {
        boolean active = this.tab == which;
        boolean hover = mouseX >= x && mouseX < x + TAB_W + (active ? 4 : 0) && mouseY >= y && mouseY < y + TAB_H;
        int w = active ? TAB_W + 4 : TAB_W;
        int bg = active ? 0xFF3A3A3A : (hover ? 0xFF2A2A2A : 0xFF1A1A1A);
        int edge = active ? 0xFFE0C060 : 0xFF666666;
        fill(pose, x, y, x + w, y + TAB_H, bg);
        fill(pose, x + w - 2, y, x + w, y + TAB_H, edge);
        fill(pose, x, y, x + w, y + 2, edge);
        fill(pose, x, y + TAB_H - 2, x + w, y + TAB_H, edge);
        drawCenteredString(pose, this.font, num, x + w / 2, y + 8, active ? 0xFFFFE080 : 0xFFCCCCCC);
        this.drawVerticalLabel(pose, title, x + (w - 8) / 2, y + 24, active ? 0xFFFFE080 : 0xFFAAAAAA);
    }

    private void drawSideIconTab(PoseStack pose, int x, int y, Tab which, ResourceLocation icon, String tip, int mouseX, int mouseY) {
        boolean active = this.tab == which;
        boolean hover = mouseX >= x && mouseX < x + TAB_W + (active ? 4 : 0) && mouseY >= y && mouseY < y + TAB_ICON_H;
        int w = active ? TAB_W + 4 : TAB_W;
        int bg = active ? 0xFF3A3A3A : (hover ? 0xFF2A2A2A : 0xFF1A1A1A);
        int edge = active ? 0xFFE0C060 : 0xFF666666;
        fill(pose, x, y, x + w, y + TAB_ICON_H, bg);
        fill(pose, x + w - 2, y, x + w, y + TAB_ICON_H, edge);
        fill(pose, x, y, x + w, y + 2, edge);
        fill(pose, x, y + TAB_ICON_H - 2, x + w, y + TAB_ICON_H, edge);
        int ix = x + (w - TAB_TEX) / 2;
        int iy = y + (TAB_ICON_H - TAB_TEX) / 2;
        // Clip slightly into the narrow tab: draw centered 32×32 (may overhang left/right a bit — OK for crisp icons).
        blitTabIcon(pose, icon, ix, iy);
        this.iconHits.add(new IconHit(x, y, w, TAB_ICON_H, List.of(tip)));
    }

    private void drawVerticalLabel(PoseStack pose, String text, int x, int y, int color) {
        int yy = y;
        for (int i = 0; i < text.length() && yy + 9 < y + TAB_H - 8; i++) {
            drawCenteredString(pose, this.font, text.substring(i, i + 1), x + 4, yy, color);
            yy += 9;
        }
    }

    private void renderBrowsePanel(PoseStack pose, int mouseX, int mouseY) {
        int left = this.contentLeft();
        String title = this.isCaveTab() ? "Cave Biomes" : "Surface Biomes";
        drawString(pose, this.font, title, left + 4, 12, 0xFFE080);

        int listX = left + 4;
        int listY = 50;
        int listW = 210;
        int listH = this.height - 100;
        fill(pose, listX, listY, listX + listW, listY + listH, 0x88000000);
        drawString(pose, this.font, "Biome list", listX + 2, listY - 12, 0xFFDDDDDD);

        int rowH = 12;
        int visible = Math.max(1, listH / rowH);
        int maxScroll = Math.max(0, this.filteredIds.size() - visible);
        this.listScroll = Math.min(this.listScroll, maxScroll);

        for (int i = 0; i < visible; i++) {
            int idx = this.listScroll + i;
            if (idx >= this.filteredIds.size()) {
                break;
            }
            int y = listY + i * rowH;
            boolean sel = idx == this.selectedIndex;
            boolean hover = mouseX >= listX && mouseX < listX + listW + 70 && mouseY >= y && mouseY < y + rowH;
            if (sel) {
                fill(pose, listX, y, listX + listW, y + rowH, 0xAA335577);
            } else if (hover) {
                fill(pose, listX, y, listX + listW, y + rowH, 0x55333333);
            }
            drawString(pose, this.font, trim(this.filteredIds.get(idx).toString(), listW - 8), listX + 4, y + 2, sel ? 0xFFFFFFAA : 0xFFEEEEEE);
            if (sel || hover) {
                int bx = listX + listW + 2;
                fill(pose, bx, y, bx + 52, y + rowH, sel ? 0xAA226644 : 0xAA444444);
                drawString(pose, this.font, "Edit", bx + 8, y + 2, 0xFF88FF88);
            }
        }

        int hx = listX + listW + 60;
        int hy = 50;
        int hw = Math.max(80, this.width - hx - 8);
        // Asset is 144x144; shrink only if the panel is too short.
        int preview = Math.min(BiomePreviewIcons.SIZE, Math.max(72, this.height - hy - 120));
        preview = Math.min(preview, Math.max(72, hw / 2));
        fill(pose, hx, hy, hx + preview, hy + preview, 0x66000000);

        if (this.isCaveTab()) {
            this.renderCaveBrowseDetail(pose, mouseX, mouseY, hx, hy, hw, preview);
        } else {
            this.renderSurfaceBrowseDetail(pose, mouseX, mouseY, hx, hy, hw, preview);
        }

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, left + 4, this.height - 40, 0xFFAAFFAA);
        }
        if (!this.isCaveTab()) {
            drawString(pose, this.font, "Defaults: " + (BiomeRuleDefaults.isEnabled() ? "ON" : "OFF"), left + 4, this.height - 52, 0xFF888888);
        }
    }

    private void renderSurfaceBrowseDetail(PoseStack pose, int mouseX, int mouseY, int hx, int hy, int hw, int preview) {
        if (this.selectedRule != null) {
            blitPreview(pose, BiomePreviewIcons.forBiome(this.selectedRule.biome), hx, hy, preview);
        } else {
            drawCenteredString(pose, this.font, "img", hx + preview / 2, hy + preview / 2 - 4, 0xFF666666);
        }

        String name = this.selectedRule != null ? this.selectedRule.biome : "(select a biome)";
        int textX = hx + preview + 8;
        int textW = Math.max(40, this.width - textX - 8);
        drawString(pose, this.font, trim(name, textW), textX, hy + 6, 0xFFFFFFFF);
        String desc = this.selectedRule == null
                ? "Pick a biome, then Edit. Hover icons for name + chance."
                : "slope=" + this.selectedRule.canBeOnSlope;
        drawString(pose, this.font, trim(desc, textW), textX, hy + 22, 0xFFCCCCCC);

        int rowY = hy + preview + 8;
        rowY = this.drawIconRow(pose, "Terrains", hx, rowY, hw, mouseX, mouseY, this.terrainIcons(), false, false);
        rowY = this.drawIconRow(pose, "Subterrains", hx, rowY, hw, mouseX, mouseY, this.subterrainIcons(), false, false);
        rowY = this.drawIconRow(pose, "Climate", hx, rowY, hw, mouseX, mouseY, this.climateIcons(), false, false);
        this.drawIconRow(pose, "Zone flags", hx, rowY, hw, mouseX, mouseY, this.zoneIcons(), false, false);
    }

    private void renderCaveBrowseDetail(PoseStack pose, int mouseX, int mouseY, int hx, int hy, int hw, int preview) {
        if (this.selectedCaveRule != null) {
            blitPreview(pose, BiomePreviewIcons.forBiome(this.selectedCaveRule.biome), hx, hy, preview);
        } else {
            drawCenteredString(pose, this.font, "img", hx + preview / 2, hy + preview / 2 - 4, 0xFF666666);
        }

        String name = this.selectedCaveRule != null ? this.selectedCaveRule.biome : "(select a biome)";
        int textX = hx + preview + 8;
        int textW = Math.max(40, this.width - textX - 8);
        drawString(pose, this.font, trim(name, textW), textX, hy + 6, 0xFFFFFFFF);
        String desc = this.selectedCaveRule == null
                ? "Pick a cave biome, then Edit."
                : "gen=" + CaveBiomeRule.generationAlias(this.selectedCaveRule.category);
        drawString(pose, this.font, trim(desc, textW), textX, hy + 22, 0xFFCCCCCC);

        int rowY = hy + preview + 8;
        rowY = this.drawIconRow(pose, "Generation", hx, rowY, hw, mouseX, mouseY, this.caveGenerationIcons(), false, false);
        rowY = this.drawIconRow(pose, "Climates", hx, rowY, hw, mouseX, mouseY, this.caveClimateIcons(), false, false);
        this.drawIconRow(pose, "Systems", hx, rowY, hw, mouseX, mouseY, this.caveSystemIcons(), false, false);
    }

    private void renderEditPanel(PoseStack pose, int mouseX, int mouseY) {
        if (this.isCaveTab()) {
            this.renderCaveEditPanel(pose, mouseX, mouseY);
            return;
        }
        int left = this.contentLeft();
        drawString(pose, this.font, "Surface Biome Rule Settings", left + 4, 10, 0xFFE080);
        String name = this.selectedRule != null ? this.selectedRule.biome : "(none)";
        drawString(pose, this.font, name, left + 4, 24, 0xFFFFFFFF);

        int panelX = left + 4;
        int panelY = 40;
        int panelW = this.width - panelX - 8;
        int panelH = this.height - panelY - 36;
        fill(pose, panelX, panelY, panelX + panelW, panelY + panelH, 0x88000000);

        if (this.selectedRule == null) {
            drawString(pose, this.font, "No rule selected.", panelX + 8, panelY + 8, 0xFFFF8888);
            return;
        }

        int y = panelY + 6;
        // Slope checklist
        int box = 10;
        fill(pose, panelX + 8, y, panelX + 8 + box, y + box, 0xFF000000);
        fill(pose, panelX + 9, y + 1, panelX + 7 + box, y + box - 1, 0xFF555555);
        if (this.selectedRule.canBeOnSlope) {
            fill(pose, panelX + 10, y + 2, panelX + 6 + box, y + box - 2, 0xFF88FF88);
        }
        drawString(pose, this.font, "Slope (can_be_on_slope)", panelX + 22, y + 1, 0xFFFFFFFF);
        this.clickHits.add(new ClickHit(panelX + 8, y, 160, box + 2, -1, this::toggleSlope));
        y += 18;

        drawString(pose, this.font, "Terrains (LMB=chance, RMB=remove):", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.terrainIcons(), true, false);
        int addX = panelX + panelW - 28;
        int addY = y - ICON - 4;
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, addY);
        this.iconHits.add(new IconHit(addX, addY, ICON, ICON, List.of("Add terrain")));
        this.clickHits.add(new ClickHit(addX, addY, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_TERRAIN)));
        y += 4;

        drawString(pose, this.font, "Subterrains:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.subterrainIcons(), true, true);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Add subterrain")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_SUBTERRAIN)));
        y += 4;

        drawString(pose, this.font, "Climate:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.climateIcons(), false, false);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Add climate")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_CLIMATE)));
        y += 4;

        drawString(pose, this.font, "Zone flags:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.zoneIcons(), false, false);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Add zone flag")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_ZONE)));

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, left + 4, this.height - 40, 0xFFAAFFAA);
        }
    }

    private void renderCaveEditPanel(PoseStack pose, int mouseX, int mouseY) {
        int left = this.contentLeft();
        drawString(pose, this.font, "Cave Biome Rule Settings", left + 4, 10, 0xFFE080);
        String name = this.selectedCaveRule != null ? this.selectedCaveRule.biome : "(none)";
        drawString(pose, this.font, name, left + 4, 24, 0xFFFFFFFF);

        int panelX = left + 4;
        int panelY = 40;
        int panelW = this.width - panelX - 8;
        int panelH = this.height - panelY - 36;
        fill(pose, panelX, panelY, panelX + panelW, panelY + panelH, 0x88000000);

        if (this.selectedCaveRule == null) {
            drawString(pose, this.font, "No rule selected.", panelX + 8, panelY + 8, 0xFFFF8888);
            return;
        }

        int y = panelY + 6;
        int addX = panelX + panelW - 28;

        drawString(pose, this.font, "Generation type (click to change):", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.caveGenerationIcons(), false, false);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Change generation type")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_GENERATION)));
        y += 4;

        drawString(pose, this.font, "Climates (click to remove):", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.caveClimateIcons(), false, false);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Add climate")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_CAVE_CLIMATE)));
        y += 4;

        drawString(pose, this.font, "Systems (click to remove):", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 40, mouseX, mouseY, this.caveSystemIcons(), false, false);
        blitIcon(pose, BiomeRuleIcons.ui("icon_add"), addX, y - ICON - 4);
        this.iconHits.add(new IconHit(addX, y - ICON - 4, ICON, ICON, List.of("Add system")));
        this.clickHits.add(new ClickHit(addX, y - ICON - 4, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_CAVE_SYSTEM)));

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, left + 4, this.height - 40, 0xFFAAFFAA);
        }
    }

    private void renderPopup(PoseStack pose, int mouseX, int mouseY) {
        fill(pose, 0, 0, this.width, this.height, 0x66000000);
        if (this.popup == Popup.CHANCE) {
            int px = this.width / 2 - 54;
            int py = this.height / 2 - 22;
            fill(pose, px, py, px + 108, py + 42, 0xEE1A1A1A);
            fill(pose, px, py, px + 108, py + 1, 0xFFE0C060);
            fill(pose, px, py + 41, px + 108, py + 42, 0xFFE0C060);
            drawCenteredString(pose, this.font, this.popupKey, this.width / 2, py + 4, 0xFFFFE080);
            drawCenteredString(pose, this.font, "chance  [Enter]", this.width / 2, py + 36, 0xFFAAAAAA);
            return;
        }
        // ADD list
        int px = this.width / 2 - 110;
        int py = this.height / 2 - 80;
        fill(pose, px, py, px + 220, py + 160, 0xFF2A2A2A);
        String title = switch (this.popup) {
            case ADD_SUBTERRAIN -> "Add subterrain";
            case ADD_CLIMATE -> "Add climate";
            case ADD_ZONE -> "Add zone flag";
            case ADD_CAVE_CLIMATE -> "Add cave climate";
            case ADD_CAVE_SYSTEM -> "Add cave system";
            case ADD_GENERATION -> "Generation type";
            default -> "Add terrain";
        };
        drawCenteredString(pose, this.font, title, this.width / 2, py + 6, 0xFFFFE080);
        List<String> options = switch (this.popup) {
            case ADD_SUBTERRAIN -> ADDABLE_SUBS;
            case ADD_CLIMATE -> ADDABLE_CLIMATES;
            case ADD_ZONE -> ADDABLE_ZONES;
            case ADD_CAVE_CLIMATE -> CaveBiomeRule.allClimateAliases();
            case ADD_CAVE_SYSTEM -> CaveBiomeRule.allSystems();
            case ADD_GENERATION -> CaveBiomeRule.allGenerationTypes();
            default -> ADDABLE_TERRAINS;
        };
        Set<String> have;
        if (this.popup == Popup.ADD_CAVE_CLIMATE || this.popup == Popup.ADD_CAVE_SYSTEM || this.popup == Popup.ADD_GENERATION) {
            if (this.selectedCaveRule == null) {
                have = Set.of();
            } else if (this.popup == Popup.ADD_CAVE_CLIMATE) {
                LinkedHashSet<String> aliases = new LinkedHashSet<>();
                for (CaveClimateType c : this.selectedCaveRule.climates) {
                    aliases.add(c.alias());
                }
                have = aliases;
            } else if (this.popup == Popup.ADD_CAVE_SYSTEM) {
                have = this.selectedCaveRule.systems;
            } else {
                have = Set.of(CaveBiomeRule.generationAlias(this.selectedCaveRule.category));
            }
        } else if (this.selectedRule == null) {
            have = Set.of();
        } else if (this.popup == Popup.ADD_SUBTERRAIN) {
            have = this.selectedRule.subterrains.keySet();
        } else if (this.popup == Popup.ADD_CLIMATE) {
            have = Set.copyOf(this.selectedRule.climateTags);
        } else if (this.popup == Popup.ADD_ZONE) {
            have = this.selectedRule.zoneFlags.keySet();
        } else {
            have = this.selectedRule.terrains.keySet();
        }
        int rowH = 12;
        int listTop = py + 22;
        int visible = 9;
        int shown = 0;
        int skipped = 0;
        for (String opt : options) {
            if (have.contains(opt)) {
                continue;
            }
            if (skipped < this.addScroll) {
                skipped++;
                continue;
            }
            if (shown >= visible) {
                break;
            }
            int yy = listTop + shown * rowH;
            boolean hover = mouseX >= px + 8 && mouseX < px + 212 && mouseY >= yy && mouseY < yy + rowH;
            if (hover) {
                fill(pose, px + 6, yy, px + 214, yy + rowH, 0xAA335577);
            }
            drawString(pose, this.font, opt, px + 10, yy + 2, 0xFFFFFFFF);
            final String pick = opt;
            Runnable action = switch (this.popup) {
                case ADD_SUBTERRAIN -> () -> this.addNamed(pick, true);
                case ADD_CLIMATE -> () -> this.addClimate(pick);
                case ADD_ZONE -> () -> this.addZone(pick);
                case ADD_CAVE_CLIMATE -> () -> this.addCaveClimate(pick);
                case ADD_CAVE_SYSTEM -> () -> this.addCaveSystem(pick);
                case ADD_GENERATION -> () -> this.setCaveGeneration(pick);
                default -> () -> this.addNamed(pick, false);
            };
            this.clickHits.add(new ClickHit(px + 6, yy, 208, rowH, 0, action));
            shown++;
        }
        drawCenteredString(pose, this.font, "RMB / Esc = close", this.width / 2, py + 142, 0xFF888888);
    }

    private record IconSpec(
            ResourceLocation tex, String key, String title, String detail,
            boolean chanceEditable, boolean sub, Kind kind
    ) {
        enum Kind { TERRAIN, SUB, CLIMATE, ZONE, CAVE_CLIMATE, CAVE_SYSTEM, CAVE_GEN }
    }

    private List<IconSpec> terrainIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (Map.Entry<String, Float> e : this.selectedRule.terrains.entrySet()) {
            out.add(new IconSpec(BiomeRuleIcons.terrain(e.getKey()), e.getKey(), e.getKey(), "chance " + fmt(e.getValue()), true, false, IconSpec.Kind.TERRAIN));
        }
        return out;
    }

    private List<IconSpec> subterrainIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (Map.Entry<String, Float> e : this.selectedRule.subterrains.entrySet()) {
            ResourceLocation icon = BiomeRuleIcons.subterrain(e.getKey());
            if (icon == null) {
                icon = BiomeRuleIcons.terrain(e.getKey());
            }
            out.add(new IconSpec(icon, e.getKey(), e.getKey(), "chance " + fmt(e.getValue()), true, true, IconSpec.Kind.SUB));
        }
        return out;
    }

    private List<IconSpec> climateIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (String tag : this.selectedRule.climateTags) {
            out.add(new IconSpec(BiomeRuleIcons.climate(tag), tag, tag, "climate tag", false, false, IconSpec.Kind.CLIMATE));
        }
        return out;
    }

    private List<IconSpec> zoneIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (Map.Entry<String, BiomeRule.ZoneFlag> e : this.selectedRule.zoneFlags.entrySet()) {
            BiomeRule.ZoneFlag z = e.getValue();
            if (!z.enabled()) {
                continue;
            }
            out.add(new IconSpec(
                    BiomeRuleIcons.zone(e.getKey()),
                    e.getKey(),
                    e.getKey(),
                    "radius " + fmt(z.radiusBlocks()) + "  chance " + fmt(z.chance()),
                    false,
                    false,
                    IconSpec.Kind.ZONE
            ));
        }
        return out;
    }

    private List<IconSpec> caveGenerationIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedCaveRule == null) {
            return out;
        }
        String gen = CaveBiomeRule.generationAlias(this.selectedCaveRule.category);
        out.add(new IconSpec(
                BiomeRuleIcons.caveGeneration(gen), gen, gen, "generation_type",
                false, false, IconSpec.Kind.CAVE_GEN
        ));
        return out;
    }

    private List<IconSpec> caveClimateIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedCaveRule == null) {
            return out;
        }
        for (CaveClimateType c : this.selectedCaveRule.climates) {
            String alias = c.alias();
            out.add(new IconSpec(
                    BiomeRuleIcons.caveClimate(alias), alias, alias, "climate",
                    false, false, IconSpec.Kind.CAVE_CLIMATE
            ));
        }
        return out;
    }

    private List<IconSpec> caveSystemIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedCaveRule == null) {
            return out;
        }
        for (String sys : this.selectedCaveRule.systems) {
            out.add(new IconSpec(
                    BiomeRuleIcons.caveSystem(sys), sys, sys, "system",
                    false, false, IconSpec.Kind.CAVE_SYSTEM
            ));
        }
        return out;
    }

    private int drawIconRow(
            PoseStack pose, String label, int x, int y, int maxW, int mouseX, int mouseY,
            List<IconSpec> icons, boolean clickEdits, boolean subDefault
    ) {
        int labelW = 0;
        if (label != null) {
            drawString(pose, this.font, label + ":", x, y + 4, 0xFFFFE080);
            labelW = this.font.width(label + ": ") + 4;
        }
        int ix = x + labelW;
        if (icons.isEmpty()) {
            drawString(pose, this.font, "(none)", ix, y + 4, 0xFF888888);
            return y + ICON + 6;
        }
        int right = x + maxW;
        for (IconSpec spec : icons) {
            if (ix + ICON > right) {
                drawString(pose, this.font, "...", ix, y + 4, 0xFFAAAAAA);
                break;
            }
            blitIcon(pose, spec.tex, ix, iySafe(y));
            this.iconHits.add(new IconHit(ix, y, ICON, ICON, List.of(spec.title, spec.detail)));
            if (this.editing && this.popup == Popup.NONE) {
                final String key = spec.key;
                final boolean sub = spec.sub || subDefault;
                if (clickEdits && spec.chanceEditable) {
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, () -> this.openChancePopup(key, sub)));
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 1, () -> {
                        this.setChance(key, sub, 0.0F);
                        this.status = "Removed " + key;
                    }));
                } else if (spec.kind == IconSpec.Kind.CLIMATE) {
                    Runnable rem = () -> this.removeClimate(key);
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, rem));
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 1, rem));
                } else if (spec.kind == IconSpec.Kind.ZONE) {
                    Runnable rem = () -> this.removeZone(key);
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, rem));
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 1, rem));
                } else if (spec.kind == IconSpec.Kind.CAVE_CLIMATE) {
                    Runnable rem = () -> this.removeCaveClimate(key);
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, rem));
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 1, rem));
                } else if (spec.kind == IconSpec.Kind.CAVE_SYSTEM) {
                    Runnable rem = () -> this.removeCaveSystem(key);
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, rem));
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 1, rem));
                } else if (spec.kind == IconSpec.Kind.CAVE_GEN) {
                    this.clickHits.add(new ClickHit(ix, y, ICON, ICON, 0, () -> this.openAddPopup(Popup.ADD_GENERATION)));
                }
            }
            if (mouseX >= ix && mouseX < ix + ICON && mouseY >= y && mouseY < y + ICON) {
                fill(pose, ix - 1, y - 1, ix + ICON + 1, y, 0xFFFFFFFF);
                fill(pose, ix - 1, y + ICON, ix + ICON + 1, y + ICON + 1, 0xFFFFFFFF);
            }
            ix += ICON + ICON_GAP;
        }
        return y + ICON + 6;
    }

    private static int iySafe(int y) {
        return y;
    }

    private static void blitIcon(PoseStack pose, ResourceLocation tex, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        blit(pose, x, y, 0, 0, ICON, ICON, ICON, ICON);
    }

    private static void blitTabIcon(PoseStack pose, ResourceLocation tex, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        blit(pose, x, y, 0, 0, TAB_TEX, TAB_TEX, TAB_TEX, TAB_TEX);
    }

    private static void blitPreview(PoseStack pose, ResourceLocation tex, int x, int y, int drawSize) {
        int s = BiomePreviewIcons.SIZE;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        blit(pose, x, y, drawSize, drawSize, 0.0F, 0.0F, s, s, s, s);
    }

    private void renderHoveredIconTooltip(PoseStack pose, int mouseX, int mouseY) {
        if (this.popup == Popup.CHANCE) {
            return;
        }
        for (IconHit hit : this.iconHits) {
            if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                List<Component> lines = new ArrayList<>();
                for (String s : hit.tip) {
                    lines.add(new TextComponent(s));
                }
                this.renderComponentTooltip(pose, lines, mouseX, mouseY);
                return;
            }
        }
    }

    private static String fmt(float v) {
        if (Math.abs(v - Math.rint(v)) < 0.001F) {
            return Integer.toString(Math.round(v));
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String trim(String s, int maxPxApprox) {
        int maxChars = Math.max(8, maxPxApprox / 6);
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, Math.max(1, maxChars - 1)) + "...";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.popup != Popup.NONE && this.popup != Popup.CHANCE) {
            this.popup = Popup.NONE;
            this.init();
            return true;
        }
        if (button == 0 || button == 1) {
            int y1 = 28;
            int y2 = y1 + TAB_H + 6;
            int y3 = y2 + TAB_ICON_H + 4;
            if (button == 0 && mouseX >= 0 && mouseX < TAB_W + 6) {
                if (mouseY >= y1 && mouseY < y1 + TAB_H) {
                    this.setTab(Tab.UNTESTED);
                    return true;
                }
                if (mouseY >= y2 && mouseY < y2 + TAB_ICON_H) {
                    this.setTab(Tab.SURFACE_BIOMES);
                    return true;
                }
                if (mouseY >= y3 && mouseY < y3 + TAB_ICON_H) {
                    this.setTab(Tab.CAVE_BIOMES);
                    return true;
                }
            }

            // Popup / edit click hits first
            for (int i = this.clickHits.size() - 1; i >= 0; i--) {
                ClickHit h = this.clickHits.get(i);
                if (h.button != -1 && h.button != button) {
                    continue;
                }
                if (mouseX >= h.x && mouseX < h.x + h.w && mouseY >= h.y && mouseY < h.y + h.h) {
                    h.action.run();
                    return true;
                }
            }

            if (button == 0 && this.isRulesTab() && !this.editing && this.popup == Popup.NONE) {
                int left = this.contentLeft();
                int listX = left + 4;
                int listY = 50;
                int listW = 210;
                int listH = this.height - 100;
                int rowH = 12;
                if (mouseY >= listY && mouseY < listY + listH) {
                    int i = (int) ((mouseY - listY) / rowH);
                    int idx = this.listScroll + i;
                    if (idx >= 0 && idx < this.filteredIds.size()) {
                        if (mouseX >= listX + listW + 2 && mouseX < listX + listW + 54) {
                            this.selectIndex(idx);
                            this.openEdit();
                            return true;
                        }
                        if (mouseX >= listX && mouseX < listX + listW) {
                            this.selectIndex(idx);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.popup == Popup.ADD_TERRAIN || this.popup == Popup.ADD_SUBTERRAIN
                || this.popup == Popup.ADD_CLIMATE || this.popup == Popup.ADD_ZONE
                || this.popup == Popup.ADD_CAVE_CLIMATE || this.popup == Popup.ADD_CAVE_SYSTEM
                || this.popup == Popup.ADD_GENERATION) {
            this.addScroll = Math.max(0, this.addScroll - (int) Math.signum(delta));
            return true;
        }
        if (this.isRulesTab() && !this.editing) {
            int left = this.contentLeft();
            int listX = left + 4;
            int listY = 50;
            int listW = 210;
            int listH = this.height - 100;
            if (mouseX >= listX && mouseX < listX + listW + 54 && mouseY >= listY && mouseY < listY + listH) {
                this.listScroll = Math.max(0, this.listScroll - (int) Math.signum(delta));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter commits chance tooltip.
        if (this.popup == Popup.CHANCE && (keyCode == 257 || keyCode == 335)) {
            this.commitChancePopup();
            return true;
        }
        // Esc: close popup → exit edit → close EGF
        if (keyCode == 256) {
            if (this.popup != Popup.NONE) {
                this.popup = Popup.NONE;
                this.init();
                return true;
            }
            if (this.editing) {
                this.exitEdit();
                return true;
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Small square icon button (reload / pencil). */
    private static final class IconButton extends Button {
        private final ResourceLocation texture;

        IconButton(int x, int y, int w, int h, ResourceLocation texture, String narr, OnPress onPress) {
            super(x, y, w, h, new TextComponent(narr), onPress);
            this.texture = texture;
        }

        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            int bg = this.isHoveredOrFocused() ? 0xFF555555 : 0xFF333333;
            fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, bg);
            fill(pose, this.x, this.y, this.x + this.width, this.y + 1, 0xFF888888);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, this.texture);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int ix = this.x + (this.width - ICON) / 2;
            int iy = this.y + (this.height - ICON) / 2;
            blit(pose, ix, iy, 0, 0, ICON, ICON, ICON, ICON);
        }
    }
}
