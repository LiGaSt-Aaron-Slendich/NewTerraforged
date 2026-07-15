package com.terraforged.mod.compat;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** Terralith / BOP / RU etc. — not TF-native namespaces. */
public final class ModBiomeIntegration {
    private ModBiomeIntegration() {
    }

    public static boolean isExternalModBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> ModBiomeIntegration.isExternalModNamespace(key.location())).orElse(false);
    }

    public static boolean isExternalModNamespace(ResourceLocation id) {
        String ns = id.getNamespace();
        return !"minecraft".equals(ns) && !"newterraforged".equals(ns) && !"terraforged".equals(ns);
    }
}
