package com.terraforged.mod.worldgen.terrain;

import com.terraforged.noise.util.NoiseUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Comparator;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;

public class StructureTerrain {
   private static final int RADIUS = 20;
   private static final Comparator<StructurePiece> PIECE_SORTER = Comparator.comparing(o -> o.getBoundingBox().minY());
   private final ObjectList<StructurePiece> rigids = new ObjectArrayList(10);
   protected final ObjectListIterator<StructurePiece> pieceIterator;
   protected final BlockState air = Blocks.AIR.defaultBlockState();
   protected final BlockState solid = Blocks.STONE.defaultBlockState();
   protected final MutableBlockPos pos = new MutableBlockPos();

   public StructureTerrain(ChunkAccess chunk, StructureFeatureManager manager) {
      ChunkPos chunkpos = chunk.getPos();
      SectionPos sectionpos = SectionPos.bottomOf(chunk);
      manager.startsForFeature(sectionpos, cf -> cf.adaptNoise).forEach(start -> {
         for (StructurePiece structurepiece : start.getPieces()) {
            if (structurepiece.isCloseToChunk(chunkpos, 20) && structurepiece.getNoiseEffect() == NoiseEffect.BEARD) {
               if (structurepiece instanceof PoolElementStructurePiece poolelementstructurepiece) {
                  Projection projection = poolelementstructurepiece.getElement().getProjection();
                  if (projection == Projection.RIGID) {
                     this.rigids.add(poolelementstructurepiece);
                  }
               } else {
                  this.rigids.add(structurepiece);
               }
            }
         }
      });
      this.rigids.sort(PIECE_SORTER);
      this.pieceIterator = this.rigids.iterator();
   }

   public void modify(int x, int z, ChunkAccess chunk, TerrainData terrainData) {
      int i = chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z);
      float f = i;
      int j = i;
      StructurePiece structurepiece = null;

      while (this.pieceIterator.hasNext()) {
         StructurePiece structurepiece1 = (StructurePiece)this.pieceIterator.next();
         BoundingBox boundingbox = structurepiece1.getBoundingBox();
         int k = Math.max(boundingbox.getXSpan(), boundingbox.getZSpan());
         float f1 = Math.max(4, 20 - k);
         int l = getPieceY(structurepiece1);
         j = Math.max(j, l);
         if (structurepiece == null && l > i) {
            f = raise(x, z, boundingbox, l, f, f1);
         }

         if (x >= boundingbox.minX()
            && x <= boundingbox.maxX()
            && z >= boundingbox.minZ()
            && z <= boundingbox.maxZ()
            && (structurepiece == null || boundingbox.minY() > structurepiece.getBoundingBox().minY())) {
            structurepiece = structurepiece1;
         }
      }

      boolean flag = this.raiseTerrain(x, i, z, f, chunk, terrainData);
      boolean flag1 = this.carveTerrain(x, j, z, chunk, structurepiece);
      if (flag || flag1) {
         terrainData.getHeight().set(x, z, chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z));
      }

      this.reset();
   }

   protected boolean raiseTerrain(int x, int y, int z, float maxY, ChunkAccess chunk, TerrainData terrainData) {
      int i = (int)maxY;
      if (y + 1 >= i) {
         return false;
      } else {
         for (int j = y; j < i; j++) {
            chunk.setBlockState(this.pos.set(x, j, z), this.solid, false);
         }

         return true;
      }
   }

   protected boolean carveTerrain(int x, int y, int z, ChunkAccess chunk, StructurePiece piece) {
      if (piece == null) {
         return false;
      } else {
         int i = chunk.getHeight(Types.OCEAN_FLOOR_WG, x, z);
         int j = Math.max(y, i);
         BoundingBox boundingbox = piece.getBoundingBox();
         int k = getPieceY(piece);
         int l = boundingbox.maxY();
         if (j > l + 5) {
            float f = getEllipseDistAlpha(x, z, boundingbox);
            float f1 = (j - l) * 0.5F;
            l += NoiseUtil.round(f1 * f);
         }

         for (int i1 = k; i1 <= l; i1++) {
            chunk.setBlockState(this.pos.set(x, i1, z), this.air, false);
         }

         return true;
      }
   }

   protected void reset() {
      this.pieceIterator.back(this.rigids.size());
   }

   private static float raise(int x, int z, BoundingBox bounds, float level, float surface, float borderRadius) {
      float f = Math.max(1.0F, borderRadius * borderRadius);
      float f1 = 1.0F - getDistAlpha(x, z, bounds, f);
      float f2 = NoiseUtil.pow(f1, 2.0F - f1);
      return NoiseUtil.lerp(surface, level, f2);
   }

   protected static float getEllipseDistAlpha(int x, int z, BoundingBox bounds) {
      float f = bounds.getXSpan() * 0.5F;
      float f1 = bounds.getZSpan() * 0.5F;
      float f2 = (bounds.minX() + bounds.maxX()) * 0.5F;
      float f3 = (bounds.minZ() + bounds.maxZ()) * 0.5F;
      float f4 = x - f2;
      float f5 = z - f3;
      float f6 = f4 * f4 / (f * f);
      float f7 = f5 * f5 / (f1 * f1);
      return NoiseUtil.clamp(1.0F - f6 - f7, 0.0F, 1.0F);
   }

   protected static float getDistAlpha(int x, int z, BoundingBox box, float radius2) {
      int i = getDist(x, box.minX(), box.maxX());
      int j = getDist(z, box.minZ(), box.maxZ());
      return getDistAlpha(i, j, radius2);
   }

   protected static float getDistAlpha(int dx, int dz, float radius2) {
      int i = dx * dx + dz * dz;
      if (i == 0) {
         return 0.0F;
      } else {
         return i >= radius2 ? 1.0F : NoiseUtil.sqrt(i / radius2);
      }
   }

   protected static int getPieceY(StructurePiece piece) {
      int i = piece.getBoundingBox().minY();
      if (piece instanceof PoolElementStructurePiece poolelementstructurepiece) {
         i += poolelementstructurepiece.getGroundLevelDelta();
      }

      return i;
   }

   protected static int getDist(int pos, int min, int max) {
      return Math.max(0, Math.max(min - pos, pos - max));
   }
}
