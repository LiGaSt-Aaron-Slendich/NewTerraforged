package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

/**
 * Debug cartography layers for mega/giga cave systems (stat heatmaps only on TF118).
 * Feature-layer maps stay deferred until CaveFeature* KEEP is wired.
 */
public final class CaveDebugMaps {
    private static final Color OUTSIDE = new Color(20, 20, 24);

    private CaveDebugMaps() {
    }

    public record ExportResult(List<Path> mapFiles, List<Path> legendFiles) {
    }

    public static ExportResult exportAll(
        Path dir,
        String base,
        int seed,
        CaveMegaGigaLayout layout,
        boolean[][] inFootprint,
        int originX,
        int originZ,
        int step,
        Registry<Biome> biomeRegistry
    ) throws IOException {
        ArrayList<Path> maps = new ArrayList<>();
        ArrayList<Path> legends = new ArrayList<>();
        int gridH = inFootprint.length;
        int gridW = inFootprint[0].length;
        float[][] temperature = new float[gridH][gridW];
        float[][] moisture = new float[gridH][gridW];
        float[][] fertility = new float[gridH][gridW];
        for (int gz = 0; gz < gridH; ++gz) {
            for (int gx = 0; gx < gridW; ++gx) {
                if (!inFootprint[gz][gx]) {
                    continue;
                }
                int wx = originX + gx * step + step / 2;
                int wz = originZ + gz * step + step / 2;
                CaveStatVector stats = layout.statsAt(wx, wz);
                temperature[gz][gx] = stats.temperature();
                moisture[gz][gx] = stats.moisture();
                fertility[gz][gx] = stats.fertility();
            }
        }
        Path tempFile = dir.resolve(base + "_temperature.png");
        CaveDebugMaps.writeStatHeatmap(tempFile, inFootprint, temperature, StatLayer.TEMPERATURE, layout, originX, originZ, step);
        maps.add(tempFile);
        Path moistFile = dir.resolve(base + "_moisture.png");
        CaveDebugMaps.writeStatHeatmap(moistFile, inFootprint, moisture, StatLayer.MOISTURE, layout, originX, originZ, step);
        maps.add(moistFile);
        Path fertFile = dir.resolve(base + "_fertility.png");
        CaveDebugMaps.writeStatHeatmap(fertFile, inFootprint, fertility, StatLayer.FERTILITY, layout, originX, originZ, step);
        maps.add(fertFile);
        Path genLegend = dir.resolve(base + "_generators_legend.txt");
        Files.writeString(genLegend, CaveDebugMaps.buildGeneratorLegend(layout), StandardCharsets.UTF_8);
        legends.add(genLegend);
        Path note = dir.resolve(base + "_deferred.txt");
        Files.writeString(
            note,
            "# Vegetation/feature map layers deferred until CaveFeature* KEEP compiles on TF118 carver.\n",
            StandardCharsets.UTF_8
        );
        legends.add(note);
        return new ExportResult(maps, legends);
    }

    private static void writeStatHeatmap(
        Path path,
        boolean[][] inFootprint,
        float[][] values,
        StatLayer layer,
        CaveMegaGigaLayout layout,
        int originX,
        int originZ,
        int step
    ) throws IOException {
        int h = values.length;
        int w = values[0].length;
        int scale = Math.max(1, Math.min(2, 1024 / Math.max(w, h)));
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        for (int gy = 0; gy < h; ++gy) {
            for (int gx = 0; gx < w; ++gx) {
                Color color = !inFootprint[gy][gx] ? OUTSIDE : layer.color(values[gy][gx]);
                CaveDebugMaps.fillPixel(img, gx, gy, scale, color);
            }
        }
        CaveDebugMaps.drawGeneratorMarkers(img, inFootprint, layout, originX, originZ, step, scale);
        ImageIO.write(img, "png", path.toFile());
    }

    private static void drawGeneratorMarkers(
        BufferedImage img,
        boolean[][] inFootprint,
        CaveMegaGigaLayout layout,
        int originX,
        int originZ,
        int step,
        int scale
    ) {
        if (layout == null) {
            return;
        }
        Color ring = new Color(255, 255, 255);
        int h = inFootprint.length;
        int w = inFootprint[0].length;
        CaveClimateType climate = layout.climateType();
        for (CaveMegaGigaLayout.GeneratorNode generator : layout.generators()) {
            int gx = (int)Math.floor((generator.x() - (float)originX - (float)step * 0.5f) / (float)step);
            int gz = (int)Math.floor((generator.z() - (float)originZ - (float)step * 0.5f) / (float)step);
            if (gx < 0 || gz < 0 || gx >= w || gz >= h) {
                continue;
            }
            int cx = gx * scale + scale / 2;
            int cy = gz * scale + scale / 2;
            CaveGeneratorKind kind = CaveGeneratorKind.resolve(generator, climate);
            CaveDebugMaps.fillMarker(img, gx, gz, scale, ring, 3);
            kind.drawIcon(img, cx, cy);
        }
    }

    private static void fillMarker(BufferedImage img, int gx, int gy, int scale, Color color, int radius) {
        int cx = gx * scale + scale / 2;
        int cy = gy * scale + scale / 2;
        for (int dy = -radius; dy <= radius; ++dy) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int px = cx + dx;
                int py = cy + dy;
                if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) {
                    continue;
                }
                if (dx * dx + dy * dy <= radius * radius + 1) {
                    img.setRGB(px, py, color.getRGB());
                }
            }
        }
    }

    private static void fillPixel(BufferedImage img, int gx, int gy, int scale, Color color) {
        for (int py = 0; py < scale; ++py) {
            for (int px = 0; px < scale; ++px) {
                img.setRGB(gx * scale + px, gy * scale + py, color.getRGB());
            }
        }
    }

    private static String buildGeneratorLegend(CaveMegaGigaLayout layout) {
        if (layout == null || layout.generators().isEmpty()) {
            return "# No stat generators in this layout\n";
        }
        StringBuilder sb = new StringBuilder("# Generator icons on temperature/moisture/fertility maps\n");
        sb.append("# HEAT = fire | COLD = snowflake | MOISTURE = droplet | FERTILITY = grass\n\n");
        CaveClimateType climate = layout.climateType();
        for (CaveMegaGigaLayout.GeneratorNode generator : layout.generators()) {
            CaveGeneratorKind kind = CaveGeneratorKind.resolve(generator, climate);
            sb.append(
                String.format(
                    Locale.ROOT,
                    "%s @ %.0f, %.0f -> %s (%s)%n",
                    generator.biome().biome(),
                    generator.x(),
                    generator.z(),
                    kind.name(),
                    generator.biome().biome()
                )
            );
        }
        return sb.toString();
    }

    private enum StatLayer {
        TEMPERATURE {
            @Override
            Color color(float value) {
                float t = (NoiseUtil.clamp(value, -10.0f, 10.0f) + 10.0f) / 20.0f;
                return CaveDebugMaps.lerpColor(new Color(32, 64, 200), new Color(220, 48, 32), t);
            }
        },
        MOISTURE {
            @Override
            Color color(float value) {
                float t = (NoiseUtil.clamp(value, -10.0f, 10.0f) + 10.0f) / 20.0f;
                return CaveDebugMaps.lerpColor(new Color(196, 168, 64), new Color(32, 128, 196), t);
            }
        },
        FERTILITY {
            @Override
            Color color(float value) {
                float t = (NoiseUtil.clamp(value, -10.0f, 10.0f) + 10.0f) / 20.0f;
                return CaveDebugMaps.lerpColor(new Color(96, 72, 48), new Color(48, 160, 64), t);
            }
        };

        abstract Color color(float value);
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = NoiseUtil.clamp(t, 0.0f, 1.0f);
        int r = (int)((float)a.getRed() + ((float)b.getRed() - (float)a.getRed()) * t);
        int g = (int)((float)a.getGreen() + ((float)b.getGreen() - (float)a.getGreen()) * t);
        int bl = (int)((float)a.getBlue() + ((float)b.getBlue() - (float)a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
