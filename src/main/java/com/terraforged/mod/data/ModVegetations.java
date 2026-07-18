package com.terraforged.mod.data;

import com.terraforged.engine.Seed;
import com.terraforged.mod.Environment;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.registry.lazy.LazyTag;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.viability.BiomeEdgeViability;
import com.terraforged.mod.worldgen.biome.viability.HeightViability;
import com.terraforged.mod.worldgen.biome.viability.NoiseViability;
import com.terraforged.mod.worldgen.biome.viability.SaturationViability;
import com.terraforged.mod.worldgen.biome.viability.SlopeViability;
import com.terraforged.mod.worldgen.biome.viability.SumViability;
import com.terraforged.noise.Source;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;

public interface ModVegetations extends ModRegistry {
   static void register() {
      Seed seed = ModVegetations.Factory.createSeed();
      ModRegistries.register(VEGETATION, "trees_copse", ModVegetations.Factory.copse(seed, null));
      ModRegistries.register(VEGETATION, "trees_sparse", ModVegetations.Factory.sparse(seed, null));
      ModRegistries.register(VEGETATION, "trees_patchy", ModVegetations.Factory.patchy(seed, null));
      ModRegistries.register(VEGETATION, "trees_temperate", ModVegetations.Factory.temperate(seed, null));
      ModRegistries.register(VEGETATION, "trees_hardy", ModVegetations.Factory.hardy(seed, null));
      ModRegistries.register(VEGETATION, "trees_hardy_slopes", ModVegetations.Factory.hardySlopes(seed, null));
      ModRegistries.register(VEGETATION, "trees_rainforest", ModVegetations.Factory.rainforest(seed, null));
      ModRegistries.register(VEGETATION, "trees_sparse_rainforest", ModVegetations.Factory.sparseRainforest(seed, null));
   }

   static VegetationConfig[] getVegetation(RegistryAccess access) {
      if (access == null) {
         return ModVegetations.Factory.getDefaults(null);
      } else {
         return Environment.DEV_ENV
            ? ModVegetations.Factory.getDefaults(access)
            : access.ownedRegistryOrThrow(ModRegistry.VEGETATION.get()).stream().toArray(VegetationConfig[]::new);
      }
   }

   public static class Factory {
      static Seed createSeed() {
         return new RandSeed(2353245L, 500000);
      }

      static VegetationConfig copse(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.2F,
            0.8F,
            0.6F,
            tag("trees/copses", access),
            SumViability.builder(0.0F)
               .with(0.2F, new SaturationViability(0.7F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 35.0F, 150.0F))
               .with(-0.5F, new SlopeViability(65.0F, 0.55F))
               .with(1.0F, new NoiseViability(Source.simplex(seed.next(), 110, 2).clamp(0.85, 0.95F).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig hardy(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.22F,
            0.8F,
            0.7F,
            tag("trees/hardy", access),
            SumViability.builder(0.5F)
               .with(0.2F, new SaturationViability(0.85F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 40.0F, 190.0F))
               .with(-0.8F, new SlopeViability(55.0F, 0.65F))
               .with(-0.8F, new BiomeEdgeViability(0.65F))
               .with(-0.4F, new NoiseViability(Source.simplex(seed.next(), 120, 2).clamp(0.4, 0.8).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig hardySlopes(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.2F,
            0.8F,
            0.7F,
            tag("trees/hardy_slopes", access),
            SumViability.builder(0.2F)
               .with(0.2F, new SaturationViability(0.8F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 40.0F, 150.0F))
               .with(1.0F, new SlopeViability(60.0F, 0.5F))
               .with(-0.8F, new BiomeEdgeViability(0.65F))
               .with(-0.5F, new NoiseViability(Source.simplex(seed.next(), 140, 2).clamp(0.2, 0.9).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig sparse(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.15F,
            0.75F,
            0.35F,
            tag("trees/sparse", access),
            SumViability.builder(0.0F)
               .with(0.4F, new SaturationViability(0.95F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 50.0F, 175.0F))
               .with(-1.0F, new SlopeViability(65.0F, 0.6F))
               .with(1.0F, new NoiseViability(Source.simplex(seed.next(), 100, 3).clamp(0.8, 0.85).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig rainforest(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.35F,
            0.75F,
            0.7F,
            tag("trees/rainforest", access),
            SumViability.builder(0.45F)
               .with(0.25F, new SaturationViability(0.7F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 60.0F, 180.0F))
               .with(-0.5F, new SlopeViability(55.0F, 0.65F))
               .with(-0.8F, new BiomeEdgeViability(0.7F))
               .with(-0.4F, new NoiseViability(Source.simplex(seed.next(), 100, 2).clamp(0.7, 0.9).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig sparseRainforest(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.15F,
            0.8F,
            0.45F,
            tag("trees/sparse_rainforest", access),
            SumViability.builder(0.0F)
               .with(0.2F, new SaturationViability(0.65F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 20.0F, 150.0F))
               .with(-0.5F, new SlopeViability(65.0F, 0.75F))
               .with(0.5F, new NoiseViability(Source.simplex(seed.next(), 80, 2).clamp(0.5, 0.7).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig temperate(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.2F,
            0.8F,
            0.6F,
            tag("trees/temperate", access),
            SumViability.builder(0.7F)
               .with(0.25F, new SaturationViability(0.95F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 45.0F, 150.0F))
               .with(-0.6F, new SlopeViability(55.0F, 0.65F))
               .with(-0.8F, new BiomeEdgeViability(0.7F))
               .with(-0.5F, new NoiseViability(Source.simplex(seed.next(), 120, 2).clamp(0.4, 0.6).map(0.0, 1.0)))
               .build()
         );
      }

      static VegetationConfig patchy(Seed seed, RegistryAccess access) {
         return new VegetationConfig(
            0.2F,
            0.75F,
            0.5F,
            tag("trees/patchy", access),
            SumViability.builder(0.65F)
               .with(0.2F, new SaturationViability(0.9F, 1.0F))
               .with(-1.0F, new HeightViability(-100.0F, 40.0F, 165.0F))
               .with(-1.0F, new SlopeViability(60.0F, 0.65F))
               .with(-0.75F, new BiomeEdgeViability(0.8F))
               .with(-0.45F, new NoiseViability(Source.simplex(seed.next(), 150, 3).clamp(0.4, 0.7).map(0.0, 1.0)))
               .build()
         );
      }

      static LazyTag<Biome> tag(String name, RegistryAccess access) {
         return LazyTag.biome(name);
      }

      static VegetationConfig[] getDefaults(RegistryAccess access) {
         RandSeed randseed = new RandSeed(2353245L, 500000);
         return new VegetationConfig[]{
            copse(randseed, access),
            hardy(randseed, access),
            hardySlopes(randseed, access),
            sparse(randseed, access),
            rainforest(randseed, access),
            sparseRainforest(randseed, access),
            temperate(randseed, access),
            patchy(randseed, access)
         };
      }
   }
}
