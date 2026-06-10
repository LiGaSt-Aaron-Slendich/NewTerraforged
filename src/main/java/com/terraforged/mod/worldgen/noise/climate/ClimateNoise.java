package com.terraforged.mod.worldgen.noise.climate;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.world.GeneratorContext;
import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.climate.Moisture;
import com.terraforged.engine.world.climate.Temperature;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.mod.worldgen.noise.continent.cell.CellShape;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class ClimateNoise {
    private static final float MOISTURE_SIZE = 2.5f;
    private final int seed;
    private final float jitter = 0.8f;
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
        int biomeSize = settings.climate.biomeShape.biomeSize;
        float tempScaler = settings.climate.temperature.scale;
        float moistScaler = (float)settings.climate.moisture.scale * 2.5f;
        float biomeFreq = 1.0f / (float)biomeSize;
        float moistureSize = moistScaler * (float)biomeSize;
        float temperatureSize = tempScaler * (float)biomeSize;
        int moistScale = NoiseUtil.round(moistureSize * biomeFreq);
        int tempScale = NoiseUtil.round(temperatureSize * biomeFreq);
        int warpScale = settings.climate.biomeShape.biomeWarpScale;
        this.seed = seed.next();
        this.frequency = 1.0f / (float)biomeSize;
        Seed moistureSeed = seed.offset(settings.climate.moisture.seedOffset);
        Moisture moisture = new Moisture(moistureSeed.next(), moistScale, settings.climate.moisture.falloff);
        this.moisture = settings.climate.moisture.apply(moisture).warp(moistureSeed.next(), Math.max(1, moistScale / 2), 1, (double)moistScale / 4.0).warp(moistureSeed.next(), Math.max(1, moistScale / 6), 2, (double)moistScale / 12.0);
        Seed tempSeed = seed.offset(settings.climate.temperature.seedOffset);
        Temperature temperature = new Temperature(1.0f / (float)tempScale, settings.climate.temperature.falloff);
        this.temperature = settings.climate.temperature.apply(temperature).warp(tempSeed.next(), tempScale * 4, 2, tempScale * 4).warp(tempSeed.next(), tempScale, 1, tempScale);
        this.warp = Domain.warp(Source.build(seed.next(), warpScale, 3).lacunarity(2.4).gain(0.3).simplex2(), Source.build(seed.next(), warpScale, 3).lacunarity(2.4).gain(0.3).simplex2(), Source.constant((double)settings.climate.biomeShape.biomeWarpStrength * 0.75));
    }

    public ClimateSample getSample(int seed, float x, float y) {
        ClimateSample sample = this.localSample.get().reset();
        this.sample(seed, x, y, sample);
        return sample;
    }

    public void sample(int seed, float x, float y, ClimateSample sample) {
        float px = this.warp.getX(x, y);
        float py = this.warp.getY(x, y);
        px *= this.frequency;
        py *= this.frequency;
        px = this.cellShape.adjustX(px);
        py = this.cellShape.adjustY(py);
        this.sampleBiome(seed, px, py, sample);
        sample.climateType = BiomeType.get(sample.temperature, sample.moisture);
        if (sample.climateType == BiomeType.COLD_STEPPE || sample.climateType == BiomeType.STEPPE) {
            sample.climateType = BiomeType.GRASSLAND;
        }
    }

    private void sampleBiome(int seed, float x, float y, ClimateSample sample) {
        int minX = NoiseUtil.floor(x) - 1;
        int minY = NoiseUtil.floor(y) - 1;
        int maxX = NoiseUtil.floor(x) + 2;
        int maxY = NoiseUtil.floor(y) + 2;
        float nearestX = x;
        float nearestY = y;
        int nearestHash = 0;
        float distance = Float.MAX_VALUE;
        float distance2 = Float.MAX_VALUE;
        for (int cy = minY; cy <= maxY; ++cy) {
            for (int cx = minX; cx <= maxX; ++cx) {
                float py;
                int hash = MathUtil.hash(seed, cx, cy);
                float px = this.cellShape.getCellX(hash, cx, cy, 0.8f);
                float d2 = NoiseUtil.dist2(x, y, px, py = this.cellShape.getCellY(hash, cx, cy, 0.8f));
                if (d2 < distance) {
                    distance2 = distance;
                    distance = d2;
                    nearestX = px;
                    nearestY = py;
                    nearestHash = hash;
                    continue;
                }
                if (!(d2 < distance2)) continue;
                distance2 = d2;
            }
        }
        sample.biomeNoise = MathUtil.rand(nearestHash, 1236785);
        sample.biomeEdgeNoise = 1.0f - NoiseUtil.sqrt(distance / distance2);
        sample.moisture = this.moisture.getValue(nearestX, nearestY);
        sample.temperature = this.temperature.getValue(nearestX, nearestY);
    }
}
