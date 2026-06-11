package com.terraforged.mod.worldgen.biome.util;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.storage.WeightMap;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import com.terraforged.mod.worldgen.biome.util.SurfaceBiomeConfigLoader;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class BiomeMapManager {
    private static final BiomeType[] TYPES = BiomeType.values();
    private static final BiomeTypeHolder[] HOLDERS = (BiomeTypeHolder[])Stream.of(TYPES).map(BiomeTypeHolder::new).toArray(BiomeTypeHolder[]::new);
    private static BiomeMapManager cached;
    private static int cachedBiomeCount = -1;
    private static int cachedClimateCount = -1;
    private final Registry<Biome> biomes;
    private final Registry<ClimateType> climateTypes;
    private final List<Holder<Biome>> overworldBiomes;
    private final Map<BiomeType, WeightMap<Holder<Biome>>> biomeMap;

    public BiomeMapManager(RegistryAccess access) {
        this.biomes = access.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
        this.climateTypes = access.ownedRegistryOrThrow(TerraForged.CLIMATES.get());
        this.overworldBiomes = BiomeMapManager.getOverworldBiomes(this.biomes, this.climateTypes);
        this.biomeMap = this.buildBiomeMap();
    }

    public static BiomeMapManager getOrCreate(RegistryAccess access) {
        Registry<Biome> biomes = access.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
        Registry<ClimateType> climates = access.ownedRegistryOrThrow(TerraForged.CLIMATES.get());
        int biomeCount = biomes.size();
        int climateCount = climates.size();
        if (cached != null && cachedBiomeCount == biomeCount && cachedClimateCount == climateCount) {
            return cached;
        }
        cached = new BiomeMapManager(access);
        cachedBiomeCount = biomeCount;
        cachedClimateCount = climateCount;
        return cached;
    }

    public Holder<Biome> get(ResourceKey<Biome> key) {
        return this.biomes.getHolderOrThrow(key);
    }

    public Registry<Biome> getBiomes() {
        return this.biomes;
    }

    public List<Holder<Biome>> getOverworldBiomes() {
        return this.overworldBiomes;
    }

    public List<Holder<Biome>> getPossibleBiomeSourceBiomes() {
        ObjectOpenHashSet set = new ObjectOpenHashSet(this.overworldBiomes);
        for (Holder holder : this.biomes.asHolderIdMap()) {
            ResourceLocation loc;
            Optional keyOpt = holder.unwrapKey();
            if (keyOpt.isEmpty() || !CaveBiomeIds.isUndergroundBiome(loc = ((ResourceKey<?>) keyOpt.get()).location()) || CaveBiomeIds.isBlockedCaveBiome(loc)) continue;
            set.add(holder);
        }
        ObjectArrayList list = new ObjectArrayList((ObjectCollection)set);
        list.sort(BiomeUtil.BIOME_SORTER);
        return list;
    }

    public Map<BiomeType, WeightMap<Holder<Biome>>> getBiomeMap() {
        return this.biomeMap;
    }

    private Map<BiomeType, WeightMap<Holder<Biome>>> buildBiomeMap() {
        Map<BiomeType, Object2FloatMap<Holder<Biome>>> map = this.getWeightsMap();
        EnumMap<BiomeType, WeightMap<Holder<Biome>>> result = new EnumMap<BiomeType, WeightMap<Holder<Biome>>>(BiomeType.class);
        for (Map.Entry<BiomeType, Object2FloatMap<Holder<Biome>>> entry : map.entrySet()) {
            Holder[] values = (Holder[])entry.getValue().keySet().toArray(Holder[]::new);
            float[] weights = entry.getValue().values().toFloatArray();
            result.put(entry.getKey(), new WeightMap<>(values, weights));
        }
        return result;
    }

    private Map<BiomeType, Object2FloatMap<Holder<Biome>>> getWeightsMap() {
        HashMap<BiomeType, Object2FloatMap<Holder<Biome>>> map = new HashMap<BiomeType, Object2FloatMap<Holder<Biome>>>();
        ObjectOpenHashSet registered = new ObjectOpenHashSet();
        SurfaceBiomeConfigLoader.ConfigOverlay overlay = SurfaceBiomeConfigLoader.load(this.biomes);
        for (BiomeTypeHolder typeHolder : HOLDERS) {
            ClimateType biomeType = (ClimateType)this.climateTypes.get(typeHolder.name);
            if (biomeType == null) {
                map.put(typeHolder.type(), BiomeMapManager.newMutableWeightMap());
                continue;
            }
            Object2FloatMap<Holder<Biome>> typeMap = BiomeMapManager.getBiomeWeights(biomeType, this.biomes, arg_0 -> ((ObjectOpenHashSet)registered).add(arg_0));
            map.put(typeHolder.type(), typeMap);
        }
        for (Map.Entry entry : overlay.explicit().entrySet()) {
            Object2FloatMap typeMap = map.computeIfAbsent((BiomeType)(entry.getKey()), t -> BiomeMapManager.newMutableWeightMap());
            for (Object2FloatMap.Entry<Holder<Biome>> biomeEntry : ((Object2FloatMap<Holder<Biome>>) entry.getValue()).object2FloatEntrySet()) {
                if (!BiomeUtil.isOverworldSurfaceBiome((Holder<Biome>)((Holder)biomeEntry.getKey()))) continue;
                typeMap.put(((Holder)biomeEntry.getKey()), biomeEntry.getFloatValue());
                registered.add(((Holder)biomeEntry.getKey()));
            }
        }
        int modBiomes = 0;
        for (Holder<Biome> biome : this.overworldBiomes) {
            BiomeType type;
            if (!BiomeUtil.isOverworldSurfaceBiome(biome) || registered.contains(biome) || overlay.configured().contains(biome) || !overlay.autoDetectModBiomes() && BiomeMapManager.isModBiome(biome) || (type = BiomeUtil.getType(biome)) == null) continue;
            float weight = BiomeMapManager.isModBiome(biome) ? overlay.autoModWeight() : 1.0f;
            map.computeIfAbsent(type, t -> new Object2FloatLinkedOpenHashMap()).put(biome, weight);
            if (!BiomeMapManager.isModBiome(biome)) continue;
            ++modBiomes;
        }
        if (modBiomes > 0) {
            TerraForged.LOG.info("[BiomeMapManager] Auto-added {} mod biomes (weight={})", modBiomes, Float.valueOf(overlay.autoModWeight()));
        }
        return map;
    }

    private static Object2FloatMap<Holder<Biome>> getBiomeWeights(ClimateType type, Registry<Biome> biomes, Consumer<Holder<Biome>> registered) {
        Object2FloatMap<Holder<Biome>> map = BiomeMapManager.newMutableWeightMap();
        for (Object2FloatMap.Entry entry : type.getWeights().object2FloatEntrySet()) {
            ResourceKey key = biomes.getResourceKey(((Biome)biomes.getOptional((ResourceLocation)entry.getKey()).orElseThrow())).orElseThrow();
            Holder biome = biomes.getHolderOrThrow(key);
            if (!BiomeUtil.isOverworldSurfaceBiome((Holder<Biome>)biome)) continue;
            map.put(biome, entry.getFloatValue());
            registered.accept((Holder<Biome>)biome);
        }
        return map;
    }

    private static List<Holder<Biome>> getOverworldBiomes(Registry<Biome> biomes, Registry<ClimateType> biomeTypes) {
        List<Holder<Biome>> list = BiomeUtil.getOverworldBiomes(biomes);
        ObjectOpenHashSet added = new ObjectOpenHashSet(list);
        for (ClimateType type : biomeTypes) {
            for (ResourceLocation name : type.getWeights().keySet()) {
                ResourceKey key = ResourceKey.create(Registry.BIOME_REGISTRY, (ResourceLocation)name);
                Holder biome = biomes.getHolderOrThrow(key);
                if (!added.add(biome)) continue;
                list.add((Holder<Biome>)biome);
            }
        }
        list.sort(BiomeUtil.BIOME_SORTER);
        return list;
    }

    private static Object2FloatMap<Holder<Biome>> newMutableWeightMap() {
        return new Object2FloatLinkedOpenHashMap();
    }

    private static boolean isModBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> !"minecraft".equals(key.location().getNamespace())).orElse(false);
    }

    private record BiomeTypeHolder(BiomeType type, ResourceLocation name) {
        public BiomeTypeHolder(BiomeType type) {
            this(type, TerraForged.location(type.name().toLowerCase(Locale.ROOT)));
        }
    }
}
