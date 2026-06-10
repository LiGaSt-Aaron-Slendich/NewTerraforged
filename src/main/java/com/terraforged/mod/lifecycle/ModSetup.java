package com.terraforged.mod.lifecycle;

import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.lifecycle.Stage;
import com.terraforged.mod.registry.RegistryManager;
import com.terraforged.mod.worldgen.asset.ClimateType;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import net.minecraft.world.level.biome.Biome;

public class ModSetup
extends Stage {
    public static final ModSetup STAGE = new ModSetup();

    @Override
    protected void doInit() {
        TerraForged.LOG.info("Setting up registries");
        RegistryManager registryManager = CommonAPI.get().getRegistryManager();
        registryManager.create(TerraForged.CAVES, NoiseCave.CODEC);
        registryManager.create(TerraForged.CLIMATES, ClimateType.CODEC);
        registryManager.create(TerraForged.TERRAINS, TerrainNoise.CODEC);
        registryManager.create(TerraForged.TERRAIN_TYPES, TerrainType.DIRECT);
        registryManager.create(TerraForged.VEGETATIONS, VegetationConfig.CODEC);
        registryManager.create(TerraForged.BIOMES, Biome.DIRECT_CODEC, false);
    }
}
