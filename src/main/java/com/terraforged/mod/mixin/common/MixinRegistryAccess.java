package com.terraforged.mod.mixin.common;

import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ImmutableRegistryAccess.class})
public class MixinRegistryAccess {
   @Inject(
      method = {"ownedRegistries"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onOwnedRegistries(CallbackInfoReturnable<Stream<RegistryEntry<?>>> cir) {
      cir.setReturnValue(cir.getReturnValue().filter(e -> RegistryAccess.REGISTRIES.containsKey(e.key())));
   }
}
