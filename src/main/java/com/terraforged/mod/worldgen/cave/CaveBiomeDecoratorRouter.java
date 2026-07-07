package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Routes painted cave biomes to the decorator backend that works best for them.
 */
public final class CaveBiomeDecoratorRouter {
    private CaveBiomeDecoratorRouter() {
    }

    public static CaveDecoratorKind resolve(Holder<Biome> biome) {
        ResourceLocation id = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) {
            return CaveDecoratorKind.COMPROMISE;
        }
        String path = id.getPath().toLowerCase();
        if (CaveBiomeDecoratorRouter.isLegacyFungalBiome(path)) {
            return CaveDecoratorKind.LEGACY;
        }
        if (CaveBiomeDecoratorRouter.isOfficialBiome(path, id)) {
            return CaveDecoratorKind.OFFICIAL;
        }
        if (CaveBiomeDecoratorRouter.isVanillaBiome(path)) {
            return CaveDecoratorKind.VANILLA;
        }
        return CaveDecoratorKind.COMPROMISE;
    }

    /** Scorching / dripstone / stone cover — original TF decorator. */
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
        if (path.contains("fungal")) {
            return true;
        }
        return CaveBiomeIds.isEmptyStoneCave(id) || "minecraft".equals(id.getNamespace()) && path.contains("cave");
    }

    /** Bioshroom / mycotoxic / mushroom caves — legacy scatter with mushroom budgets. */
    private static boolean isLegacyFungalBiome(String path) {
        return path.contains("bioshroom") || path.contains("glowshroom") || path.contains("mycotoxic")
                || path.contains("mushroom") && path.contains("cave");
    }

    /** Glowing grotto and similar — vanilla multi-origin pass. */
    private static boolean isVanillaBiome(String path) {
        return path.contains("glowing_grotto") || path.contains("undergarden") && !path.contains("fungal");
    }
}
