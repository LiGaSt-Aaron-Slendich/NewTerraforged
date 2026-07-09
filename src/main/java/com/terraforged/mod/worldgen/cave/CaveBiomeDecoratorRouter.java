package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Classifies cave biomes for diagnostics and feature-stage routing.
 * All biomes now go through the unified official TF decorator — this router
 * is kept only for CaveFeatureDiagnostics verdicts and optional per-biome checks.
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
        if (CaveBiomeDecoratorRouter.isOfficialBiome(path, id)) {
            return CaveDecoratorKind.OFFICIAL;
        }
        if (CaveBiomeDecoratorRouter.isVanillaBiome(path)) {
            return CaveDecoratorKind.VANILLA;
        }
        return CaveDecoratorKind.OFFICIAL;
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

    /** Glowing grotto and similar — vanilla multi-origin pass. */
    private static boolean isVanillaBiome(String path) {
        return path.contains("glowing_grotto") || path.contains("undergarden") && !path.contains("fungal");
    }
}
