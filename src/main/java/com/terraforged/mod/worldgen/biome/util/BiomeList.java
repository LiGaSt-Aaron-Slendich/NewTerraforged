package com.terraforged.mod.worldgen.biome.util;

import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class BiomeList {
    private int size = 0;
    private Holder<Biome>[] biomes;

    public BiomeList reset() {
        this.size = 0;
        return this;
    }

    public int size() {
        return this.size;
    }

    public Holder<Biome> get(int i) {
        return this.biomes[i];
    }

    public boolean contains(Holder<Biome> biome) {
        if (this.biomes == null) {
            return false;
        }
        for (int i = 0; i < this.size; ++i) {
            if (this.biomes[i] != biome) continue;
            return true;
        }
        return false;
    }

    public void add(Holder<Biome> biome) {
        if (this.contains(biome)) {
            return;
        }
        this.grow(this.size + 1);
        this.biomes[this.size] = biome;
        ++this.size;
    }

    private void grow(int size) {
        if (this.biomes == null) {
            this.biomes = new Holder[size];
        } else if (this.biomes.length <= size) {
            this.biomes = Arrays.copyOf(this.biomes, size);
        }
    }
}
