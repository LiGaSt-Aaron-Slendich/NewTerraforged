package com.terraforged.mod.platform.forge;

import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.gen.TerraForgedDataProvider;
import com.terraforged.mod.lifecycle.CommonSetup;
import com.terraforged.mod.lifecycle.DataGenSetup;
import com.terraforged.mod.lifecycle.Stage;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;

public class TFData
extends Stage {
    public static final TFData STAGE = new TFData();

    @Override
    protected void doInit() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::onGenerateData);
        DeferredRegister register = DeferredRegister.create(Registry.BIOME_REGISTRY, (String)"newterraforged");
        register.register(eventBus);
        DataGenSetup.STAGE.run();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : CommonAPI.get().getRegistryManager().getRegistry(TerraForged.BIOMES)) {
            register.register(entry.getKey().location().getPath(), entry::getValue);
        }
    }

    void onGenerateData(GatherDataEvent event) {
        CommonSetup.STAGE.run();
        Path path = event.getGenerator().getOutputFolder().resolve("resources/default");
        event.getGenerator().addProvider((DataProvider)new TerraForgedDataProvider(path));
    }
}
