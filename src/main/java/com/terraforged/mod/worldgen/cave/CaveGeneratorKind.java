package com.terraforged.mod.worldgen.cave;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.minecraft.resources.ResourceLocation;

/** Stat role of a mega/giga generator — used for debug-map icons. */
public enum CaveGeneratorKind {
    HEAT(new Color(255, 120, 32), new Color(255, 220, 64)),
    COLD(new Color(180, 220, 255), new Color(240, 248, 255)),
    MOISTURE(new Color(48, 140, 220), new Color(120, 200, 255)),
    FERTILITY(new Color(48, 160, 56), new Color(120, 220, 72));

    private final Color primary;
    private final Color accent;

    CaveGeneratorKind(Color primary, Color accent) {
        this.primary = primary;
        this.accent = accent;
    }

    public static CaveGeneratorKind resolve(CaveMegaGigaLayout.GeneratorNode node, CaveClimateType climate) {
        if (node == null || node.biome() == null) {
            return FERTILITY;
        }
        ResourceLocation id = node.biome().biome();
        if (CaveBiomeClimateAffinity.isColdGenerator(id)) {
            return COLD;
        }
        if (CaveBiomeClimateAffinity.isHeatGenerator(id) || CaveBiomeClimateAffinity.isSpringGenerator(id)) {
            return HEAT;
        }
        CaveStatVector local = node.biome().stats().local();
        CaveStatVector global = node.biome().stats().globalForClimate(climate);
        float temp = Math.abs(local.temperature()) + Math.abs(global.temperature()) * 0.75f;
        float moist = Math.abs(local.moisture()) + Math.abs(global.moisture()) * 0.75f;
        float fert = Math.abs(local.fertility()) + Math.abs(global.fertility()) * 0.75f;
        if (temp >= moist && temp >= fert) {
            return local.temperature() + global.temperature() < 0.0f ? COLD : HEAT;
        }
        if (moist >= fert) {
            return MOISTURE;
        }
        return FERTILITY;
    }

    public void drawIcon(BufferedImage img, int cx, int cy) {
        switch (this) {
            case HEAT -> CaveGeneratorKind.drawFire(img, cx, cy, this.primary, this.accent);
            case COLD -> CaveGeneratorKind.drawSnowflake(img, cx, cy, this.primary, this.accent);
            case MOISTURE -> CaveGeneratorKind.drawDroplet(img, cx, cy, this.primary, this.accent);
            case FERTILITY -> CaveGeneratorKind.drawGrass(img, cx, cy, this.primary, this.accent);
        }
    }

    private static void plot(BufferedImage img, int x, int y, Color color) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
            return;
        }
        img.setRGB(x, y, color.getRGB());
    }

    private static void drawFire(BufferedImage img, int cx, int cy, Color core, Color tip) {
        CaveGeneratorKind.plot(img, cx, cy + 2, core);
        CaveGeneratorKind.plot(img, cx - 1, cy + 1, core);
        CaveGeneratorKind.plot(img, cx + 1, cy + 1, core);
        CaveGeneratorKind.plot(img, cx, cy, tip);
        CaveGeneratorKind.plot(img, cx - 1, cy, core);
        CaveGeneratorKind.plot(img, cx + 1, cy, core);
        CaveGeneratorKind.plot(img, cx, cy - 1, tip);
        CaveGeneratorKind.plot(img, cx, cy - 2, tip);
    }

    private static void drawSnowflake(BufferedImage img, int cx, int cy, Color line, Color highlight) {
        for (int d = -2; d <= 2; ++d) {
            CaveGeneratorKind.plot(img, cx + d, cy, line);
            CaveGeneratorKind.plot(img, cx, cy + d, line);
            CaveGeneratorKind.plot(img, cx + d, cy + d, highlight);
            CaveGeneratorKind.plot(img, cx + d, cy - d, highlight);
        }
        CaveGeneratorKind.plot(img, cx, cy, Color.WHITE);
    }

    private static void drawDroplet(BufferedImage img, int cx, int cy, Color body, Color shine) {
        CaveGeneratorKind.plot(img, cx, cy - 2, shine);
        CaveGeneratorKind.plot(img, cx - 1, cy - 1, body);
        CaveGeneratorKind.plot(img, cx, cy - 1, body);
        CaveGeneratorKind.plot(img, cx + 1, cy - 1, body);
        CaveGeneratorKind.plot(img, cx - 1, cy, body);
        CaveGeneratorKind.plot(img, cx, cy, body);
        CaveGeneratorKind.plot(img, cx + 1, cy, body);
        CaveGeneratorKind.plot(img, cx - 1, cy + 1, body);
        CaveGeneratorKind.plot(img, cx, cy + 1, body);
        CaveGeneratorKind.plot(img, cx + 1, cy + 1, body);
        CaveGeneratorKind.plot(img, cx, cy + 2, body);
    }

    private static void drawGrass(BufferedImage img, int cx, int cy, Color blade, Color tip) {
        CaveGeneratorKind.plot(img, cx - 2, cy + 1, blade);
        CaveGeneratorKind.plot(img, cx - 2, cy, tip);
        CaveGeneratorKind.plot(img, cx - 1, cy + 2, blade);
        CaveGeneratorKind.plot(img, cx - 1, cy + 1, tip);
        CaveGeneratorKind.plot(img, cx - 1, cy, tip);
        CaveGeneratorKind.plot(img, cx, cy + 2, blade);
        CaveGeneratorKind.plot(img, cx, cy + 1, tip);
        CaveGeneratorKind.plot(img, cx + 1, cy + 2, blade);
        CaveGeneratorKind.plot(img, cx + 1, cy + 1, tip);
        CaveGeneratorKind.plot(img, cx + 1, cy, tip);
        CaveGeneratorKind.plot(img, cx + 2, cy + 1, blade);
        CaveGeneratorKind.plot(img, cx + 2, cy, tip);
    }
}
