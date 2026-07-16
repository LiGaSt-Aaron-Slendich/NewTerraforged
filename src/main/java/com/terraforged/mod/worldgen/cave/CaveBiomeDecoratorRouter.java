package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.compat.TerraBlenderBiomeAuthority;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Classifies cave biomes for hybrid decorator routing.
 * When TerraBlender owns paint, datapack cave biomes use TF-0.3-style
 * {@code placeWithBiomeCheck} (VANILLA pass) instead of heavy OFFICIAL filters.
 */
public final class CaveBiomeDecoratorRouter {
    private CaveBiomeDecoratorRouter() {
    }

    public static CaveDecoratorKind resolve(Holder<Biome> biome) {
        ResourceLocation id = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) {
            return CaveDecoratorKind.OFFICIAL;
        }
        String path = id.getPath().toLowerCase();
        // Volcanic / scorching still need OFFICIAL accent + filter routing.
        if (CaveBiomeIds.isScorchingCaveBiome(id) || CaveBiomeIds.isVolcanicCaveBiome(id)
                || path.contains("mantle") || path.contains("brimstone") || path.contains("magma")) {
            return CaveDecoratorKind.OFFICIAL;
        }
        if (TerraBlenderBiomeAuthority.isActive() && CaveBiomeIds.isUndergroundBiome(id)
                && CaveBiomeDecoratorRouter.isTerraBlenderDatapackCave(id, path)) {
            return CaveDecoratorKind.VANILLA;
        }
        if (CaveBiomeDecoratorRouter.isOfficialBiome(path, id)) {
            return CaveDecoratorKind.OFFICIAL;
        }
        if (CaveBiomeDecoratorRouter.isVanillaBiome(path)) {
            return CaveDecoratorKind.VANILLA;
        }
        return CaveDecoratorKind.OFFICIAL;
    }

    /** Terralith / BOP / RU / BYG / WilderNature cave biomes painted via TB ParameterLists. */
    static boolean isTerraBlenderDatapackCave(ResourceLocation id, String path) {
        String ns = id.getNamespace();
        if ("terralith".equals(ns) || "biomesoplenty".equals(ns) || "regions_unexplored".equals(ns)
                || "byg".equals(ns) || "wildernature".equals(ns) || "wythers".equals(ns)) {
            return path.contains("cave") || path.contains("grotto") || path.contains("fungal")
                    || path.contains("bioshroom") || path.contains("glowshroom") || path.contains("dripstone")
                    || path.contains("lush_caves") || path.contains("underground") || path.contains("karst")
                    || path.contains("prismachasm") || path.contains("mycotoxic") || path.contains("undergarden");
        }
        return false;
    }

    /** All TF and mod cave biomes — unified official decorator handles feature/tag routing. */
    private static boolean isOfficialBiome(String path, ResourceLocation id) {
        if (CaveBiomeIds.isScorchingCaveBiome(id) || CaveBiomeIds.isVolcanicCaveBiome(id)) {
            return true;
        }
        if (path.contains("mantle") || path.contains("brimstone") || path.contains("magma")) {
            return true;
        }
        if (path.contains("dripstone") || path.contains("karst") || path.contains("limestone") || path.contains("tuff_caves") || path.contains("tuff_cave")) {
            return true;
        }
        if (path.contains("icicle") || path.contains("stalactite")) {
            return true;
        }
        if (path.contains("fungal") || path.contains("mycotoxic") || path.contains("bioshroom") || path.contains("glowshroom")) {
            return true;
        }
        if (path.contains("mushroom") && path.contains("cave")) {
            return true;
        }
        return CaveBiomeIds.isEmptyStoneCave(id) || "minecraft".equals(id.getNamespace()) && path.contains("cave");
    }

    /** Glowing grotto and similar — vanilla multi-origin pass (Terralith/BOP model). */
    private static boolean isVanillaBiome(String path) {
        return path.contains("glowing_grotto") || path.contains("glowshroom_caves") || path.contains("glowshroom_cave")
                || path.contains("undergarden") && !path.contains("fungal");
    }
}
