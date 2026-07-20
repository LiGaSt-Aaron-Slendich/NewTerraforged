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
import net.minecraft.world.level.levelgen.WorldGenSettings;

public class ScreenUtil {
   private static final Predicate<String> TF_PRESET = s -> s.equals(GeneratorPreset.TRANSLATION_KEY);
   private static final Predicate<String> DEFAULT_PRESET = s -> s.equals("generator.default");

   /**
    * Align the World Type cycle button with {@code name}.
    * <p>
    * Critical: {@link CycleButton#onPress()} rebuilds {@link WorldGenSettings} from the preset
    * factory and would wipe Customize → Done settings. If the overworld is already a NewTF
    * {@link com.terraforged.mod.worldgen.Generator}, we never keep those factory defaults —
    * restore the previous settings after any UI sync.
    */
   public static void enforceDefaultPreset(CreateWorldScreen screen, String name) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton == null) {
         return;
      }

      WorldGenSettings before = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
      boolean keepCustomTf = GeneratorPreset.isTerraForgedWorld(before);
      // Applied NewTF generator must stay NewTF in the UI — never demote to forge "default".
      Predicate<String> target = keepCustomTf ? TF_PRESET : createKeyPredicate(name);

      if (isPresetSelected(cyclebutton, target)) {
         return;
      }

      Object start = cyclebutton.getValue();
      while (!isPresetSelected(cyclebutton, target)) {
         cyclebutton.onPress();
         if (cyclebutton.getValue() == start) {
            break;
         }
      }

      if (keepCustomTf) {
         // onPress replaced the chunk generator with factory defaults — put Customize back.
         screen.worldGenSettingsComponent.updateSettings(before);
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
      if (name.equals("terraforged") || name.equals("newterraforged")) {
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
      Object value = button.getValue();
      if (!(value instanceof WorldPreset worldpreset)) {
         return false;
      }
      return matchesPreset(worldpreset, predicate);
   }

   private static boolean matchesPreset(WorldPreset worldpreset, Predicate<String> predicate) {
      return walkTranslationKeys(worldpreset.description(), predicate);
   }

   private static boolean walkTranslationKeys(Component component, Predicate<String> predicate) {
      if (component instanceof TranslatableComponent tc) {
         if (predicate.test(tc.getKey())) {
            return true;
         }
         for (Object arg : tc.getArgs()) {
            if (arg instanceof Component child && walkTranslationKeys(child, predicate)) {
               return true;
            }
         }
      }
      for (Component sibling : component.getSiblings()) {
         if (walkTranslationKeys(sibling, predicate)) {
            return true;
         }
      }
      return false;
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
