package com.terraforged.mod.worldgen.util;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;

public class BiomeBuffer2D
implements BiomeResolver {
    public final Holder<Biome>[] biomeBuffer2D = BiomeBuffer2D.create(16);

    public void set(int x, int z, Holder<Biome> biome) {
        this.biomeBuffer2D[(z &= 3) << 2 | (x &= 3)] = biome;
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return this.biomeBuffer2D[(z &= 3) << 2 | (x &= 3)];
    }

    private static Holder<Biome>[] create(int size) {
        return new Holder[size];
    }
}
