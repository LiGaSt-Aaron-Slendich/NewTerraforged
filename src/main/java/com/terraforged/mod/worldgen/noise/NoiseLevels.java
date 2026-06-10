package com.terraforged.mod.worldgen.noise;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import com.terraforged.noise.util.NoiseUtil;

public class NoiseLevels {
    public final boolean auto;
    public final float scale;
    public final float unit;
    public final float depthMin;
    public final float depthRange;
    public final float heightMin;
    public final float baseRange;
    public final float heightRange;
    public final float frequency;

    public NoiseLevels(boolean autoScale, float scale, int seaLevel, int seaFloor, int worldHeight, int baseHeight) {
        this.auto = autoScale;
        this.scale = scale;
        this.unit = 1.0f / (float)worldHeight;
        this.depthMin = (float)seaFloor / (float)worldHeight;
        this.heightMin = (float)seaLevel / (float)worldHeight;
        this.baseRange = (float)baseHeight / (float)worldHeight;
        this.heightRange = 1.0f - (this.heightMin + this.baseRange);
        this.depthRange = this.heightMin - this.depthMin;
        this.frequency = NoiseLevels.calcFrequency(worldHeight - seaLevel, this.auto, scale);
        TerraForged.LOG.debug("Sea Level: {}, Base Height:  {}, World Height: {}", seaLevel, baseHeight, worldHeight);
    }

    public float scale(int level) {
        return (float)level * this.unit;
    }

    public float toDepthNoise(float noise) {
        return this.depthMin + noise * this.depthRange;
    }

    public float toHeightNoise(float baseNoise, float heightNoise) {
        return this.heightMin + this.baseRange * baseNoise + this.heightRange * heightNoise;
    }

    public float floor(float value) {
        return (float)NoiseUtil.floor(value / this.unit) * this.unit;
    }

    public float ceil(float value) {
        return this.unit + (float)NoiseUtil.floor(value / this.unit) * this.unit;
    }

    public static NoiseLevels getDefault() {
        return TerrainLevels.DEFAULT.get().noiseLevels;
    }

    public static float calcFrequency(int verticalRange, boolean auto, float scale) {
        float f = scale = scale <= 0.0f ? 1.0f : scale;
        if (!auto) {
            return scale;
        }
        float frequency = 194.0f / (float)verticalRange;
        return frequency * scale;
    }
}
