package com.terraforged.mod.platform.forge;

import com.terraforged.mod.lifecycle.ClientSetup;
import com.terraforged.mod.lifecycle.Stage;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class TFClient
extends Stage {
    public static final TFClient STAGE = new TFClient();

    @Override
    protected void doInit() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientInit);
    }

    void onClientInit(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup.STAGE::run);
    }
}
