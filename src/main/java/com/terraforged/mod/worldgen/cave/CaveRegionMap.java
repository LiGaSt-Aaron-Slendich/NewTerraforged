package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveBiomeEntry;
import com.terraforged.mod.worldgen.cave.CaveBiomeRegistry;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveStatInitializer;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public final class CaveRegionMap {
    private final CaveMegaGigaLayout layout;

    public CaveRegionMap(int seed, int cx, int cz, int radius, CaveBiomeRegistry registry, CaveSystemConfig config, boolean isMega, Holder<Biome> surfaceBiome, int sampleY, int surfaceY) {
        CaveStatInitializer.CaveStatSnapshot snapshot = CaveStatInitializer.initialize(surfaceBiome, sampleY, surfaceY, 0.0f, 0.0f, seed, cx, cz);
        this.layout = CaveMegaGigaLayout.build(seed, cx, cz, radius, registry, config, isMega, snapshot);
    }

    public CaveBiomeEntry getBiomeAt(int x, int z) {
        return this.layout.getBiomeAt(x, z);
    }

    public CaveMegaGigaLayout layout() {
        return this.layout;
    }
}
