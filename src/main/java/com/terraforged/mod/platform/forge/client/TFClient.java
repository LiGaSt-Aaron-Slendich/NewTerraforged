package com.terraforged.mod.platform.forge.client;

import com.terraforged.mod.client.Client;
import com.terraforged.mod.client.gui.screen.ConfigScreen;
import com.terraforged.mod.client.screen.ScreenUtil;
import com.terraforged.mod.platform.ClientAPI;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.event.ScreenEvent.InitScreenEvent.Post;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
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
   }

   void onScreenOpen(Post event) {
      if (!(event.getScreen() instanceof CreateWorldScreen createworldscreen)) {
         return;
      }
      String s = (String)ForgeConfig.COMMON.defaultWorldType.get();
      ScreenUtil.enforceDefaultPreset(createworldscreen, s);
      if (ScreenUtil.isPresetEnabled(createworldscreen)) {
         int x = createworldscreen.width / 2 + 5;
         int y = createworldscreen.height - 52;
         event.addListener(new Button(x, y, 150, 20, new TranslatableComponent("newterraforged.gui.config.customize"), b -> ConfigScreen.open(createworldscreen)));
      }
   }

   private static class ForgeClientAPI implements ClientAPI {
      @Override
      public boolean hasPreset() {
         return true;
      }
   }
}
