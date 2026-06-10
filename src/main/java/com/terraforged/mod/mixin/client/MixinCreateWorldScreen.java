package com.terraforged.mod.mixin.client;

import com.terraforged.mod.hooks.DatapackHook;
import com.terraforged.mod.hooks.WorldGenHook;
import com.terraforged.mod.mixin.client.WorldGenSettingsComponentAccess;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CreateWorldScreen.class})
public abstract class MixinCreateWorldScreen {
    @Shadow
    public WorldGenSettingsComponent worldGenSettingsComponent;

    @Shadow
    protected abstract Path getTempDataPackDir();

    @Inject(method={"init"}, at={@At(value="RETURN")})
    private void onInit(CallbackInfo ci) {
        DatapackHook.selectPreset(this);
    }

    @Inject(method={"tryApplyNewDataPacks"}, at={@At(value="HEAD")})
    private void onTryApplyNewDataPacks(PackRepository repository, CallbackInfo ci) {
        DatapackHook.injectDatapack(repository, this.getTempDataPackDir());
    }

    @ModifyVariable(method={"onCreate"}, at=@At(value="INVOKE_ASSIGN", target="Lnet/minecraft/client/gui/screens/worldselection/WorldGenSettingsComponent;makeSettings(Z)Lnet/minecraft/world/level/levelgen/WorldGenSettings;"), ordinal=0)
    private WorldGenSettings newtf$forceGenerator(WorldGenSettings settings) {
        WorldGenSettingsComponentAccess access = (WorldGenSettingsComponentAccess)this.worldGenSettingsComponent;
        DatapackHook.reselectPreset(access.newtf$getTypeButton());
        return WorldGenHook.applyIfSelected(access.newtf$getTypeButton(), this.worldGenSettingsComponent.registryHolder(), settings);
    }

    @Inject(method={"removed"}, at={@At(value="HEAD")})
    private void onRemoved(CallbackInfo ci) {
        DatapackHook.clearNewTerraForgedIntent();
    }
}
