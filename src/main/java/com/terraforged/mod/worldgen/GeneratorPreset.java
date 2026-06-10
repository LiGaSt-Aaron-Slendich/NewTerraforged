package com.terraforged.mod.worldgen;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import com.terraforged.mod.platform.forge.TFCaveSystemConfig;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.VanillaGen;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.biome.BiomeGenerator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistryLoader;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class GeneratorPreset {
    public static Source createBiomeSource(long seed, INoiseGenerator noiseGenerator, RegistryAccess registries) {
        Registry biomes = registries.registryOrThrow(Registry.BIOME_REGISTRY);
        if (TFCaveBiomeConfig.INSTANCE != null && TFCaveSystemConfig.INSTANCE != null) {
            CaveBiomeRegistry caveRegistry = CaveBiomeRegistryLoader.build((Registry<Biome>)biomes, TFCaveBiomeConfig.INSTANCE);
            CaveSystemConfig systemConfig = TFCaveSystemConfig.INSTANCE.toSystemConfig();
            TerraForged.LOG.info("[GeneratorPreset] Cave biomes: vanillaFallback={}, primary={}", caveRegistry.isVanillaFallback(), caveRegistry.getPrimary().size());
            return new Source(seed, noiseGenerator, registries, caveRegistry, systemConfig);
        }
        TerraForged.LOG.warn("[GeneratorPreset] Cave configs not loaded \u0432\u0402\u201d cave biomes use vanilla fallback");
        return new Source(seed, noiseGenerator, registries);
    }

    public static Generator build(long seed, TerrainLevels levels, RegistryAccess registries) {
        TerrainNoise[] terrain = TerraForged.TERRAINS.entries(registries, TerrainNoise[]::new);
        BiomeGenerator biomeGenerator = new BiomeGenerator(seed, registries);
        INoiseGenerator noiseGenerator = new NoiseGenerator(seed, levels, terrain).withErosion();
        Source biomeSource = GeneratorPreset.createBiomeSource(seed, noiseGenerator, registries);
        VanillaGen vanillaGen = GeneratorPreset.getVanillaGen(seed, biomeSource, registries);
        return new Generator(seed, levels, vanillaGen, biomeSource, biomeGenerator, noiseGenerator);
    }

    public static LevelStem getDefault(RegistryAccess registries) {
        Generator generator = GeneratorPreset.build(0L, TerrainLevels.DEFAULT.get().copy(), registries);
        Registry type = registries.ownedRegistryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        ResourceKey overworld = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, (ResourceLocation)new ResourceLocation("minecraft", "overworld"));
        return new LevelStem(type.getHolderOrThrow(overworld), (ChunkGenerator)generator);
    }

    public static VanillaGen getVanillaGen(long seed, BiomeSource biomes, RegistryAccess access) {
        Registry structures = access.ownedRegistryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry parameters = access.registryOrThrow(Registry.NOISE_REGISTRY);
        Holder settings = access.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        return new VanillaGen(seed, biomes, (Holder<NoiseGeneratorSettings>)settings, (Registry<NormalNoise.NoiseParameters>)parameters, (Registry<StructureSet>)structures);
    }

    public static boolean isTerraForgedWorld(WorldGenSettings settings) {
        LevelStem stem = (LevelStem)settings.dimensions().getOrThrow(LevelStem.OVERWORLD);
        return GeneratorPreset.getGenerator(stem.generator()) != null;
    }

    public static boolean isTerraForgedWorld(ServerLevel level) {
        return GeneratorPreset.getGenerator(level) != null;
    }

    public static Generator getGenerator(ServerLevel level) {
        return GeneratorPreset.getGenerator(level.getChunkSource().getGenerator());
    }

    private static Generator getGenerator(ChunkGenerator chunkGenerator) {
        if (chunkGenerator instanceof Generator) {
            Generator generator = (Generator)chunkGenerator;
            return generator;
        }
        return null;
    }
}
