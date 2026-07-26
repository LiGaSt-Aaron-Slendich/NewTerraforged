package com.terraforged.mod.compat;

import com.terraforged.mod.TerraForged;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;

/**
 * Keeps TerraBlender region-registered mod biomes visible to NewTerraForged without replacing terrain.
 * TB still owns climate→biome for content mods; NTFG owns heightfield/rivers/caves.
 */
public final class TerraBlenderRegionBridge {
    private static final Set<ResourceLocation> REGION_BIOME_IDS = new HashSet<>();
    private static boolean scanned;

    private TerraBlenderRegionBridge() {
    }

    public static void onWorldLoad(MinecraftServer server) {
        if (!TerraBlenderCompat.isTerraBlenderLoaded()) {
            return;
        }
        TerraBlenderRegionBridge.invokeLevelUtils(server);
        TerraBlenderRegionBridge.scan(server.registryAccess());
    }

    public static Set<ResourceLocation> regionBiomeIds() {
        return Collections.unmodifiableSet(TerraBlenderRegionBridge.REGION_BIOME_IDS);
    }

    public static boolean isTerraBlenderRegionBiome(ResourceLocation id) {
        return TerraBlenderRegionBridge.REGION_BIOME_IDS.contains(id);
    }

    public static void scan(RegistryAccess access) {
        if (!TerraBlenderCompat.isTerraBlenderLoaded() || scanned) {
            return;
        }
        scanned = true;
        TerraBlenderRegionBridge.REGION_BIOME_IDS.clear();
        TerraBlenderRegionBridge.collectFromRegionsApi();
        TerraBlenderRegionBridge.collectKnownModNamespaces(access);
        if (!TerraBlenderRegionBridge.REGION_BIOME_IDS.isEmpty()) {
            TerraForged.LOG.info("[TerraBlenderRegionBridge] Tracking {} mod biomes from TB regions / registry", TerraBlenderRegionBridge.REGION_BIOME_IDS.size());
        }
    }

    private static void invokeLevelUtils(MinecraftServer server) {
        try {
            Class<?> levelUtils = Class.forName("terrablender.util.LevelUtils");
            for (Method method : levelUtils.getMethods()) {
                if (!"onServerAboutToStart".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                method.invoke(null, server);
                TerraForged.LOG.debug("TerraBlender LevelUtils.onServerAboutToStart invoked for NTFG world");
                return;
            }
        }
        catch (Throwable t) {
            TerraForged.LOG.debug("TerraBlender LevelUtils hook skipped: {}", t.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectFromRegionsApi() {
        try {
            Class<?> regionsClass = Class.forName("terrablender.api.Regions");
            Object map = null;
            for (Method method : regionsClass.getMethods()) {
                if (method.getParameterCount() != 0 || !Map.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                String name = method.getName().toLowerCase();
                if (!name.contains("region")) {
                    continue;
                }
                map = method.invoke(null);
                if (map instanceof Map<?, ?> m && !m.isEmpty()) {
                    break;
                }
            }
            if (!(map instanceof Map<?, ?> regions) || regions.isEmpty()) {
                return;
            }
            for (Object region : regions.values()) {
                TerraBlenderRegionBridge.collectBiomesFromRegion(region);
            }
        }
        catch (Throwable t) {
            TerraForged.LOG.debug("TerraBlender Regions API scan skipped: {}", t.toString());
        }
    }

    private static void collectBiomesFromRegion(Object region) {
        for (Method method : region.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !Iterable.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("biome")) {
                continue;
            }
            try {
                Iterable<?> iterable = (Iterable<?>) method.invoke(region);
                for (Object entry : iterable) {
                    TerraBlenderRegionBridge.registerBiomeKey(entry);
                }
            }
            catch (Throwable ignored) {
            }
        }
    }

    private static void registerBiomeKey(Object key) {
        if (key instanceof ResourceKey<?> rk) {
            if (rk.registry().equals(Registry.BIOME_REGISTRY)) {
                TerraBlenderRegionBridge.REGION_BIOME_IDS.add(rk.location());
            }
            return;
        }
        if (key instanceof ResourceLocation loc) {
            TerraBlenderRegionBridge.REGION_BIOME_IDS.add(loc);
            return;
        }
        if (key instanceof Holder<?> holder) {
            holder.unwrapKey().ifPresent(k -> TerraBlenderRegionBridge.REGION_BIOME_IDS.add(k.location()));
        }
    }

    private static void collectKnownModNamespaces(RegistryAccess access) {
        Registry<Biome> biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        for (Holder<Biome> holder : biomes.asHolderIdMap()) {
            ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null || "minecraft".equals(id.getNamespace()) || "newterraforged".equals(id.getNamespace())
                    || "terraforged".equals(id.getNamespace()) || "terrablender".equals(id.getNamespace())) {
                continue;
            }
            TerraBlenderRegionBridge.REGION_BIOME_IDS.add(id);
        }
    }
}
