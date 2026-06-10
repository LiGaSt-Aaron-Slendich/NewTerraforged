package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.util.seed.ContextSeedable;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.noise.NoiseCodec;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;

public class NoiseCave
implements ContextSeedable<NoiseCave> {
        public static final Codec<NoiseCave> CODEC = LazyCodec.record(instance -> instance.group(
            Codec.INT.optionalFieldOf("seed", 0).forGetter(c -> c.seed),
            CaveType.CODEC.fieldOf("type").forGetter(c -> c.type),
            CavePlacementType.CODEC.optionalFieldOf("placement_type", CavePlacementType.FULL_REGION).forGetter(c -> c.placementType),
            NoiseCodec.CODEC.fieldOf("elevation").forGetter(c -> c.elevation),
            NoiseCodec.CODEC.fieldOf("shape").forGetter(c -> c.shape),
            NoiseCodec.CODEC.fieldOf("floor").forGetter(c -> c.floor),
            Codec.INT.fieldOf("size").forGetter(c -> c.size),
            Codec.INT.optionalFieldOf("min_y", -32).forGetter(c -> c.minY),
            Codec.INT.fieldOf("max_y").forGetter(c -> c.maxY)
    ).apply(instance, NoiseCave::new));

    private final int seed;
    private final CaveType type;
    private final CavePlacementType placementType;
    private final Module elevation;
    private final Module shape;
    private final Module floor;
    private final int size;
    private final int minY;
    private final int maxY;
    private final int rangeY;

    public NoiseCave(int seed, CaveType type, CavePlacementType placementType, Module elevation, Module shape, Module floor, int size, int minY, int maxY) {
        this.seed = seed;
        this.type = type;
        this.placementType = placementType;
        this.elevation = elevation;
        this.shape = shape;
        this.floor = floor;
        this.size = size;
        this.minY = minY;
        this.maxY = maxY;
        this.rangeY = maxY - minY;
    }

    public NoiseCave(int seed, CaveType type, Module elevation, Module shape, Module floor, int size, int minY, int maxY) {
        this(seed, type, CavePlacementType.FULL_REGION, elevation, shape, floor, size, minY, maxY);
    }

    @Override
    public NoiseCave withSeed(long seed) {
        Module e = this.withSeed(seed, this.elevation, Module.class);
        Module s = this.withSeed(seed, this.shape, Module.class);
        Module f = this.withSeed(seed, this.floor, Module.class);
        return new NoiseCave(this.seed, this.type, this.placementType, e, s, f, this.size, this.minY, this.maxY);
    }

    public int getSeed() {
        return this.seed;
    }

    public CaveType getType() {
        return this.type;
    }

    public CavePlacementType getPlacementType() {
        return this.placementType;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getHeight(int seed, int x, int z) {
        return NoiseCave.getScaleValue(seed, x, z, 1.0f, this.minY, this.rangeY, this.elevation);
    }

    public int getCavernSize(int seed, int x, int z, float modifier) {
        return NoiseCave.getScaleValue(seed, x, z, modifier, 0, this.size, this.shape);
    }

    public int getFloorDepth(int seed, int x, int z, int caveSize) {
        return NoiseCave.getScaleValue(seed, x, z, 1.0f, 0, caveSize, this.floor);
    }

    public static int calcCeilingPatchHeight(int caveHeight, float patchMin, float patchMax, float factor) {
        float pct = patchMin + factor * (patchMax - patchMin);
        return Math.max(1, NoiseUtil.floor((float)caveHeight * pct));
    }

    public static int calcIslandRadius(float maxRadiusChunks) {
        return Math.max(1, NoiseUtil.floor(maxRadiusChunks * 16.0f));
    }

    public static int calcIslandHeight(int radiusBlocks) {
        return Math.max(1, (int)((float)radiusBlocks * 3.0f));
    }

    public String toString() {
        return "NoiseCave{type=" + this.type + ", placement=" + this.placementType + ", minY=" + this.minY + ", maxY=" + this.maxY + "}";
    }

    private static int getScaleValue(int seed, int x, int z, float modifier, int min, int range, Module noise) {
        if (range <= 0) {
            return 0;
        }
        return min + NoiseUtil.floor(CaveNoise.sample(noise, seed, x, z) * (float)range * modifier);
    }
}
