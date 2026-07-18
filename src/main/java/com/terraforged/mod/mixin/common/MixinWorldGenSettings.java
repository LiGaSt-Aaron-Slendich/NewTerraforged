package com.terraforged.mod.mixin.common;

import com.terraforged.mod.hooks.GeneratorSeedHook;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({WorldGenSettings.class})
public class MixinWorldGenSettings {
   @ModifyVariable(
      method = {"<init>(JZZLnet/minecraft/core/Registry;Ljava/util/Optional;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private static Registry<LevelStem> onInit(Registry<LevelStem> dimensions, long seed) {
      return GeneratorSeedHook.reseed(seed, dimensions);
   }
}
