package com.terraforged.mod.client.screen;

import com.terraforged.mod.client.gui.screen.AppliedCustomizeState;
import com.terraforged.mod.client.gui.screen.egf.EgfFeatureGate;
import com.terraforged.mod.platform.ClientAPI;
import com.terraforged.mod.platform.forge.client.ShipwreckedPreset;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public class ScreenUtil {
   private static final Predicate<String> TF_PRESET = s ->
           s.equals(GeneratorPreset.TRANSLATION_KEY) || s.equals(ShipwreckedPreset.TRANSLATION_KEY);
   private static final Predicate<String> TF_PRESET_VISIBLE = s -> {
      if (s.equals(GeneratorPreset.TRANSLATION_KEY)) {
         return true;
      }
      return s.equals(ShipwreckedPreset.TRANSLATION_KEY) && EgfFeatureGate.shipwreckedWorldTypeAllowed();
   };
   private static final Predicate<String> DEFAULT_PRESET = s -> s.equals("generator.default");
   private static final Predicate<String> SHIPWRECKED_PRESET = s -> s.equals(ShipwreckedPreset.TRANSLATION_KEY);
   /**
    * Keep the World Type button aligned with NewTF while this screen is open.
    * Session cleanup/reset timing is owned by CreateWorldScreen exit points.
    */
   public static void prepareCreateWorldScreen(CreateWorldScreen screen, String name) {
      enforceDefaultPreset(screen, name);
      // If EGF archipelago features are off, leave Shipwrecked even if it was last selected.
      if (!EgfFeatureGate.shipwreckedWorldTypeAllowed() && isShipwreckedWorldType(screen)) {
         cycleAwayFromShipwrecked(screen);
      }
   }

   public static void resetCreateWorldSession(CreateWorldScreen screen) {
      AppliedCustomizeState.clear();
   }

   /**
    * Align the World Type cycle button with {@code name}.
    * <p>
    * Critical: {@link CycleButton#onPress()} rebuilds {@link WorldGenSettings} from the preset
    * factory and would wipe Customize → Done settings. If the overworld is already a NewTF
    * {@link com.terraforged.mod.worldgen.Generator}, we never keep those factory defaults —
    * restore the previous settings after any UI sync. Also re-applies
    * {@link AppliedCustomizeState} when present.
    */
   public static void enforceDefaultPreset(CreateWorldScreen screen, String name) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton == null) {
         return;
      }

      WorldGenSettings before = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
      boolean keepCustomTf = GeneratorPreset.isTerraForgedWorld(before) || AppliedCustomizeState.present();
      // Applied NewTF generator must stay NewTF in the UI — never demote to forge "default".
      // Prefer visible NewTF presets only (Shipwrecked may be EGF-gated).
      Predicate<String> target = keepCustomTf ? TF_PRESET_VISIBLE : createKeyPredicate(name);

      if (!isPresetSelected(cyclebutton, target)) {
         Object start = cyclebutton.getValue();
         while (!isPresetSelected(cyclebutton, target)) {
            cyclebutton.onPress();
            if (cyclebutton.getValue() == start) {
               break;
            }
         }
      }

      if (keepCustomTf) {
         // onPress may have replaced the chunk generator with factory defaults — put Customize back.
         if (GeneratorPreset.isTerraForgedWorld(before)) {
            screen.worldGenSettingsComponent.updateSettings(before);
         } else if (AppliedCustomizeState.present()) {
            reapplyStored(screen);
         }
      }
   }

   private static void reapplyStored(CreateWorldScreen screen) {
      GeneratorSettings gs = AppliedCustomizeState.settings();
      TerrainLevels levels = AppliedCustomizeState.levels();
      if (gs == null || levels == null) {
         return;
      }
      long seed = AppliedCustomizeState.seed();
      if (seed == -1L) {
         try {
            seed = screen.worldGenSettingsComponent.makeSettings(screen.hardCore).seed();
         } catch (Throwable ignored) {
            seed = 0L;
         }
      }
      RegistryAccess access = screen.worldGenSettingsComponent.registryHolder();
      WorldGenSettings current = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
      Generator generator = GeneratorPreset.build(seed, levels, gs, access);
      Registry<LevelStem> dimensions = WorldGenSettings.withOverworld(
              access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
              current.dimensions(),
              generator
      );
      screen.worldGenSettingsComponent.updateSettings(
              new WorldGenSettings(seed, current.generateFeatures(), current.generateBonusChest(), dimensions)
      );
   }

   public static boolean isPresetEnabled(CreateWorldScreen screen) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton == null) {
         return false;
      } else {
         return !ClientAPI.get().hasPreset() ? isPresetSelected(cyclebutton, DEFAULT_PRESET) : isPresetSelected(cyclebutton, TF_PRESET);
      }
   }

   public static boolean isShipwreckedWorldType(CreateWorldScreen screen) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      return cyclebutton != null && isPresetSelected(cyclebutton, SHIPWRECKED_PRESET);
   }

   public static boolean isNewTerraForgedFamily(CreateWorldScreen screen) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      return cyclebutton != null && isPresetSelected(cyclebutton, TF_PRESET);
   }

   private static void cycleAwayFromShipwrecked(CreateWorldScreen screen) {
      CycleButton<?> cyclebutton = getPresetButton(screen);
      if (cyclebutton == null) {
         return;
      }
      WorldGenSettings before = screen.worldGenSettingsComponent.makeSettings(screen.hardCore);
      Object start = cyclebutton.getValue();
      do {
         cyclebutton.onPress();
         if (!isPresetSelected(cyclebutton, SHIPWRECKED_PRESET)) {
            break;
         }
      } while (cyclebutton.getValue() != start);
      // Prefer NewTerraForged main preset when leaving Shipwrecked.
      if (!isPresetSelected(cyclebutton, s -> s.equals(GeneratorPreset.TRANSLATION_KEY))) {
         Object start2 = cyclebutton.getValue();
         while (!isPresetSelected(cyclebutton, s -> s.equals(GeneratorPreset.TRANSLATION_KEY))) {
            cyclebutton.onPress();
            if (cyclebutton.getValue() == start2) {
               break;
            }
         }
      }
      if (GeneratorPreset.isTerraForgedWorld(before) && !isShipwreckedWorldType(screen)) {
         // Keep customized settings if they were already NewTF (non-shipwrecked path).
         screen.worldGenSettingsComponent.updateSettings(before);
      }
   }

   private static Predicate<String> createKeyPredicate(String name) {
      if (name.equals("terraforged") || name.equals("newterraforged") || name.equals("shipwrecked")) {
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
