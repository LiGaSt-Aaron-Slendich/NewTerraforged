package com.terraforged.mod.mixin.common;

import com.terraforged.mod.hooks.BoatHook;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Boat.class})
public class MixinBoat {
    @Inject(method={"isUnderwater"}, at={@At(value="RETURN")}, cancellable=true)
    private void onIsUnderwater(CallbackInfoReturnable<Boat.Status> cir) {
        if (cir.getReturnValue() != null && BoatHook.floatTheBoat((Boat)(Object)this)) {
            cir.setReturnValue(null);
        }
    }
}
