package com.terraforged.mod.client.gui.screen.nv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleIO;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Experimental Generation Features — tabbed:
 * <ul>
 *   <li>Tab 1: untested EGF toggles (Archipelago…)</li>
 *   <li>Tab 2: Biome Rule Settings Environment (editor scaffold)</li>
 * </ul>
 */
public final class NvFlagPanel extends Screen {
    private enum Tab {
        UNTESTED,
        BIOME_RULES
    }

    private final Screen parent;
    private Tab tab = Tab.UNTESTED;

    private Button tabUntested;
    private Button tabBiomeRules;
    private final List<Button> untestedWidgets = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> biomeWidgets = new ArrayList<>();

    private EditBox biomeFilter;
    private final List<ResourceLocation> biomeIds = new ArrayList<>();
    private final List<ResourceLocation> filteredIds = new ArrayList<>();
    private int listScroll;
    private int selectedIndex = -1;
    private BiomeRule selectedRule;
    private String status = "";

    public NvFlagPanel(Screen parent) {
        super(new TextComponent("Experimental Generation Features"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.untestedWidgets.clear();
        this.biomeWidgets.clear();

        int tabW = 140;
        this.tabUntested = this.addRenderableWidget(new Button(
                8, 8, tabW, 20,
                new TextComponent(this.tab == Tab.UNTESTED ? "[ Tab 1: Untested ]" : "Tab 1: Untested"),
                b -> this.setTab(Tab.UNTESTED)
        ));
        this.tabBiomeRules = this.addRenderableWidget(new Button(
                8 + tabW + 4, 8, tabW + 40, 20,
                new TextComponent(this.tab == Tab.BIOME_RULES ? "[ Tab 2: Biome Rules ]" : "Tab 2: Biome Rules"),
                b -> this.setTab(Tab.BIOME_RULES)
        ));

        this.addRenderableWidget(new Button(this.width / 2 - 60, this.height - 28, 120, 20, new TextComponent("Done"), b -> this.onClose()));

        if (this.tab == Tab.UNTESTED) {
            this.initUntestedTab();
        } else {
            this.initBiomeRulesTab();
        }
    }

    private void setTab(Tab next) {
        this.tab = next;
        this.init();
    }

    private void initUntestedTab() {
        int cx = this.width / 2;
        int y = 56;
        Button arch = this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Archipelago", TFNoiseVariantFlags.archipelagoEnabled()),
                b -> {
                    boolean next = !TFNoiseVariantFlags.archipelagoEnabled();
                    TFNoiseVariantFlags.setArchipelago(next);
                    b.setMessage(label("Archipelago", next));
                }
        ));
        this.untestedWidgets.add(arch);
        y += 28;
        Button scattered = this.addRenderableWidget(new Button(
                cx - 140, y, 280, 20,
                label("Scattered Archipelago", TFNoiseVariantFlags.scatteredArchipelagoEnabled()),
                b -> {
                    boolean next = !TFNoiseVariantFlags.scatteredArchipelagoEnabled();
                    TFNoiseVariantFlags.setScatteredArchipelago(next);
                    b.setMessage(label("Scattered Archipelago", next));
                }
        ));
        this.untestedWidgets.add(scattered);
    }

    private void initBiomeRulesTab() {
        this.reloadBiomeIds();
        this.biomeFilter = new EditBox(this.font, 10, 36, 160, 18, new TextComponent("filter"));
        this.biomeFilter.setMaxLength(64);
        this.biomeFilter.setResponder(s -> {
            this.applyFilter();
            this.listScroll = 0;
        });
        this.addRenderableWidget(this.biomeFilter);
        this.biomeWidgets.add(this.biomeFilter);

        Button reload = this.addRenderableWidget(new Button(174, 34, 70, 20, new TextComponent("Reload"), b -> {
            BiomeRuleRegistry.syncAtGameLaunch();
            this.reloadBiomeIds();
            this.status = "Reloaded rules (" + this.biomeIds.size() + ")";
            this.selectIndex(this.selectedIndex);
        }));
        this.biomeWidgets.add(reload);

        Button toggleVolcano = this.addRenderableWidget(new Button(
                this.width - 210, this.height - 52, 200, 20,
                new TextComponent("Toggle near_active_volcano"),
                b -> this.toggleNearVolcano()
        ));
        this.biomeWidgets.add(toggleVolcano);

        Button save = this.addRenderableWidget(new Button(
                this.width - 210, this.height - 76, 200, 20,
                new TextComponent("Save selected rule"),
                b -> this.saveSelected()
        ));
        this.biomeWidgets.add(save);

        this.applyFilter();
        if (!this.filteredIds.isEmpty() && this.selectedIndex < 0) {
            this.selectIndex(0);
        }
    }

    private void reloadBiomeIds() {
        this.biomeIds.clear();
        this.biomeIds.addAll(BiomeRuleRegistry.snapshot().keySet());
        this.biomeIds.sort(Comparator.comparing(ResourceLocation::toString));
        this.applyFilter();
    }

    private void applyFilter() {
        String q = this.biomeFilter == null ? "" : this.biomeFilter.getValue().trim().toLowerCase(Locale.ROOT);
        this.filteredIds.clear();
        for (ResourceLocation id : this.biomeIds) {
            if (q.isEmpty() || id.toString().contains(q)) {
                this.filteredIds.add(id);
            }
        }
        if (this.selectedIndex >= this.filteredIds.size()) {
            this.selectedIndex = this.filteredIds.isEmpty() ? -1 : 0;
        }
        this.selectIndex(this.selectedIndex);
    }

    private void selectIndex(int idx) {
        this.selectedIndex = idx;
        this.selectedRule = null;
        if (idx < 0 || idx >= this.filteredIds.size()) {
            return;
        }
        this.selectedRule = BiomeRuleRegistry.get(this.filteredIds.get(idx));
    }

    private void toggleNearVolcano() {
        if (this.selectedRule == null || this.selectedIndex < 0 || this.selectedIndex >= this.filteredIds.size()) {
            this.status = "No biome selected";
            return;
        }
        ResourceLocation id = this.filteredIds.get(this.selectedIndex);
        Map<String, BiomeRule.ZoneFlag> zones = new java.util.LinkedHashMap<>(this.selectedRule.zoneFlags);
        if (this.selectedRule.requiresZone(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO)) {
            zones.remove(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO);
            this.status = "Removed near_active_volcano";
        } else {
            zones.put(BiomeRule.ZONE_NEAR_ACTIVE_VOLCANO, BiomeRule.ZoneFlag.enabled(96.0F, 1.0F));
            this.status = "Added near_active_volcano (radius 96)";
        }
        this.selectedRule = new BiomeRule(
                this.selectedRule.biome,
                this.selectedRule.canBeOnSlope,
                this.selectedRule.climateTags,
                this.selectedRule.terrains,
                this.selectedRule.subterrains,
                zones,
                false
        );
    }

    private void saveSelected() {
        if (this.selectedRule == null || this.selectedIndex < 0 || this.selectedIndex >= this.filteredIds.size()) {
            this.status = "Nothing to save";
            return;
        }
        ResourceLocation id = this.filteredIds.get(this.selectedIndex);
        Path file = BiomeRuleRegistry.biomesRoot().resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        try {
            Files.createDirectories(file.getParent());
            BiomeRuleIO.write(file, this.selectedRule);
            BiomeRuleRegistry.syncAtGameLaunch();
            this.reloadBiomeIds();
            // re-select same id
            for (int i = 0; i < this.filteredIds.size(); i++) {
                if (this.filteredIds.get(i).equals(id)) {
                    this.selectIndex(i);
                    break;
                }
            }
            this.status = "Saved " + id;
        } catch (Exception e) {
            this.status = "Save failed: " + e.getMessage();
        }
    }

    private static TextComponent label(String name, boolean on) {
        return new TextComponent(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        if (this.tab == Tab.UNTESTED) {
            drawCenteredString(pose, this.font, this.title, this.width / 2, 34, 0xFFE080);
            drawCenteredString(pose, this.font, "Unstable / unfinished worldgen. Default OFF for releases.", this.width / 2, 46, 0xFFAAAAAA);
        } else {
            this.renderBiomeRules(pose, mouseX, mouseY);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private void renderBiomeRules(PoseStack pose, int mouseX, int mouseY) {
        drawString(pose, this.font, "Biome Rule Settings Environment", 10, 22, 0xFFE080);

        // Header: image placeholder + name + description
        int headerX = 260;
        int headerY = 34;
        fill(pose, headerX, headerY, headerX + 48, headerY + 48, 0x66000000);
        drawCenteredString(pose, this.font, "img", headerX + 24, headerY + 20, 0xFF666666);

        String name = this.selectedRule != null ? this.selectedRule.biome : "(no selection)";
        drawString(pose, this.font, "Name: " + name, headerX + 56, headerY + 4, 0xFFFFFFFF);
        String desc = this.selectedRule == null
                ? "Select a biome from the list. Defaults library is OFF — editing player config rules."
                : "auto=" + this.selectedRule.autoGenerated
                + "  slope=" + this.selectedRule.canBeOnSlope
                + "  climates=" + String.join(",", this.selectedRule.climateTags);
        drawString(pose, this.font, trim(desc, this.width - headerX - 70), headerX + 56, headerY + 18, 0xFFCCCCCC);
        if (this.selectedRule != null) {
            String zones = this.selectedRule.zoneFlags.isEmpty()
                    ? "zone_flags: (none)"
                    : "zone_flags: " + String.join(",", this.selectedRule.zoneFlags.keySet());
            drawString(pose, this.font, trim(zones, this.width - headerX - 70), headerX + 56, headerY + 32, 0xFFAADDFF);
        }

        // Category rows
        int rowY = 90;
        drawString(pose, this.font, "Allowed terrains:", 260, rowY, 0xFFFFE080);
        String terrains = this.selectedRule == null ? "-" : String.join(", ", this.selectedRule.terrains.keySet());
        drawString(pose, this.font, trim(terrains, this.width - 280), 370, rowY, 0xFFFFFFFF);
        rowY += 14;
        drawString(pose, this.font, "Climate tags:", 260, rowY, 0xFFFFE080);
        String climates = this.selectedRule == null ? "-" : (this.selectedRule.climateTags.isEmpty() ? "(empty)" : String.join(", ", this.selectedRule.climateTags));
        drawString(pose, this.font, trim(climates, this.width - 280), 370, rowY, 0xFFFFFFFF);

        // Left list
        int listX = 10;
        int listY = 58;
        int listW = 230;
        int listH = this.height - 100;
        fill(pose, listX, listY, listX + listW, listY + listH, 0x88000000);
        drawString(pose, this.font, "Biome list", listX + 4, listY - 12, 0xFFDDDDDD);

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
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + rowH;
            if (sel) {
                fill(pose, listX, y, listX + listW, y + rowH, 0xAA335577);
            } else if (hover) {
                fill(pose, listX, y, listX + listW, y + rowH, 0x55333333);
            }
            String line = this.filteredIds.get(idx).toString();
            drawString(pose, this.font, trim(line, listW - 8), listX + 4, y + 2, sel ? 0xFFFFFFAA : 0xFFEEEEEE);
            // Hover edit hint strip
            if (hover) {
                drawString(pose, this.font, ">", listX + listW - 10, y + 2, 0xFF88FF88);
            }
        }

        // Right edit menu
        int editX = 260;
        int editY = 120;
        int editW = this.width - editX - 10;
        int editH = this.height - editY - 90;
        fill(pose, editX, editY, editX + editW, editY + editH, 0x66000000);
        drawString(pose, this.font, "Edit menu", editX + 6, editY + 6, 0xFFFFE080);
        drawString(pose, this.font, "Short info / terrains / zone flags — use buttons below to edit.", editX + 6, editY + 22, 0xFFBBBBBB);
        if (this.selectedRule != null) {
            int ly = editY + 40;
            drawString(pose, this.font, "Terrains:", editX + 6, ly, 0xFF88FF88);
            ly += 12;
            for (Map.Entry<String, Float> e : this.selectedRule.terrains.entrySet()) {
                drawString(pose, this.font, "  " + e.getKey() + " = " + e.getValue(), editX + 6, ly, 0xFFFFFFFF);
                ly += 11;
                if (ly > editY + editH - 40) {
                    drawString(pose, this.font, "  …", editX + 6, ly, 0xFF888888);
                    break;
                }
            }
            ly += 6;
            drawString(pose, this.font, "Subterrains: " + (this.selectedRule.subterrains.isEmpty() ? "(any)" : String.join(", ", this.selectedRule.subterrains.keySet())), editX + 6, ly, 0xFFCCCCFF);
        }

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, 10, this.height - 40, 0xFFAAFFAA);
        }
        drawString(pose, this.font, "Defaults library: " + (com.terraforged.mod.worldgen.biome.rules.BiomeRuleDefaults.isEnabled() ? "ON" : "OFF (autogen emergency)"), 10, this.height - 52, 0xFFAAAAAA);
    }

    private static String trim(String s, int maxPxApprox) {
        int maxChars = Math.max(8, maxPxApprox / 6);
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.tab == Tab.BIOME_RULES && button == 0) {
            int listX = 10;
            int listY = 58;
            int listW = 230;
            int listH = this.height - 100;
            int rowH = 12;
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                int i = (int) ((mouseY - listY) / rowH);
                int idx = this.listScroll + i;
                if (idx >= 0 && idx < this.filteredIds.size()) {
                    this.selectIndex(idx);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.tab == Tab.BIOME_RULES) {
            int listX = 10;
            int listY = 58;
            int listW = 230;
            int listH = this.height - 100;
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                this.listScroll = Math.max(0, this.listScroll - (int) Math.signum(delta));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
