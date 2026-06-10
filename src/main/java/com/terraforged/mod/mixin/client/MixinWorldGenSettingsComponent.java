package com.terraforged.mod.mixin.client;

import com.terraforged.mod.hooks.DatapackHook;
import com.terraforged.mod.hooks.WorldGenHook;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={WorldGenSettingsComponent.class})
public abstract class MixinWorldGenSettingsComponent {
    @Shadow
    private CycleButton<WorldPreset> typeButton;
    @Shadow
    private WorldGenSettings settings;

    @Shadow
    public abstract RegistryAccess registryHolder();

    @Inject(method={"makeSettings"}, at={@At(value="RETURN")}, cancellable=true)
    private void onMakeSettings(boolean hardcore, CallbackInfoReturnable<WorldGenSettings> cir) {
        WorldGenSettings updated = WorldGenHook.applyIfSelected(this.typeButton, this.registryHolder(), (WorldGenSettings)cir.getReturnValue());
        if (updated != cir.getReturnValue()) {
            cir.setReturnValue(updated);
        }
    }

    @Inject(method={"updateDataPacks"}, at={@At(value="RETURN")})
    private void onUpdateDataPacks(WorldStem stem, CallbackInfo ci) {
        DatapackHook.reselectPreset(this.typeButton);
        WorldGenSettings updated = WorldGenHook.applyIfSelected(this.typeButton, this.registryHolder(), this.settings);
        if (updated != this.settings) {
            this.settings = updated;
        }
    }
}
