package com.terraforged.mod.lifecycle;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.ModBiomes;
import com.terraforged.mod.data.ModCaves;
import com.terraforged.mod.data.ModClimates;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.data.ModTerrains;
import com.terraforged.mod.data.ModVegetations;
import com.terraforged.mod.lifecycle.Stage;

public class DataGenSetup
extends Stage {
    public static final Stage STAGE = new DataGenSetup();

    @Override
    protected void doInit() {
        TerraForged.LOG.info("Registering data-gen content");
        ModTerrainTypes.register();
        ModTerrains.register();
        ModCaves.register();
        ModVegetations.register();
        ModClimates.register();
        ModBiomes.register();
    }
}
