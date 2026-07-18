package com.terraforged.mod.worldgen.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record VolcanoConfig(
   double density,
   double jitter,
   VolcanoConfig.Range radius0,
   VolcanoConfig.Range radius1,
   VolcanoConfig.Range radius2,
   VolcanoConfig.Range height0,
   VolcanoConfig.Range height1,
   VolcanoConfig.Range fluidLevel
) implements FeatureConfiguration {
   public static final Codec<VolcanoConfig> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.DOUBLE.fieldOf("density").forGetter(VolcanoConfig::density),
            Codec.DOUBLE.fieldOf("jitter").forGetter(VolcanoConfig::jitter),
            VolcanoConfig.Range.CODEC.fieldOf("pool_radius").forGetter(VolcanoConfig::radius0),
            VolcanoConfig.Range.CODEC.fieldOf("mouth_radius").forGetter(VolcanoConfig::radius1),
            VolcanoConfig.Range.CODEC.fieldOf("base_radius").forGetter(VolcanoConfig::radius2),
            VolcanoConfig.Range.CODEC.fieldOf("pool_height").forGetter(VolcanoConfig::height0),
            VolcanoConfig.Range.CODEC.fieldOf("mouth_height").forGetter(VolcanoConfig::height1),
            VolcanoConfig.Range.CODEC.fieldOf("fluid_level").forGetter(VolcanoConfig::fluidLevel)
         )
         .apply(instance, VolcanoConfig::new)
   );

   public boolean validBiome(Holder<Biome> biome) {
      return true;
   }

   public double scale() {
      return this.radius2.max * 1.0;
   }

   public static VolcanoConfig defaultConfig() {
      return new VolcanoConfig(
         1.0,
         0.8,
         new VolcanoConfig.Range(5, 15),
         new VolcanoConfig.Range(20, 30),
         new VolcanoConfig.Range(100, 200),
         new VolcanoConfig.Range(40, 60),
         new VolcanoConfig.Range(100, 200),
         new VolcanoConfig.Range(70, 80)
      );
   }

   public record Range(int min, int max) {
      public static final Codec<VolcanoConfig.Range> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(Codec.INT.fieldOf("min").forGetter(VolcanoConfig.Range::min), Codec.INT.fieldOf("max").forGetter(VolcanoConfig.Range::max))
            .apply(instance, VolcanoConfig.Range::new)
      );

      public double get(double rand) {
         return Mth.lerp(rand, this.min, this.max);
      }
   }
}
