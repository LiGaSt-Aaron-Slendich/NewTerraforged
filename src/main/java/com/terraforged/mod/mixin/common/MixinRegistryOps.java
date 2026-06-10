package com.terraforged.mod.mixin.common;

import com.mojang.serialization.DynamicOps;
import com.terraforged.mod.hooks.BuiltinHook;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryResourceAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={RegistryOps.class})
public class MixinRegistryOps {
    @Inject(method={"createAndLoad(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/core/RegistryAccess$Writable;Lnet/minecraft/resources/RegistryResourceAccess;)Lnet/minecraft/resources/RegistryOps;"}, at={@At(value="RETURN")})
    private static <T> void onCreateAndLoad(DynamicOps<T> ops, RegistryAccess.Writable writable, RegistryResourceAccess resource, CallbackInfoReturnable<RegistryOps<T>> cir) {
        BuiltinHook.load(writable, (RegistryOps)cir.getReturnValue());
    }
}
