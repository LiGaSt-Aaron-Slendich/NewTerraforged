package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Middle ground between the fast vanilla {@code PlacedFeature} pass and the heavy legacy anchor/scatter stack.
 * Uses themed cover + bounded scatter ({@link CaveBiomeVolumeDecorator}) and sparse mega accents
 * ({@link CaveMegaAccentDecorator}) without the per-cave {@link NoiseCaveDecorator} loop.
 */
public final class CaveBiomeCompromiseDecorator {
    private CaveBiomeCompromiseDecorator() {
    }

    public static void decorateVolume(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        CarverColumnCache columns = carver.columnCache();
        CaveBiomeVolumeDecorator.decorateChunk(chunk, carver, region, generator);
        if (columns.anyMegaGiga()) {
            CaveMegaAccentDecorator.decorate(chunk, carver, region, generator);
            if (carver.hasTunnelRiver()) {
                CaveTunnelRiverDecorator.decorate(chunk, carver, region, generator);
            }
        }
    }

    public static void decorateEntrances(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator) {
        if (carver.hasAnyEntranceColumn()) {
            CaveEntranceSurfaceDecorator.decorate(chunk, carver, region, generator);
            CaveEntranceVanillaDecorator.decorate(chunk, carver, region, generator);
        }
        CarverColumnCache columns = carver.columnCache();
        if (!columns.anyMegaGiga() && carver.hasDecorateAnchors()) {
            CaveBiomeVolumeDecorator.decorateChunk(chunk, carver, region, generator);
        }
    }
}
