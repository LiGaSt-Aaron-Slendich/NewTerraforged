/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.cereal.spec.Context;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;

public class Line
implements Module {
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final float dx;
    private final float dy;
    private final float orthX1;
    private final float orthY1;
    private final float orthX2;
    private final float orthY2;
    private final float length2;
    private final float featherBias;
    private final float featherScale;
    private final Module fadeIn;
    private final Module fadeOut;
    private final Module radius;

    public Line(float x1, float y1, float x2, float y2, Module radius2, Module fadeIn, Module fadeOut, float feather) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.orthX1 = x1 + (y2 - y1);
        this.orthY1 = y1 + (x1 - x2);
        this.orthX2 = x2 + (y2 - y1);
        this.orthY2 = y2 + (x1 - x2);
        this.dx = x2 - x1;
        this.dy = y2 - y1;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.radius = radius2;
        this.featherScale = feather;
        this.featherBias = 1.0f - feather;
        this.length2 = this.dx * this.dx + this.dy * this.dy;
    }

    @Override
    public String getSpecName() {
        return "Line";
    }

    @Override
    public float getValue(float x, float y) {
        float widthMod = this.getWidthModifier(x, y);
        return this.getValue(x, y, widthMod);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Line line = (Line)o;
        if (Float.compare(line.x1, this.x1) != 0) {
            return false;
        }
        if (Float.compare(line.y1, this.y1) != 0) {
            return false;
        }
        if (Float.compare(line.x2, this.x2) != 0) {
            return false;
        }
        if (Float.compare(line.y2, this.y2) != 0) {
            return false;
        }
        if (Float.compare(line.dx, this.dx) != 0) {
            return false;
        }
        if (Float.compare(line.dy, this.dy) != 0) {
            return false;
        }
        if (Float.compare(line.orthX1, this.orthX1) != 0) {
            return false;
        }
        if (Float.compare(line.orthY1, this.orthY1) != 0) {
            return false;
        }
        if (Float.compare(line.orthX2, this.orthX2) != 0) {
            return false;
        }
        if (Float.compare(line.orthY2, this.orthY2) != 0) {
            return false;
        }
        if (Float.compare(line.length2, this.length2) != 0) {
            return false;
        }
        if (Float.compare(line.featherBias, this.featherBias) != 0) {
            return false;
        }
        if (Float.compare(line.featherScale, this.featherScale) != 0) {
            return false;
        }
        if (!this.fadeIn.equals(line.fadeIn)) {
            return false;
        }
        if (!this.fadeOut.equals(line.fadeOut)) {
            return false;
        }
        return this.radius.equals(line.radius);
    }

    public int hashCode() {
        int result = this.x1 != 0.0f ? Float.floatToIntBits(this.x1) : 0;
        result = 31 * result + (this.y1 != 0.0f ? Float.floatToIntBits(this.y1) : 0);
        result = 31 * result + (this.x2 != 0.0f ? Float.floatToIntBits(this.x2) : 0);
        result = 31 * result + (this.y2 != 0.0f ? Float.floatToIntBits(this.y2) : 0);
        result = 31 * result + (this.dx != 0.0f ? Float.floatToIntBits(this.dx) : 0);
        result = 31 * result + (this.dy != 0.0f ? Float.floatToIntBits(this.dy) : 0);
        result = 31 * result + (this.orthX1 != 0.0f ? Float.floatToIntBits(this.orthX1) : 0);
        result = 31 * result + (this.orthY1 != 0.0f ? Float.floatToIntBits(this.orthY1) : 0);
        result = 31 * result + (this.orthX2 != 0.0f ? Float.floatToIntBits(this.orthX2) : 0);
        result = 31 * result + (this.orthY2 != 0.0f ? Float.floatToIntBits(this.orthY2) : 0);
        result = 31 * result + (this.length2 != 0.0f ? Float.floatToIntBits(this.length2) : 0);
        result = 31 * result + (this.featherBias != 0.0f ? Float.floatToIntBits(this.featherBias) : 0);
        result = 31 * result + (this.featherScale != 0.0f ? Float.floatToIntBits(this.featherScale) : 0);
        result = 31 * result + this.fadeIn.hashCode();
        result = 31 * result + this.fadeOut.hashCode();
        result = 31 * result + this.radius.hashCode();
        return result;
    }

    public float getValue(float x, float y, float widthModifier) {
        return this.getValue(x, y, 0.0f, widthModifier);
    }

    public float getValue(float x, float y, float minWidth2, float widthModifier) {
        float radius2;
        float dist2 = this.getDistance2(x, y);
        if (dist2 > (radius2 = minWidth2 + this.radius.getValue(x, y) * widthModifier)) {
            return 0.0f;
        }
        float value = dist2 / radius2;
        if (this.featherScale == 0.0f) {
            return 1.0f - value;
        }
        float feather = this.featherBias + widthModifier * this.featherScale;
        return (1.0f - value) * feather;
    }

    public boolean clipStart(float x, float y) {
        return Line.sign(x, y, this.x1, this.y1, this.orthX1, this.orthY1) > 0;
    }

    public boolean clipEnd(float x, float y) {
        return Line.sign(x, y, this.x2, this.y2, this.orthX2, this.orthY2) < 0;
    }

    public float getWidthModifier(float x, float y) {
        float dist;
        float d1 = Line.dist2(x, y, this.x1, this.y1);
        if (d1 == 0.0f) {
            return 0.0f;
        }
        float d2 = Line.dist2(x, y, this.x2, this.y2);
        if (d2 == 0.0f) {
            return 0.0f;
        }
        float fade = 1.0f;
        float in = this.fadeIn.getValue(x, y);
        float out = this.fadeOut.getValue(x, y);
        if (in > 0.0f && d1 < (dist = in * this.length2)) {
            fade *= d1 / dist;
        }
        if (out > 0.0f && d2 < (dist = out * this.length2)) {
            fade *= d2 / dist;
        }
        return fade;
    }

    private float getDistance2(float x, float y) {
        float t = (x - this.x1) * this.dx + (y - this.y1) * this.dy;
        float s = NoiseUtil.clamp(t / this.length2, 0.0f, 1.0f);
        float ix = this.x1 + s * this.dx;
        float iy = this.y1 + s * this.dy;
        return Line.dist2(x, y, ix, iy);
    }

    public static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static int sign(float x, float y, float x1, float y1, float x2, float y2) {
        float value = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
        if (value == 0.0f) {
            return 0;
        }
        if (value < 0.0f) {
            return -1;
        }
        return 1;
    }

    public static boolean intersect(float ax1, float ay1, float ax2, float ay2, float bx1, float by1, float bx2, float by2) {
        return Line.relativeCCW(ax1, ay1, ax2, ay2, bx1, by1) * Line.relativeCCW(ax1, ay1, ax2, ay2, bx2, by2) <= 0 && Line.relativeCCW(bx1, by1, bx2, by2, ax1, ay1) * Line.relativeCCW(bx1, by1, bx2, by2, ax2, ay2) <= 0;
    }

    public static float distanceOnLine(float x, float y, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float v = (x - ax) * dx + (y - ay) * dy;
        return v / (dx * dx + dy * dy);
    }

    public static float distance2Line(float x, float y, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float v = (x - ax) * dx + (y - ay) * dy;
        if ((v /= dx * dx + dy * dy) < 0.0f || v > 1.0f) {
            return -1.0f;
        }
        float ox = ax + dx * v;
        float oy = ay + dy * v;
        return NoiseUtil.dist2(x, y, ox, oy);
    }

    public static float distance2LineIncEnds(float x, float y, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float v = (x - ax) * dx + (y - ay) * dy;
        if ((v /= dx * dx + dy * dy) < 0.0f) {
            return Line.dist2(x, y, ax, ay);
        }
        if (v > 1.0f) {
            return Line.dist2(x, y, bx, by);
        }
        return Line.dist2(x, y, ax + dx * v, ay + dy * v);
    }

    private static int relativeCCW(float x1, float y1, float x2, float y2, float px, float py) {
        double ccw = (px -= x1) * (y2 -= y1) - (py -= y1) * (x2 -= x1);
        if (ccw == 0.0 && (ccw = (double)(px * x2 + py * y2)) > 0.0 && (ccw = (double)((px -= x2) * x2 + (py -= y2) * y2)) < 0.0) {
            ccw = 0.0;
        }
        return ccw < 0.0 ? -1 : (ccw > 0.0 ? 1 : 0);
    }

    private static Line create(DataObject data, DataSpec<Line> spec, Context context) {
        return new Line(spec.get("x1", data, DataValue::asFloat).floatValue(), spec.get("y1", data, DataValue::asFloat).floatValue(), spec.get("x2", data, DataValue::asFloat).floatValue(), spec.get("y2", data, DataValue::asFloat).floatValue(), spec.get("radius", data, Module.class), spec.get("fade_in", data, Module.class), spec.get("fade_out", data, Module.class), spec.get("feather", data, DataValue::asFloat).floatValue());
    }

    public static DataSpec<Line> spec() {
        return DataSpec.builder("Line", Line.class, Line::create).add("x1", (Object)Float.valueOf(0.0f), l -> Float.valueOf(l.x1)).add("y1", (Object)Float.valueOf(0.0f), l -> Float.valueOf(l.y1)).add("x2", (Object)Float.valueOf(0.0f), l -> Float.valueOf(l.x2)).add("y2", (Object)Float.valueOf(0.0f), l -> Float.valueOf(l.y2)).add("feather", (Object)Float.valueOf(0.0f), l -> Float.valueOf(l.featherScale)).addObj("radius", Module.class, l -> l.radius).addObj("fade_in", Module.class, l -> l.fadeIn).addObj("fade_out", Module.class, l -> l.fadeOut).build();
    }
}

