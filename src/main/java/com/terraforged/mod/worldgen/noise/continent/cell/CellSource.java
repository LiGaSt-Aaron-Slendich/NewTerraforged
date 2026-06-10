package com.terraforged.mod.worldgen.noise.continent.cell;

import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.util.Noise;

/*
 * Uses 'sealed' constructs - enablewith --sealed true
 */
public enum CellSource {
    PERLIN{

        @Override
        public float sample(int seed, float x, float y) {
            return Noise.singlePerlin2(x, y, seed, Interpolation.CURVE3);
        }
    }
    ,
    SIMPLEX{

        @Override
        public float sample(int seed, float x, float y) {
            return Noise.singleSimplex(x, y, seed);
        }
    }
    ,
    CUBIC{

        @Override
        public float sample(int seed, float x, float y) {
            return Noise.singleCubic(x, y, seed);
        }
    };


    public abstract float sample(int var1, float var2, float var3);

    public float getValue(int seed, float x, float y) {
        return (1.0f + this.sample(seed, x, y)) * 0.5f;
    }
}
