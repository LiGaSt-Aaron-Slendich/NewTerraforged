package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.util.seed.Seedable;
import com.terraforged.mod.util.storage.Object2FloatCache;
import com.terraforged.mod.util.storage.WeightMap;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import com.terraforged.noise.util.NoiseUtil;

public class TerrainBlender
implements Seedable<TerrainBlender> {
    private static final int REGION_SEED_OFFSET = 21491124;
    private static final int WARP_SEED_OFFSET = 12678;
    private final int scale;
    private final float frequency;
    private final float jitter;
    private final float blending;
    private final Domain warp;
    private final WeightMap<TerrainNoise> terrains;
    private final ThreadLocal<Blender> localBlender = ThreadLocal.withInitial(Blender::new);

    public TerrainBlender(long seed, int scale, float jitter, float blending, TerrainNoise[] terrains) {
        this.scale = scale;
        this.frequency = 1.0f / (float)scale;
        this.jitter = jitter;
        this.blending = blending;
        this.terrains = WeightMap.of(terrains);
        this.warp = Domain.warp(Source.SIMPLEX, (int)seed + 12678, scale, 3, (float)scale / 2.5f);
    }

    @Override
    public TerrainBlender withSeed(long seed) {
        TerrainNoise[] input = this.terrains.getValues();
        TerrainNoise[] output = new TerrainNoise[input.length];
        for (int i = 0; i < output.length; ++i) {
            output[i] = input[i].withSeed(seed);
        }
        return new TerrainBlender(seed, this.scale, this.jitter, this.blending, output);
    }

    public float getValue(int seed, float x, float z) {
        Blender blender = this.localBlender.get();
        return this.getValue(seed, x, z, blender);
    }

    public float getValue(int seed, float x, float z, Blender blender) {
        float rx = this.warp.getX(x, z) * this.frequency;
        float rz = this.warp.getY(x, z) * this.frequency;
        TerrainBlender.getCell(seed + 21491124, rx, rz, this.jitter, blender);
        return blender.getValue(seed, x, z, this.blending, this.terrains);
    }

    public Blender getBlenderResource() {
        return this.localBlender.get();
    }

    public Terrain getTerrain(Blender blender) {
        float index = blender.getCentreNoiseIndex();
        return this.terrains.getValue(index).terrain();
    }

    public SpiralIterator.PositionFinder findNearest(int seed, float x, float z, int minRadius, int maxRadius, Terrain type) {
        TerrainNoise terrain = this.terrains.find(t -> t.terrain().getName().equals(type.getName()));
        if (terrain == null) {
            return null;
        }
        long band = this.terrains.getBand(terrain);
        float lower = PosUtil.unpackLeftf(band);
        float upper = PosUtil.unpackRightf(band);
        return this.iterator(seed, x, z, minRadius, maxRadius).finder(it -> {
            long pos = TerrainBlender.find(seed + 21491124, this.jitter, lower, upper, it);
            float px = PosUtil.unpackLeftf(pos) / this.frequency;
            float pz = PosUtil.unpackRightf(pos) / this.frequency;
            return PosUtil.packf(px, pz);
        });
    }

    public SpiralIterator iterator(int seed, float x, float z, int min, int max) {
        float rx = this.warp.getX(x, z) * this.frequency;
        float rz = this.warp.getY(x, z) * this.frequency;
        int cx = NoiseUtil.floor(rx);
        int cz = NoiseUtil.floor(rz);
        return new SpiralIterator(cx, cz, min, max);
    }

    private static long find(int seed, float jitter, float lower, float upper, SpiralIterator iterator) {
        while (iterator.hasNext()) {
            int cz;
            long next = iterator.next();
            int cx = PosUtil.unpackLeft(next);
            int hash = NoiseUtil.hash2D(seed, cx, cz = PosUtil.unpackRight(next));
            float noise = MathUtil.rand(hash);
            if (!(noise > lower) || !(noise <= upper)) continue;
            float dx = MathUtil.rand(hash, 1619);
            float dz = MathUtil.rand(hash, 31337);
            float px = (float)cx + dx * jitter;
            float pz = (float)cz + dz * jitter;
            return PosUtil.packf(px, pz);
        }
        return 0L;
    }

    private static void getCell(int seed, float x, float z, float jitter, Blender blender) {
        int maxX = NoiseUtil.floor(x) + 1;
        int maxZ = NoiseUtil.floor(z) + 1;
        blender.closestIndex = 0;
        blender.closestIndex2 = 0;
        int nearestIndex = -1;
        int nearestIndex2 = -1;
        float nearestDistance = Float.MAX_VALUE;
        float nearestDistance2 = Float.MAX_VALUE;
        int i = 0;
        for (int cz = maxZ - 2; cz <= maxZ; ++cz) {
            int cx = maxX - 2;
            while (cx <= maxX) {
                int hash = NoiseUtil.hash2D(seed, cx, cz);
                float dx = MathUtil.rand(hash, 1619);
                float dz = MathUtil.rand(hash, 31337);
                float px = (float)cx + dx * jitter;
                float pz = (float)cz + dz * jitter;
                float dist2 = NoiseUtil.dist2(x, z, px, pz);
                blender.hashes[i] = hash;
                blender.distances[i] = dist2;
                if (dist2 < nearestDistance) {
                    nearestDistance2 = nearestDistance;
                    nearestDistance = dist2;
                    nearestIndex2 = nearestIndex;
                    nearestIndex = i;
                } else if (dist2 < nearestDistance2) {
                    nearestDistance2 = dist2;
                    nearestIndex2 = i;
                }
                ++cx;
                ++i;
            }
        }
        blender.closestIndex = nearestIndex;
        blender.closestIndex2 = nearestIndex2;
    }

    public static class Blender {
        protected int closestIndex;
        protected int closestIndex2;
        protected final int[] hashes = new int[9];
        protected final float[] distances = new float[9];
        protected final Object2FloatCache<TerrainNoise> cache = new Object2FloatCache(9);

        public float getCentreNoiseIndex() {
            return this.getNoiseIndex(this.closestIndex);
        }

        public float getDistance(int index) {
            return NoiseUtil.sqrt(this.distances[index]);
        }

        public float getCentreValue(int seed, float x, float z, WeightMap<TerrainNoise> terrains) {
            float noise = this.getCentreNoiseIndex();
            return terrains.getValue(noise).noise().getValue(x, z);
        }

        public float getValue(int seed, float x, float z, float blending, WeightMap<TerrainNoise> terrains) {
            float blendRadius;
            float dist1;
            float borderDistance;
            float blendStart;
            float dist0 = this.getDistance(this.closestIndex);
            if (dist0 <= (blendStart = (borderDistance = (dist0 + (dist1 = this.getDistance(this.closestIndex2))) * 0.5f) - (blendRadius = borderDistance * blending))) {
                return this.getCentreValue(seed, x, z, terrains);
            }
            return this.getBlendedValue(seed, x, z, dist0, dist1, blendRadius, terrains);
        }

        public float getBlendedValue(int seed, float x, float z, float nearest, float nearest2, float blendRange, WeightMap<TerrainNoise> terrains) {
            this.cache.clear();
            float sumNoise = this.getCacheValue(seed, this.closestIndex, x, z, terrains);
            float sumWeight = Blender.getWeight(nearest, nearest, blendRange);
            float nearestWeight2 = Blender.getWeight(nearest2, nearest, blendRange);
            if (nearestWeight2 > 0.0f) {
                sumNoise += this.getCacheValue(seed, this.closestIndex2, x, z, terrains) * nearestWeight2;
                sumWeight += nearestWeight2;
            }
            for (int i = 0; i < 9; ++i) {
                float weight;
                if (i == this.closestIndex || i == this.closestIndex2 || !((weight = Blender.getWeight(this.getDistance(i), nearest, blendRange)) > 0.0f)) continue;
                sumNoise += this.getCacheValue(seed, i, x, z, terrains) * weight;
                sumWeight += weight;
            }
            return NoiseUtil.clamp(sumNoise / sumWeight, 0.0f, 1.0f);
        }

        private float getCacheValue(int seed, int index, float x, float z, WeightMap<TerrainNoise> terrains) {
            float noiseIndex = this.getNoiseIndex(index);
            TerrainNoise terrain = terrains.getValue(noiseIndex);
            float value = this.cache.get(terrain);
            if (Float.isNaN(value)) {
                value = terrain.noise().getValue(x, z);
                this.cache.put(terrain, value);
            }
            return value;
        }

        private float getNoiseIndex(int index) {
            return MathUtil.rand(this.hashes[index]);
        }

        private static float getWeight(float dist, float origin, float blendRange) {
            float delta = dist - origin;
            if (delta <= 0.0f) {
                return 1.0f;
            }
            if (delta >= blendRange) {
                return 0.0f;
            }
            float weight = 1.0f - delta / blendRange;
            return weight * weight;
        }
    }
}
