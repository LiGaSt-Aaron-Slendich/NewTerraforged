/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.func;

import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;

public enum CellFunc {
    CELL_VALUE{

        @Override
        public float apply(int xc, int yc, float distance, int seed, Vec2f vec2f, Module lookup) {
            return NoiseUtil.valCoord2D(seed, xc, yc);
        }
    }
    ,
    NOISE_LOOKUP{

        @Override
        public float apply(int xc, int yc, float distance, int seed, Vec2f vec2f, Module lookup) {
            return lookup.getValue((float)xc + vec2f.x, (float)yc + vec2f.y);
        }

        @Override
        public float mapValue(float value, float min, float max, float range) {
            return value;
        }
    }
    ,
    DISTANCE{

        @Override
        public float apply(int xc, int yc, float distance, int seed, Vec2f vec2f, Module lookup) {
            return distance - 1.0f;
        }

        @Override
        public float mapValue(float value, float min, float max, float range) {
            return 0.0f;
        }
    };


    public abstract float apply(int var1, int var2, float var3, int var4, Vec2f var5, Module var6);

    public float mapValue(float value, float min, float max, float range) {
        return NoiseUtil.map(value, min, max, range);
    }
}

