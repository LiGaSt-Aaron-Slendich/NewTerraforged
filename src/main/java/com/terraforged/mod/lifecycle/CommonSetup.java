package com.terraforged.mod.lifecycle;

import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.compat.TerraBlenderCompat;
import com.terraforged.mod.data.ModBiomes;
import com.terraforged.mod.data.ModCaves;
import com.terraforged.mod.data.ModClimates;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.data.ModVegetations;
import com.terraforged.mod.lifecycle.Stage;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.datapack.DataPackExporter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class CommonSetup
extends Stage {
    public static final CommonSetup STAGE = new CommonSetup();

    CommonSetup() {
    }

    @Override
    protected void doInit() {
        TerraBlenderCompat.init();
        TerraForged.LOG.info("Registering world-gen core codecs");
        Registry.register((Registry)Registry.BIOME_SOURCE, (ResourceLocation)TerraForged.location("climate"), Source.CODEC);
        Registry.register((Registry)Registry.CHUNK_GENERATOR, (ResourceLocation)TerraForged.location("generator"), Generator.CODEC);
        Registry.register((Registry)Registry.CHUNK_GENERATOR, (ResourceLocation)new ResourceLocation("terraforged", "generator"), Generator.CODEC);
        if (!Environment.DATA_GEN) {
            TerraForged.LOG.info("Populating world-gen data registries");
            ModTerrainTypes.register();
            ModTerrains.register();
            ModCaves.register();
            ModVegetations.register();
            ModClimates.register();
            ModBiomes.register();
            DataPackExporter.extractDefaultPack();
        }
    }
}
