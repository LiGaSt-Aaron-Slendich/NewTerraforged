package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.platform.forge.TFCaveBiomeConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Routes painted cave biomes to the decorator backend that works best for them.
 * Routing table: {@code Critical Options/Hybrid options/decorator-routing.toml}.
 */
public final class CaveBiomeDecoratorRouter {
    private CaveBiomeDecoratorRouter() {
    }

    public static CaveDecoratorKind resolve(Holder<Biome> biome) {
        ResourceLocation id = biome.unwrapKey().map(key -> key.location()).orElse(null);
        CaveDecoratorRoutingTable table = TFCaveBiomeConfig.INSTANCE != null ? TFCaveBiomeConfig.INSTANCE.decoratorRouting : null;
        if (table == null) {
            table = CaveDecoratorRoutingTable.defaults();
        }
        return table.resolve(id);
    }
}
