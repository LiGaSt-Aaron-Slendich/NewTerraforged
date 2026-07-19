package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

public final class MegaCaveStructureFilter {
    private static final String[] BLOCKED = new String[]{"mineshaft", "monster_room", "fossil", "geode", "stronghold", "dungeon", "dungeon_extra", "ruined_portal", "ancient_city", "trial_chambers", "trial_chamber", "buried_treasure", "shipwreck", "ocean_ruin", "outpost", "fortress", "bastion", "end_city", "pillager_outpost", "village", "desert_pyramid", "jungle_pyramid", "swamp_hut", "igloo", "woodland_mansion"};

    private MegaCaveStructureFilter() {
    }

    public static void beginChunkCache(Generator generator, ChunkAccess chunk, int padding) {
        CarverColumnCache columns = null;
        CarverChunk carver = generator.peekCaveCarver(chunk.getPos());
        if (carver != null && carver.isColumnCacheReady()) {
            columns = carver.columnCache();
        }
        MegaGigaChunkCache.begin(generator, chunk, padding, columns);
    }

    public static void endChunkCache() {
        MegaGigaChunkCache.end();
    }

    public static boolean shouldSkip(Generator generator, BlockPos chunkCenter, Holder<ConfiguredStructureFeature<?, ?>> structure) {
        if (!MegaCaveStructureFilter.isInMegaOrGigaCave(generator, chunkCenter.getX(), chunkCenter.getZ())) {
            return false;
        }
        ResourceLocation id = structure.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) {
            return false;
        }
        return MegaCaveStructureFilter.matches(id.getPath());
    }

    public static boolean isInMegaOrGigaCave(Generator generator, int x, int z) {
        return MegaGigaChunkCache.isInMegaOrGiga(generator, x, z);
    }

    public static boolean isUndergroundRiverFloor(Generator generator, int x, int floorY, int z) {
        if (!MegaCaveStructureFilter.isInMegaOrGigaCave(generator, x, z)) {
            return false;
        }
        int surface = generator.getOceanFloorHeight(x, z);
        return floorY < surface - 6;
    }

    public static boolean isInMegaOrGigaCaveAt(Generator generator, int x, int y, int z) {
        return MegaGigaChunkCache.isInMegaOrGigaAt(generator, x, y, z);
    }

    private static boolean matches(String path) {
        for (String keyword : BLOCKED) {
            if (!path.contains(keyword)) continue;
            return true;
        }
        return false;
    }
}
