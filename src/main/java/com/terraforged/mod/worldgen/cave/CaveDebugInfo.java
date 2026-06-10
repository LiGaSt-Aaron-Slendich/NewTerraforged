package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class CaveDebugInfo {
    private static final float GIGA_CAVE = 0.1f;
    private static final int SURFACE_SHELL = 14;

    private CaveDebugInfo() {
    }

    public static void append(Generator generator, BlockPos pos, List<String> lines) {
        int x = pos.getX();
        int z = pos.getZ();
        int y = pos.getY();
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, x, y, z);
        lines.add("Cave System: " + caveSystem);
        CaveDebugInfo.appendCaveRegion(generator, pos, caveSystem, lines);
    }

    private static void appendCaveRegion(Generator generator, BlockPos pos, String caveSystem, List<String> lines) {
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
            ResourceLocation regionBiome = source.getCaveBiomeSampler().getPrimaryRegionBiomeId(seed, x, z, type);
            if (regionBiome != null) {
                lines.add("Cave Region: " + CaveDebugInfo.formatRegionName(regionBiome));
                return;
            }
        }
        if ("Surface".equals(caveSystem)) {
            return;
        }
        Holder<Biome> caveBiome = CaveDebugInfo.sampleCaveBiome(generator, x, y, z);
        if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
            lines.add("Cave Region: " + CaveDebugInfo.formatRegionName(caveBiome));
        }
    }

    public static String resolveCaveSystem(Generator generator, int x, int y, int z) {
        int surface = generator.getOceanFloorHeight(x, z);
        if (y >= surface - 14) {
            return "Surface";
        }
        if (MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, x, y, z)) {
            int seed = Seeds.get(generator.getSeed());
            float giga = CaveNoise.sample(CaveModifiers.giga(), seed, x, z);
            if (giga > 0.1f) {
                return "Giga";
            }
            return "Mega";
        }
        Holder<Biome> caveBiome = CaveDebugInfo.sampleCaveBiome(generator, x, y, z);
        if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
            return y < 48 ? "Normal" : "Synapse";
        }
        if (MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z)) {
            return "Mega Shell";
        }
        return "Surface";
    }

    private static Holder<Biome> sampleCaveBiome(Generator generator, int x, int y, int z) {
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int surface = generator.getOceanFloorHeight(x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        return source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome, y, surface, x, z, 256);
    }

    public static String formatRegionName(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> CaveDebugInfo.formatRegionName(key.location())).orElse("Unknown Region");
    }

    public static String formatRegionName(ResourceLocation id) {
        String word = CaveDebugInfo.extractFirstBiomeWord(id);
        if (word.isBlank()) {
            return "Unknown Region";
        }
        return CaveDebugInfo.capitalizeWord(word) + " Region";
    }

    static String extractFirstBiomeWord(ResourceLocation id) {
        int split;
        String core;
        String path = id.getPath();
        int slash = path.lastIndexOf(47);
        String string = core = slash >= 0 ? path.substring(slash + 1) : path;
        if (core.endsWith("_caves")) {
            core = core.substring(0, core.length() - 6);
        } else if (core.endsWith("_cave")) {
            core = core.substring(0, core.length() - 5);
        } else if (core.endsWith("_hypogeal")) {
            core = core.substring(0, core.length() - 9);
        } else if (core.endsWith("_caverns")) {
            core = core.substring(0, core.length() - 8);
        }
        if (core.startsWith("cave_")) {
            core = core.substring(5);
        }
        return (split = core.indexOf(95)) > 0 ? core.substring(0, split) : core;
    }

    private static String capitalizeWord(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}
