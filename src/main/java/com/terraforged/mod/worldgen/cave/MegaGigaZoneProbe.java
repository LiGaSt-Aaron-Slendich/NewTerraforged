package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.noise.Module;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared mega/giga column classification — thresholds and merged noise match {@link CarverColumnCache}.
 */
public final class MegaGigaZoneProbe {
    public static final byte NONE = 0;
    public static final byte MEGA = 1;
    public static final byte GIGA = 2;
    static final float MEGA_THRESHOLD = 0.08f;
    static final float GIGA_THRESHOLD = 0.07f;

    private MegaGigaZoneProbe() {
    }

    public static byte classify(Generator generator, int x, int z) {
        int seed = Seeds.get(generator.getSeed());
        Module giga = CaveModifiers.giga();
        if (giga != null && CaveNoise.sampleMerged(giga, seed, x, z) > GIGA_THRESHOLD && CaveReliefFilter.qualifiesGigaTerrain(generator, x, z)) {
            return GIGA;
        }
        Module mega = CaveModifiers.mega();
        if (mega != null && CaveNoise.sampleMerged(mega, seed, x, z) > MEGA_THRESHOLD) {
            return MEGA;
        }
        return NONE;
    }

    public static byte classifyAt(Generator generator, int x, int y, int z) {
        byte zone = MegaGigaZoneProbe.classifyWithCarverCache(generator, x, z);
        if (zone == NONE) {
            return NONE;
        }
        int surface = generator.getOceanFloorHeight(x, z);
        if (y >= surface - 6) {
            return NONE;
        }
        return zone;
    }

    public static byte classifyWithCarverCache(Generator generator, int x, int z) {
        CarverChunk carver = generator.peekCaveCarver(new ChunkPos(x >> 4, z >> 4));
        if (carver != null && carver.isColumnCacheReady()) {
            byte cached = carver.columnCache().megaGigaFlag(x & 0xF, z & 0xF);
            if (cached != NONE) {
                return cached;
            }
        }
        return MegaGigaZoneProbe.classify(generator, x, z);
    }
}
