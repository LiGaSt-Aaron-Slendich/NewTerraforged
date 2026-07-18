package com.terraforged.mod.worldgen.util.delegate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class DelegateRegion extends WorldGenRegion {
   public static final ThreadLocal<DelegateRegion.Builder> LOCAL_BUILDER = ThreadLocal.withInitial(DelegateRegion.Builder::new);

   public DelegateRegion(ServerLevel level, List<ChunkAccess> chunks, ChunkStatus status, ChunkStatus stage) {
      super(level, chunks, status, getCutoffRadius(stage));
   }

   public static int getCutoffRadius(ChunkStatus status) {
      int i = 0;
      if (status == ChunkStatus.STRUCTURE_REFERENCES) {
         i = -1;
      } else if (status == ChunkStatus.BIOMES) {
         i = -1;
      } else if (status == ChunkStatus.FEATURES) {
         i = 1;
      } else if (status == ChunkStatus.SPAWN) {
         i = -1;
      }

      return i;
   }

   public static class Builder {
      private ServerLevel level;
      private ChunkStatus status;
      private final List<ChunkAccess> chunks = new ArrayList<>(289);

      public DelegateRegion.Builder source(WorldGenRegion region) {
         this.chunks.clear();
         this.level = region.getLevel();
         this.status = ChunkStatus.EMPTY;
         ChunkPos chunkpos = region.getCenter();

         for (int i = -8; i <= 8; i++) {
            for (int j = -8; j <= 8; j++) {
               int k = chunkpos.x + j;
               int l = chunkpos.z + i;
               if (region.hasChunk(k, l)) {
                  ChunkAccess chunkaccess = region.getChunk(k, l);
                  this.chunks.add(chunkaccess);
                  if (j == 0 && i == 0) {
                     this.status = chunkaccess.getStatus();
                  }
               }
            }
         }

         return this;
      }

      public <T extends WorldGenRegion> T build(ChunkStatus stage, DelegateRegion.Factory<T> factory) {
         return factory.build(this.level, this.chunks, this.status, stage);
      }
   }

   public interface Factory<T extends WorldGenRegion> {
      T build(ServerLevel var1, List<ChunkAccess> var2, ChunkStatus var3, ChunkStatus var4);
   }
}
