package com.terraforged.mod.worldgen.structure;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public final class VillagePlacementValidator {
    private static final int SAMPLE_RADIUS = 40;
    private static final int GRID_STEP = 8;
    private static final int MAX_HEIGHT_RANGE = 7;
    private static final int MIN_LAND_ABOVE_SEA = 2;
    private static final float MAX_WATER_SAMPLES = 0.18f;

    private VillagePlacementValidator() {
    }

    public static boolean isValid(PieceGeneratorSupplier.Context context) {
        if (context == null) {
            return false;
        }
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        if (generator instanceof Generator) {
            Generator tfGen = (Generator)generator;
            return VillagePlacementValidator.validateTerraforged(tfGen, centerX, centerZ);
        }
        return VillagePlacementValidator.validateGeneric(context, centerX, centerZ);
    }

    private static boolean validateTerraforged(Generator generator, int centerX, int centerZ) {
        int sea = generator.getSeaLevel();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int samples = 0;
        int water = 0;
        for (int dx = -40; dx <= 40; dx += 8) {
            for (int dz = -40; dz <= 40; dz += 8) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (dx * dx + dz * dz > 1600) continue;
                int floor = generator.getOceanFloorHeight(x, z);
                ++samples;
                minY = Math.min(minY, floor);
                maxY = Math.max(maxY, floor);
                if (floor > sea + 2) continue;
                ++water;
            }
        }
        if (samples == 0) {
            return false;
        }
        if ((float)water > (float)samples * 0.18f) {
            return false;
        }
        return maxY - minY <= 7;
    }

    private static boolean validateGeneric(PieceGeneratorSupplier.Context context, int centerX, int centerZ) {
        LevelHeightAccessor accessor = context.heightAccessor();
        ChunkGenerator generator = context.chunkGenerator();
        int sea = generator.getSeaLevel();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int samples = 0;
        int water = 0;
        for (int dx = -40; dx <= 40; dx += 8) {
            for (int dz = -40; dz <= 40; dz += 8) {
                if (dx * dx + dz * dz > 1600) continue;
                int x = centerX + dx;
                int z = centerZ + dz;
                int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, accessor);
                ++samples;
                minY = Math.min(minY, floor);
                maxY = Math.max(maxY, floor);
                if (floor > sea + 2) continue;
                ++water;
            }
        }
        if (samples == 0) {
            return false;
        }
        if ((float)water > (float)samples * 0.18f) {
            return false;
        }
        return maxY - minY <= 7;
    }
}
