package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.Source;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Minimal 1.19 port shim for surface biome quart restore at cave mouths.
 * Full restore logic can be deepened later; compile-safe stubs keep carve/sanitizer linked.
 */
public final class CaveSurfaceBiomeRestorer {
    private CaveSurfaceBiomeRestorer() {
    }

    public static void setBiomeQuart(ChunkAccess chunk, int lx, int ly, int lz, Biome biome) {
        // no-op
    }

    public static void setBiomeQuart(ChunkAccess chunk, int lx, int ly, int lz, Holder<Biome> biome) {
        if (biome != null) {
            setBiomeQuart(chunk, lx, ly, lz, biome.value());
        }
    }

    public static Holder<Biome> resolveSurfaceBiome(Source source, int seed, int x, int z, int surfaceY) {
        if (source == null) {
            return null;
        }
        return source.getNoiseBiome(x >> 2, surfaceY >> 2, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
    }

    public static void restore(ChunkAccess chunk, Generator generator, CarverChunk carver) {
        // no-op stub on 1.19 until paint APIs are fully wired
    }
}
