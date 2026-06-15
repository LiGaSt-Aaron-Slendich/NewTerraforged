package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Debug cartography layers for mega/giga cave systems (not performance-critical).
 */
public final class CaveDebugMaps {
    private static final Color OUTSIDE = new Color(20, 20, 24);

    private CaveDebugMaps() {
    }

    public record ExportResult(List<Path> mapFiles, List<Path> legendFiles) {
    }

    public static ExportResult exportAll(Path dir, String base, int seed, CaveMegaGigaLayout layout, boolean[][] inFootprint, int originX, int originZ, int step, Registry<Biome> biomeRegistry) throws IOException {
        ArrayList<Path> maps = new ArrayList<Path>();
        ArrayList<Path> legends = new ArrayList<Path>();
        int gridH = inFootprint.length;
        int gridW = inFootprint[0].length;
        float[][] temperature = new float[gridH][gridW];
        float[][] moisture = new float[gridH][gridW];
        float[][] fertility = new float[gridH][gridW];
        ResourceLocation[][] vegetation = new ResourceLocation[gridH][gridW];
        ResourceLocation[][] features = new ResourceLocation[gridH][gridW];
        HashMap<ResourceLocation, Color> vegPalette = new HashMap<ResourceLocation, Color>();
        HashMap<ResourceLocation, Color> featPalette = new HashMap<ResourceLocation, Color>();
        CaveFeaturePlan.Cache planCache = new CaveFeaturePlan.Cache();
        for (int gz = 0; gz < gridH; ++gz) {
            for (int gx = 0; gx < gridW; ++gx) {
                if (!inFootprint[gz][gx]) continue;
                int wx = originX + gx * step + step / 2;
                int wz = originZ + gz * step + step / 2;
                CaveStatVector stats = layout.statsAt(wx, wz);
                temperature[gz][gx] = stats.temperature();
                moisture[gz][gx] = stats.moisture();
                fertility[gz][gx] = stats.fertility();
                CaveBiomeEntry entry = layout.getBiomeAt(wx, wz);
                if (entry == null) continue;
                Holder<Biome> biome = biomeRegistry.getHolder(ResourceKey.create(Registry.BIOME_REGISTRY, entry.biome())).orElse(null);
                if (biome == null) continue;
                vegetation[gz][gx] = CaveDebugMaps.pickVegetationFeature(seed, wx, wz, biome, vegPalette);
                features[gz][gx] = CaveDebugMaps.pickCaveFeature(seed, wx, wz, biome, planCache, featPalette);
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
        Path vegFile = dir.resolve(base + "_vegetation.png");
        CaveDebugMaps.writeFeatureMap(vegFile, inFootprint, vegetation, vegPalette);
        maps.add(vegFile);
        Path vegLegend = dir.resolve(base + "_vegetation_legend.txt");
        Files.writeString(vegLegend, CaveDebugMaps.buildLegend("Vegetation features per cell", vegetation, vegPalette), StandardCharsets.UTF_8);
        legends.add(vegLegend);
        Path featFile = dir.resolve(base + "_features.png");
        CaveDebugMaps.writeFeatureMap(featFile, inFootprint, features, featPalette);
        maps.add(featFile);
        Path featLegend = dir.resolve(base + "_features_legend.txt");
        Files.writeString(featLegend, CaveDebugMaps.buildLegend("Cave features per cell", features, featPalette), StandardCharsets.UTF_8);
        legends.add(featLegend);
        return new ExportResult(maps, legends);
    }

    private static ResourceLocation pickVegetationFeature(int seed, int wx, int wz, Holder<Biome> biome, Map<ResourceLocation, Color> palette) {
        ArrayList<ResourceLocation> candidates = new ArrayList<ResourceLocation>();
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex)) continue;
            net.minecraft.core.HolderSet stage = (net.minecraft.core.HolderSet)stages.get(stageIndex);
            if (stage == null) continue;
            for (int i = 0; i < stage.size(); ++i) {
                Holder placed = stage.get(i);
                if (!CaveDebugMaps.isVegetationCandidate((Holder<PlacedFeature>)placed, biome)) continue;
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null || candidates.contains(id)) continue;
                candidates.add(id);
                palette.putIfAbsent(id, CaveDebugMaps.colorFor(id));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(NoiseUtil.hash2D(seed ^ 0x0BEE71, wx, wz), candidates.size());
        return candidates.get(idx);
    }

    private static ResourceLocation pickCaveFeature(int seed, int wx, int wz, Holder<Biome> biome, CaveFeaturePlan.Cache planCache, Map<ResourceLocation, Color> palette) {
        ArrayList<ResourceLocation> candidates = new ArrayList<ResourceLocation>();
        CaveFeaturePlan plan = planCache.get(biome);
        CaveDebugMaps.collectFeatureIds(plan.forAnchor(CaveFeatureRules.Anchor.FLOOR), biome, candidates, palette);
        CaveDebugMaps.collectFeatureIds(plan.forAnchor(CaveFeatureRules.Anchor.CEILING), biome, candidates, palette);
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex)) continue;
            net.minecraft.core.HolderSet stage = (net.minecraft.core.HolderSet)stages.get(stageIndex);
            if (stage == null) continue;
            for (int i = 0; i < stage.size(); ++i) {
                Holder placed = stage.get(i);
                if (!CaveFeatureFilters.isModCaveFeatureAllowed((Holder<PlacedFeature>)placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome((Holder<PlacedFeature>)placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome((Holder<PlacedFeature>)placed, biome) || CaveFeatureFilters.isDeferredOrGlobalFeature((Holder<PlacedFeature>)placed)) continue;
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null || candidates.contains(id)) continue;
                candidates.add(id);
                palette.putIfAbsent(id, CaveDebugMaps.colorFor(id));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(NoiseUtil.hash2D(seed ^ 0xFEA71, wx, wz), candidates.size());
        return candidates.get(idx);
    }

    private static void collectFeatureIds(CaveFeaturePlan.StageFeature[] entries, Holder<Biome> biome, List<ResourceLocation> candidates, Map<ResourceLocation, Color> palette) {
        for (CaveFeaturePlan.StageFeature entry : entries) {
            Holder<PlacedFeature> placed = entry.feature();
            if (!CaveFeatureFilters.belongsToModCaveBiome(placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) continue;
            ResourceLocation id = FeatureMassClassifier.featurePath(placed);
            if (id == null || candidates.contains(id)) continue;
            candidates.add(id);
            palette.putIfAbsent(id, CaveDebugMaps.colorFor(id));
        }
    }

    private static boolean isVegetationCandidate(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isDeferredOrGlobalFeature(placed) || !CaveFeatureFilters.isModCaveFeatureAllowed(placed, biome) || !CaveFeatureFilters.belongsToModCaveBiome(placed, biome) || CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return false;
        }
        if (FeatureMassClassifier.isTree(placed)) {
            return true;
        }
        if (FeatureMassClassifier.spawnsSurfaceVegetation(placed)) {
            return true;
        }
        ResourceLocation id = FeatureMassClassifier.featurePath(placed);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        return path.contains("grass") && !path.contains("seagrass") || path.contains("flower") || path.contains("mushroom") || path.contains("shroom") || path.contains("fern") || path.contains("vine") || path.contains("bamboo") || path.contains("bush") || path.contains("plant") || path.contains("flora") || path.contains("vegetation") || path.contains("leaf_litter");
    }

    private static void writeStatHeatmap(Path path, boolean[][] inFootprint, float[][] values, StatLayer layer, CaveMegaGigaLayout layout, int originX, int originZ, int step) throws IOException {
        int h = values.length;
        int w = values[0].length;
        int scale = Math.max(1, Math.min(2, 1024 / Math.max(w, h)));
        BufferedImage img = new BufferedImage(w * scale, h * scale, 1);
        for (int gy = 0; gy < h; ++gy) {
            for (int gx = 0; gx < w; ++gx) {
                Color color = !inFootprint[gy][gx] ? OUTSIDE : layer.color(values[gy][gx]);
                CaveDebugMaps.fillPixel(img, gx, gy, scale, color);
            }
        }
        CaveDebugMaps.drawGeneratorMarkers(img, inFootprint, layout, originX, originZ, step, scale);
        ImageIO.write(img, "png", path.toFile());
    }

    private static void drawGeneratorMarkers(BufferedImage img, boolean[][] inFootprint, CaveMegaGigaLayout layout, int originX, int originZ, int step, int scale) {
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

    private static void writeStatHeatmap(Path path, boolean[][] inFootprint, float[][] values, StatLayer layer) throws IOException {
        CaveDebugMaps.writeStatHeatmap(path, inFootprint, values, layer, null, 0, 0, 1);
    }

    private static void writeFeatureMap(Path path, boolean[][] inFootprint, ResourceLocation[][] ids, Map<ResourceLocation, Color> palette) throws IOException {
        int h = ids.length;
        int w = ids[0].length;
        int scale = Math.max(1, Math.min(2, 1024 / Math.max(w, h)));
        BufferedImage img = new BufferedImage(w * scale, h * scale, 1);
        Color none = new Color(48, 48, 56);
        for (int gy = 0; gy < h; ++gy) {
            for (int gx = 0; gx < w; ++gx) {
                Color color;
                if (!inFootprint[gy][gx]) {
                    color = OUTSIDE;
                } else {
                    ResourceLocation id = ids[gy][gx];
                    color = id == null ? none : palette.getOrDefault(id, CaveDebugMaps.colorFor(id));
                }
                CaveDebugMaps.fillPixel(img, gx, gy, scale, color);
            }
        }
        ImageIO.write(img, "png", path.toFile());
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
            sb.append(String.format(Locale.ROOT, "%s @ %.0f, %.0f -> %s (%s)%n", generator.biome().biome(), generator.x(), generator.z(), kind.name(), generator.biome().biome()));
        }
        return sb.toString();
    }

    private static String buildLegend(String title, ResourceLocation[][] ids, Map<ResourceLocation, Color> palette) {
        HashMap<ResourceLocation, Integer> counts = new HashMap<ResourceLocation, Integer>();
        int total = 0;
        int empty = 0;
        for (ResourceLocation[] row : ids) {
            for (ResourceLocation id : row) {
                if (id == null) {
                    ++empty;
                    continue;
                }
                ++total;
                counts.merge(id, 1, Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append('\n');
        sb.append("# Cells with assignment: ").append(total).append(" | empty: ").append(empty).append('\n');
        counts.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).forEach(entry -> {
            Color c = palette.getOrDefault(entry.getKey(), Color.GRAY);
            sb.append(String.format(Locale.ROOT, "%s (%d) #%02X%02X%02X%n", entry.getKey(), entry.getValue(), c.getRed(), c.getGreen(), c.getBlue()));
        });
        return sb.toString();
    }

    private static Color colorFor(ResourceLocation id) {
        int hash = id.hashCode();
        int r = 64 + (hash >> 16 & 0x7F);
        int g = 64 + (hash >> 8 & 0x7F);
        int b = 64 + (hash & 0x7F);
        return new Color(r, g, b);
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
