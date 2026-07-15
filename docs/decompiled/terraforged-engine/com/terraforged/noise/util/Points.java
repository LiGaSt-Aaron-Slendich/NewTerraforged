/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.util;

import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Points {
    public static List<Vec2f> poisson(int seedX, int seedZ, int width, int height, float radius, int samples) {
        return Points.poisson(new Random(NoiseUtil.seed(seedX, seedZ)), width, height, radius, samples);
    }

    public static List<Vec2f> poisson(Random random, int width, int height, float radius, int samples) {
        float cellSize = radius / NoiseUtil.SQRT2;
        float maxDistance = radius * 2.0f;
        int w = (int)Math.ceil((float)width / cellSize);
        int h = (int)Math.ceil((float)height / cellSize);
        int[][] grid = new int[w][h];
        ArrayList<Vec2f> points = new ArrayList<Vec2f>();
        ArrayList<Vec2f> spawns = new ArrayList<Vec2f>();
        spawns.add(new Vec2f(random.nextInt(width), random.nextInt(height)));
        while (spawns.size() > 0) {
            int index = random.nextInt(spawns.size());
            Vec2f spawn = (Vec2f)spawns.get(index);
            boolean valid = false;
            for (int i = 0; i < samples; ++i) {
                float z;
                float angle = random.nextFloat() * ((float)Math.PI * 2);
                float distance = radius + random.nextFloat() * maxDistance;
                float x = spawn.x + NoiseUtil.sin(angle) * distance;
                if (!Points.valid(x, z = spawn.y + NoiseUtil.cos(angle) * distance, width, height, cellSize, radius, points, grid)) continue;
                valid = true;
                Vec2f vec = new Vec2f(x, z);
                points.add(vec);
                spawns.add(vec);
                int cellX = (int)(x / cellSize);
                int cellZ = (int)(z / cellSize);
                grid[cellZ][cellX] = points.size();
                break;
            }
            if (valid) continue;
            spawns.remove(index);
        }
        return points;
    }

    private static boolean valid(float x, float z, int width, int height, float cellSize, float radius, List<Vec2f> points, int[][] grid) {
        if (x < 0.0f || x >= (float)width || z < 0.0f || z >= (float)height) {
            return false;
        }
        int cellX = (int)(x / cellSize);
        int cellZ = (int)(z / cellSize);
        int searchRadius = 2;
        float radius2 = radius * radius;
        int minX = Math.max(0, cellX - searchRadius);
        int maxX = Math.min(grid[0].length - 1, cellX + searchRadius);
        int minZ = Math.max(0, cellZ - searchRadius);
        int maxZ = Math.min(grid.length - 1, cellZ + searchRadius);
        for (int dz = minZ; dz <= maxZ; ++dz) {
            for (int dx = minX; dx <= maxX; ++dx) {
                Vec2f point;
                int index = grid[dz][dx] - 1;
                if (index == -1 || !((point = points.get(index)).dist2(x, z) < radius2)) continue;
                return false;
            }
        }
        return true;
    }
}

