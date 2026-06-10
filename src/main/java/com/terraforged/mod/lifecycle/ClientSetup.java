package com.terraforged.mod.lifecycle;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.client.ingame.DimensionEffects;
import com.terraforged.mod.lifecycle.Stage;

public class ClientSetup
extends Stage {
    public static final ClientSetup STAGE = new ClientSetup();

    ClientSetup() {
    }

    @Override
    protected void doInit() {
        TerraForged.LOG.info("Registering custom overworld effects");
        DimensionEffects.register(TerraForged.location("overworld"), new DimensionEffects());
    }
}
