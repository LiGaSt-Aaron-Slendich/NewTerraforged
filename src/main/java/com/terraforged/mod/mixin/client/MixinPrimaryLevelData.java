package com.terraforged.mod.mixin.client;

import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.worldgen.GeneratorPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PrimaryLevelData.class})
public class MixinPrimaryLevelData {
   @Final
   @Shadow
   private WorldGenSettings worldGenSettings;

   @Inject(
      method = {"worldGenSettingsLifecycle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onWorldGenSettingsLifecycle(CallbackInfoReturnable<Lifecycle> cir) {
      if (GeneratorPreset.isTerraForgedWorld(this.worldGenSettings)) {
         cir.setReturnValue(Lifecycle.stable());
      }
   }

   @Inject(
      method = {"setTagData"},
      at = {@At("RETURN")}
   )
   private void onSetTagData(RegistryAccess access, CompoundTag data, CompoundTag otherData, CallbackInfo ci) {
      if (GeneratorPreset.isTerraForgedWorld(this.worldGenSettings) && data.contains("forgeLifecycle")) {
         data.putString("forgeLifecycle", "stable");
         data.putBoolean("confirmedExperimentalSettings", true);
      }
   }
}
