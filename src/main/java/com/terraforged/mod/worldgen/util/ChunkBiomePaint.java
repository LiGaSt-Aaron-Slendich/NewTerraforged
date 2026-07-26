package com.terraforged.mod.worldgen.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

/**
 * Prevents {@code IllegalArgumentException: No value with id -1} when chunk biome palettes
 * are written with null or unbound Holders (client then fails to decode and "holes" appear).
 */
public final class ChunkBiomePaint {
    private ChunkBiomePaint() {
    }

    public static Holder<Biome> plains(Registry<Biome> biomes) {
        return biomes.getHolderOrThrow(Biomes.PLAINS);
    }

    /**
     * Returns a Holder that is guaranteed to resolve in {@code biomes} (id &gt;= 0), or plains.
     */
    public static Holder<Biome> sanitize(@Nullable Holder<Biome> biome, Registry<Biome> biomes) {
        Holder<Biome> plains = plains(biomes);
        if (biome == null) {
            return plains;
        }
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key != null) {
            return biomes.getHolder(key).orElse(plains);
        }
        // Direct/unbound holder — never write into a section palette.
        return plains;
    }

    public static void set(LevelChunkSection section, int quartX, int quartY, int quartZ, @Nullable Holder<Biome> biome, Registry<Biome> biomes) {
        Holder<Biome> safe = sanitize(biome, biomes);
        ((net.minecraft.world.level.chunk.PalettedContainer)(Object)(section).getBiomes()).getAndSetUnchecked(quartX, quartY, quartZ, safe);
    }
}
