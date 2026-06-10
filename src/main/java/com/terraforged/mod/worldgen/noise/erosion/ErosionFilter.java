package com.terraforged.mod.worldgen.noise.erosion;

import com.terraforged.engine.settings.FilterSettings;
import com.terraforged.engine.util.FastRandom;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.mod.worldgen.noise.erosion.NoiseTileSize;
import com.terraforged.noise.util.NoiseUtil;

public class ErosionFilter {
    private static final float HEIGHT_FALL_OFF = 0.4f;
    private static final int HEIGHT = 0;
    private static final int GRAD_X = 1;
    private static final int GRAD_Y = 2;
    private static final int erosionRadius = 7;
    private static final float inertia = 0.005f;
    private static final float sedimentCapacityFactor = 7.0f;
    private static final float minSedimentCapacity = 0.008f;
    private static final float evaporateSpeed = 0.35f;
    private static final float gravity = 2.5f;
    private final float erodeSpeed;
    private final float depositSpeed;
    private final float initialSpeed;
    private final float initialWaterVolume;
    private final int maxDropletLifetime;
    private final int[][] erosionBrushIndices;
    private final float[][] erosionBrushWeights;
    private final int iterations;

    public ErosionFilter(int mapSize, FilterSettings.Erosion settings) {
        this.iterations = settings.dropletsPerChunk;
        this.erodeSpeed = settings.erosionRate;
        this.depositSpeed = settings.depositeRate;
        this.initialSpeed = settings.dropletVelocity;
        this.initialWaterVolume = settings.dropletVolume;
        this.maxDropletLifetime = settings.dropletLifetime;
        this.erosionBrushIndices = new int[mapSize * mapSize][];
        this.erosionBrushWeights = new float[mapSize * mapSize][];
        this.initBrushes(mapSize, 7);
    }

    public void apply(int seed, int chunkX, int chunkZ, NoiseTileSize size, Resource resource, FastRandom random, float[] map) {
        int maxIndex = size.regionLength - 2;
        for (int i = 0; i < this.iterations; ++i) {
            long iterationSeed = NoiseUtil.seed(seed, i);
            for (int dz = size.chunkMin; dz < size.chunkMax; ++dz) {
                int startZ = dz - size.chunkMin << 4;
                for (int dx = size.chunkMin; dx < size.chunkMax; ++dx) {
                    int startX = dx - size.chunkMin << 4;
                    long chunkSeed = NoiseUtil.seed(chunkX + dx, chunkZ + dz);
                    random.seed(chunkSeed, iterationSeed);
                    int x = startX + random.nextInt(16);
                    int z = startZ + random.nextInt(16);
                    x = MathUtil.clamp(x, 1, maxIndex);
                    z = MathUtil.clamp(z, 1, maxIndex);
                    this.applyDrop(x, z, map, size.regionLength, resource);
                }
            }
        }
    }

    private void applyDrop(float posX, float posY, float[] map, int mapSize, Resource resource) {
        float dirX = 0.0f;
        float dirY = 0.0f;
        float sediment = 0.0f;
        float speed = this.initialSpeed;
        float water = this.initialWaterVolume;
        for (int lifetime = 0; lifetime < this.maxDropletLifetime; ++lifetime) {
            int nodeX = (int)posX;
            int nodeY = (int)posY;
            int dropletIndex = nodeY * mapSize + nodeX;
            float cellOffsetX = posX - (float)nodeX;
            float cellOffsetY = posY - (float)nodeY;
            float[] gradient = this.grad(map, mapSize, posX, posY, resource.grad1);
            float len2 = (dirX = dirX * 0.005f - gradient[1] * 0.995f) * dirX + (dirY = dirY * 0.005f - gradient[2] * 0.995f) * dirY;
            if (len2 == 0.0f) {
                return;
            }
            float len = NoiseUtil.sqrt(len2);
            posX += (dirX /= len);
            posY += (dirY /= len);
            if (dirX == 0.0f && dirY == 0.0f || posX < 0.0f || posX >= (float)(mapSize - 1) || posY < 0.0f || posY >= (float)(mapSize - 1)) {
                return;
            }
            float falloff = ErosionFilter.getFalloff(map[dropletIndex]);
            float newHeight = this.grad(map, mapSize, posX, posY, resource.grad2)[0];
            float deltaHeight = (newHeight - gradient[0]) * falloff;
            float sedimentCapacity = Math.max(-deltaHeight * speed * water * 7.0f, 0.008f);
            if (sediment > sedimentCapacity || deltaHeight > 0.0f) {
                float amountToDeposit = deltaHeight > 0.0f ? Math.min(deltaHeight, sediment) : (sediment - sedimentCapacity) * this.depositSpeed;
                sediment -= amountToDeposit;
                int n = dropletIndex;
                map[n] = map[n] + amountToDeposit * (1.0f - cellOffsetX) * (1.0f - cellOffsetY);
                int n2 = dropletIndex + 1;
                map[n2] = map[n2] + amountToDeposit * cellOffsetX * (1.0f - cellOffsetY);
                int n3 = dropletIndex + mapSize;
                map[n3] = map[n3] + amountToDeposit * (1.0f - cellOffsetX) * cellOffsetY;
                int n4 = dropletIndex + mapSize + 1;
                map[n4] = map[n4] + amountToDeposit * cellOffsetX * cellOffsetY;
            } else {
                float amountToErode = Math.min((sedimentCapacity - sediment) * this.erodeSpeed, -deltaHeight);
                for (int brushPointIndex = 0; brushPointIndex < this.erosionBrushIndices[dropletIndex].length; ++brushPointIndex) {
                    int nodeIndex = this.erosionBrushIndices[dropletIndex][brushPointIndex];
                    float weighedErodeAmount = amountToErode * this.erosionBrushWeights[dropletIndex][brushPointIndex];
                    float deltaSediment = Math.min(map[nodeIndex], weighedErodeAmount);
                    int n = nodeIndex;
                    map[n] = map[n] - deltaSediment;
                    sediment += deltaSediment;
                }
            }
            float speed2 = speed * speed + deltaHeight * 2.5f;
            if (speed2 <= 0.0f) {
                return;
            }
            speed = NoiseUtil.sqrt(speed2);
            water *= 0.65f;
        }
    }

    private void initBrushes(int size, int radius) {
        int[] xOffsets = new int[radius * radius * 4];
        int[] yOffsets = new int[radius * radius * 4];
        float[] weights = new float[radius * radius * 4];
        float weightSum = 0.0f;
        int addIndex = 0;
        for (int i = 0; i < this.erosionBrushIndices.length; ++i) {
            int centreX = i % size;
            int centreY = i / size;
            if (centreY <= radius || centreY >= size - radius || centreX <= radius + 1 || centreX >= size - radius) {
                weightSum = 0.0f;
                addIndex = 0;
                for (int y = -radius; y <= radius; ++y) {
                    for (int x = -radius; x <= radius; ++x) {
                        float sqrDst = x * x + y * y;
                        if (!(sqrDst < (float)(radius * radius))) continue;
                        int coordX = centreX + x;
                        int coordY = centreY + y;
                        if (coordX < 0 || coordX >= size || coordY < 0 || coordY >= size) continue;
                        float weight = 1.0f - (float)Math.sqrt(sqrDst) / (float)radius;
                        weightSum += weight;
                        weights[addIndex] = weight;
                        xOffsets[addIndex] = x;
                        yOffsets[addIndex] = y;
                        ++addIndex;
                    }
                }
            }
            int numEntries = addIndex;
            this.erosionBrushIndices[i] = new int[numEntries];
            this.erosionBrushWeights[i] = new float[numEntries];
            for (int j = 0; j < numEntries; ++j) {
                this.erosionBrushIndices[i][j] = (yOffsets[j] + centreY) * size + xOffsets[j] + centreX;
                this.erosionBrushWeights[i][j] = weights[j] / weightSum;
            }
        }
    }

    private float[] grad(float[] nodes, int mapSize, float posX, float posY, float[] resource) {
        int coordX = (int)posX;
        int coordY = (int)posY;
        float x = posX - (float)coordX;
        float y = posY - (float)coordY;
        int nodeIndexNW = coordY * mapSize + coordX;
        float heightNW = nodes[nodeIndexNW];
        float heightNE = nodes[nodeIndexNW + 1];
        float heightSW = nodes[nodeIndexNW + mapSize];
        float heightSE = nodes[nodeIndexNW + mapSize + 1];
        resource[0] = heightNW * (1.0f - x) * (1.0f - y) + heightNE * x * (1.0f - y) + heightSW * (1.0f - x) * y + heightSE * x * y;
        resource[1] = (heightNE - heightNW) * (1.0f - y) + (heightSE - heightSW) * y;
        resource[2] = (heightSW - heightNW) * (1.0f - x) + (heightSE - heightNE) * x;
        return resource;
    }

    private static float getFalloff(float height) {
        if (height >= 0.4f) {
            return 1.0f;
        }
        return height / 0.4f;
    }

    public static class Resource {
        public final float[] grad1 = new float[3];
        public final float[] grad2 = new float[3];
    }
}
