package com.terraforged.mod.mixin.common;

import com.terraforged.mod.hooks.BoatHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Boat.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Boat.class})
public class MixinBoat {
   @Inject(
      method = {"isUnderwater"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onIsUnderwater(CallbackInfoReturnable<Status> cir) {
      Status status = (Status)cir.getReturnValue();
      if (status != null && BoatHook.floatTheBoat((Boat)(Object)this)) {
         cir.setReturnValue(null);
      }
   }
}
