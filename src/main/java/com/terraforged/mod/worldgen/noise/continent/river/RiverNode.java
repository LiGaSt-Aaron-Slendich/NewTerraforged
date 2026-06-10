package com.terraforged.mod.worldgen.noise.continent.river;

import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.source.Line;
import com.terraforged.noise.util.NoiseUtil;

public record RiverNode(float ax, float ay, float bx, float by, float ah, float bh, float ar, float br, float displacement) {
    public float getProjection(float x, float y) {
        float dx = this.bx - this.ax;
        float dy = this.by - this.ay;
        float v = (x - this.ax) * dx + (y - this.ay) * dy;
        return (v /= dx * dx + dy * dy) < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public float getDistance2(float x, float y, float t) {
        float pad = 0.05f;
        float alpha = NoiseUtil.map(t, pad, 1.0f - pad, 1.0f - pad * 2.0f);
        alpha = alpha < 0.5f ? alpha / 0.5f : (1.0f - alpha) / 0.5f;
        alpha = Interpolation.CURVE3.apply(alpha);
        float tx = this.getX(t);
        float ty = this.getY(t);
        float px = tx - (this.by - this.ay) * (alpha *= this.displacement);
        float py = ty + (this.bx - this.ax) * alpha;
        return Line.dist2(x, y, px, py);
    }

    public float getDistance(float x, float y, float t) {
        float d2 = this.getDistance2(x, y, t);
        return NoiseUtil.sqrt(d2);
    }

    public float getDistance(float t, float d2) {
        float tr = this.getRadius(t);
        return d2 >= tr * tr ? 0.0f : 1.0f - NoiseUtil.sqrt(d2) / tr;
    }

    public float getX(float t) {
        return this.ax + t * (this.bx - this.ax);
    }

    public float getY(float t) {
        return this.ay + t * (this.by - this.ay);
    }

    public float getHeight(float t) {
        return this.ah + t * (this.bh - this.ah);
    }

    public float getRadius(float t) {
        return this.ar + t * (this.br - this.ar);
    }
}
