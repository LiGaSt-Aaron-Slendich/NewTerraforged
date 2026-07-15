/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.source;

import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.func.CellFunc;
import com.terraforged.noise.func.DistanceFunc;
import com.terraforged.noise.func.EdgeFunc;
import com.terraforged.noise.func.Interpolation;
import com.terraforged.noise.source.BillowNoise;
import com.terraforged.noise.source.CellEdgeNoise;
import com.terraforged.noise.source.CellNoise;
import com.terraforged.noise.source.Constant;
import com.terraforged.noise.source.CubicNoise;
import com.terraforged.noise.source.NoiseSource;
import com.terraforged.noise.source.PerlinNoise;
import com.terraforged.noise.source.PerlinNoise2;
import com.terraforged.noise.source.Rand;
import com.terraforged.noise.source.RidgeNoise;
import com.terraforged.noise.source.SimplexNoise;
import com.terraforged.noise.source.SimplexNoise2;
import com.terraforged.noise.source.SimplexRidgeNoise;
import com.terraforged.noise.source.Sin;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Builder {
    public static final int DEFAULT_SEED = 1337;
    public static final int DEFAULT_OCTAVES = 1;
    public static final float DEFAULT_GAIN = 0.5f;
    public static final float DEFAULT_RIDGE_GAIN = 0.975f;
    public static final float DEFAULT_LACUNARITY = 2.0f;
    public static final float DEFAULT_FREQUENCY = 1.0f;
    public static final float DEFAULT_DISTANCE = 1.0f;
    public static final CellFunc DEFAULT_CELL_FUNC = CellFunc.CELL_VALUE;
    public static final EdgeFunc DEFAULT_EDGE_FUNC = EdgeFunc.DISTANCE_2;
    public static final DistanceFunc DEFAULT_DIST_FUNC = DistanceFunc.EUCLIDEAN;
    public static Interpolation DEFAULT_INTERPOLATION = Interpolation.CURVE3;
    private int seed = 1337;
    private int octaves = 1;
    private float gain = Float.MAX_VALUE;
    private float lacunarity = 2.0f;
    private float frequency = 1.0f;
    private float displacement = 1.0f;
    private Module source = Source.ZERO;
    private CellFunc cellFunc = DEFAULT_CELL_FUNC;
    private EdgeFunc edgeFunc = DEFAULT_EDGE_FUNC;
    private DistanceFunc distFunc = DEFAULT_DIST_FUNC;
    private Interpolation interpolation = DEFAULT_INTERPOLATION;

    public int getSeed() {
        return this.seed;
    }

    public int getOctaves() {
        return this.octaves;
    }

    public float getGain() {
        if (this.gain == Float.MAX_VALUE) {
            this.gain = 0.5f;
        }
        return this.gain;
    }

    public float getFrequency() {
        return this.frequency;
    }

    public float getDisplacement() {
        return this.displacement;
    }

    public float getLacunarity() {
        return this.lacunarity;
    }

    public Interpolation getInterp() {
        return this.interpolation;
    }

    public CellFunc getCellFunc() {
        return this.cellFunc;
    }

    public EdgeFunc getEdgeFunc() {
        return this.edgeFunc;
    }

    public DistanceFunc getDistFunc() {
        return this.distFunc;
    }

    public Module getSource() {
        return this.source;
    }

    public Builder seed(int seed) {
        this.seed = seed;
        return this;
    }

    public Builder octaves(int octaves) {
        this.octaves = octaves;
        return this;
    }

    public Builder gain(double gain) {
        this.gain = (float)gain;
        return this;
    }

    public Builder lacunarity(double lacunarity) {
        this.lacunarity = (float)lacunarity;
        return this;
    }

    public Builder scale(int frequency) {
        this.frequency = 1.0f / (float)frequency;
        return this;
    }

    public Builder frequency(double frequency) {
        this.frequency = (float)frequency;
        return this;
    }

    public Builder displacement(double displacement) {
        this.displacement = (float)displacement;
        return this;
    }

    public Builder interp(Interpolation interpolation) {
        this.interpolation = interpolation;
        return this;
    }

    public Builder cellFunc(CellFunc cellFunc) {
        this.cellFunc = cellFunc;
        return this;
    }

    public Builder edgeFunc(EdgeFunc cellType) {
        this.edgeFunc = cellType;
        return this;
    }

    public Builder distFunc(DistanceFunc cellDistance) {
        this.distFunc = cellDistance;
        return this;
    }

    public Builder source(Module source) {
        this.source = source;
        return this;
    }

    public NoiseSource perlin() {
        return new PerlinNoise(this);
    }

    public NoiseSource perlin2() {
        return new PerlinNoise2(this);
    }

    public NoiseSource simplex() {
        return new SimplexNoise(this);
    }

    public NoiseSource simplex2() {
        return new SimplexNoise2(this);
    }

    public NoiseSource ridge() {
        if (this.gain == Float.MAX_VALUE) {
            this.gain = 0.975f;
        }
        return new RidgeNoise(this);
    }

    public NoiseSource simplexRidge() {
        if (this.gain == Float.MAX_VALUE) {
            this.gain = 0.975f;
        }
        return new SimplexRidgeNoise(this);
    }

    public NoiseSource billow() {
        return new BillowNoise(this);
    }

    public NoiseSource cubic() {
        return new CubicNoise(this);
    }

    public NoiseSource cell() {
        return new CellNoise(this);
    }

    public NoiseSource cellEdge() {
        return new CellEdgeNoise(this);
    }

    public NoiseSource sin() {
        return new Sin(this);
    }

    public Module constant() {
        return new Constant(this);
    }

    public Rand rand() {
        return new Rand(this);
    }

    public Module build(Source source) {
        return source.build(this);
    }

    public Module build(Class<? extends Module> type) {
        try {
            Constructor<? extends Module> constructor = type.getConstructor(Builder.class);
            return constructor.newInstance(this);
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return this.perlin();
        }
    }
}

