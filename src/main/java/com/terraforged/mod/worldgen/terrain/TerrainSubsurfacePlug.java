package com.terraforged.mod.worldgen.terrain;

import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveCarvingGate;
import com.terraforged.mod.worldgen.cave.CaveChunkSurfaceRepair;
import com.terraforged.mod.worldgen.cave.CaveColumnScan;
import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fills unintended air between terrain floor and the surface shell.
 * <p>
 * {@link com.terraforged.mod.worldgen.util.ChunkUtil#fillChunk} only places stone/water up to
 * terrain height; surface rules and features often build higher — leaving a wide hollow that looks
 * like a cave even when {@link com.terraforged.mod.worldgen.cave.CaveCarvingGate} is off.
 */
public final class TerrainSubsurfacePlug {
    private static final int MIN_PLUG_GAP = 2;
    /** When carving is on, do not plug columns that already host a real cave chamber. */
    private static final int CAVE_AIR_SKIP_SPAN = 8;

    private TerrainSubsurfacePlug() {
    }

    public static void plugSolidCore(ChunkAccess chunk, TerrainData terrain, Generator generator, CarverChunk carver) {
        if (terrain == null) {
            return;
        }
        int sea = generator.getSeaLevel();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        boolean carveOn = CaveCarvingGate.isEnabled();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (carver != null && carver.isEntranceColumn(lx, lz)) {
                    continue;
                }
                int floor = Math.max(terrain.getHeight(lx, lz),
                        chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz));
                int shell = CaveChunkSurfaceRepair.findSurfaceShellTop(chunk, lx, lz);
                if (shell - floor < MIN_PLUG_GAP) {
                    continue;
                }
                if (carveOn) {
                    int midY = Math.min(maxY, Math.max(minY, (floor + shell) / 2));
                    if (CaveColumnScan.measureAirColumnSpan(chunk, lx, midY, lz, minY, maxY) >= CAVE_AIR_SKIP_SPAN) {
                        continue;
                    }
                }
                int waterY = TerrainLevels.getWaterLevel(lx, lz, sea, terrain);
                int bedY = TerrainSubsurfacePlug.isRiverBedColumn(terrain, lx, lz)
                        ? terrain.getHeight(lx, lz)
                        : floor;
                boolean riverBed = TerrainSubsurfacePlug.isRiverBedColumn(terrain, lx, lz);
                for (int y = floor + 1; y < shell; ++y) {
                    if (riverBed && y > bedY && y <= waterY) {
                        continue;
                    }
                    pos.set(lx, y, lz);
                    BlockState state = chunk.getBlockState(pos);
                    if (!state.isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos, stone, false);
                }
            }
        }
        ChunkUtil.refreshHeightmaps(chunk);
    }

    private static boolean isRiverBedColumn(TerrainData terrain, int lx, int lz) {
        Terrain type = terrain.getTerrain().get(lx, lz);
        return (type.isRiver() || type.isLake()) && terrain.getRiver().get(lx, lz) == 0.0f;
    }
}
