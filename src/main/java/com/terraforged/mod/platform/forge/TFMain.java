package com.terraforged.mod.platform.forge;

import com.terraforged.mod.Common;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.command.TFCommands;
import com.terraforged.mod.data.ModBiomes;
import com.terraforged.mod.data.ModTags;
import com.terraforged.mod.data.gen.DataGen;
import com.terraforged.mod.platform.CommonAPI;
import com.terraforged.mod.platform.forge.client.TFClient;
import com.terraforged.mod.platform.forge.client.TFPreset;
import com.terraforged.mod.platform.forge.util.ForgeRegistrar;
import com.terraforged.mod.registry.lazy.LazyTag;
import com.terraforged.mod.registry.registrar.NoopRegistrar;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeMatcher;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeTagMatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod("newterraforged")
public class TFMain extends TerraForged {
   public TFMain() {
      super(TFMain::getRootPath);
      TFConfigs.register();
      TFCaveBiomes.register(FMLJavaModLoadingContext.get().getModEventBus());
      CommonAPI.HOLDER.set(new TFMain.ForgeCommonAPI());
      MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onInit);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onGenerateData);
      FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Biome.class, this::onBiomes);
      FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(ForgeWorldPreset.class, this::onPresets);
      this.setRegistrar(Registry.BIOME_REGISTRY, NoopRegistrar.none());
      if (FMLLoader.getDist().isClient()) {
         new TFClient();
      }
   }

   void onInit(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         Common.INSTANCE.init();
         com.terraforged.mod.compat.TerraBlenderCompat.init();
      });
   }

   void onRegisterCommands(RegisterCommandsEvent event) {
      TFCommands.register(event.getDispatcher());
   }

   void onBiomes(Register<Biome> event) {
      ModBiomes.register(new ForgeRegistrar(event.getRegistry()));
   }

   void onPresets(Register<ForgeWorldPreset> event) {
      TerraForged.LOG.debug("Registering world-preset");
      event.getRegistry().register(TFPreset.create());
   }

   void onGenerateData(GatherDataEvent event) {
      Common.INSTANCE.init();
      final Path path = event.getGenerator().getOutputFolder().resolve("resources/default");
      event.getGenerator().addProvider(new DataProvider() {
         public String getName() {
            return "TerraForged";
         }

         public void run(HashCache cache) throws IOException {
            try {
               DataGen.export(path);
            } catch (Throwable throwable) {
               throwable.printStackTrace();
            }

            new Timer().schedule(new TimerTask() {
               @Override
               public void run() {
                  TerraForged.LOG.warn("Forcibly shutting down datagen process");
                  System.exit(0);
               }
            }, 1000L);
         }
      });
   }

   private static Path getRootPath() {
      return ((ModContainer)ModList.get().getModContainerById("newterraforged").orElseThrow()).getModInfo().getOwningFile().getFile().getFilePath();
   }

   private static class ForgeCommonAPI implements CommonAPI {
      public static final LazyTag<Biome> FORGE_OVERWORLD = LazyTag.biome("forge:overworld");

      @Override
      public BiomeMatcher getOverworldMatcher() {
         return new BiomeTagMatcher.Overworld(FORGE_OVERWORLD.get(), ModTags.OVERWORLD.get()) {
            @Override
            public boolean test(Holder<Biome> biome) {
               return super.test(biome) || this.testDictionary(biome);
            }

            @Deprecated
            private boolean testDictionary(Holder<Biome> biome) {
               return BiomeDictionary.hasType((ResourceKey)biome.unwrapKey().orElseThrow(), Type.OVERWORLD);
            }
         };
      }
   }
}
