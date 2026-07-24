package com.terraforged.mod.worldgen.noise.climate;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.climate.Moisture;
import com.terraforged.engine.world.climate.Temperature;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.continent.cell.CellShape;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class ClimateNoise {
   private static final float MOISTURE_SIZE = 2.5F;
   private final int seed;
   private final float jitter = 0.8F;
   private final float frequency;
   private final CellShape cellShape = CellShape.SQUARE;
   private final Domain warp;
   private final Module moisture;
   private final Module temperature;
   private final ThreadLocal<ClimateSample> localSample = ThreadLocal.withInitial(ClimateSample::new);

   public ClimateNoise(GeneratorContext context) {
      this(context.seed, context.settings);
   }

   public ClimateNoise(Seed seed, Settings settings) {
      int i = settings.climate.biomeShape.biomeSize;
      int continentScale = settings.world != null && settings.world.continent != null
            ? settings.world.continent.continentScale
            : com.terraforged.engine.settings.WorldSettings.DEFAULT_CONTINENT_SCALE;
      // UI/settings store percent (50–300); noise needs absolute periods.
      float f = ClimateScaleResolver.absoluteScale(
            continentScale, ClimateScaleResolver.migratePercent(settings.climate.temperature.scale));
      float f1 = ClimateScaleResolver.absoluteScale(
            continentScale, ClimateScaleResolver.migratePercent(settings.climate.moisture.scale))
            * 2.5F;
      float f2 = 1.0F / i;
      float f3 = f1 * i;
      float f4 = f * i;
      int j = NoiseUtil.round(f3 * f2);
      int k = NoiseUtil.round(f4 * f2);
      int l = settings.climate.biomeShape.biomeWarpScale;
      this.seed = seed.next();
      this.frequency = 1.0F / i;
      Seed seedx = seed.offset(settings.climate.moisture.seedOffset);
      Module module = new Moisture(seedx.next(), j, settings.climate.moisture.falloff);
      this.moisture = settings.climate
         .moisture
         .apply(module)
         .warp(seedx.next(), Math.max(1, j / 2), 1, j / 4.0)
         .warp(seedx.next(), Math.max(1, j / 6), 2, j / 12.0);
      Seed seed1 = seed.offset(settings.climate.temperature.seedOffset);
      Module module1 = new Temperature(1.0F / k, settings.climate.temperature.falloff);
      this.temperature = settings.climate.temperature.apply(module1).warp(seed1.next(), k * 4, 2, k * 4).warp(seed1.next(), k, 1, k);
      this.warp = Domain.warp(
         Source.build(seed.next(), l, 3).lacunarity(2.4).gain(0.3).simplex2(),
         Source.build(seed.next(), l, 3).lacunarity(2.4).gain(0.3).simplex2(),
         Source.constant(settings.climate.biomeShape.biomeWarpStrength * 0.75)
      );
   }

   public ClimateSample getSample(float x, float y) {
      ClimateSample climatesample = this.localSample.get().reset();
      this.sample(x, y, climatesample);
      return climatesample;
   }

   public void sample(float x, float y, ClimateSample sample) {
      float f = this.warp.getX(x, y);
      float f1 = this.warp.getY(x, y);
      f *= this.frequency;
      f1 *= this.frequency;
      f = this.cellShape.adjustX(f);
      f1 = this.cellShape.adjustY(f1);
      this.sampleBiome(f, f1, sample);
      sample.climateType = BiomeType.get(sample.temperature, sample.moisture);
      if (sample.climateType == BiomeType.COLD_STEPPE || sample.climateType == BiomeType.STEPPE) {
         sample.climateType = BiomeType.GRASSLAND;
      }
   }

   private void sampleBiome(float x, float y, ClimateSample sample) {
      int i = NoiseUtil.floor(x) - 1;
      int j = NoiseUtil.floor(y) - 1;
      int k = NoiseUtil.floor(x) + 2;
      int l = NoiseUtil.floor(y) + 2;
      float f = x;
      float f1 = y;
      int i1 = 0;
      float f2 = Float.MAX_VALUE;
      float f3 = Float.MAX_VALUE;

      for (int j1 = j; j1 <= l; j1++) {
         for (int k1 = i; k1 <= k; k1++) {
            int l1 = MathUtil.hash(this.seed, k1, j1);
            float f4 = this.cellShape.getCellX(l1, k1, j1, 0.8F);
            float f5 = this.cellShape.getCellY(l1, k1, j1, 0.8F);
            float f6 = NoiseUtil.dist2(x, y, f4, f5);
            if (f6 < f2) {
               f3 = f2;
               f2 = f6;
               f = f4;
               f1 = f5;
               i1 = l1;
            } else if (f6 < f3) {
               f3 = f6;
            }
         }
      }

      sample.biomeNoise = MathUtil.rand(i1, 1236785);
      sample.biomeEdgeNoise = 1.0F - NoiseUtil.sqrt(f2 / f3);
      sample.moisture = this.moisture.getValue(f, f1);
      sample.temperature = this.temperature.getValue(f, f1);
   }
}
