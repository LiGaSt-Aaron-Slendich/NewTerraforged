package com.terraforged.mod.hooks;

import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.IGenerator;
import com.terraforged.mod.worldgen.profiler.GeneratorProfiler;
import java.util.OptionalInt;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public class GeneratorSeedHook {
   public static Registry<LevelStem> reseed(long seed, Registry<LevelStem> registry) {
      LevelStem levelstem = (LevelStem)registry.getOrThrow(LevelStem.OVERWORLD);
      ChunkGenerator chunkgenerator = levelstem.generator();
      if (chunkgenerator instanceof IGenerator) {
         chunkgenerator = chunkgenerator.withSeed(seed);
      }

      if (Environment.PROFILING && !(chunkgenerator instanceof GeneratorProfiler)) {
         chunkgenerator = withProfiler(chunkgenerator);
      }

      if (chunkgenerator == levelstem.generator()) {
         return registry;
      } else {
         Lifecycle lifecycle = registry.lifecycle(levelstem);
         LevelStem levelstem1 = new LevelStem(levelstem.typeHolder(), chunkgenerator);
         ((MappedRegistry)registry).registerOrOverride(OptionalInt.empty(), LevelStem.OVERWORLD, levelstem1, lifecycle);
         TerraForged.LOG.info("Re-seeded TerraForged generator: {}", seed);
         if (GeneratorProfiler.PROFILING.get()) {
            TerraForged.LOG.info("Attached TerraForged generator profiler");
         }

         return registry;
      }
   }

   public static ChunkGenerator withProfiler(ChunkGenerator generator) {
      return (ChunkGenerator)(GeneratorProfiler.PROFILING.get() ? GeneratorProfiler.wrap(generator) : generator);
   }
}
