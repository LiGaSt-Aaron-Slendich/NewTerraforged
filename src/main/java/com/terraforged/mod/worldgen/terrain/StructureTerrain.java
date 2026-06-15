package com.terraforged.mod.worldgen.terrain;

import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class StructureTerrain {
    private static final int RADIUS = 20;
    private static final Comparator<StructurePiece> PIECE_SORTER = Comparator.comparing(o -> o.getBoundingBox().minY());
    protected final ObjectList<StructurePiece> rigids = new ObjectArrayList(10);
    protected final ObjectListIterator<StructurePiece> pieceIterator;
    protected final BlockState air = Blocks.AIR.defaultBlockState();
    protected final BlockState solid = Blocks.STONE.defaultBlockState();
    protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public StructureTerrain(ChunkAccess chunk, StructureFeatureManager manager) {
        ChunkPos chunkPos = chunk.getPos();
        chunk.getAllStarts().values().forEach(start -> {
            for (StructurePiece piece : start.getPieces()) {
                if (!piece.isCloseToChunk(chunkPos, 20)) continue;
                if (piece instanceof PoolElementStructurePiece) {
                    PoolElementStructurePiece element = (PoolElementStructurePiece)piece;
                    StructureTemplatePool.Projection projection = element.getElement().getProjection();
                    if (projection != StructureTemplatePool.Projection.RIGID) continue;
                    this.rigids.add(element);
                    continue;
                }
                this.rigids.add(piece);
            }
        });
        this.rigids.sort(PIECE_SORTER);
        this.pieceIterator = this.rigids.iterator();
    }

    public void modify(int x, int z, ChunkAccess chunk, TerrainData terrainData) {
        int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        float maxY = y;
        int maxPosY = y;
        StructurePiece highest = null;
        while (this.pieceIterator.hasNext()) {
            StructurePiece piece = (StructurePiece)this.pieceIterator.next();
            BoundingBox bounds = piece.getBoundingBox();
            int length = Math.max(bounds.getXSpan(), bounds.getZSpan());
            float radius = Math.max(4, 20 - length);
            int posY = StructureTerrain.getPieceY(piece);
            maxPosY = Math.max(maxPosY, posY);
            if (highest == null && posY > y) {
                maxY = StructureTerrain.raise(x, z, bounds, posY, maxY, radius);
            }
            if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ() || highest != null && bounds.minY() <= highest.getBoundingBox().minY()) continue;
            highest = piece;
        }
        boolean raised = false;
        boolean carved = false;
        if (highest != null) {
            BoundingBox bounds = highest.getBoundingBox();
            if (x >= bounds.minX() && x <= bounds.maxX() && z >= bounds.minZ() && z <= bounds.maxZ()) {
                raised = this.raiseTerrain(x, y, z, maxY, chunk);
            }
            carved = this.carveTerrain(x, maxPosY, z, chunk, highest);
        }
        if (raised || carved) {
            terrainData.getHeight().set(x, z, chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z));
        }
        this.reset();
    }

    protected boolean raiseTerrain(int x, int y, int z, float maxY, ChunkAccess chunk) {
        int max = (int)maxY;
        if (y + 1 >= max) {
            return false;
        }
        for (int py = y; py < max; ++py) {
            chunk.setBlockState((BlockPos)this.pos.set(x, py, z), this.solid, false);
        }
        return true;
    }

    protected boolean carveTerrain(int x, int y, int z, ChunkAccess chunk, StructurePiece piece) {
        if (piece == null) {
            return false;
        }
        BoundingBox bounds = piece.getBoundingBox();
        int minY = StructureTerrain.getPieceY(piece);
        int maxY = bounds.maxY();
        for (int py = minY; py <= maxY; ++py) {
            chunk.setBlockState((BlockPos)this.pos.set(x, py, z), this.air, false);
        }
        return true;
    }

    protected void reset() {
        this.pieceIterator.back(this.rigids.size());
    }

    private static float raise(int x, int z, BoundingBox bounds, float level, float surface, float borderRadius) {
        float radius2 = Math.max(1.0f, borderRadius * borderRadius);
        float distAlpha = 1.0f - StructureTerrain.getDistAlpha(x, z, bounds, radius2);
        float alpha = NoiseUtil.pow(distAlpha, 2.0f - distAlpha);
        return NoiseUtil.lerp(surface, level, alpha);
    }

    protected static float getEllipseDistAlpha(int x, int z, BoundingBox bounds) {
        float radiusX = (float)bounds.getXSpan() * 0.5f;
        float radiusZ = (float)bounds.getZSpan() * 0.5f;
        float centerX = (float)(bounds.minX() + bounds.maxX()) * 0.5f;
        float centerZ = (float)(bounds.minZ() + bounds.maxZ()) * 0.5f;
        float dx = (float)x - centerX;
        float dz = (float)z - centerZ;
        float qx = dx * dx / (radiusX * radiusX);
        float qz = dz * dz / (radiusZ * radiusZ);
        return NoiseUtil.clamp(1.0f - qx - qz, 0.0f, 1.0f);
    }

    protected static float getDistAlpha(int x, int z, BoundingBox box, float radius2) {
        int dx = StructureTerrain.getDist(x, box.minX(), box.maxX());
        int dz = StructureTerrain.getDist(z, box.minZ(), box.maxZ());
        return StructureTerrain.getDistAlpha(dx, dz, radius2);
    }

    protected static float getDistAlpha(int dx, int dz, float radius2) {
        int d2 = dx * dx + dz * dz;
        if (d2 == 0) {
            return 0.0f;
        }
        if ((float)d2 >= radius2) {
            return 1.0f;
        }
        return NoiseUtil.sqrt((float)d2 / radius2);
    }

    protected static int getPieceY(StructurePiece piece) {
        int y = piece.getBoundingBox().minY();
        if (piece instanceof PoolElementStructurePiece) {
            PoolElementStructurePiece element = (PoolElementStructurePiece)piece;
            y += element.getGroundLevelDelta();
        }
        return y;
    }

    protected static int getDist(int pos, int min, int max) {
        return Math.max(0, Math.max(min - pos, pos - max));
    }
}
