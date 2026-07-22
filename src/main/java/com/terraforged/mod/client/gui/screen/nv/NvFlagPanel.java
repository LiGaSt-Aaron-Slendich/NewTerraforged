package com.terraforged.mod.client.gui.screen.nv;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.biome.rules.BiomeRule;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleDefaults;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleIO;
import com.terraforged.mod.worldgen.biome.rules.BiomeRuleRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Experimental Generation Features:
 * <ul>
 *   <li>Side tab 1 — untested worldgen toggles</li>
 *   <li>Side tab 2 — Biome Rule Settings Environment (icon rows + full edit panel)</li>
 * </ul>
 */
public final class NvFlagPanel extends Screen {
    private enum Tab {
        UNTESTED,
        BIOME_RULES
    }

    private static final int TAB_W = 26;
    private static final int TAB_H = 72;
    private static final int ICON = BiomeRuleIcons.SIZE;
    private static final int ICON_GAP = 2;

    private final Screen parent;
    private Tab tab = Tab.UNTESTED;
    /** Browse vs full-parameter edit panel (tab 2). */
    private boolean editing;

    private EditBox biomeFilter;
    private Button editButton;
    private Button backButton;
    private Button saveButton;
    private Button toggleVolcanoButton;
    private Button reloadButton;

    private final List<ResourceLocation> biomeIds = new ArrayList<>();
    private final List<ResourceLocation> filteredIds = new ArrayList<>();
    private int listScroll;
    private int selectedIndex = -1;
    private BiomeRule selectedRule;
    private String status = "";

    /** Hitboxes for icon tooltips: [x,y,w,h] + tooltip lines. */
    private final List<IconHit> iconHits = new ArrayList<>();

    private record IconHit(int x, int y, int w, int h, List<String> tip) {}

    public NvFlagPanel(Screen parent) {
        super(new TextComponent("Experimental Generation Features"));
        this.parent = parent;
    }

    private int contentLeft() {
        return TAB_W + 6;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.iconHits.clear();

        this.addRenderableWidget(new Button(this.width / 2 - 60, this.height - 28, 120, 20, new TextComponent("Done"), b -> this.onClose()));

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
        this.init();
    }

    private void initUntestedTab() {
        int cx = (this.contentLeft() + this.width) / 2;
        int y = 56;
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
    }

    private void initBrowseMode() {
        this.reloadBiomeIds();
        int left = this.contentLeft();

        this.biomeFilter = new EditBox(this.font, left + 4, 36, 150, 18, new TextComponent("filter"));
        this.biomeFilter.setMaxLength(64);
        this.biomeFilter.setResponder(s -> {
            this.applyFilter();
            this.listScroll = 0;
        });
        this.addRenderableWidget(this.biomeFilter);

        this.reloadButton = this.addRenderableWidget(new Button(left + 158, 34, 64, 20, new TextComponent("Reload"), b -> {
            BiomeRuleRegistry.syncAtGameLaunch();
            this.reloadBiomeIds();
            this.status = "Reloaded (" + this.biomeIds.size() + ")";
            this.selectIndex(this.selectedIndex);
        }));

        this.editButton = this.addRenderableWidget(new Button(left + 226, 34, 90, 20, new TextComponent("Edit"), b -> this.openEdit()));
        this.editButton.visible = this.selectedRule != null;
        this.editButton.active = this.selectedRule != null;

        this.applyFilter();
        if (!this.filteredIds.isEmpty() && this.selectedIndex < 0) {
            this.selectIndex(0);
        }
        this.refreshEditVisibility();
    }

    private void initEditMode() {
        int left = this.contentLeft();
        int btnY = this.height - 52;

        this.backButton = this.addRenderableWidget(new Button(left + 4, btnY, 80, 20, new TextComponent("Back"), b -> {
            this.editing = false;
            this.init();
        }));
        this.saveButton = this.addRenderableWidget(new Button(left + 90, btnY, 120, 20, new TextComponent("Save rule"), b -> this.saveSelected()));
        this.toggleVolcanoButton = this.addRenderableWidget(new Button(
                left + 220, btnY, 200, 20,
                new TextComponent("Toggle near_active_volcano"),
                b -> this.toggleNearVolcano()
        ));
    }

    private void openEdit() {
        if (this.selectedRule == null) {
            this.status = "Select a biome first";
            return;
        }
        this.editing = true;
        this.init();
    }

    private void refreshEditVisibility() {
        if (this.editButton != null) {
            boolean on = this.selectedRule != null;
            this.editButton.visible = on;
            this.editButton.active = on;
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
        if (idx >= 0 && idx < this.filteredIds.size()) {
            this.selectedRule = BiomeRuleRegistry.get(this.filteredIds.get(idx));
        }
        this.refreshEditVisibility();
    }

    private void toggleNearVolcano() {
        if (this.selectedRule == null) {
            this.status = "No biome selected";
            return;
        }
        Map<String, BiomeRule.ZoneFlag> zones = new LinkedHashMap<>(this.selectedRule.zoneFlags);
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
        this.iconHits.clear();
        this.renderSideTabs(pose, mouseX, mouseY);

        if (this.tab == Tab.UNTESTED) {
            int mid = (this.contentLeft() + this.width) / 2;
            drawCenteredString(pose, this.font, this.title, mid, 18, 0xFFE080);
            drawCenteredString(pose, this.font, "Unstable / unfinished worldgen. Default OFF for releases.", mid, 32, 0xFFAAAAAA);
        } else if (this.editing) {
            this.renderEditPanel(pose, mouseX, mouseY);
        } else {
            this.renderBrowsePanel(pose, mouseX, mouseY);
        }

        super.render(pose, mouseX, mouseY, partialTick);
        this.renderHoveredIconTooltip(pose, mouseX, mouseY);
    }

    private void renderSideTabs(PoseStack pose, int mouseX, int mouseY) {
        int y1 = 40;
        int y2 = y1 + TAB_H + 4;
        this.drawSideTab(pose, 0, y1, Tab.UNTESTED, "1", "Untested", mouseX, mouseY);
        this.drawSideTab(pose, 0, y2, Tab.BIOME_RULES, "2", "Biome Rules", mouseX, mouseY);
    }

    private void drawSideTab(PoseStack pose, int x, int y, Tab which, String num, String title, int mouseX, int mouseY) {
        boolean active = this.tab == which;
        boolean hover = mouseX >= x && mouseX < x + TAB_W + (active ? 4 : 0) && mouseY >= y && mouseY < y + TAB_H;
        int w = active ? TAB_W + 4 : TAB_W;
        int bg = active ? 0xFF3A3A3A : (hover ? 0xFF2A2A2A : 0xFF1A1A1A);
        int edge = active ? 0xFFE0C060 : 0xFF666666;
        fill(pose, x, y, x + w, y + TAB_H, bg);
        fill(pose, x + w - 2, y, x + w, y + TAB_H, edge);
        // top/bottom lip so it reads as a tab
        fill(pose, x, y, x + w, y + 2, edge);
        fill(pose, x, y + TAB_H - 2, x + w, y + TAB_H, edge);

        drawCenteredString(pose, this.font, num, x + w / 2, y + 10, active ? 0xFFFFE080 : 0xFFCCCCCC);
        this.drawVerticalLabel(pose, title, x + (w - 8) / 2, y + 28, active ? 0xFFFFE080 : 0xFFAAAAAA);
    }

    /** Draw short label rotated as stacked characters (no GL rotate needed). */
    private void drawVerticalLabel(PoseStack pose, String text, int x, int y, int color) {
        int yy = y;
        for (int i = 0; i < text.length() && yy < y + 40; i++) {
            String ch = text.substring(i, i + 1);
            drawCenteredString(pose, this.font, ch, x + 4, yy, color);
            yy += 9;
        }
    }

    private void renderBrowsePanel(PoseStack pose, int mouseX, int mouseY) {
        int left = this.contentLeft();
        drawString(pose, this.font, "Biome Rule Settings Environment", left + 4, 18, 0xFFE080);

        int listX = left + 4;
        int listY = 58;
        int listW = 210;
        int listH = this.height - 110;
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

            // Hover / selection edit affordance strip
            if (sel || hover) {
                int bx = listX + listW + 2;
                fill(pose, bx, y, bx + 52, y + rowH, sel ? 0xAA226644 : 0xAA444444);
                drawString(pose, this.font, "Edit", bx + 8, y + 2, 0xFF88FF88);
            }
        }
        // Right: header + icon rows
        int hx = listX + listW + 60;
        int hy = 58;
        int hw = this.width - hx - 8;
        fill(pose, hx, hy, hx + 48, hy + 48, 0x66000000);
        drawCenteredString(pose, this.font, "img", hx + 24, hy + 20, 0xFF666666);

        String name = this.selectedRule != null ? this.selectedRule.biome : "(select a biome)";
        drawString(pose, this.font, trim(name, hw - 60), hx + 56, hy + 6, 0xFFFFFFFF);
        String desc = this.selectedRule == null
                ? "Pick a biome, then Edit. Icons show terrains / climate; hover for name + chance."
                : "auto=" + this.selectedRule.autoGenerated + "  slope=" + this.selectedRule.canBeOnSlope;
        drawString(pose, this.font, trim(desc, hw - 60), hx + 56, hy + 22, 0xFFCCCCCC);

        int rowY = hy + 56;
        rowY = this.drawIconRow(pose, "Terrains", hx, rowY, hw, mouseX, mouseY, this.terrainIcons());
        rowY = this.drawIconRow(pose, "Subterrains", hx, rowY, hw, mouseX, mouseY, this.subterrainIcons());
        rowY = this.drawIconRow(pose, "Climate", hx, rowY, hw, mouseX, mouseY, this.climateIcons());
        rowY = this.drawIconRow(pose, "Zone flags", hx, rowY, hw, mouseX, mouseY, this.zoneIcons());
        if (this.selectedRule != null && this.selectedRule.canBeOnSlope) {
            this.drawIconRow(pose, "Flags", hx, rowY, hw, mouseX, mouseY, List.of(
                    new IconSpec(BiomeRuleIcons.slope(), "can_be_on_slope", "allowed on steep slopes")
            ));
        }

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, left + 4, this.height - 42, 0xFFAAFFAA);
        }
        drawString(pose, this.font, "Defaults: " + (BiomeRuleDefaults.isEnabled() ? "ON" : "OFF"), left + 4, this.height - 54, 0xFF888888);
    }

    private void renderEditPanel(PoseStack pose, int mouseX, int mouseY) {
        int left = this.contentLeft();
        drawString(pose, this.font, "Edit menu — full parameters", left + 4, 18, 0xFFE080);
        String name = this.selectedRule != null ? this.selectedRule.biome : "(none)";
        drawString(pose, this.font, name, left + 4, 34, 0xFFFFFFFF);

        int panelX = left + 4;
        int panelY = 52;
        int panelW = this.width - panelX - 8;
        int panelH = this.height - panelY - 60;
        fill(pose, panelX, panelY, panelX + panelW, panelY + panelH, 0x88000000);

        if (this.selectedRule == null) {
            drawString(pose, this.font, "No rule selected.", panelX + 8, panelY + 8, 0xFFFF8888);
            return;
        }

        int y = panelY + 8;
        drawString(pose, this.font, "auto=" + this.selectedRule.autoGenerated + "  slope=" + this.selectedRule.canBeOnSlope, panelX + 8, y, 0xFFCCCCCC);
        y += 16;

        drawString(pose, this.font, "Terrains (hover icon = name + chance):", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 16, mouseX, mouseY, this.terrainIcons()) + 4;

        // Full text list — fits because edit panel is full width
        for (Map.Entry<String, Float> e : this.selectedRule.terrains.entrySet()) {
            drawString(pose, this.font, "  " + e.getKey() + " = " + fmt(e.getValue()), panelX + 8, y, 0xFFFFFFFF);
            y += 11;
            if (y > panelY + panelH - 80) {
                drawString(pose, this.font, "  …", panelX + 8, y, 0xFF888888);
                y += 11;
                break;
            }
        }
        y += 6;
        drawString(pose, this.font, "Subterrains:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 16, mouseX, mouseY, this.subterrainIcons()) + 4;
        if (this.selectedRule.subterrains.isEmpty()) {
            drawString(pose, this.font, "  (any)", panelX + 8, y, 0xFFAAAAAA);
            y += 11;
        } else {
            for (Map.Entry<String, Float> e : this.selectedRule.subterrains.entrySet()) {
                drawString(pose, this.font, "  " + e.getKey() + " = " + fmt(e.getValue()), panelX + 8, y, 0xFFCCCCFF);
                y += 11;
                if (y > panelY + panelH - 50) {
                    break;
                }
            }
        }
        y += 6;
        drawString(pose, this.font, "Climate:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        y = this.drawIconRow(pose, null, panelX + 8, y, panelW - 16, mouseX, mouseY, this.climateIcons()) + 4;
        y += 4;
        drawString(pose, this.font, "Zone flags:", panelX + 8, y, 0xFFFFE080);
        y += 12;
        this.drawIconRow(pose, null, panelX + 8, y, panelW - 16, mouseX, mouseY, this.zoneIcons());

        if (!this.status.isEmpty()) {
            drawString(pose, this.font, this.status, left + 4, this.height - 42, 0xFFAAFFAA);
        }
    }

    private record IconSpec(ResourceLocation tex, String title, String detail) {}

    private List<IconSpec> terrainIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (Map.Entry<String, Float> e : this.selectedRule.terrains.entrySet()) {
            out.add(new IconSpec(BiomeRuleIcons.terrain(e.getKey()), e.getKey(), "chance " + fmt(e.getValue())));
        }
        return out;
    }

    private List<IconSpec> subterrainIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (Map.Entry<String, Float> e : this.selectedRule.subterrains.entrySet()) {
            out.add(new IconSpec(BiomeRuleIcons.terrain(e.getKey()), e.getKey(), "chance " + fmt(e.getValue())));
        }
        return out;
    }

    private List<IconSpec> climateIcons() {
        List<IconSpec> out = new ArrayList<>();
        if (this.selectedRule == null) {
            return out;
        }
        for (String tag : this.selectedRule.climateTags) {
            out.add(new IconSpec(BiomeRuleIcons.climate(tag), tag, "climate tag"));
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
                    "radius " + fmt(z.radiusBlocks()) + "  chance " + fmt(z.chance())
            ));
        }
        return out;
    }

    /** One horizontal row of icons; returns y below the row. */
    private int drawIconRow(PoseStack pose, String label, int x, int y, int maxW, int mouseX, int mouseY, List<IconSpec> icons) {
        int labelW = 0;
        if (label != null) {
            drawString(pose, this.font, label + ":", x, y + 4, 0xFFFFE080);
            labelW = this.font.width(label + ": ") + 4;
        }
        int ix = x + labelW;
        int iy = y;
        if (icons.isEmpty()) {
            drawString(pose, this.font, "(none)", ix, y + 4, 0xFF888888);
            return y + ICON + 6;
        }
        int right = x + maxW;
        for (IconSpec spec : icons) {
            if (ix + ICON > right) {
                // wrap only if absolutely needed — prefer single row clip with "…"
                drawString(pose, this.font, "…", ix, iy + 4, 0xFFAAAAAA);
                break;
            }
            blitIcon(pose, spec.tex, ix, iy);
            this.iconHits.add(new IconHit(ix, iy, ICON, ICON, List.of(spec.title, spec.detail)));
            // subtle hover frame
            if (mouseX >= ix && mouseX < ix + ICON && mouseY >= iy && mouseY < iy + ICON) {
                fill(pose, ix - 1, iy - 1, ix + ICON + 1, iy, 0xFFFFFFFF);
                fill(pose, ix - 1, iy + ICON, ix + ICON + 1, iy + ICON + 1, 0xFFFFFFFF);
            }
            ix += ICON + ICON_GAP;
        }
        return y + ICON + 6;
    }

    private static void blitIcon(PoseStack pose, ResourceLocation tex, int x, int y) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        blit(pose, x, y, 0, 0, ICON, ICON, ICON, ICON);
    }

    private void renderHoveredIconTooltip(PoseStack pose, int mouseX, int mouseY) {
        for (IconHit hit : this.iconHits) {
            if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
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
        return s.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Side tabs
            int y1 = 40;
            int y2 = y1 + TAB_H + 4;
            if (mouseX >= 0 && mouseX < TAB_W + 6) {
                if (mouseY >= y1 && mouseY < y1 + TAB_H) {
                    this.setTab(Tab.UNTESTED);
                    return true;
                }
                if (mouseY >= y2 && mouseY < y2 + TAB_H) {
                    this.setTab(Tab.BIOME_RULES);
                    return true;
                }
            }

            if (this.tab == Tab.BIOME_RULES && !this.editing) {
                int left = this.contentLeft();
                int listX = left + 4;
                int listY = 58;
                int listW = 210;
                int listH = this.height - 110;
                int rowH = 12;
                if (mouseY >= listY && mouseY < listY + listH) {
                    int i = (int) ((mouseY - listY) / rowH);
                    int idx = this.listScroll + i;
                    if (idx >= 0 && idx < this.filteredIds.size()) {
                        // click Edit strip
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
        if (this.tab == Tab.BIOME_RULES && !this.editing) {
            int left = this.contentLeft();
            int listX = left + 4;
            int listY = 58;
            int listW = 210;
            int listH = this.height - 110;
            if (mouseX >= listX && mouseX < listX + listW + 54 && mouseY >= listY && mouseY < listY + listH) {
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
