package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.noise.Module;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

public final class CaveCartography {
    private static final int CHAT_PREVIEW = 24;
    private static final int FOOTPRINT_BLOCK_STEP = 4;
    private static final int MAX_FOOTPRINT_GRID = 512;
    private static final int SECTION_WIDTH = 384;
    private static final Color OUTSIDE = new Color(20, 20, 24);
    private static final Color SPINE = new Color(220, 48, 48);
    private static final Color AIR = new Color(36, 36, 44);

    private CaveCartography() {
    }

    public record Result(List<String> chatLines, Path textFile, Path imageFile, Path sectionFile) {
    }

    public static Result render(ServerLevel level, Generator generator, int centerX, int centerZ, CaveType type) {
        int seed = Seeds.get(generator.getSeed());
        Source source = generator.getBiomeSource();
        int radius = type == CaveType.GIGA ? CaveModifiers.GIGA_CELL_SCALE / 2 : CaveModifiers.MEGA_CELL_SCALE / 2;
        int cx = Math.floorDiv(centerX, radius * 2) * radius * 2 + radius;
        int cz = Math.floorDiv(centerZ, radius * 2) * radius * 2 + radius;
        int surfaceY = generator.getOceanFloorHeight(cx, cz);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(cx >> 2, 0, cz >> 2, Source.NOOP_CLIMATE_SAMPLER);
        CaveMegaGigaLayout layout = source.getCaveBiomeSampler().getMegaGigaLayout(seed, cx, cz, radius, type, surfaceBiome, 32, surfaceY);
        if (layout == null) {
            return new Result(List.of("Cartography: no mega/giga layout at this position."), null, null, null);
        }
        NoiseCave caveConfig = CaveLocator.findConfig(generator, type);
        Module modifier = type == CaveType.GIGA ? CaveModifiers.giga() : CaveModifiers.mega();
        CaveBiomeRegistry registry = source.getCaveBiomeRegistry();
        int layoutCx = Math.round(layout.centerX());
        int layoutCz = Math.round(layout.centerZ());
        int spanBlocks = radius * 2;
        int step = FOOTPRINT_BLOCK_STEP;
        int gridSize = spanBlocks / step + 1;
        while (gridSize > MAX_FOOTPRINT_GRID) {
            step += 2;
            gridSize = spanBlocks / step + 1;
        }
        int originX = layoutCx - spanBlocks / 2;
        int originZ = layoutCz - spanBlocks / 2;
        HashMap<ResourceLocation, Color> palette = new HashMap<ResourceLocation, Color>();
        HashMap<ResourceLocation, Character> legend = new HashMap<ResourceLocation, Character>();
        HashMap<ResourceLocation, Integer> biomeCounts = new HashMap<ResourceLocation, Integer>();
        boolean[][] inCave = new boolean[gridSize][gridSize];
        ResourceLocation[][] grid = new ResourceLocation[gridSize][gridSize];
        CaveCartography.floodCaveFootprint(seed, generator, caveConfig, modifier, layout, originX, originZ, gridSize, step, inCave, grid, palette, legend, biomeCounts);
        ArrayList<String> chat = new ArrayList<String>();
        chat.add(String.format(Locale.ROOT, "=== Cave cartography (%s @ %d,%d) ===", type.name(), layoutCx, layoutCz));
        chat.add(String.format(Locale.ROOT, "Footprint step: %d blocks | grid: %dx%d (~%d m span)", step, gridSize, gridSize, gridSize * step));
        int caveCells = 0;
        for (int gy = 0; gy < gridSize; ++gy) {
            for (int gx = 0; gx < gridSize; ++gx) {
                if (!inCave[gy][gx]) continue;
                ++caveCells;
            }
        }
        chat.add(String.format(Locale.ROOT, "Cave footprint cells: %d / %d (%.1f%% of grid)", caveCells, gridSize * gridSize, 100.0 * (double)caveCells / (double)(gridSize * gridSize)));
        CaveCartography.appendDominance(chat, biomeCounts, caveCells);
        int preview = Math.min(CHAT_PREVIEW, gridSize);
        int start = Math.max(0, (gridSize - preview) / 2);
        chat.add("Preview (cave footprint only, one char = one layout cell):");
        for (int gy = start; gy < start + preview && gy < gridSize; ++gy) {
            StringBuilder row = new StringBuilder(preview);
            for (int gx = start; gx < start + preview && gx < gridSize; ++gx) {
                if (!inCave[gy][gx]) {
                    row.append(' ');
                    continue;
                }
                ResourceLocation id = grid[gy][gx];
                row.append(id == null ? '?' : legend.getOrDefault(id, Character.valueOf('?')).charValue());
            }
            chat.add(row.toString());
        }
        chat.add("Legend:");
        for (Map.Entry<ResourceLocation, Character> entry : legend.entrySet()) {
            Color c = palette.get(entry.getKey());
            int count = biomeCounts.getOrDefault(entry.getKey(), 0);
            chat.add(String.format(Locale.ROOT, "  %c = %s (%d cells)  #%02X%02X%02X", entry.getValue(), entry.getKey(), count, c.getRed(), c.getGreen(), c.getBlue()));
        }
        Path textFile = null;
        Path imageFile = null;
        Path sectionFile = null;
        try {
            Path dir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("cave_maps");
            Files.createDirectories(dir);
            String base = String.format(Locale.ROOT, "cave_%s_%d_%d", type.name().toLowerCase(Locale.ROOT), cx, cz);
            textFile = dir.resolve(base + ".txt");
            Files.writeString(textFile, CaveCartography.buildAsciiMap(grid, inCave, legend, step, layoutCx, layoutCz, type, biomeCounts, caveCells), StandardCharsets.UTF_8);
            chat.add("Text map: " + textFile.toAbsolutePath());
            imageFile = dir.resolve(base + ".png");
            CaveCartography.writeFootprintPng(imageFile, grid, inCave, palette);
            chat.add("Footprint PNG: " + imageFile.toAbsolutePath());
            sectionFile = dir.resolve(base + "_section.png");
            CaveCartography.writeCrossSection(sectionFile, seed, generator, caveConfig, modifier, layout, registry, layoutCx, layoutCz, radius, palette, legend);
            chat.add("Cross-section PNG (spine = mid floor/ceiling): " + sectionFile.toAbsolutePath());
        }
        catch (IOException e) {
            chat.add("File export failed: " + e.getMessage());
        }
        return new Result(chat, textFile, imageFile, sectionFile);
    }

    private static void floodCaveFootprint(int seed, Generator generator, NoiseCave caveConfig, Module modifier, CaveMegaGigaLayout layout, int originX, int originZ, int gridSize, int step, boolean[][] inCave, ResourceLocation[][] grid, Map<ResourceLocation, Color> palette, Map<ResourceLocation, Character> legend, Map<ResourceLocation, Integer> biomeCounts) {
        for (int gz = 0; gz < gridSize; ++gz) {
            for (int gx = 0; gx < gridSize; ++gx) {
                int wx = originX + gx * step + step / 2;
                int wz = originZ + gz * step + step / 2;
                CaveColumnSimulator.Sample sample = CaveColumnSimulator.sampleMegaGigaForCartography(generator, caveConfig, seed, modifier, wx, wz);
                if (sample == null) continue;
                inCave[gz][gx] = true;
                CaveBiomeEntry entry = layout.getBiomeAt(wx, wz);
                ResourceLocation id = entry != null ? entry.biome() : null;
                grid[gz][gx] = id;
                if (id == null) continue;
                palette.putIfAbsent(id, CaveCartography.colorFor(id));
                legend.putIfAbsent(id, CaveCartography.symbolFor(id, legend.size()));
                biomeCounts.merge(id, 1, Integer::sum);
            }
        }
    }

    private static void appendDominance(List<String> chat, Map<ResourceLocation, Integer> biomeCounts, int caveCells) {
        if (caveCells <= 0 || biomeCounts.isEmpty()) {
            return;
        }
        ResourceLocation top = null;
        int topCount = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : biomeCounts.entrySet()) {
            if (entry.getValue() <= topCount) continue;
            topCount = entry.getValue();
            top = entry.getKey();
        }
        if (top != null) {
            chat.add(String.format(Locale.ROOT, "Dominant biome: %s = %.1f%% of cave footprint", top, 100.0 * (double)topCount / (double)caveCells));
            if ((double)topCount / (double)caveCells > 0.85) {
                chat.add("Note: one biome covers most of the footprint — this is the regional layout shell, not tunnel width.");
            }
        }
    }

    private static void writeCrossSection(Path path, int seed, Generator generator, NoiseCave caveConfig, Module modifier, CaveMegaGigaLayout layout, CaveBiomeRegistry registry, int cx, int cz, int radius, Map<ResourceLocation, Color> palette, Map<ResourceLocation, Character> legend) throws IOException {
        int minY = generator.getMinY();
        int maxSurface = generator.getOceanFloorHeight(cx, cz);
        for (int dx = -SECTION_WIDTH / 2; dx < SECTION_WIDTH / 2; ++dx) {
            maxSurface = Math.max(maxSurface, generator.getOceanFloorHeight(cx + dx, cz));
        }
        int height = maxSurface - minY + 16;
        int scaleX = 2;
        int scaleY = 1;
        BufferedImage img = new BufferedImage(SECTION_WIDTH * scaleX, height * scaleY, 1);
        for (int py = 0; py < height; ++py) {
            for (int px = 0; px < SECTION_WIDTH; ++px) {
                Color color = OUTSIDE;
                for (int sy = 0; sy < scaleY; ++sy) {
                    for (int sx = 0; sx < scaleX; ++sx) {
                        img.setRGB(px * scaleX + sx, py * scaleY + sy, color.getRGB());
                    }
                }
            }
        }
        for (int px = 0; px < SECTION_WIDTH; ++px) {
            int wx = cx - SECTION_WIDTH / 2 + px;
            CaveColumnSimulator.Sample sample = CaveColumnSimulator.sampleMegaGigaForCartography(generator, caveConfig, seed, modifier, wx, cz);
            if (sample == null) continue;
            CaveBiomeEntry floorEntry = layout.getBiomeAt(wx, cz);
            ResourceLocation floorId = floorEntry != null ? floorEntry.biome() : null;
            Color floorColor = floorId != null ? palette.computeIfAbsent(floorId, CaveCartography::colorFor) : Color.GRAY;
            ResourceLocation ceilId = floorId;
            Color ceilColor = floorColor;
            CaveBiomeEntry patch = CavePatchPlacer.previewCeilingPatch(seed, wx, cz, sample.floorY(), sample.ceilingY(), registry);
            if (patch != null) {
                ceilId = patch.biome();
                ceilColor = palette.computeIfAbsent(ceilId, CaveCartography::colorFor);
                legend.putIfAbsent(ceilId, CaveCartography.symbolFor(ceilId, legend.size()));
            }
            int bandBottom = sample.ceilingY();
            if (patch != null) {
                float factor = com.terraforged.noise.util.NoiseUtil.clamp((com.terraforged.noise.util.NoiseUtil.valCoord2D(seed, wx, cz) + 1.0f) * 0.5f, 0.0f, 1.0f);
                int band = NoiseCave.calcCeilingPatchHeight(sample.ceilingY() - sample.floorY(), patch.ceilingPatchMin(), patch.ceilingPatchMax(), factor);
                bandBottom = Math.max(sample.floorY() + 2, sample.ceilingY() - band);
            }
            for (int y = sample.floorY(); y <= sample.ceilingY(); ++y) {
                int py = maxSurface + 8 - y;
                if (py < 0 || py >= height) continue;
                Color color = y >= bandBottom ? ceilColor : floorColor;
                if (y == sample.midY()) {
                    color = SPINE;
                }
                for (int sy = 0; sy < scaleY; ++sy) {
                    for (int sx = 0; sx < scaleX; ++sx) {
                        img.setRGB(px * scaleX + sx, py * scaleY + sy, color.getRGB());
                    }
                }
            }
            int spinePy = maxSurface + 8 - sample.midY();
            if (spinePy >= 0 && spinePy < height) {
                for (int sx = 0; sx < scaleX; ++sx) {
                    img.setRGB(px * scaleX + sx, spinePy, SPINE.getRGB());
                }
            }
        }
        ImageIO.write(img, "png", path.toFile());
    }

    private static long packCell(int gx, int gz) {
        return (long)gx << 32 | (long)gz & 0xFFFFFFFFL;
    }

    private static String buildAsciiMap(ResourceLocation[][] grid, boolean[][] inCave, Map<ResourceLocation, Character> legend, int cell, int cx, int cz, CaveType type, Map<ResourceLocation, Integer> biomeCounts, int caveCells) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "# Cave map %s center=%d,%d cell=%d (footprint flood-fill)%n", type, cx, cz, cell));
        for (int gy = 0; gy < grid.length; ++gy) {
            for (int gx = 0; gx < grid[gy].length; ++gx) {
                if (!inCave[gy][gx]) {
                    sb.append(' ');
                    continue;
                }
                ResourceLocation id = grid[gy][gx];
                sb.append(id == null ? '?' : legend.getOrDefault(id, Character.valueOf('?')).charValue());
            }
            sb.append('\n');
        }
        sb.append(String.format(Locale.ROOT, "%n# Cave cells: %d / %d%n", caveCells, grid.length * grid[0].length));
        sb.append("\n# Legend\n");
        for (Map.Entry<ResourceLocation, Character> entry : legend.entrySet()) {
            sb.append(entry.getValue()).append(" = ").append(entry.getKey());
            Integer count = biomeCounts.get(entry.getKey());
            if (count != null) {
                sb.append(" (").append(count).append(')');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void writeFootprintPng(Path path, ResourceLocation[][] grid, boolean[][] inCave, Map<ResourceLocation, Color> palette) throws IOException {
        int h = grid.length;
        int w = grid[0].length;
        int scale = Math.max(1, Math.min(2, 1024 / Math.max(w, h)));
        BufferedImage img = new BufferedImage(w * scale, h * scale, 1);
        for (int gy = 0; gy < h; ++gy) {
            for (int gx = 0; gx < w; ++gx) {
                Color color = !inCave[gy][gx] ? OUTSIDE : (grid[gy][gx] == null ? AIR : palette.getOrDefault(grid[gy][gx], Color.GRAY));
                for (int py = 0; py < scale; ++py) {
                    for (int px = 0; px < scale; ++px) {
                        img.setRGB(gx * scale + px, gy * scale + py, color.getRGB());
                    }
                }
            }
        }
        ImageIO.write(img, "png", path.toFile());
    }

    private static char symbolFor(ResourceLocation id, int index) {
        String path = id.getPath();
        if (!path.isEmpty()) {
            char c = Character.toUpperCase(path.charAt(path.lastIndexOf(47) + 1));
            if (Character.isLetterOrDigit(c)) {
                return c;
            }
        }
        return (char)(65 + index % 26);
    }

    private static Color colorFor(ResourceLocation id) {
        int hash = id.hashCode();
        int r = 64 + (hash >> 16 & 0x7F);
        int g = 64 + (hash >> 8 & 0x7F);
        int b = 64 + (hash & 0x7F);
        return new Color(r, g, b);
    }
}
