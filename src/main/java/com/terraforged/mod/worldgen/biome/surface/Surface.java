package com.terraforged.mod.worldgen.biome.surface;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
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
    private static final int MAX_CLIFF_FILL = 14;
    private static final float CLIFF_GRADIENT_MIN = 0.52f;

    public static void apply(TerrainData terrainData, ChunkAccess chunk, ChunkGenerator generator) {
        float norm = 55.0f * ((float)generator.getGenDepth() / 255.0f);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (y < generator.getSeaLevel()) {
                    continue;
                }
                pos.set(dx, y, dz);
                BlockState top = chunk.getBlockState((BlockPos)pos);
                float gradient = terrainData.getGradient(dx, dz, norm);
                if (gradient >= CLIFF_GRADIENT_MIN) {
                    BlockState solid = Surface.findSolid(pos.set(dx, y, dz), chunk);
                    BlockState fillMaterial = Surface.resolveCliffFill(pos.set(dx, y, dz), chunk, solid);
                    if (fillMaterial != null) {
                        int bottom = pos.getY();
                        int fillTop = y;
                        int fillBottom = Math.max(bottom, fillTop - MAX_CLIFF_FILL);
                        for (int fy = fillTop; fy > fillBottom; --fy) {
                            chunk.setBlockState((BlockPos)pos.setY(fy), fillMaterial, false);
                        }
                    }
                    continue;
                }
                if (!Surface.needsSurfaceCover(top) || !chunk.getBlockState((BlockPos)pos.setY(y + 1)).isAir()) {
                    continue;
                }
                BlockState fill = Surface.resolveCliffFill(pos.set(dx, y, dz), chunk, null);
                if (fill != null && !Surface.needsSurfaceCover(fill)) {
                    chunk.setBlockState((BlockPos)pos.setY(y), fill, false);
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
                if (gradient < 0.72f) {
                    if (!(state.getBlock() instanceof SnowLayerBlock)) continue;
                    Surface.smoothSnow(pos, state, chunk, terrainData, generator);
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

    protected static void smoothSnow(BlockPos.MutableBlockPos pos, BlockState state, ChunkAccess chunk, TerrainData terrain, ChunkGenerator generator) {
        int x = pos.getX();
        int z = pos.getZ();
        float height = terrain.getHeight().get(x, z);
        float norm = 55.0f * ((float)generator.getGenDepth() / 255.0f);
        float gradient = terrain.getGradient(x, z, norm);
        if (gradient > 0.22f) {
            float avg = Surface.averageNeighborHeight(terrain, x, z);
            height = NoiseUtil.lerp(height, avg, Math.min(0.75f, gradient * 0.9f));
        }
        float delta = height - (float)terrain.getLevels().getHeight(height);
        float layerScale = gradient > 0.45f ? 2.5f : (gradient > 0.25f ? 4.5f : 7.9999f);
        int layers = 1 + NoiseUtil.floor(delta * layerScale);
        int maxLayers = gradient > 0.45f ? 3 : (gradient > 0.25f ? 5 : 8);
        layers = Math.max(1, Math.min(layers, maxLayers));
        state = (BlockState)state.setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(layers));
        chunk.setBlockState((BlockPos)pos, state, false);
    }

    private static float averageNeighborHeight(TerrainData terrain, int x, int z) {
        float sum = terrain.getHeight().get(x, z);
        int count = 1;
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                sum += terrain.getHeight().get(x + dx, z + dz);
                ++count;
            }
        }
        return sum / (float)count;
    }

    protected static void erodeSnow(BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        BlockState state = chunk.getBlockState((BlockPos)pos);
        if (state.is(BlockTags.SNOW)) {
            chunk.setBlockState((BlockPos)pos, Blocks.AIR.defaultBlockState(), false);
        }
    }

    public static void repairExposedCover(ChunkAccess chunk, WorldGenLevel region, Generator generator, TerrainData terrainData) {
        Surface.repairExposedCover(chunk, region, generator, terrainData, null);
    }

    public static void repairExposedCover(ChunkAccess chunk, WorldGenLevel region, Generator generator, TerrainData terrainData, CarverChunk carver) {
        if (carver != null && carver.anyMegaGiga() && !carver.hasSurfaceRisk()) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                if (carver != null && carver.isEntranceColumn(dx, dz)) {
                    continue;
                }
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                if (y <= generator.getSeaLevel()) {
                    continue;
                }
                pos.set(dx, y, dz);
                BlockState top = chunk.getBlockState((BlockPos)pos);
                if (chunk.getBlockState((BlockPos)pos.setY(y + 1)).isAir()) {
                    if (Surface.needsGrassRestore(top)) {
                        chunk.setBlockState((BlockPos)pos.setY(y), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                        continue;
                    }
                }
                if (!Surface.needsSurfaceCover(top)) {
                    continue;
                }
                if (!chunk.getBlockState((BlockPos)pos.setY(y + 1)).isAir()) {
                    continue;
                }
                if (Surface.isCarvedSurfaceMouth(chunk, dx, y, dz)) {
                    continue;
                }
                BlockState fill = Surface.sampleNeighborCover(chunk, dx, dz);
                if (fill == null) {
                    fill = Surface.resolveCliffFill(pos.set(dx, y, dz), chunk, null);
                }
                if (fill == null || Surface.needsSurfaceCover(fill)) {
                    fill = Blocks.GRASS_BLOCK.defaultBlockState();
                }
                chunk.setBlockState((BlockPos)pos.setY(y), fill, false);
                if (y > chunk.getMinBuildHeight()) {
                    BlockState under = chunk.getBlockState((BlockPos)pos.setY(y - 1));
                    if (Surface.needsSurfaceCover(under)) {
                        chunk.setBlockState((BlockPos)pos.setY(y - 1), Blocks.DIRT.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static boolean isCarvedSurfaceMouth(ChunkAccess chunk, int lx, int y, int lz) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        if (y < surface - 1) {
            return CaveOpenAirCheck.isOpenAir(chunk, lx, y, lz);
        }
        for (int dy = 1; dy <= 4; ++dy) {
            int cy = y - dy;
            if (cy <= chunk.getMinBuildHeight()) {
                break;
            }
            BlockState state = chunk.getBlockState(new BlockPos(lx, cy, lz));
            if (state.isAir()) {
                return true;
            }
            if (state.getFluidState().isEmpty() && !CaveOpenAirCheck.isIgnoredCover(state)) {
                return false;
            }
        }
        return false;
    }

    private static BlockState sampleNeighborCover(ChunkAccess chunk, int lx, int lz) {
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {0, 2}, {-2, 0}, {0, -2}};
        for (int[] offset : offsets) {
            int nx = lx + offset[0];
            int nz = lz + offset[1];
            if (nx < 0 || nx > 15 || nz < 0 || nz > 15) {
                continue;
            }
            int ny = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nx, nz);
            BlockState neighbor = chunk.getBlockState(new BlockPos(nx, ny, nz));
            if (Surface.isErodible(neighbor) || neighbor.is(Blocks.GRASS_BLOCK) || neighbor.is(Blocks.PODZOL) || neighbor.is(Blocks.MYCELIUM)) {
                return neighbor;
            }
        }
        return null;
    }

    private static boolean needsGrassRestore(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
    }

    private static boolean needsSurfaceCover(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (Surface.isErodible(state) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE);
    }

    private static boolean isBareRock(BlockState state) {
        return Surface.needsSurfaceCover(state);
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
