package com.terraforged.mod.mixin.client;

import com.terraforged.mod.client.gui.screen.egf.EgfFeatureGate;
import com.terraforged.mod.platform.forge.client.ShipwreckedPreset;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide NTF (DS:Shipwrecked) world type while EGF archipelago features are off. */
@Mixin(WorldPreset.class)
public class MixinWorldPreset {
    @Inject(method = "isVisibleByDefault", at = @At("HEAD"), cancellable = true)
    private static void newtf$gateShipwrecked(WorldPreset preset, CallbackInfoReturnable<Boolean> cir) {
        if (EgfFeatureGate.shipwreckedWorldTypeAllowed()) {
            return;
        }
        if (matchesShipwrecked(preset.description())) {
            cir.setReturnValue(false);
        }
    }

    private static boolean matchesShipwrecked(Component component) {
        if (component instanceof TranslatableComponent tc) {
            if (ShipwreckedPreset.TRANSLATION_KEY.equals(tc.getKey())) {
                return true;
            }
            for (Object arg : tc.getArgs()) {
                if (arg instanceof Component child && matchesShipwrecked(child)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (matchesShipwrecked(sibling)) {
                return true;
            }
        }
        // Forge wraps display name as plain/styled Component from getDisplayName().
        String plain = component.getString();
        return plain != null && plain.toLowerCase().contains("shipwrecked");
    }
}
