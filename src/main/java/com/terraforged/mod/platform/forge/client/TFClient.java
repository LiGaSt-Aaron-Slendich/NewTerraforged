package com.terraforged.mod.platform.forge.client;

import com.terraforged.mod.client.Client;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.screen.ScreenUtil;
import com.terraforged.mod.platform.ClientAPI;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraftforge.client.ForgeWorldPresetScreens;
import net.minecraftforge.client.event.ScreenEvent.InitScreenEvent.Post;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class TFClient {
   public TFClient() {
      ClientAPI.HOLDER.set(new TFClient.ForgeClientAPI());
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientInit);
      MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onScreenOpen);
   }

   void onClientInit(FMLClientSetupEvent event) {
      event.enqueueWork(Client.INSTANCE::init);
      event.enqueueWork(TFPreset::makeDefault);
      event.enqueueWork(this::registerCustomizeEditor);
   }

   /**
    * Hooks the vanilla More World Options {@code Customize} button under World Type
    * (same slot as Flat/Amplified editors).
    */
   private void registerCustomizeEditor() {
      ForgeWorldPreset preset = TFPreset.INSTANCE;
      if (preset == null) {
         return;
      }
      try {
         ForgeWorldPresetScreens.registerPresetEditor(preset, (createWorldScreen, worldGenSettings) -> new ConfigScreen(createWorldScreen));
      } catch (IllegalStateException already) {
         // Client re-init / dual-load — ignore duplicate registration.
      }
   }

   void onScreenOpen(Post event) {
      if (!(event.getScreen() instanceof CreateWorldScreen createworldscreen)) {
         return;
      }
      // Returning from Customize re-inits this screen. Prefer keeping an applied NewTF
      // generator over Forge's defaultWorldType (often still "default"), which would
      // otherwise CycleButton.onPress() back to vanilla and drop all Customize settings.
      String s = (String)ForgeConfig.COMMON.defaultWorldType.get();
      if (s == null || s.isEmpty() || "default".equals(s) || "terraforged".equals(s)) {
         s = "newterraforged";
      }
      ScreenUtil.enforceDefaultPreset(createworldscreen, s);
   }

   private static class ForgeClientAPI implements ClientAPI {
      @Override
      public boolean hasPreset() {
         return true;
      }
   }
}
