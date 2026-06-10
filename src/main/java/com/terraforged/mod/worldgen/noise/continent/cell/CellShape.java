package com.terraforged.mod.worldgen.noise.continent.cell;

import com.terraforged.mod.util.MathUtil;

/*
 * Uses 'sealed' constructs - enablewith --sealed true
 */
public enum CellShape {
    SQUARE,
    HEXAGON{

        @Override
        public float adjustY(float y) {
            return y * 1.2f;
        }

        @Override
        public float getCellX(int hash, int cx, int cy, float jitter) {
            float ox = (float)(cy & 1) * 0.5f;
            float jx = ox > 0.0f ? jitter * 0.5f : jitter;
            return MathUtil.getPosX(hash, cx, jx) + ox;
        }
    };


    public float adjustX(float x) {
        return x;
    }

    public float adjustY(float y) {
        return y;
    }

    public float getCellX(int hash, int cx, int cy, float jitter) {
        return MathUtil.getPosX(hash, cx, jitter);
    }

    public float getCellY(int hash, int cx, int cy, float jitter) {
        return MathUtil.getPosY(hash, cy, jitter);
    }
}
