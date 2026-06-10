package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

public class BiomeUtil {
    private static final Map<BiomeType, ResourceLocation> TYPE_NAMES = new EnumMap<BiomeType, ResourceLocation>(BiomeType.class);
    private static final Comparator<ResourceKey<?>> KEY_COMPARATOR = Comparator.comparing(ResourceKey::location);
    public static Comparator<Holder<Biome>> BIOME_SORTER = (o1, o2) -> {
        ResourceKey k1 = o1.unwrapKey().orElseThrow();
        ResourceKey k2 = o2.unwrapKey().orElseThrow();
        Objects.requireNonNull(k1);
        Objects.requireNonNull(k2);
        return KEY_COMPARATOR.compare(k1, k2);
    };

    public static ResourceLocation getRegistryName(BiomeType type) {
        return TYPE_NAMES.get(type);
    }

    public static List<Holder<Biome>> getOverworldBiomes(RegistryAccess access) {
        return BiomeUtil.getOverworldBiomes((Registry<Biome>)access.registryOrThrow(Registry.BIOME_REGISTRY));
    }

    public static List<Holder<Biome>> getOverworldBiomes(Registry<Biome> biomes) {
        ObjectArrayList holders = new ObjectArrayList();
        for (Holder biome : biomes.asHolderIdMap()) {
            if (!BiomeUtil.isOverworldSurfaceBiome((Holder<Biome>)biome)) continue;
            holders.add(biome);
        }
        holders.sort(BIOME_SORTER);
        return holders;
    }

    public static BiomeType getType(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.HAS_VILLAGE_DESERT)) {
            return BiomeType.DESERT;
        }
        if (biome.is(BiomeTags.HAS_VILLAGE_SAVANNA)) {
            return BiomeType.SAVANNA;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return BiomeType.TROPICAL_RAINFOREST;
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return BiomeUtil.getByTemp((Biome)biome.value(), BiomeType.TUNDRA, BiomeType.TAIGA);
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return BiomeUtil.getByRain((Biome)biome.value(), BiomeType.TUNDRA, BiomeType.TEMPERATE_RAINFOREST, BiomeType.TEMPERATE_FOREST);
        }
        return BiomeType.GRASSLAND;
    }

    public static BiomeType getByRain(Biome biome, BiomeType frozen, BiomeType wetter, BiomeType dryer) {
        if (biome.getPrecipitation() == Biome.Precipitation.SNOW) {
            return frozen;
        }
        return (double)biome.getDownfall() >= 0.8 ? wetter : dryer;
    }

    public static BiomeType getByTemp(Biome biome, BiomeType colder, BiomeType warmer) {
        return biome.getPrecipitation() == Biome.Precipitation.SNOW ? colder : warmer;
    }

    public static BiomeType getByTemp(Biome biome, BiomeType cold, BiomeType temperate, BiomeType hot) {
        if (biome.getPrecipitation() == Biome.Precipitation.SNOW) {
            return cold;
        }
        if (biome.getPrecipitation() == Biome.Precipitation.NONE || (double)biome.getBaseTemperature() > 1.0) {
            return hot;
        }
        return temperate;
    }

    public static boolean isOverworldSurfaceBiome(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_NETHER)) {
            return false;
        }
        Biome.BiomeCategory category = Biome.getBiomeCategory(biome);
        if (category == Biome.BiomeCategory.NETHER || category == Biome.BiomeCategory.THEEND || category == Biome.BiomeCategory.UNDERGROUND) {
            return false;
        }
        return biome.unwrapKey().map(key -> BiomeUtil.isOverworldSurfaceBiomePath(key.location())).orElse(false);
    }

    private static boolean isOverworldSurfaceBiomePath(ResourceLocation id) {
        String ns = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if ("newterraforged".equals(ns) && (path.startsWith("cave_") || "cave".equals(path))) {
            return false;
        }
        if (BiomeUtil.isUndergroundBiomePath(path)) {
            return false;
        }
        if (BiomeUtil.isNetherThemedBiomePath(path)) {
            return false;
        }
        return !BiomeUtil.isEndThemedBiomePath(path);
    }

    private static boolean isUndergroundBiomePath(String path) {
        return path.contains("cave") || path.contains("cavern") || path.contains("grotto") || path.contains("hypogeal") || path.contains("underground") || path.contains("undergarden") || path.contains("prismachasm") || path.contains("mycotoxic");
    }

    private static boolean isNetherThemedBiomePath(String path) {
        return path.contains("nether") || path.contains("crimson") || path.contains("warped") || path.contains("soul_sand") || path.contains("soul_valley") || path.contains("basalt_deltas") || path.contains("magma_waste") || path.contains("glowstone_garden") || path.contains("inferno") || path.contains("wailing_garth") || path.contains("weeping_mire") || path.contains("sythian") || path.contains("arisian") || path.contains("embur_bog") || path.contains("brimstone_cavern");
    }

    private static boolean isEndThemedBiomePath(String path) {
        return path.contains("shulkren") || path.contains("ethereal_islands") || path.contains("nightshade") || path.contains("bulbis_gardens") || path.contains("imparius_grove") || path.contains("_end") || path.equals("end") || path.contains("void");
    }

    private static Set<Holder<Biome>> getVanillaOverworldBiomes(Registry<Biome> biomes) {
        return MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(biomes).possibleBiomes();
    }

    static {
        for (BiomeType type : BiomeType.values()) {
            TYPE_NAMES.put(type, TerraForged.location(type.name().toLowerCase(Locale.ROOT)));
        }
    }
}
