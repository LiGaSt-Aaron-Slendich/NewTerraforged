package com.terraforged.mod.worldgen.biome.viability;

import com.terraforged.mod.worldgen.biome.IBiomeSampler;
import com.terraforged.mod.worldgen.biome.viability.MultViability;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.Arrays;

public interface Viability {
    public static final Viability NONE = (x, z, ctx) -> 1.0f;

    public float getFitness(int var1, int var2, Context var3);

    default public float getScaler(TerrainLevels levels) {
        return (float)levels.maxY / 255.0f;
    }

    default public Viability mult(Viability ... others) {
        Viability[] copy = Arrays.copyOf(others, others.length + 1);
        copy[others.length] = this;
        return new MultViability(copy);
    }

    public static float getFallOff(float value, float max) {
        return value < max ? 1.0f - value / max : 0.0f;
    }

    public static float getFallOff(float value, float min, float mid, float max) {
        if (value < min) {
            return 0.0f;
        }
        if (value < mid) {
            return (value - min) / (mid - min);
        }
        if (value < max) {
            return 1.0f - (value - mid) / (max - mid);
        }
        return 0.0f;
    }

    public static interface Context {
        public int seed();

        public boolean edge();

        public TerrainLevels getLevels();

        public TerrainData getTerrain();

        public IBiomeSampler getClimateSampler();
    }
}
