package com.terraforged.mod.worldgen.noise.continent.config;

import com.terraforged.mod.worldgen.noise.continent.config.FloatRange;

public class RiverConfig {
    public float erosion = 0.075f;
    public final FloatRange bedWidth = new FloatRange(1.0f, 7.0f);
    public final FloatRange bankWidth = new FloatRange(3.0f, 30.0f);
    public final FloatRange valleyWidth = new FloatRange(55.0f, 150.0f);
    public final FloatRange bedDepth = new FloatRange(1.25f, 5.0f);
    public final FloatRange bankDepth = new FloatRange(1.25f, 3.0f);

    public RiverConfig copy(RiverConfig config) {
        this.erosion = config.erosion;
        this.bedDepth.copy(config.bedDepth);
        this.bankDepth.copy(config.bankDepth);
        this.bedWidth.copy(config.bedWidth);
        this.bankWidth.copy(config.bankWidth);
        this.valleyWidth.copy(config.valleyWidth);
        return this;
    }

    public RiverConfig scale(float frequency) {
        this.bedWidth.scale(frequency);
        this.bankWidth.scale(frequency);
        this.valleyWidth.scale(frequency);
        return this;
    }

    public static RiverConfig lake() {
        RiverConfig config = new RiverConfig();
        config.bankWidth.min = 22.0f;
        config.bankWidth.max = 36.0f;
        config.bankDepth.min = 0.75f;
        config.bankDepth.max = 1.25f;
        config.bedWidth.min = 8.0f;
        config.bedWidth.max = 15.0f;
        config.bedDepth.min = 2.0f;
        config.bedDepth.max = 8.0f;
        config.valleyWidth.min = 38.0f;
        config.valleyWidth.max = 62.0f;
        return config;
    }
}
