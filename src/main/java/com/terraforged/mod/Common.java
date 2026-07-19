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
import com.terraforged.mod.worldgen.profiler.GeneratorProfiler;
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
      // Product id first; legacy terraforged:generator uses a distinct Codec instance to avoid Forge "duplicate value".
      Registry.register(Registry.CHUNK_GENERATOR, new net.minecraft.resources.ResourceLocation("newterraforged", "generator"), Generator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, TerraForged.location("generator"),
         Generator.CODEC.xmap(g -> g, g -> g));
      Registry.register(Registry.CHUNK_GENERATOR, TerraForged.location("profiler"), GeneratorProfiler.CODEC);
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
