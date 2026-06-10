package com.terraforged.mod.worldgen.biome.surface;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

public class Surface {
    protected static final TagKey<Block> ERODIBLE = BlockTags.DIRT;
    private static final int MAX_CLIFF_FILL = 10;
    private static final float CLIFF_GRADIENT_MIN = 0.72f;

    public static void apply(TerrainData terrainData, ChunkAccess chunk, ChunkGenerator generator) {
        float norm = 55.0f * ((float)generator.getGenDepth() / 255.0f);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                float gradient = terrainData.getGradient(dx, dz, norm);
                if (y < generator.getSeaLevel() || gradient < 0.72f) continue;
                BlockState solid = Surface.findSolid(pos.set(dx, y, dz), chunk);
                BlockState fillMaterial = Surface.resolveCliffFill(pos.set(dx, y, dz), chunk, solid);
                if (fillMaterial == null) continue;
                int bottom = pos.getY();
                int fillTop = y;
                int fillBottom = Math.max(bottom, fillTop - 10);
                for (int fy = fillTop; fy > fillBottom; --fy) {
                    chunk.setBlockState((BlockPos)pos.setY(fy), fillMaterial, false);
                }
            }
        }
    }

    public static void applyPost(ChunkAccess chunk, TerrainData terrainData, ChunkGenerator generator) {
        float norm = 70.0f * ((float)generator.getGenDepth() / 255.0f);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz) + 1;
                pos.set(dx, y, dz);
                BlockState state = chunk.getBlockState((BlockPos)pos);
                float gradient = terrainData.getGradient(dx, dz, norm);
                if (gradient < 0.625f) {
                    if (!(state.getBlock() instanceof SnowLayerBlock)) continue;
                    Surface.smoothSnow(pos, state, chunk, terrainData);
                    continue;
                }
                if (state.isAir()) {
                    state = chunk.getBlockState((BlockPos)pos.setY(y - 1));
                }
                if (!state.is(BlockTags.SNOW)) continue;
                Surface.erodeSnow(pos, chunk);
            }
        }
    }

    public static void smoothWater(ChunkAccess chunk, WorldGenLevel region, TerrainData terrainData) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockState waterState = (BlockState)Blocks.WATER.defaultBlockState().setValue((Property)LiquidBlock.LEVEL, (Comparable)Integer.valueOf(2));
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                if (!Surface.isSmoothable(dx, dz, terrainData)) continue;
                int x = minX + dx;
                int z = minZ + dz;
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                BlockState state = chunk.getBlockState((BlockPos)pos.set(x, y, z));
                if (!state.is(Blocks.WATER) || (Integer)state.getValue((Property)LiquidBlock.LEVEL) != 0 || !Surface.shouldSmooth(x, y, z, chunk, region, pos)) continue;
                chunk.setBlockState((BlockPos)pos.set(x, y, z), waterState, false);
            }
        }
    }

    protected static boolean shouldSmooth(int x, int y, int z, ChunkAccess chunk, WorldGenLevel region, BlockPos.MutableBlockPos pos) {
        int radius = 6;
        int radius2 = radius * radius;
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int d2 = dx * dx + dz * dz;
                if (d2 == 0 || d2 > radius2) continue;
                pos.set(x + dx, y, z + dz);
                var world = Surface.sameChunk((BlockPos)pos, chunk.getPos()) ? chunk : region;
                BlockState state = world.getBlockState((BlockPos)pos);
                if (!state.isAir()) continue;
                return true;
            }
        }
        return false;
    }

    protected static boolean isSmoothable(int x, int z, TerrainData terrainData) {
        float river = terrainData.getRiver().get(x, z);
        Terrain terrain = terrainData.getTerrain().get(x, z);
        return (terrain.isRiver() || terrain.isLake()) && river == 0.0f;
    }

    protected static void smoothSnow(BlockPos.MutableBlockPos pos, BlockState state, ChunkAccess chunk, TerrainData terrain) {
        float height = terrain.getHeight().get(pos.getX(), pos.getZ());
        float delta = height - (float)terrain.getLevels().getHeight(height);
        int layers = 1 + NoiseUtil.floor(delta * 7.9999f);
        state = (BlockState)state.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(layers));
        chunk.setBlockState((BlockPos)pos, state, false);
    }

    protected static void erodeSnow(BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        chunk.setBlockState((BlockPos)pos, Blocks.AIR.defaultBlockState(), false);
        int y0 = pos.getY() - 1;
        int y1 = Math.max(pos.getY() - 15, 0);
        for (int y = y0; y > y1; --y) {
            pos.setY(y);
            BlockState state = chunk.getBlockState((BlockPos)pos);
            if (!Surface.isErodible(state)) {
                return;
            }
            chunk.setBlockState((BlockPos)pos, Blocks.STONE.defaultBlockState(), false);
        }
    }

    public static boolean isErodible(BlockState state) {
        return state.is(ERODIBLE) || state.is(BlockTags.SNOW);
    }

    protected static boolean sameChunk(BlockPos pos, ChunkPos chunk) {
        return pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
    }

    protected static BlockState resolveCliffFill(BlockPos.MutableBlockPos pos, ChunkAccess chunk, BlockState solidBelow) {
        BlockState topState = chunk.getBlockState((BlockPos)pos);
        if (Surface.isErodible(topState)) {
            return topState;
        }
        if (solidBelow != null && Surface.isErodible(solidBelow)) {
            return solidBelow;
        }
        for (int dy = 1; dy <= 6; ++dy) {
            BlockState above = chunk.getBlockState((BlockPos)pos.setY(pos.getY() + dy));
            if (Surface.isErodible(above)) {
                return above;
            }
            if (!above.isAir()) break;
        }
        return Blocks.DIRT.defaultBlockState();
    }

    protected static BlockState findSolid(BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        BlockState state = chunk.getBlockState((BlockPos)pos);
        if (!Surface.isErodible(state)) {
            return null;
        }
        int bottom = Math.max(0, pos.getY() - 20);
        for (int y = pos.getY() - 1; y > bottom; --y) {
            state = chunk.getBlockState((BlockPos)pos.setY(y));
            if (Surface.isErodible(state)) continue;
            return state;
        }
        return null;
    }
}
