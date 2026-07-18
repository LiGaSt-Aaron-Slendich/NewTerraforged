package com.terraforged.mod.worldgen.util;

import com.google.common.base.Suppliers;
import com.terraforged.mod.worldgen.GeneratorResource;
import com.terraforged.mod.worldgen.terrain.StructureTerrain;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.QuartPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class ChunkUtil {
   public static final ChunkUtil.FillerBlock FILLER = ChunkUtil::getFiller;
   public static final Supplier<ByteBuf> FULL_SECTION = Suppliers.memoize(ChunkUtil::createFullPalette);

   public static void fillNoiseBiomes(ChunkAccess chunk, BiomeSource source, Sampler sampler, GeneratorResource resource) {
      ChunkPos chunkpos = chunk.getPos();
      int i = QuartPos.fromBlock(chunkpos.getMinBlockX());
      int j = QuartPos.fromBlock(chunkpos.getMinBlockZ());
      LevelHeightAccessor levelheightaccessor = chunk.getHeightAccessorForGeneration();
      Holder<Biome>[] holder = resource.biomeBuffer2D;

      for (int k = 0; k < 4; k++) {
         for (int l = 0; l < 4; l++) {
            Holder<Biome> holder1 = source.getNoiseBiome(i + l, -1, j + k, sampler);
            holder[k << 2 | l] = holder1;
         }
      }

      for (int i1 = levelheightaccessor.getMinSection(); i1 < levelheightaccessor.getMaxSection(); i1++) {
         LevelChunkSection levelchunksection = chunk.getSection(chunk.getSectionIndexFromSectionY(i1));
         fillNoiseBiomes(levelchunksection, holder);
      }
   }

   private static void fillNoiseBiomes(LevelChunkSection section, Holder<Biome>[] biomeBuffer) {
      PalettedContainer<Holder<Biome>> palettedcontainer = section.getBiomes();
      palettedcontainer.acquire();

      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            Holder<Biome> holder = biomeBuffer[i << 2 | j];

            for (int k = 0; k < 4; k++) {
               palettedcontainer.getAndSetUnchecked(j, k, i, holder);
            }
         }
      }

      palettedcontainer.release();
   }

   public static void fillChunk(int seaLevel, ChunkAccess chunk, TerrainData terrainData, ChunkUtil.FillerBlock filler, GeneratorResource resource) {
      int i = chunk.getMaxBuildHeight();
      int j = Math.min(i, getLowestSection(terrainData));
      int k = Math.min(i, getHighestSection(terrainData));
      FriendlyByteBuf friendlybytebuf = resource.fullSection;

      for (int l = chunk.getMinBuildHeight(); l < j; l += 16) {
         int i1 = chunk.getSectionIndex(l);
         LevelChunkSection levelchunksection = chunk.getSection(i1);
         friendlybytebuf.resetReaderIndex();
         levelchunksection.getStates().read(friendlybytebuf);
         levelchunksection.recalcBlockCounts();
      }

      for (int j1 = j; j1 <= k; j1 += 16) {
         int k1 = chunk.getSectionIndex(j1);
         LevelChunkSection levelchunksection1 = chunk.getSection(k1);
         fillSection(j1, seaLevel, terrainData, chunk, levelchunksection1, filler);
      }
   }

   public static void primeHeightmaps(int seaLevel, ChunkAccess chunk, TerrainData terrainData, ChunkUtil.FillerBlock filler) {
      BlockState blockstate = Blocks.STONE.defaultBlockState();
      Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Types.OCEAN_FLOOR_WG);
      Heightmap heightmap1 = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE_WG);
      int i = 0;

      for (int j = 0; i < 16; i++) {
         for (int k = 0; k < 16; j++) {
            int l = terrainData.getHeight(k, i);
            int i1 = Math.max(seaLevel, l);
            BlockState blockstate1 = filler.getState(i1, l);
            heightmap.update(k, l, i, blockstate);
            heightmap1.update(k, i1, i, blockstate1);
            k++;
         }
      }
   }

   public static void buildStructureTerrain(ChunkAccess chunk, TerrainData terrainData, StructureFeatureManager structureFeatures) {
      int i = chunk.getPos().getMinBlockX();
      int j = chunk.getPos().getMinBlockZ();
      StructureTerrain structureterrain = new StructureTerrain(chunk, structureFeatures);

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            structureterrain.modify(i + l, j + k, chunk, terrainData);
         }
      }
   }

   private static void fillSection(
      int startY, int seaLevel, TerrainData terrainData, ChunkAccess chunk, LevelChunkSection section, ChunkUtil.FillerBlock filler
   ) {
      section.acquire();
      int i = startY + 16;
      int j = 0;

      for (int k = 0; j < 16; j++) {
         for (int l = 0; l < 16; k++) {
            int i1 = terrainData.getHeight(l, j);
            int j1 = TerrainLevels.getWaterLevel(l, j, seaLevel, terrainData);
            int k1 = Math.max(i1, j1) + 1;
            int l1 = Math.min(i, k1);

            for (int i2 = startY; i2 < l1; i2++) {
               BlockState blockstate = filler.getState(i2, i1);
               section.setBlockState(l, i2 & 15, j, blockstate, false);
               if (blockstate.getLightEmission() != 0 && chunk instanceof ProtoChunk protochunk) {
                  protochunk.addLight(new BlockPos(l, i2, j));
               }
            }

            l++;
         }
      }

      section.release();
   }

   protected static BlockState getFiller(int y, int surfaceSolid) {
      return y <= surfaceSolid ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState();
   }

   protected static int getHighestSection(TerrainData terrainData) {
      int i = Math.max(terrainData.getMaxBase(), terrainData.getMax());
      return i >> 4 << 4;
   }

   protected static int getLowestSection(TerrainData terrainData) {
      int i = terrainData.getMin();
      return i >> 4 << 4;
   }

   protected static ByteBuf createFullPalette() {
      IdMapper<BlockState> idmapper = Block.BLOCK_STATE_REGISTRY;
      PalettedContainer<BlockState> palettedcontainer = new PalettedContainer(idmapper, Blocks.STONE.defaultBlockState(), Strategy.SECTION_STATES);
      palettedcontainer.acquire();

      for (int i = 0; i < 16; i++) {
         for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
               palettedcontainer.getAndSetUnchecked(i, j, k, Blocks.STONE.defaultBlockState());
            }
         }
      }

      palettedcontainer.release();
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(Unpooled.buffer());
      palettedcontainer.write(friendlybytebuf);
      return friendlybytebuf;
   }

   public static FriendlyByteBuf getFullSection() {
      return new FriendlyByteBuf(FULL_SECTION.get().copy());
   }

   public interface FillerBlock {
      BlockState getState(int var1, int var2);
   }
}
