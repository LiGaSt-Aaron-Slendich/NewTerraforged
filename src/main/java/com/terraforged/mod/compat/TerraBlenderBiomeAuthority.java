package com.terraforged.mod.compat;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.GenerationFeatureGates;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * TerraBlender biome authority for NewTerraForged worlds.
 * Builds combined climate ParameterLists from TB {@code Regions} and paints biomes from TF climate proxies.
 * Does not replace terrain — only replaces {@code BiomeSampler} weight-map selection when enabled.
 * Uses public {@link Climate.ParameterList} (RTree is protected on 1.18.2).
 */
public final class TerraBlenderBiomeAuthority {
    private static final ResourceLocation DEFERRED_PLACEHOLDER = new ResourceLocation("terrablender", "deferred_placeholder");
    private static Climate.ParameterList<Holder<Biome>> surfaceList;
    private static Climate.ParameterList<Holder<Biome>> undergroundList;
    private static boolean ready;
    private static Holder<Biome> plainsFallback;

    private TerraBlenderBiomeAuthority() {
    }

    public static boolean isActive() {
        return GenerationFeatureGates.terraBlenderBiomeAuthorityEnabled
                && TerraBlenderCompat.isTerraBlenderLoaded()
                && ready
                && surfaceList != null;
    }

    public static void onWorldLoad(MinecraftServer server) {
        if (server == null) {
            return;
        }
        TerraBlenderBiomeAuthority.init(server.registryAccess());
    }

    /** Build trees as soon as registries exist (createLevels HEAD) so spawn chunks use TB. */
    public static void init(RegistryAccess access) {
        if (!GenerationFeatureGates.terraBlenderBiomeAuthorityEnabled || !TerraBlenderCompat.isTerraBlenderLoaded()) {
            ready = false;
            return;
        }
        try {
            TerraBlenderBiomeAuthority.buildTrees(access);
            ready = surfaceList != null;
            if (ready) {
                TerraForged.LOG.info("[TerraBlenderBiomeAuthority] Active — TF BiomeSampler paint suppressed; TB region ParameterLists in use");
            }
        }
        catch (Throwable t) {
            ready = false;
            TerraForged.LOG.warn("[TerraBlenderBiomeAuthority] Failed to build region trees: {}", t.toString());
        }
    }

    public static void clear() {
        surfaceList = null;
        undergroundList = null;
        plainsFallback = null;
        ready = false;
    }

    public static Holder<Biome> sampleSurface(ClimateSample sample) {
        return TerraBlenderBiomeAuthority.find(surfaceList, TerraBlenderBiomeAuthority.toTarget(sample, 0.0f));
    }

    /**
     * Depth-aware TB pick. Returns null if the nearest point is not an underground biome
     * so {@code CaveBiomeSampler} can take over.
     */
    public static Holder<Biome> sampleUnderground(ClimateSample sample, float depth01) {
        if (undergroundList == null) {
            return null;
        }
        float depth = Math.max(0.15f, Math.min(1.1f, depth01));
        Holder<Biome> found = TerraBlenderBiomeAuthority.find(undergroundList, TerraBlenderBiomeAuthority.toTarget(sample, depth));
        if (found == null || !CaveBiomeIds.isUndergroundBiome(found)) {
            return null;
        }
        return found;
    }

    private static Holder<Biome> find(Climate.ParameterList<Holder<Biome>> list, Climate.TargetPoint target) {
        if (list == null) {
            return plainsFallback;
        }
        Holder<Biome> found = list.findValue(target);
        if (found != null && TerraBlenderBiomeAuthority.isDeferredOrVoid(found)) {
            return plainsFallback;
        }
        return found != null ? found : plainsFallback;
    }

    /**
     * Map TF climate/continent into vanilla MultiNoise axes (approx).
     * Good enough for TB ParameterPoint nearest-neighbor; not a full vanilla sampler.
     */
    public static Climate.TargetPoint toTarget(ClimateSample sample, float depth) {
        float temp = sample.temperature * 2.0f - 1.0f;
        float humid = sample.moisture * 2.0f - 1.0f;
        // continentNoise: deep ocean ~0 … inland ~1 → MultiNoise continentalness ~-1.2 … 1
        float cont = sample.continentNoise * 2.2f - 1.2f;
        // river channel (riverNoise~0) → slightly lower erosion so river ParameterPoints can match
        float erosion = sample.riverNoise <= 0.05f ? -0.85f : (0.5f - sample.biomeEdgeNoise);
        float weirdness = sample.biomeNoise * 2.0f - 1.0f;
        return Climate.target(temp, humid, cont, erosion, depth, weirdness);
    }

    private static boolean isDeferredOrVoid(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> {
            ResourceLocation loc = key.location();
            return DEFERRED_PLACEHOLDER.equals(loc) || "the_void".equals(loc.getPath());
        }).orElse(false);
    }

    @SuppressWarnings("unchecked")
    private static void buildTrees(RegistryAccess access) throws Exception {
        Registry<Biome> biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        plainsFallback = biomes.getHolderOrThrow(Biomes.PLAINS);

        Class<?> regionsClass = Class.forName("terrablender.api.Regions");
        Class<?> regionTypeClass = Class.forName("terrablender.api.RegionType");
        Object overworld = Enum.valueOf((Class<Enum>) regionTypeClass.asSubclass(Enum.class), "OVERWORLD");

        Method get = null;
        for (Method method : regionsClass.getMethods()) {
            if ("get".equals(method.getName()) && method.getParameterCount() == 1) {
                get = method;
                break;
            }
        }
        if (get == null) {
            throw new IllegalStateException("Regions.get not found");
        }
        Iterable<?> regions = (Iterable<?>) get.invoke(null, overworld);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> surface = new ArrayList<>();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> underground = new ArrayList<>();

        for (Object region : regions) {
            Method addBiomes = region.getClass().getMethod("addBiomes", Registry.class, Consumer.class);
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer = pair -> {
                ResourceKey<Biome> key = pair.getSecond();
                if (DEFERRED_PLACEHOLDER.equals(key.location()) || "the_void".equals(key.location().getPath())) {
                    return;
                }
                Holder<Biome> holder = biomes.getHolder(key).orElse(null);
                if (holder == null || TerraBlenderBiomeAuthority.isDeferredOrVoid(holder)) {
                    return;
                }
                Climate.ParameterPoint point = pair.getFirst();
                surface.add(Pair.of(point, holder));
                float depthMid = (Climate.unquantizeCoord(point.depth().min()) + Climate.unquantizeCoord(point.depth().max())) * 0.5f;
                if (depthMid >= 0.1f && CaveBiomeIds.isUndergroundBiome(key.location())) {
                    underground.add(Pair.of(point, holder));
                }
            };
            addBiomes.invoke(region, biomes, consumer);
        }

        if (surface.isEmpty()) {
            surfaceList = null;
            undergroundList = null;
            TerraForged.LOG.warn("[TerraBlenderBiomeAuthority] No ParameterPoints collected from TB regions");
            return;
        }
        surfaceList = new Climate.ParameterList<>(ImmutableList.copyOf(surface));
        // Never fall back to surface list for underground paint — that paints plains into caves.
        undergroundList = underground.isEmpty() ? null : new Climate.ParameterList<>(ImmutableList.copyOf(underground));
        TerraForged.LOG.info("[TerraBlenderBiomeAuthority] Built trees: surface={} underground={}", surface.size(), underground.size());
    }
}
