package com.terraforged.mod;

import com.google.common.base.Suppliers;
import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.Environment;
import com.terraforged.mod.lifecycle.ModSetup;
import com.terraforged.mod.registry.key.RegistryKey;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TerraForged
implements CommonAPI {
    public static final String MODID = "newterraforged";
    public static final String TITLE = "NewTerraforged";
    public static final String DATAPACK_VERSION = "v0.2";
    public static final Logger LOG = LogManager.getLogger("NewTerraforged");
    public static final ResourceLocation WORLD_PRESET = TerraForged.location("normal");
    public static final ResourceLocation DIMENSION_EFFECTS = TerraForged.location("overworld");
    public static final RegistryKey<Biome> BIOMES = TerraForged.registry("minecraft:worldgen/biome");
    public static final RegistryKey<ClimateType> CLIMATES = TerraForged.registry("minecraft:worldgen/climate");
    public static final RegistryKey<NoiseCave> CAVES = TerraForged.registry("minecraft:worldgen/cave");
    public static final RegistryKey<TerrainNoise> TERRAINS = TerraForged.registry("minecraft:worldgen/terrain_noise");
    public static final RegistryKey<TerrainType> TERRAIN_TYPES = TerraForged.registry("minecraft:worldgen/terrain_type");
    public static final RegistryKey<VegetationConfig> VEGETATIONS = TerraForged.registry("minecraft:worldgen/vegetation");
    private final Supplier<Path> path;

    protected TerraForged(Supplier<Path> path) {
        this.path = Suppliers.memoize(path::get);
        Environment.log();
        CommonAPI.HOLDER.set(this);
        ModSetup.STAGE.run();
    }

    @Override
    public final Path getContainer() {
        return this.path.get();
    }

    public static ResourceLocation location(String name) {
        if (name.contains(":")) {
            return new ResourceLocation(name);
        }
        return new ResourceLocation(MODID, name);
    }

    public static <T> RegistryKey<T> registry(String name) {
        return new RegistryKey(TerraForged.location(name));
    }

    public static <T> void register(RegistryKey<T> key, String name, T t) {
        ResourceKey entryKey = ResourceKey.create((key.get()), (ResourceLocation)TerraForged.location(name));
        CommonAPI.get().getRegistryManager().register(key, entryKey, t);
    }
}
