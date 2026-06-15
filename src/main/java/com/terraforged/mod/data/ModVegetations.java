package com.terraforged.mod.data;

import com.terraforged.engine.Seed;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.lazy.LazyTag;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.viability.BiomeEdgeViability;
import com.terraforged.mod.worldgen.biome.viability.HeightViability;
import com.terraforged.mod.worldgen.biome.viability.NoiseViability;
import com.terraforged.mod.worldgen.biome.viability.SaturationViability;
import com.terraforged.mod.worldgen.biome.viability.SlopeViability;
import com.terraforged.mod.worldgen.biome.viability.SumViability;
import com.terraforged.mod.worldgen.biome.viability.Viability;
import com.terraforged.noise.Source;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;

public interface ModVegetations {
    public static void register() {
        Seed seed = Factory.createSeed();
        TerraForged.register(TerraForged.VEGETATIONS, "trees_copse", Factory.copse(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_sparse", Factory.sparse(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_patchy", Factory.patchy(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_temperate", Factory.temperate(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_hardy", Factory.hardy(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_hardy_slopes", Factory.hardySlopes(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_rainforest", Factory.rainforest(seed, null));
        TerraForged.register(TerraForged.VEGETATIONS, "trees_sparse_rainforest", Factory.sparseRainforest(seed, null));
    }

    public static class Factory {
        static Seed createSeed() {
            return new RandSeed(2353245L, 500000);
        }

        static VegetationConfig copse(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.2f, 0.8f, 0.6f, Factory.tag("trees/copses", access), (Viability)SumViability.builder(0.0f).with(0.2f, new SaturationViability(0.7f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 35.0f, 150.0f)).with(-0.25f, new SlopeViability(65.0f, 0.55f)).with(1.0f, new NoiseViability(Source.simplex(seed.next(), 110, 2).clamp(0.85, 0.95f).map(0.0, 1.0))).build());
        }

        static VegetationConfig hardy(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.22f, 0.8f, 0.7f, Factory.tag("trees/hardy", access), (Viability)SumViability.builder(0.5f).with(0.2f, new SaturationViability(0.85f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 40.0f, 190.0f)).with(-0.35f, new SlopeViability(55.0f, 0.65f)).with(-0.8f, new BiomeEdgeViability(0.65f)).with(-0.4f, new NoiseViability(Source.simplex(seed.next(), 120, 2).clamp(0.4, 0.8).map(0.0, 1.0))).build());
        }

        static VegetationConfig hardySlopes(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.2f, 0.8f, 0.7f, Factory.tag("trees/hardy_slopes", access), (Viability)SumViability.builder(0.2f).with(0.2f, new SaturationViability(0.8f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 40.0f, 150.0f)).with(1.0f, new SlopeViability(60.0f, 0.5f)).with(-0.8f, new BiomeEdgeViability(0.65f)).with(-0.5f, new NoiseViability(Source.simplex(seed.next(), 140, 2).clamp(0.2, 0.9).map(0.0, 1.0))).build());
        }

        static VegetationConfig sparse(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.15f, 0.75f, 0.35f, Factory.tag("trees/sparse", access), (Viability)SumViability.builder(0.0f).with(0.4f, new SaturationViability(0.95f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 50.0f, 175.0f)).with(-0.45f, new SlopeViability(65.0f, 0.6f)).with(1.0f, new NoiseViability(Source.simplex(seed.next(), 100, 3).clamp(0.8, 0.85).map(0.0, 1.0))).build());
        }

        static VegetationConfig rainforest(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.35f, 0.75f, 0.7f, Factory.tag("trees/rainforest", access), (Viability)SumViability.builder(0.45f).with(0.25f, new SaturationViability(0.7f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 60.0f, 180.0f)).with(-0.25f, new SlopeViability(55.0f, 0.65f)).with(-0.8f, new BiomeEdgeViability(0.7f)).with(-0.4f, new NoiseViability(Source.simplex(seed.next(), 100, 2).clamp(0.7, 0.9).map(0.0, 1.0))).build());
        }

        static VegetationConfig sparseRainforest(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.15f, 0.8f, 0.45f, Factory.tag("trees/sparse_rainforest", access), (Viability)SumViability.builder(0.0f).with(0.2f, new SaturationViability(0.65f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 20.0f, 150.0f)).with(-0.5f, new SlopeViability(65.0f, 0.75f)).with(0.5f, new NoiseViability(Source.simplex(seed.next(), 80, 2).clamp(0.5, 0.7).map(0.0, 1.0))).build());
        }

        static VegetationConfig temperate(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.2f, 0.8f, 0.6f, Factory.tag("trees/temperate", access), (Viability)SumViability.builder(0.7f).with(0.25f, new SaturationViability(0.95f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 45.0f, 150.0f)).with(-0.3f, new SlopeViability(55.0f, 0.65f)).with(-0.8f, new BiomeEdgeViability(0.7f)).with(-0.5f, new NoiseViability(Source.simplex(seed.next(), 120, 2).clamp(0.4, 0.6).map(0.0, 1.0))).build());
        }

        static VegetationConfig patchy(Seed seed, RegistryAccess access) {
            return new VegetationConfig(0.2f, 0.75f, 0.5f, Factory.tag("trees/patchy", access), (Viability)SumViability.builder(0.65f).with(0.2f, new SaturationViability(0.9f, 1.0f)).with(-1.0f, new HeightViability(-100.0f, 40.0f, 165.0f)).with(-0.45f, new SlopeViability(60.0f, 0.65f)).with(-0.75f, new BiomeEdgeViability(0.8f)).with(-0.45f, new NoiseViability(Source.simplex(seed.next(), 150, 3).clamp(0.4, 0.7).map(0.0, 1.0))).build());
        }

        static LazyTag<Biome> tag(String name, RegistryAccess access) {
            return LazyTag.biome(name);
        }

        static VegetationConfig[] getDefaults(RegistryAccess access) {
            RandSeed seed = new RandSeed(2353245L, 500000);
            return new VegetationConfig[]{Factory.copse(seed, access), Factory.hardy(seed, access), Factory.hardySlopes(seed, access), Factory.sparse(seed, access), Factory.rainforest(seed, access), Factory.sparseRainforest(seed, access), Factory.temperate(seed, access), Factory.patchy(seed, access)};
        }
    }
}
