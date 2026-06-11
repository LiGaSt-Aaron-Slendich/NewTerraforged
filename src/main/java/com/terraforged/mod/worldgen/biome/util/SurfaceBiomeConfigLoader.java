package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.platform.forge.TFSurfaceBiomeConfig;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class SurfaceBiomeConfigLoader {
    private static ConfigOverlay cached;
    private static int cachedBiomeCount = -1;

    private SurfaceBiomeConfigLoader() {
    }

    public static ConfigOverlay load(Registry<Biome> biomes) {
        int biomeCount = biomes.size();
        if (cached != null && cachedBiomeCount == biomeCount) {
            return cached;
        }
        if (TFSurfaceBiomeConfig.INSTANCE == null) {
            return ConfigOverlay.EMPTY;
        }
        TFSurfaceBiomeConfig config = TFSurfaceBiomeConfig.INSTANCE;
        HashMap<BiomeType, Object2FloatMap<Holder<Biome>>> explicit = new HashMap<BiomeType, Object2FloatMap<Holder<Biome>>>();
        HashSet<Holder<Biome>> configured = new HashSet<Holder<Biome>>();
        int added = 0;
        for (BiomeType type : BiomeType.values()) {
            for (TFSurfaceBiomeConfig.Entry parsed : config.getClimateList(type)) {
                ResourceLocation loc;
                try {
                    loc = new ResourceLocation(parsed.biomeId().trim());
                }
                catch (Exception e) {
                    continue;
                }
                if (!biomes.containsKey(loc)) continue;
                Holder holder = biomes.getHolderOrThrow(ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)loc));
                explicit.computeIfAbsent(type, t -> new Object2FloatLinkedOpenHashMap()).put(holder, parsed.weight());
                configured.add((Holder<Biome>)holder);
                ++added;
            }
        }
        if (added > 0) {
            TerraForged.LOG.info("[SurfaceBiomeConfig] Loaded {} explicit surface biome assignments", added);
        }
        cached = new ConfigOverlay(explicit, configured, config.modBiomeWeight, config.autoDetectModBiomes);
        cachedBiomeCount = biomeCount;
        return cached;
    }

    public record ConfigOverlay(Map<BiomeType, Object2FloatMap<Holder<Biome>>> explicit, Set<Holder<Biome>> configured, float autoModWeight, boolean autoDetectModBiomes) {
        public static final ConfigOverlay EMPTY = new ConfigOverlay(Map.of(), Set.of(), 5.0f, true);
    }
}
