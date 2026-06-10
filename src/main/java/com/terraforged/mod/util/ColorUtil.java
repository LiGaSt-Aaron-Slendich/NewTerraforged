package com.terraforged.mod.util;

import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;

public class ColorUtil {
    public static int shade(float brightness) {
        return Color.HSBtoRGB(0.0f, 0.0f, brightness);
    }

    public static int shade(Color color, float brightness) {
        return ColorUtil.shade(color.getRed(), color.getGreen(), color.getBlue(), brightness);
    }

    public static int shade(int rgb, float brightness) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb >> 0 & 0xFF;
        return ColorUtil.shade(r, g, b, brightness);
    }

    public static int shade(int r, int g, int b, float brightness) {
        r = NoiseUtil.floor((float)r * brightness);
        g = NoiseUtil.floor((float)g * brightness);
        b = NoiseUtil.floor((float)b * brightness);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int getBiomeColor(ClimateSample sample, float biomeNoiseStrength) {
        return ColorUtil.getBiomeColor(sample, sample.biomeNoise, biomeNoiseStrength);
    }

    public static int getBiomeColor(ClimateSample sample, float shadeNoise, float shadeStrength) {
        return ColorUtil.getColor(sample, NoiseUtil.lerp(1.0f, shadeNoise, shadeStrength));
    }

    public static int getColor(ClimateSample sample, float shade) {
        if (sample.continentNoise <= 0.25f) {
            return 26333;
        }
        if (sample.continentNoise <= 0.5f) {
            return 39389;
        }
        if (sample.riverNoise <= 0.0f) {
            return 39389;
        }
        return ColorUtil.shade(sample.climateType.getColor(), shade);
    }
}
