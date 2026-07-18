package com.terraforged.mod.client.screen;

import com.terraforged.mod.platform.ClientAPI;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class ScreenUtil {
   private static final String WORLD_TYPE = "selectWorld.mapType";
   private static final String DEFAULT_PRESET_KEY = "generator.default";
   private static final Predicate<String> TF_PRESET = s -> s.equals(GeneratorPreset.TRANSLATION_KEY);
   private static final Predicate<String> DEFAULT_PRESET = s -> s.equals("generator.default");

   public static void enforceDefaultPreset(CreateWorldScreen screen, String name) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton != null) {
         Object object = cyclebutton.getValue();
         Predicate<String> predicate = createKeyPredicate(name);

         while (!isPresetSelected(cyclebutton, predicate)) {
            cyclebutton.onPress();
            if (cyclebutton.getValue() == object) {
               return;
            }
         }
      }
   }

   public static boolean isPresetEnabled(CreateWorldScreen screen) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton == null) {
         return false;
      } else {
         return !ClientAPI.get().hasPreset() ? isPresetSelected(cyclebutton, DEFAULT_PRESET) : isPresetSelected(cyclebutton, TF_PRESET);
      }
   }

   private static Predicate<String> createKeyPredicate(String name) {
      if (name.equals("terraforged")) {
         return TF_PRESET;
      } else {
         ResourceLocation resourcelocation = new ResourceLocation(name);
         String s = Util.makeDescriptionId("generator", resourcelocation);
         if (resourcelocation.getNamespace().equals("minecraft")) {
            String s1 = "generator." + resourcelocation.getPath();
            return sx -> sx.equals(s) || sx.equals(s1);
         } else {
            return sx -> sx.equals(s);
         }
      }
   }

   private static boolean isPresetSelected(CycleButton<?> button, Predicate<String> predicate) {
      WorldPreset worldpreset = (WorldPreset)button.getValue();
      return worldpreset.description() instanceof TranslatableComponent translatablecomponent ? predicate.test(translatablecomponent.getKey()) : false;
   }

   private static CycleButton<?> getPresetButton(Screen screen) {
      for (GuiEventListener guieventlistener : screen.children()) {
         if (guieventlistener instanceof CycleButton cyclebutton && isPresetButtonText(cyclebutton.getMessage())) {
            return cyclebutton;
         }
      }

      return null;
   }

   private static boolean isPresetButtonText(Component component) {
      if (component instanceof TranslatableComponent translatablecomponent) {
         if (translatablecomponent.getKey().equals("selectWorld.mapType")) {
            return true;
         }

         for (Object object : translatablecomponent.getArgs()) {
            if (object instanceof Component componentx && isPresetButtonText(componentx)) {
               return true;
            }
         }
      }

      return false;
   }
}
