package com.terraforged.mod.platform.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.command.CaveDebugSession;
import com.terraforged.mod.command.TFCommands;
import com.terraforged.mod.lifecycle.CommonSetup;
import com.terraforged.mod.platform.forge.TFCaveBiomes;
import com.terraforged.mod.platform.forge.TFClient;
import com.terraforged.mod.platform.forge.TFConfigs;
import com.terraforged.mod.platform.forge.TFData;
import com.terraforged.mod.platform.forge.TFPreset;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod(value="newterraforged")
public class TFMain
extends TerraForged
implements CommonAPI {
    public TFMain() {
        super(TFMain::getRootPath);
        TFConfigs.register();
        TFCaveBiomes.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onInit);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(ForgeWorldPreset.class, this::onPresets);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(CaveDebugSession.class);
        if (Environment.DATA_GEN) {
            TFData.STAGE.run();
        }
        if (FMLLoader.getDist().isClient()) {
            TFClient.STAGE.run();
        }
    }

    void onInit(FMLCommonSetupEvent event) {
        event.enqueueWork(CommonSetup.STAGE::run);
    }

    void onRegisterCommands(RegisterCommandsEvent event) {
        TFCommands.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }

    void onPresets(RegistryEvent.Register<ForgeWorldPreset> event) {
        TerraForged.LOG.info("Registering NewTerraForged world preset");
        event.getRegistry().register(TFPreset.create());
    }

    private static Path getRootPath() {
        return ((ModContainer)ModList.get().getModContainerById("newterraforged").orElseThrow()).getModInfo().getOwningFile().getFile().getFilePath();
    }
}
