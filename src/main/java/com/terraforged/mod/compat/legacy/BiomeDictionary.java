package com.terraforged.mod.compat.legacy;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Minimal BiomeDictionary stand-in for 1.19+ (Forge removed BiomeDictionary).
 * Tag/path heuristics only — enough for NewTF rule autogen / cave filters.
 */
public final class BiomeDictionary {
    public enum Type {
        OCEAN, RIVER, BEACH, SWAMP, FOREST, TAIGA, CONIFEROUS, PLAINS, MOUNTAIN, HILLS,
        SANDY, MESA, SAVANNA, JUNGLE, SNOWY, WASTELAND, UNDERGROUND, NETHER, END, VOID
    }

    private BiomeDictionary() {
    }

    public static boolean hasType(ResourceKey<Biome> key, Type type) {
        return getTypes(key).contains(type);
    }

    public static Set<Type> getTypes(ResourceKey<Biome> key) {
        if (key == null) {
            return Collections.emptySet();
        }
        String path = key.location().getPath().toLowerCase(Locale.ROOT);
        EnumSet<Type> out = EnumSet.noneOf(Type.class);
        addPathHints(path, out);
        return out.isEmpty() ? Collections.emptySet() : out;
    }

    public static Set<Type> getTypes(Holder<Biome> holder) {
        if (holder == null) {
            return Collections.emptySet();
        }
        EnumSet<Type> out = EnumSet.noneOf(Type.class);
        if (holder.is(BiomeTags.IS_OCEAN) || holder.is(BiomeTags.IS_DEEP_OCEAN)) out.add(Type.OCEAN);
        if (holder.is(BiomeTags.IS_RIVER)) out.add(Type.RIVER);
        if (holder.is(BiomeTags.IS_BEACH)) out.add(Type.BEACH);
        if (holder.is(BiomeTags.IS_FOREST)) out.add(Type.FOREST);
        if (holder.is(BiomeTags.IS_TAIGA)) { out.add(Type.TAIGA); out.add(Type.CONIFEROUS); }
        if (holder.is(BiomeTags.IS_MOUNTAIN) || holder.is(BiomeTags.IS_HILL)) { out.add(Type.MOUNTAIN); out.add(Type.HILLS); }
        if (holder.is(BiomeTags.IS_JUNGLE)) out.add(Type.JUNGLE);
        if (holder.is(BiomeTags.IS_SAVANNA)) out.add(Type.SAVANNA);
        if (holder.is(BiomeTags.IS_BADLANDS)) out.add(Type.MESA);
        if (holder.is(BiomeTags.IS_NETHER)) out.add(Type.NETHER);
        if (holder.is(BiomeTags.IS_END)) out.add(Type.END);
        holder.unwrapKey().ifPresent(k -> addPathHints(k.location().getPath().toLowerCase(Locale.ROOT), out));
        return out;
    }

    private static void addPathHints(String path, EnumSet<Type> out) {
        if (path.contains("ocean") || path.contains("warm_ocean") || path.contains("lukewarm") || path.contains("cold_ocean") || path.contains("frozen_ocean")) out.add(Type.OCEAN);
        if (path.contains("river")) out.add(Type.RIVER);
        if (path.contains("beach") || path.contains("shore")) out.add(Type.BEACH);
        if (path.contains("swamp") || path.contains("mangrove")) out.add(Type.SWAMP);
        if (path.contains("forest") || path.contains("grove") || path.contains("wood")) out.add(Type.FOREST);
        if (path.contains("taiga")) { out.add(Type.TAIGA); out.add(Type.CONIFEROUS); }
        if (path.contains("plains") || path.contains("meadow") || path.contains("sunflower")) out.add(Type.PLAINS);
        if (path.contains("mountain") || path.contains("peak") || path.contains("slope") || path.contains("windswept")) out.add(Type.MOUNTAIN);
        if (path.contains("hill")) out.add(Type.HILLS);
        if (path.contains("desert") || path.contains("sand")) out.add(Type.SANDY);
        if (path.contains("badland") || path.contains("mesa")) out.add(Type.MESA);
        if (path.contains("savanna")) out.add(Type.SAVANNA);
        if (path.contains("jungle")) out.add(Type.JUNGLE);
        if (path.contains("snow") || path.contains("frozen") || path.contains("ice") || path.contains("cold")) out.add(Type.SNOWY);
        if (path.contains("cave") || path.contains("dripstone") || path.contains("lush_cave") || path.contains("underground")) out.add(Type.UNDERGROUND);
        if (path.contains("nether") || path.contains("soul_sand") || path.contains("crimson") || path.contains("warped") || path.contains("basalt")) out.add(Type.NETHER);
        if (path.contains("end") || path.contains("the_end")) out.add(Type.END);
        if (path.contains("void")) out.add(Type.VOID);
    }
}
