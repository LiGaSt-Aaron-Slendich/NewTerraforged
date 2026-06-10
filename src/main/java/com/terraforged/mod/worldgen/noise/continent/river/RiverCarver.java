package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.config.RiverConfig;
import com.terraforged.mod.worldgen.noise.continent.river.CarverSample;
import com.terraforged.mod.worldgen.noise.continent.river.NodeSample;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;

public class RiverCarver {
    private static final int SEED_OFFSET = 21221;
    private static final double EROSION_FREQ = 128.0;
    private static final float BORDER_OFFSET = 0.12f;
    private static final float BORDER_RANGE = 0.88f;
    private final float blendRadius;
    private final NoiseLevels levels;
    private final Module erosionNoise;
    private final RiverConfig riverConfig = new RiverConfig();
    private final RiverConfig lakeConfig = new RiverConfig();

    public RiverCarver(NoiseLevels levels, ContinentConfig config) {
        float frequency = levels.frequency * (1.0f / (float)config.shape.scale);
        this.levels = levels;
        this.riverConfig.copy(config.rivers.rivers).scale(frequency);
        this.lakeConfig.copy(config.rivers.lakes).scale(frequency);
        this.blendRadius = RiverCarver.getBlendRadius(this.riverConfig, this.lakeConfig);
        this.erosionNoise = Source.builder().seed(config.rivers.seed + 21221).frequency(128.0).octaves(2).ridge();
    }

    public void carve(int seed, float x, float y, NoiseSample sample, CarverSample carverSample) {
        float erosion = this.erosionNoise.getValue(x, y);
        float baseModifier = this.getBaseModifier(sample);
        float baseNoise = sample.baseNoise * baseModifier;
        baseNoise = this.carve(sample, carverSample.river, this.riverConfig, baseNoise, baseModifier, erosion);
        sample.baseNoise = baseNoise = this.carve(sample, carverSample.lake, this.lakeConfig, baseNoise, baseModifier, erosion);
        sample.riverNoise = RiverCarver.clipRiverNoise(sample);
    }

    private float carve(NoiseSample sample, NodeSample nodeSample, RiverConfig config, float baseNoise, float baseModifier, float erosion) {
        float modifiedBaseNoise = this.getBaseNoise(sample, nodeSample, config, baseModifier);
        if (modifiedBaseNoise == -1.0f) {
            return baseNoise;
        }
        float baseLevel = this.levels.toHeightNoise(modifiedBaseNoise, 0.0f);
        this.carve(baseLevel, erosion, sample, nodeSample, config);
        return modifiedBaseNoise;
    }

    private void carve(float baseLevel, float erosion, NoiseSample sample, NodeSample nodeSample, RiverConfig config) {
        float riverAlpha;
        if (nodeSample.isInvalid()) {
            return;
        }
        float height = sample.heightNoise;
        float position = nodeSample.position;
        float distance = nodeSample.distance;
        float valleyWidth = config.valleyWidth.at(position);
        float bankWidth = config.bankWidth.at(position);
        float bankDepth = config.bankDepth.at(position);
        float bedWidth = config.bedWidth.at(position);
        float bedDepth = config.bedDepth.at(position);
        float bedLevel = baseLevel - bedDepth * this.levels.unit;
        float bankLevel = baseLevel + bankDepth * this.levels.unit;
        float valleyAlpha = RiverCarver.getValleyAlpha(distance, bankWidth, valleyWidth, sample.baseNoise);
        if (valleyAlpha < 1.0f) {
            float level = Math.min(bankLevel, height);
            float modifier = this.getErosionModifier(erosion * config.erosion, valleyAlpha);
            height = NoiseUtil.lerp(level, height, valleyAlpha * modifier);
            sample.riverNoise *= this.getValleyNoise(distance, bankWidth, valleyWidth);
        }
        if ((riverAlpha = RiverCarver.getAlpha(distance, bedWidth, bankWidth)) < 1.0f) {
            float level = Math.min(bedLevel, height);
            height = NoiseUtil.lerp(level, height, riverAlpha);
            sample.terrainType = nodeSample.type;
            sample.riverNoise *= this.getRiverNoise(height, baseLevel, bankLevel);
        }
        sample.heightNoise = height;
    }

    private float getBaseNoise(NoiseSample sample, NodeSample nodeSample, RiverConfig config, float modifier) {
        if (nodeSample.isInvalid()) {
            return -1.0f;
        }
        float distance = nodeSample.distance;
        float position = nodeSample.position;
        float valleyRadius = config.valleyWidth.at(position);
        if (distance >= valleyRadius) {
            return -1.0f;
        }
        float bankRadius = config.bankWidth.at(position);
        if (distance <= bankRadius) {
            return nodeSample.level * modifier;
        }
        float alpha = (distance - bankRadius) / (valleyRadius - bankRadius);
        return NoiseUtil.lerp(nodeSample.level, sample.baseNoise, alpha) * modifier;
    }

    private float getBaseModifier(NoiseSample sample) {
        float min = 0.55f;
        float max = 1.0f;
        return NoiseUtil.map(sample.continentNoise, min, max, max - min);
    }

    private float getErosionModifier(float erosionNoise, float valleyAlpha) {
        float erosionFade = 1.0f - NoiseUtil.map(valleyAlpha, 0.975f, 1.0f, 0.025f);
        return 1.0f - erosionNoise * erosionFade;
    }

    private float getValleyNoise(float distance, float bankWidth, float valleyWidth) {
        float value = 0.12f + RiverCarver.getAlpha(distance, bankWidth, valleyWidth) / 0.88f;
        return MathUtil.clamp(value, 0.0f, 1.0f);
    }

    private float getRiverNoise(float height, float waterLevel, float bankLevel) {
        float value = RiverCarver.getAlpha(height, waterLevel, bankLevel);
        return MathUtil.clamp(value, 0.0f, 1.0f);
    }

    private static float clipRiverNoise(NoiseSample sample) {
        return sample.continentNoise < 0.5f ? 1.0f : sample.riverNoise;
    }

    private static float getValleyAlpha(float distance, float bankWidth, float valleyWidth, float baseValue) {
        float alpha = RiverCarver.getAlpha(distance, bankWidth, valleyWidth);
        float shapeAlpha = RiverCarver.getAlpha(baseValue, 0.4f, 0.6f);
        return NoiseUtil.lerp(alpha * alpha, alpha, shapeAlpha);
    }

    private static float getAlpha(float value, float min, float max) {
        return value <= min ? 0.0f : (value >= max ? 1.0f : (value - min) / (max - min));
    }

    private static float getBlendRadius(RiverConfig river, RiverConfig lakes) {
        float valleyWidth = Math.max(river.valleyWidth.max, lakes.valleyWidth.max);
        return Math.min(1.0f, valleyWidth + 0.2f);
    }
}
