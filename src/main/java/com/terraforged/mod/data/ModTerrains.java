package com.terraforged.mod.data;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.TerrainSettings;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.LandForms;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.registry.lazy.LazyHolder;
import com.terraforged.mod.registry.lazy.LazyKey;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public interface ModTerrains extends ModRegistry {
   static void register() {
      Seed seed = ModTerrains.Factory.createSeed();
      ModRegistries.register(
         TERRAIN, "steppe", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 1.5F, LandForms::steppe)
      );
      ModRegistries.register(
         TERRAIN, "plains", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 2.5F, LandForms::plains)
      );
      ModRegistries.register(
         TERRAIN, "hills_1", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0F, LandForms::hills1)
      );
      ModRegistries.register(
         TERRAIN, "hills_2", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0F, LandForms::hills2)
      );
      ModRegistries.register(
         TERRAIN, "dales", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 1.5F, LandForms::dales)
      );
      ModRegistries.register(
         TERRAIN, "plateau", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, 2.0F, LandForms::plateau)
      );
      ModRegistries.register(
         TERRAIN, "badlands", ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, 1.75F, LandForms::badlands)
      );
      ModRegistries.register(TERRAIN, "torridonian", ModTerrains.Factory.create(null, seed, ModTerrainTypes.TORRIDONIAN, 2.5F, LandForms::torridonian));
      ModRegistries.register(
         TERRAIN,
         "mountains_1",
         ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains)
      );
      ModRegistries.register(
         TERRAIN,
         "mountains_2",
         ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains2)
      );
      ModRegistries.register(
         TERRAIN,
         "mountains_3",
         ModTerrains.Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains3)
      );
      ModRegistries.register(TERRAIN, "dolomites", ModTerrains.Factory.createDolomite(null, seed, ModTerrainTypes.DOLOMITES, 1.25F));
      ModRegistries.register(
         TERRAIN,
         "volcanic_island",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.VOLCANIC_ISLAND, 0.75F, LandForms::mountains)
      );
      ModRegistries.register(
         TERRAIN,
         "coastal_island",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.COASTAL_ISLAND, 0.9F, LandForms::hills1)
      );
      ModRegistries.register(
         TERRAIN,
         "scattered_archipelago",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.SCATTERED_ARCHIPELAGO, 1.0F, LandForms::plains)
      );
      ModRegistries.register(
         TERRAIN,
         "archipelago_hills",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.ARCHIPELAGO_HILLS, 1.0F, LandForms::hills1)
      );
      ModRegistries.register(
         TERRAIN,
         "archipelago_plateau",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.ARCHIPELAGO_PLATEAU, 1.1F, LandForms::plateau)
      );
      ModRegistries.register(
         TERRAIN,
         "archipelago_mountains",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.ARCHIPELAGO_MOUNTAINS, 0.9F, LandForms::mountains)
      );
      ModRegistries.register(
         TERRAIN,
         "laguna",
         ModTerrains.Factory.create(null, seed, ModTerrainTypes.LAGUNA, 0.5F, LandForms::plains)
      );
      ModRegistries.register(
         TERRAIN,
         "mountains_ridge_1",
         ModTerrains.Factory.createNF(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains2)
      );
      ModRegistries.register(
         TERRAIN,
         "mountains_ridge_2",
         ModTerrains.Factory.createNF(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains3)
      );
   }

   static TerrainNoise[] getTerrain(RegistryAccess access) {
      Registry<TerrainNoise> registry = (Registry<TerrainNoise>)(access == null
         ? ModRegistries.<TerrainNoise>getRegistry((ResourceKey<Registry<TerrainNoise>>)TERRAIN.get())
         : access.ownedRegistryOrThrow((ResourceKey)TERRAIN.get()));
      return ModRegistry.entries(registry, TerrainNoise[]::new);
   }

   public static class Factory {
      static final LandForms LAND_FORMS = new LandForms(settings(), new Levels(63, 255), Source.ZERO);
      static final LandForms LAND_FORMS_NF = new LandForms(nonFancy(), new Levels(63, 255), Source.ZERO);

      static Seed createSeed() {
         return new RandSeed(9712416L, 500000);
      }

      static TerrainNoise create(RegistryAccess access, Seed seed, Terrain type, float weight, BiFunction<LandForms, Seed, Module> factory) {
         return new TerrainNoise(getType(access, type), weight, factory.apply(LAND_FORMS, seed));
      }

      static TerrainNoise createNF(RegistryAccess access, Seed seed, Terrain type, float weight, BiFunction<LandForms, Seed, Module> factory) {
         return new TerrainNoise(getType(access, type), weight, factory.apply(LAND_FORMS_NF, seed));
      }

      static TerrainNoise createDolomite(RegistryAccess access, Seed seed, Terrain type, float weight) {
         Module module = Source.simplex(seed.next(), 80, 4).scale(0.1);
         Module module1 = Source.simplex(seed.next(), 475, 4).clamp(0.3, 1.0).map(0.0, 1.0).warp(seed.next(), 10, 2, 8.0);
         Module module2 = module1.pow(2.2).scale(0.65).add(module);
         Module module3 = Source.build(seed.next(), 400, 5)
            .lacunarity(2.7)
            .gain(0.6)
            .simplexRidge()
            .clamp(0.0, 0.675)
            .map(0.0, 1.0)
            .warp(Domain.warp(Source.SIMPLEX, seed.next(), 40, 5, 30.0))
            .alpha(0.875);
         Module module4 = module1.mult(module3).max(module2).warp(seed.next(), 800, 3, 300.0).scale(0.75);
         return new TerrainNoise(getType(access, type), weight, module4);
      }

      static Holder<TerrainType> getType(RegistryAccess access, Terrain terrain) {
         LazyKey<TerrainType> lazykey = ModRegistry.TERRAIN_TYPE.element(terrain.getName());
         return (Holder<TerrainType>)(access == null
            ? new LazyHolder<>(TerrainType.of(terrain), lazykey)
            : access.ownedRegistryOrThrow(ModRegistry.TERRAIN_TYPE.get()).getHolderOrThrow(lazykey.get()));
      }

      static TerrainSettings settings() {
         TerrainSettings terrainsettings = new TerrainSettings();
         terrainsettings.general.globalVerticalScale = 1.0F;
         return terrainsettings;
      }

      static TerrainSettings nonFancy() {
         TerrainSettings terrainsettings = settings();
         terrainsettings.general.fancyMountains = false;
         return terrainsettings;
      }

      public static TerrainNoise[] getDefault(RegistryAccess access) {
         Seed seed = createSeed();
         return new TerrainNoise[]{
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 1.5F, LandForms::steppe),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 2.5F, LandForms::plains),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0F, LandForms::hills1),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0F, LandForms::hills2),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 1.5F, LandForms::dales),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, 2.0F, LandForms::plateau),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, 1.75F, LandForms::badlands),
            create(access, seed, ModTerrainTypes.TORRIDONIAN, 2.5F, LandForms::torridonian),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains2),
            create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains3),
            createDolomite(access, seed, ModTerrainTypes.DOLOMITES, 1.25F),
            createNF(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains2),
            createNF(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25F, LandForms::mountains3)
         };
      }
   }

   public interface Weights {
      float STEPPE = 1.5F;
      float PLAINS = 2.5F;
      float HILLS = 2.0F;
      float DALES = 1.5F;
      float PLATEAU = 2.0F;
      float BADLANDS = 1.75F;
      float TORRIDONIAN = 2.5F;
      float MOUNTAINS = 1.25F;
   }
}
