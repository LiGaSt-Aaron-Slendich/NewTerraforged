package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.config.RiverConfig;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;

public class RiverCarver {
   private static final int SEED_OFFSET = 21221;
   private static final double EROSION_FREQ = 128.0;
   private static final float BORDER_OFFSET = 0.05F;
   private static final float BORDER_RANGE = 0.95F;
   private final float blendRadius;
   private final NoiseLevels levels;
   private final Module erosionNoise;
   private final RiverConfig riverConfig = new RiverConfig();
   private final RiverConfig lakeConfig = new RiverConfig();

   public RiverCarver(NoiseLevels levels, ContinentConfig config) {
      // Use unpitched riverScale so Continents Spread (pitched shape.scale) cannot shrink
      // bed/bank/valley widths out of existence. Matches ContinentNoise sample frame.
      int riverScale = config.shape.riverScale > 0 ? config.shape.riverScale : config.shape.scale;
      float f = levels.frequency * (1.0F / (float) Math.max(100, riverScale));
      this.levels = levels;
      this.riverConfig.copy(config.rivers.rivers).scale(f);
      this.lakeConfig.copy(config.rivers.lakes).scale(f);
      this.blendRadius = getBlendRadius(this.riverConfig, this.lakeConfig);
      this.erosionNoise = Source.builder().seed(config.rivers.seed + 21221).frequency(128.0).octaves(2).ridge();
   }

   public void carve(float x, float y, NoiseSample sample, CarverSample carverSample) {
      float f = this.erosionNoise.getValue(x, y);
      float f1 = this.getBaseModifier(sample);
      float f2 = sample.baseNoise * f1;
      f2 = this.carve(sample, carverSample.river, this.riverConfig, f2, f1, f);
      f2 = this.carve(sample, carverSample.lake, this.lakeConfig, f2, f1, f);
      sample.baseNoise = f2;
      sample.riverNoise = clipRiverNoise(sample);
   }

   private float carve(NoiseSample sample, NodeSample nodeSample, RiverConfig config, float baseNoise, float baseModifier, float erosion) {
      float f = this.getBaseNoise(sample, nodeSample, config, baseModifier);
      if (f == -1.0F) {
         return baseNoise;
      } else {
         float f1 = this.levels.toHeightNoise(f, 0.0F);
         this.carve(f1, erosion, sample, nodeSample, config);
         return f;
      }
   }

   private void carve(float baseLevel, float erosion, NoiseSample sample, NodeSample nodeSample, RiverConfig config) {
      if (!nodeSample.isInvalid()) {
         float f = sample.heightNoise;
         float f1 = nodeSample.position;
         float f2 = nodeSample.distance;
         float f3 = config.valleyWidth.at(f1);
         float f4 = config.bankWidth.at(f1);
         float f5 = config.bankDepth.at(f1);
         float f6 = config.bedWidth.at(f1);
         float f7 = config.bedDepth.at(f1);
         float f8 = baseLevel - f7 * this.levels.unit;
         float f9 = baseLevel + f5 * this.levels.unit;
         float f10 = getValleyAlpha(f2, f4, f3, sample.baseNoise);
         if (f10 < 1.0F) {
            float f11 = Math.min(f9, f);
            float f12 = this.getErosionModifier(erosion * config.erosion, f10);
            f = NoiseUtil.lerp(f11, f, f10 * f12);
            sample.riverNoise = sample.riverNoise * this.getValleyNoise(f2, f4, f3);
         }

         float f13 = getAlpha(f2, f6, f4);
         if (f13 < 1.0F) {
            float f14 = Math.min(f8, f);
            f = NoiseUtil.lerp(f14, f, f13);
            sample.terrainType = nodeSample.type;
            sample.riverNoise = sample.riverNoise * this.getRiverNoise(f, baseLevel, f9);
         }

         sample.heightNoise = f;
      }
   }

   private float getBaseNoise(NoiseSample sample, NodeSample nodeSample, RiverConfig config, float modifier) {
      if (nodeSample.isInvalid()) {
         return -1.0F;
      } else {
         float f = nodeSample.distance;
         float f1 = nodeSample.position;
         float f2 = config.valleyWidth.at(f1);
         if (f >= f2) {
            return -1.0F;
         } else {
            float f3 = config.bankWidth.at(f1);
            if (f <= f3) {
               return nodeSample.level * modifier;
            } else {
               float f4 = (f - f3) / (f2 - f3);
               return NoiseUtil.lerp(nodeSample.level, sample.baseNoise, f4) * modifier;
            }
         }
      }
   }

   private float getBaseModifier(NoiseSample sample) {
      float f = 0.55F;
      float f1 = 1.0F;
      return NoiseUtil.map(sample.continentNoise, f, f1, f1 - f);
   }

   private float getErosionModifier(float erosionNoise, float valleyAlpha) {
      float f = 1.0F - NoiseUtil.map(valleyAlpha, 0.975F, 1.0F, 0.025F);
      return 1.0F - erosionNoise * f;
   }

   private float getValleyNoise(float distance, float bankWidth, float valleyWidth) {
      float f = 0.05F + getAlpha(distance, bankWidth, valleyWidth) / 0.95F;
      return MathUtil.clamp(f, 0.0F, 1.0F);
   }

   private float getRiverNoise(float height, float waterLevel, float bankLevel) {
      float f = getAlpha(height, waterLevel, bankLevel);
      return MathUtil.clamp(f, 0.0F, 1.0F);
   }

   private static float clipRiverNoise(NoiseSample sample) {
      return sample.continentNoise < 0.5F ? 1.0F : sample.riverNoise;
   }

   private static float getValleyAlpha(float distance, float bankWidth, float valleyWidth, float baseValue) {
      float f = getAlpha(distance, bankWidth, valleyWidth);
      float f1 = getAlpha(baseValue, 0.4F, 0.6F);
      return NoiseUtil.lerp(f * f, f, f1);
   }

   private static float getAlpha(float value, float min, float max) {
      return value <= min ? 0.0F : (value >= max ? 1.0F : (value - min) / (max - min));
   }

   private static float getBlendRadius(RiverConfig river, RiverConfig lakes) {
      float f = Math.max(river.valleyWidth.max, lakes.valleyWidth.max);
      return Math.min(1.0F, f + 0.2F);
   }
}
