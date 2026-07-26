package com.terraforged.mod.util;

import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;

public class ColorUtil {
   public static int shade(float brightness) {
      return Color.HSBtoRGB(0.0F, 0.0F, brightness);
   }

   public static int shade(Color color, float brightness) {
      return shade(color.getRed(), color.getGreen(), color.getBlue(), brightness);
   }

   public static int shade(int rgb, float brightness) {
      int i = rgb >> 16 & 0xFF;
      int j = rgb >> 8 & 0xFF;
      int k = rgb >> 0 & 0xFF;
      return shade(i, j, k, brightness);
   }

   public static int shade(int r, int g, int b, float brightness) {
      r = NoiseUtil.floor(r * brightness);
      g = NoiseUtil.floor(g * brightness);
      b = NoiseUtil.floor(b * brightness);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static int rgb(int r, int g, int b) {
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static int getBiomeColor(ClimateSample sample, float biomeNoiseStrength) {
      return getBiomeColor(sample, sample.biomeNoise, biomeNoiseStrength);
   }

   public static int getBiomeColor(ClimateSample sample, float shadeNoise, float shadeStrength) {
      return getColor(sample, NoiseUtil.lerp(1.0F, shadeNoise, shadeStrength));
   }

   public static int getColor(ClimateSample sample, float shade) {
      if (sample.continentNoise <= 0.25F) {
         // Deep ocean — opaque blue (was missing alpha → transparent)
         return rgb(34, 85, 170);
      } else if (sample.continentNoise <= 0.5F) {
         // Shallow ocean / coast water
         return rgb(64, 140, 200);
      } else {
         return sample.riverNoise <= 0.0F ? rgb(64, 140, 200) : shade(sample.climateType.getColor(), shade);
      }
   }
}
