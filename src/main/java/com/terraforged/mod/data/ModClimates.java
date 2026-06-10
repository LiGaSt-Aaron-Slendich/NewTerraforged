package com.terraforged.mod.data;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.biome.util.BiomeUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public interface ModClimates {
    public static final float RARE = 1.0f;
    public static final float NORMAL = 5.0f;

    public static void register() {
        Registry registry = BuiltinRegistries.BIOME;
        List<Holder<Biome>> biomes = BiomeUtil.getOverworldBiomes((Registry<Biome>)registry);
        for (BiomeType type : BiomeType.values()) {
            TerraForged.register(TerraForged.CLIMATES, type.name().toLowerCase(Locale.ROOT), Factory.create(type, biomes, (Registry<Biome>)registry));
        }
    }

    public static class Factory {
        static ClimateType create(BiomeType type, List<Holder<Biome>> biomes, Registry<Biome> registry) {
            Object2FloatOpenHashMap weights = new Object2FloatOpenHashMap();
            for (Holder<Biome> biome : biomes) {
                BiomeType biomeType = BiomeUtil.getType(biome);
                if (biomeType == null || biomeType != type) continue;
                ResourceKey key = biome.unwrapKey().orElseThrow();
                weights.put(key.location(), Factory.getWeight((ResourceKey<Biome>)key, biome));
            }
            return new ClimateType((Object2FloatMap<ResourceLocation>)weights);
        }

        static float getWeight(ResourceKey<Biome> key, Holder<Biome> biome) {
            if (key == Biomes.ICE_SPIKES) {
                return 1.0f;
            }
            return 5.0f;
        }
    }
}
