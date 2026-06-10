package com.terraforged.mod.worldgen.util;

import com.google.common.base.Suppliers;
import com.terraforged.mod.worldgen.GeneratorResource;
import com.terraforged.mod.worldgen.terrain.StructureTerrain;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.IdMapper;
import net.minecraft.core.QuartPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class ChunkUtil {
    private static final Set<Heightmap.Types> GENERATION_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    public static final FillerBlock FILLER = ChunkUtil::getFiller;
    public static final Supplier<ByteBuf> FULL_SECTION = Suppliers.memoize(ChunkUtil::createFullPalette);

    public static void fillNoiseBiomes(ChunkAccess chunk, BiomeSource source, Climate.Sampler sampler, GeneratorResource resource) {
        ChunkPos pos = chunk.getPos();
        int biomeX = QuartPos.fromBlock((int)pos.getMinBlockX());
        int biomeZ = QuartPos.fromBlock((int)pos.getMinBlockZ());
        LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
        Holder<Biome>[] biomeBuffer = resource.biomeBuffer2D;
        for (int dz = 0; dz < 4; ++dz) {
            for (int dx = 0; dx < 4; ++dx) {
                biomeBuffer[dz << 2 | dx] = source.getNoiseBiome(biomeX + dx, -1, biomeZ + dz, sampler);
            }
        }
        for (int i = heightAccessor.getMinSection(); i < heightAccessor.getMaxSection(); ++i) {
            ChunkUtil.fillNoiseBiomes(chunk.getSection(chunk.getSectionIndexFromSectionY(i)), biomeBuffer);
        }
    }

    private static void fillNoiseBiomes(LevelChunkSection section, Holder<Biome>[] biomeBuffer) {
        PalettedContainer biomes = section.getBiomes();
        biomes.acquire();
        for (int dz = 0; dz < 4; ++dz) {
            for (int dx = 0; dx < 4; ++dx) {
                Holder<Biome> biome = biomeBuffer[dz << 2 | dx];
                for (int dy = 0; dy < 4; ++dy) {
                    biomes.getAndSetUnchecked(dx, dy, dz, biome);
                }
            }
        }
        biomes.release();
    }

    public static void fillChunk(int seaLevel, ChunkAccess chunk, TerrainData terrainData, FillerBlock filler, GeneratorResource resource) {
        LevelChunkSection section;
        int index;
        int sy;
        int minBuild = chunk.getMinBuildHeight();
        int maxSectionStart = minBuild + (chunk.getSectionsCount() - 1) * 16;
        int min = Math.max(minBuild, ChunkUtil.getLowestSection(terrainData));
        int max = Math.min(maxSectionStart, ChunkUtil.getHighestSection(terrainData));
        FriendlyByteBuf sectionData = resource.fullSection;
        for (sy = minBuild; sy < min; sy += 16) {
            index = chunk.getSectionIndex(sy);
            if (index < 0 || index >= chunk.getSectionsCount()) continue;
            section = chunk.getSection(index);
            sectionData.resetReaderIndex();
            section.getStates().read(sectionData);
            section.recalcBlockCounts();
        }
        for (sy = min; sy <= max; sy += 16) {
            index = chunk.getSectionIndex(sy);
            if (index < 0 || index >= chunk.getSectionsCount()) continue;
            section = chunk.getSection(index);
            ChunkUtil.fillSection(sy, seaLevel, terrainData, chunk, section, filler);
        }
    }

    public static void primeHeightmaps(int seaLevel, ChunkAccess chunk, TerrainData terrainData, FillerBlock filler) {
        BlockState solid = Blocks.STONE.defaultBlockState();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int i = 0;
        for (int z = 0; z < 16; ++z) {
            int x = 0;
            while (x < 16) {
                int floor = terrainData.getHeight(x, z);
                int surface = Math.max(seaLevel, floor);
                BlockState surfaceBlock = filler.getState(surface, floor);
                oceanFloor.update(x, floor, z, solid);
                worldSurface.update(x, surface, z, surfaceBlock);
                ++x;
                ++i;
            }
        }
    }

    public static void refreshHeightmaps(ChunkAccess chunk) {
        Heightmap.primeHeightmaps((ChunkAccess)chunk, GENERATION_HEIGHTMAPS);
    }

    public static void buildStructureTerrain(ChunkAccess chunk, TerrainData terrainData, StructureFeatureManager structureFeatures) {
        int x = chunk.getPos().getMinBlockX();
        int z = chunk.getPos().getMinBlockZ();
        StructureTerrain operation = new StructureTerrain(chunk, structureFeatures);
        for (int dz = 0; dz < 16; ++dz) {
            for (int dx = 0; dx < 16; ++dx) {
                operation.modify(x + dx, z + dz, chunk, terrainData);
            }
        }
    }

    private static void fillSection(int startY, int seaLevel, TerrainData terrainData, ChunkAccess chunk, LevelChunkSection section, FillerBlock filler) {
        section.acquire();
        int sectionMaxY = startY + 16;
        int i = 0;
        for (int z = 0; z < 16; ++z) {
            int x = 0;
            while (x < 16) {
                int solidY = terrainData.getHeight(x, z);
                int waterY = TerrainLevels.getWaterLevel(x, z, seaLevel, terrainData);
                int firstAirY = Math.max(solidY, waterY) + 1;
                int exclusiveMaxY = Math.min(sectionMaxY, firstAirY);
                for (int y = startY; y < exclusiveMaxY; ++y) {
                    BlockState state = filler.getState(y, solidY);
                    section.setBlockState(x, y & 0xF, z, state, false);
                    if (state.getLightEmission() == 0 || !(chunk instanceof ProtoChunk)) continue;
                    ProtoChunk proto = (ProtoChunk)chunk;
                    proto.addLight(new BlockPos(x, y, z));
                }
                ++x;
                ++i;
            }
        }
        section.release();
    }

    protected static BlockState getFiller(int y, int surfaceSolid) {
        return y <= surfaceSolid ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState();
    }

    protected static int getHighestSection(TerrainData terrainData) {
        int y = Math.max(terrainData.getMaxBase(), terrainData.getMax());
        return y >> 4 << 4;
    }

    protected static int getLowestSection(TerrainData terrainData) {
        int y = terrainData.getMin();
        return y >> 4 << 4;
    }

    protected static ByteBuf createFullPalette() {
        IdMapper stateRegistry = Block.BLOCK_STATE_REGISTRY;
        PalettedContainer container = new PalettedContainer((IdMap)stateRegistry, Blocks.STONE.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        container.acquire();
        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                for (int z = 0; z < 16; ++z) {
                    container.getAndSetUnchecked(x, y, z, Blocks.STONE.defaultBlockState());
                }
            }
        }
        container.release();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        container.write(buffer);
        return buffer;
    }

    public static FriendlyByteBuf getFullSection() {
        return new FriendlyByteBuf(FULL_SECTION.get().copy());
    }

    public static interface FillerBlock {
        public BlockState getState(int var1, int var2);
    }
}
