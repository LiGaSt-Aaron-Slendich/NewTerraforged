package com.terraforged.mod.worldgen.cave;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;

/** Precomputed valley / water reference grid — one pass per chunk instead of per-column world scans. */
final class RiverVoidFillContext {
    private final int sea;
    private final int radius;
    private final int radiusSq;
    private final int originX;
    private final int originZ;
    private final int size;
    private final int[] valleyWaterY;
    private final int[] blockWaterY;
    private final float[] riverNoise;

    RiverVoidFillContext(Generator generator, ChunkAccess chunk, TerrainData localTerrain, BlockGetter level, int sea,
            int radius) {
        this.sea = sea;
        this.radius = radius;
        this.radiusSq = radius * radius;
        ChunkPos cp = chunk.getPos();
        this.originX = cp.getMinBlockX() - radius;
        this.originZ = cp.getMinBlockZ() - radius;
        this.size = 16 + radius * 2;
        int cells = this.size * this.size;
        this.valleyWaterY = new int[cells];
        this.blockWaterY = new int[cells];
        this.riverNoise = new float[cells];
        for (int i = 0; i < cells; ++i) {
            this.valleyWaterY[i] = Integer.MIN_VALUE;
            this.blockWaterY[i] = Integer.MIN_VALUE;
            this.riverNoise[i] = 1.0f;
        }
        this.build(generator, chunk, localTerrain, level);
    }

    static boolean chunkNeedsFill(Generator generator, TerrainData terrain, ChunkAccess chunk) {
        if (RiverVoidFillContext.hasLocalRiverBed(terrain)) {
            return true;
        }
        int cx = chunk.getPos().getMiddleBlockX();
        int cz = chunk.getPos().getMiddleBlockZ();
        return CaveRiverProximityCache.chunkMayHaveRiver(generator, cx, cz);
    }

    static boolean hasLocalRiverBed(TerrainData terrain) {
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
                    return true;
                }
            }
        }
        return false;
    }

    float riverNoiseAt(int worldX, int worldZ) {
        int gx = worldX - this.originX;
        int gz = worldZ - this.originZ;
        if (gx < 0 || gz < 0 || gx >= this.size || gz >= this.size) {
            return 1.0f;
        }
        return this.riverNoise[gz * this.size + gx];
    }

    int nearestValleyWaterY(int worldX, int worldZ) {
        return this.nearestFromGrid(this.valleyWaterY, worldX, worldZ);
    }

    int nearestBlockWaterY(int worldX, int worldZ) {
        return this.nearestFromGrid(this.blockWaterY, worldX, worldZ);
    }

    int nearestRefWaterY(int worldX, int worldZ) {
        return Math.max(this.nearestValleyWaterY(worldX, worldZ), this.nearestBlockWaterY(worldX, worldZ));
    }

    /** True when any river/lake bed in the precomputed zone sits above sea level (elevated-river shaft bug). */
    boolean hasElevatedRiverRef(int sea) {
        for (int waterY : this.valleyWaterY) {
            if (waterY > sea) {
                return true;
            }
        }
        return false;
    }

    /** Squared world-space distance to the nearest river/lake bed column, or {@link Integer#MAX_VALUE}. */
    int nearestShoreDistSq(int worldX, int worldZ) {
        int gx = worldX - this.originX;
        int gz = worldZ - this.originZ;
        int bestDist = Integer.MAX_VALUE;
        int minGx = Math.max(0, gx - this.radius);
        int maxGx = Math.min(this.size - 1, gx + this.radius);
        int minGz = Math.max(0, gz - this.radius);
        int maxGz = Math.min(this.size - 1, gz + this.radius);
        for (int nz = minGz; nz <= maxGz; ++nz) {
            for (int nx = minGx; nx <= maxGx; ++nx) {
                if (this.valleyWaterY[nz * this.size + nx] == Integer.MIN_VALUE) {
                    continue;
                }
                int dx = nx - gx;
                int dz = nz - gz;
                int dist = dx * dx + dz * dz;
                if (dist <= this.radiusSq && dist < bestDist) {
                    bestDist = dist;
                }
            }
        }
        return bestDist;
    }

    private int nearestFromGrid(int[] grid, int worldX, int worldZ) {
        int gx = worldX - this.originX;
        int gz = worldZ - this.originZ;
        int best = Integer.MIN_VALUE;
        int bestDist = Integer.MAX_VALUE;
        int minGx = Math.max(0, gx - this.radius);
        int maxGx = Math.min(this.size - 1, gx + this.radius);
        int minGz = Math.max(0, gz - this.radius);
        int maxGz = Math.min(this.size - 1, gz + this.radius);
        for (int nz = minGz; nz <= maxGz; ++nz) {
            for (int nx = minGx; nx <= maxGx; ++nx) {
                int dx = nx - gx;
                int dz = nz - gz;
                int dist = dx * dx + dz * dz;
                if (dist > this.radiusSq) {
                    continue;
                }
                int waterY = grid[nz * this.size + nx];
                if (waterY == Integer.MIN_VALUE) {
                    continue;
                }
                if (dist < bestDist || dist == bestDist && waterY > best) {
                    bestDist = dist;
                    best = waterY;
                }
            }
        }
        return best;
    }

    private void build(Generator generator, ChunkAccess chunk, TerrainData localTerrain, BlockGetter level) {
        Long2ObjectMap<TerrainData> terrainCache = new Long2ObjectOpenHashMap<>();
        terrainCache.put(chunk.getPos().toLong(), localTerrain);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int gz = 0; gz < this.size; ++gz) {
            for (int gx = 0; gx < this.size; ++gx) {
                int wx = this.originX + gx;
                int wz = this.originZ + gz;
                int idx = gz * this.size + gx;
                ChunkPos sampleChunk = new ChunkPos(wx >> 4, wz >> 4);
                TerrainData sampleTerrain = terrainCache.computeIfAbsent(sampleChunk.toLong(),
                        k -> generator.getChunkData(sampleChunk));
                if (sampleTerrain == null) {
                    continue;
                }
                int slx = wx - sampleChunk.getMinBlockX();
                int slz = wz - sampleChunk.getMinBlockZ();
                this.riverNoise[idx] = sampleTerrain.getRiver().get(slx, slz);
                int waterY = RiverVoidFillContext.resolveValleyWaterY(sampleTerrain, slx, slz, this.sea);
                this.valleyWaterY[idx] = waterY;
                if (waterY != Integer.MIN_VALUE) {
                    this.blockWaterY[idx] = RiverVoidFillContext.scanSurfaceWaterY(level, chunk, wx, wz, pos);
                }
            }
        }
    }

    private static int resolveValleyWaterY(TerrainData terrain, int lx, int lz, int sea) {
        if (!CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
            return Integer.MIN_VALUE;
        }
        return Math.max(TerrainLevels.getWaterLevel(lx, lz, sea, terrain), terrain.getBaseHeight(lx, lz));
    }

    private static int scanSurfaceWaterY(BlockGetter level, ChunkAccess chunk, int wx, int wz, BlockPos.MutableBlockPos pos) {
        int scanTop = RiverVoidFillContext.motionBlockingY(level, chunk, wx, wz);
        int scanBottom = Math.max(chunk.getMinBuildHeight(), scanTop - 8);
        for (int y = scanTop; y >= scanBottom; --y) {
            pos.set(wx, y, wz);
            BlockState state = RiverVoidFillContext.blockAt(level, chunk, pos);
            if (!state.getFluidState().isEmpty() && state.getFluidState().is(Fluids.WATER)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static int motionBlockingY(BlockGetter level, ChunkAccess chunk, int wx, int wz) {
        ChunkPos cp = chunk.getPos();
        if (wx >= cp.getMinBlockX() && wx < cp.getMaxBlockX() && wz >= cp.getMinBlockZ() && wz < cp.getMaxBlockZ()) {
            return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, wx & 15, wz & 15);
        }
        return RiverVoidFillAccess.motionBlockingY(level, chunk, wx, wz, chunk.getHighestSectionPosition() + 15);
    }

    private static BlockState blockAt(BlockGetter level, ChunkAccess chunk, BlockPos pos) {
        BlockState state = RiverVoidFillAccess.blockState(level, chunk, pos.getX(), pos.getY(), pos.getZ());
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }
}
