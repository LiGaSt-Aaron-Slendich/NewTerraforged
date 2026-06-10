package com.terraforged.mod.worldgen.biome.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

public class DelegateBiomeManager
extends BiomeManager {
    protected BiomeManager delegate;

    public DelegateBiomeManager() {
        super(null, 0L);
    }

    protected void setDelegate(BiomeManager delegate) {
        this.delegate = delegate;
    }

    public BiomeManager withDifferentSource(BiomeManager.NoiseBiomeSource source) {
        return this.delegate.withDifferentSource(source);
    }

    public Holder<Biome> getBiome(BlockPos pos) {
        return this.delegate.getBiome(pos);
    }

    public Holder<Biome> getNoiseBiomeAtPosition(double x, double y, double z) {
        return this.delegate.getNoiseBiomeAtPosition(x, y, z);
    }

    public Holder<Biome> getNoiseBiomeAtPosition(BlockPos pos) {
        return this.delegate.getNoiseBiomeAtPosition(pos);
    }

    public Holder<Biome> getNoiseBiomeAtQuart(int x, int y, int z) {
        return this.delegate.getNoiseBiomeAtQuart(x, y, z);
    }
}
