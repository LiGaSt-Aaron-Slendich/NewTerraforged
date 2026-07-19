package com.terraforged.mod;

import com.terraforged.mod.data.Content;
import com.terraforged.mod.data.gen.DataGen;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.util.Init;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.datapack.DataPackExporter;
import net.minecraft.core.Registry;

public class Common extends Init {
   public static final Common INSTANCE = new Common();

   Common() {
   }

   @Override
   protected void doInit() {
      com.terraforged.mod.compat.WwooCompat.init();
      TerraForged.LOG.info("Registering world-gen core codecs");
      Registry.register(Registry.BIOME_SOURCE, TerraForged.location("climate"), Source.CODEC);
      // MapCodecCodec for product id — must be Generator.CODEC so dispatch flattens fields (no "value" wrapper).
      Registry.register(Registry.CHUNK_GENERATOR, new net.minecraft.resources.ResourceLocation("newterraforged", "generator"), Generator.CODEC);
      // Legacy id — separate Codec instance to avoid Forge duplicate-value warn.
      Registry.register(
         Registry.CHUNK_GENERATOR,
         TerraForged.location("generator"),
         com.mojang.serialization.Codec.of(Generator.CODEC, Generator.CODEC)
      );
      // Profiler registration disabled: GeneratorProfiler.CODEC wraps ChunkGenerator.CODEC (circular with dispatch)
      // and nulls WorldGenSettings encode during CreateWorldScreen datapack validation.
      TerraForged.LOG.info("Registering world-gen component codecs");
      ModRegistries.createRegistry(ModRegistry.CAVE, NoiseCave.CODEC);
      ModRegistries.createRegistry(ModRegistry.CLIMATE, ClimateType.CODEC);
      ModRegistries.createRegistry(ModRegistry.TERRAIN_TYPE, TerrainType.DIRECT);
      ModRegistries.createRegistry(ModRegistry.TERRAIN, TerrainNoise.CODEC);
      ModRegistries.createRegistry(ModRegistry.VEGETATION, VegetationConfig.CODEC);
      TerraForged.LOG.info("Locking mod world-gen registries");
      ModRegistries.commit();
      TerraForged.LOG.info("Registering world-gen content");
      Content.INSTANCE.init();
      DataGen.INSTANCE.init();
      DataPackExporter.extractDefaultPack();
   }
}
