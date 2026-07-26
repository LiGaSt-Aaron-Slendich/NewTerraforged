package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;

/**
 * Portable cave-debug helpers for TF118 stock Generator / CaveBiomeSampler.
 */
public final class CaveDebugInfo {
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
            CaveMegaGigaLayout layout = CaveDebugInfo.resolveLayout(generator, seed, x, y, z, type);
            if (layout != null) {
                CaveBiomeEntry entry = layout.getBiomeAt(x, z);
                if (entry != null) {
                    lines.add("Cave Region: " + CaveDebugInfo.formatRegionName(entry.biome()));
                    return;
                }
            }
        }
        if ("Surface".equals(caveSystem)) {
            return;
        }
        Holder<Biome> caveBiome = CaveDebugInfo.sampleCaveBiome(generator, x, z);
        if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
            lines.add("Cave Region: " + CaveDebugInfo.formatRegionName(caveBiome));
        }
    }

    public static String resolveCaveSystem(Generator generator, int x, int y, int z) {
        int surface = CaveDebugInfo.oceanFloorHeight(generator, x, z);
        if (y >= surface - SURFACE_SHELL) {
            return "Surface";
        }
        byte zone = MegaGigaZoneProbe.classifyWithCarverCache(generator, x, z);
        if (zone == MegaGigaZoneProbe.GIGA) {
            return "Giga";
        }
        if (zone == MegaGigaZoneProbe.MEGA) {
            return "Mega";
        }
        int seed = Seeds.get(generator.getSeed());
        if (CaveDebugInfo.inLayoutFootprint(generator, seed, x, y, z, CaveType.GIGA)) {
            return "Giga";
        }
        if (CaveDebugInfo.inLayoutFootprint(generator, seed, x, y, z, CaveType.MEGA)) {
            return "Mega";
        }
        Holder<Biome> caveBiome = CaveDebugInfo.sampleCaveBiome(generator, x, z);
        if (CaveBiomeIds.isUndergroundBiome(caveBiome)) {
            return y < 48 ? "Normal" : "Synapse";
        }
        return "Underground";
    }

    static boolean inLayoutFootprint(Generator generator, int seed, int x, int y, int z, CaveType type) {
        CaveMegaGigaLayout layout = CaveDebugInfo.resolveLayout(generator, seed, x, y, z, type);
        if (layout == null || layout.generators().isEmpty()) {
            return false;
        }
        float dx = (float) x - layout.centerX();
        float dz = (float) z - layout.centerZ();
        float radius = type == CaveType.GIGA ? 400.0F : 250.0F;
        return Math.sqrt(dx * dx + dz * dz) < radius * 1.05;
    }

    public static CaveMegaGigaLayout resolveLayout(Generator generator, int seed, int x, int y, int z, CaveType type) {
        Source source = generator.getBiomeSource();
        int surfaceY = CaveDebugInfo.oceanFloorHeight(generator, x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        int radius = type == CaveType.GIGA ? 400 : 250;
        int cx = Math.floorDiv(x, radius * 2) * radius * 2 + radius;
        int cz = Math.floorDiv(z, radius * 2) * radius * 2 + radius;
        CaveMegaGigaLayout fromSampler =
            source.getCaveBiomeSampler().getMegaGigaLayout(seed, cx, cz, radius, type, surfaceBiome, y, surfaceY);
        if (fromSampler != null) {
            return fromSampler;
        }
        Registry<Biome> biomes = source.getRegistries().registryOrThrow(Registry.BIOME_REGISTRY);
        TFCaveBiomeConfig biomeCfg = TFCaveBiomeConfig.INSTANCE;
        if (biomeCfg == null) {
            return null;
        }
        CaveBiomeRegistry registry = CaveBiomeRegistryLoader.build(biomes, biomeCfg);
        CaveSystemConfig systemCfg = TFCaveSystemConfig.INSTANCE != null
            ? TFCaveSystemConfig.INSTANCE.toSystemConfig()
            : CaveSystemConfig.DEFAULT;
        ClimateSample climate = source.getBiomeSampler().getSample(0, x, z);
        float oceanWeight = Math.max(0.0F, 1.0F - climate.continentNoise);
        float riverWeight = Math.max(0.0F, 1.0F - climate.riverNoise);
        CaveStatInitializer.CaveStatSnapshot snapshot =
            CaveStatInitializer.initialize(surfaceBiome, y, surfaceY, oceanWeight, riverWeight, seed, x, z);
        return CaveMegaGigaLayout.build(seed, cx, cz, radius, registry, systemCfg, type == CaveType.MEGA, snapshot);
    }

    private static Holder<Biome> sampleCaveBiome(Generator generator, int x, int z) {
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        return source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL);
    }

    public static int oceanFloorHeight(Generator generator, int x, int z) {
        try {
            return generator.getOceanFloorHeight(x, z);
        } catch (Throwable ignored) {
            TerrainData data = generator.getChunkData(new ChunkPos(x >> 4, z >> 4));
            return data.getHeight(x & 15, z & 15);
        }
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
