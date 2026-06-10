package com.terraforged.mod.worldgen.noise.continent;

import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.continent.shape.FalloffPoint;

public interface ContinentPoints {
    public static final float DEEP_OCEAN = 0.1f;
    public static final float SHALLOW_OCEAN = 0.25f;
    public static final float BEACH = 0.5f;
    public static final float COAST = 0.55f;
    public static final float INLAND = 0.6f;

    public static Terrain getTerrainType(float continentNoise) {
        if (continentNoise < 0.25f) {
            return TerrainType.DEEP_OCEAN;
        }
        if (continentNoise < 0.5f) {
            return TerrainType.SHALLOW_OCEAN;
        }
        if (continentNoise < 0.55f) {
            return TerrainType.COAST;
        }
        return TerrainType.NONE;
    }

    public static FalloffPoint[] getFalloff(ControlPoints controlPoints) {
        return new FalloffPoint[]{new FalloffPoint(controlPoints.inland, 1.0f, 1.0f), new FalloffPoint(controlPoints.coast, 0.55f, 1.0f), new FalloffPoint(controlPoints.beach, 0.5f, 0.55f), new FalloffPoint(controlPoints.shallowOcean, 0.25f, 0.5f), new FalloffPoint(controlPoints.deepOcean, 0.1f, 0.25f)};
    }

    public static float getFalloff(float continentNoise, FalloffPoint[] falloffCurve) {
        float previous = 1.0f;
        for (FalloffPoint falloff : falloffCurve) {
            if (continentNoise >= falloff.controlPoint()) {
                return MathUtil.map(continentNoise, falloff.controlPoint(), previous, falloff.min(), falloff.max());
            }
            previous = falloff.controlPoint();
        }
        return MathUtil.map(continentNoise, 0.0f, previous, 0.0f, 0.1f);
    }
}
