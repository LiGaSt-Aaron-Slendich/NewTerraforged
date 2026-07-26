package com.terraforged.mod.worldgen.cave;

import com.terraforged.noise.util.NoiseUtil;

public record CaveStatVector(float moisture, float temperature, float fertility) {
    public static final float MIN = -10.0f;
    public static final float MAX = 10.0f;
    public static final float NO_REQUIREMENT = -10.0f;
    public static final CaveStatVector ZERO = new CaveStatVector(0.0f, 0.0f, 0.0f);

    public CaveStatVector clamped() {
        return new CaveStatVector(NoiseUtil.clamp(this.moisture, -10.0f, 10.0f), NoiseUtil.clamp(this.temperature, -10.0f, 10.0f), NoiseUtil.clamp(this.fertility, -10.0f, 10.0f));
    }

    public CaveStatVector add(CaveStatVector other) {
        return new CaveStatVector(this.moisture + other.moisture, this.temperature + other.temperature, this.fertility + other.fertility).clamped();
    }

    public CaveStatVector scale(float factor) {
        return new CaveStatVector(this.moisture * factor, this.temperature * factor, this.fertility * factor).clamped();
    }

    public boolean meetsConditions(CaveStatVector conditions) {
        return this.moisture >= conditions.moisture && this.temperature >= conditions.temperature && this.fertility >= conditions.fertility;
    }
}
