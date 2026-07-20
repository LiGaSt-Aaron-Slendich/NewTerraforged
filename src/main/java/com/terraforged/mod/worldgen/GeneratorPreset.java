package com.terraforged.mod.worldgen;

import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.util.TranslationUtil;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.biome.BiomeGenerator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseGenerator;
import com.terraforged.mod.worldgen.profiler.GeneratorProfiler;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public class GeneratorPreset {
   /** Create-world map type id / lang key: {@code generator.newterraforged.newterraforged}. */
   public static final ResourceLocation PRESET_NAME = new ResourceLocation("newterraforged", "newterraforged");
   public static final String TRANSLATION_KEY = TranslationUtil.key("generator", PRESET_NAME);

   public static Generator build(long seed, TerrainLevels levels, RegistryAccess registries) {
      TerrainNoise[] aterrainnoise = ModTerrains.getTerrain(registries);
      BiomeGenerator biomegenerator = new BiomeGenerator(seed, registries);
      INoiseGenerator inoisegenerator = new NoiseGenerator(seed, levels, aterrainnoise).withErosion();
      Source source = new Source(seed, inoisegenerator, registries);
      VanillaGen vanillagen = getVanillaGen(seed, source, registries);
      return new Generator(seed, levels, vanillagen, source, biomegenerator, inoisegenerator);
   }

   public static LevelStem getDefault(RegistryAccess registries) {
      Generator generator = build(0L, TerrainLevels.DEFAULT.get().copy(), registries);
      Registry<DimensionType> registry = registries.ownedRegistryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
      return new LevelStem(registry.getHolderOrThrow(DimensionType.OVERWORLD_LOCATION), generator);
   }

   public static VanillaGen getVanillaGen(long seed, BiomeSource biomes, RegistryAccess access) {
      Registry<StructureSet> registry = access.ownedRegistryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      Registry<NoiseParameters> registry1 = access.registryOrThrow(Registry.NOISE_REGISTRY);
      Holder<NoiseGeneratorSettings> holder = com.terraforged.mod.compat.WwooCompat.resolveOverworldNoiseSettings(access);
      return new VanillaGen(seed, biomes, holder, registry1, registry);
   }

   public static boolean isTerraForgedWorld(WorldGenSettings settings) {
      LevelStem levelstem = (LevelStem)settings.dimensions().getOrThrow(LevelStem.OVERWORLD);
      return getGenerator(levelstem.generator()) != null;
   }

   public static boolean isTerraForgedWorld(ServerLevel level) {
      return getGenerator(level) != null;
   }

   public static Generator getGenerator(ServerLevel level) {
      return getGenerator(level.getChunkSource().getGenerator());
   }

   private static Generator getGenerator(ChunkGenerator chunkGenerator) {
      if (chunkGenerator instanceof GeneratorProfiler generatorprofiler) {
         chunkGenerator = generatorprofiler.getGenerator();
      }

      return chunkGenerator instanceof Generator generator ? generator : null;
   }
}
