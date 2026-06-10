package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.engine.world.terrain.Terrain;

public class NodeSample {
    public float projection = 0.0f;
    public float distance = Float.NaN;
    public float position = 0.0f;
    public float level = 0.0f;
    public final Terrain type;

    public NodeSample(Terrain type) {
        this.type = type;
    }

    public boolean isInvalid() {
        return Float.isNaN(this.distance);
    }

    public NodeSample reset() {
        this.distance = Float.MAX_VALUE;
        this.projection = 0.0f;
        this.position = 0.0f;
        this.level = 0.0f;
        return this;
    }

    public NodeSample invalidate() {
        this.distance = Float.NaN;
        return this;
    }
}
