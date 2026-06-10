package com.terraforged.mod.mixin.common;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.terraforged.mod.hooks.PrimaryLevelDataMarker;
import com.terraforged.mod.hooks.WorldGeneratorRestorer;
import com.terraforged.mod.worldgen.GeneratorPreset;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PrimaryLevelData.class})
public abstract class MixinPrimaryLevelData
implements PrimaryLevelDataMarker {
    @Shadow
    private WorldGenSettings worldGenSettings;
    @Unique
    private boolean newtf$terraforgedWorld;

    @Override
    public boolean newtf$isTerraforgedWorld() {
        return this.newtf$terraforgedWorld;
    }

    @Override
    public void newtf$setTerraforgedWorld(boolean value) {
        this.newtf$terraforgedWorld = value;
    }

    @Inject(method={"parse"}, at={@At(value="RETURN")})
    private static void newtf$readMarker(Dynamic<?> dynamic, DataFixer fixer, int dataVersion, CompoundTag tag, LevelSettings settings, LevelVersion version, WorldGenSettings worldGenSettings, Lifecycle lifecycle, CallbackInfoReturnable<PrimaryLevelData> cir) {
        PrimaryLevelDataMarker data = (PrimaryLevelDataMarker)cir.getReturnValue();
        if (WorldGeneratorRestorer.readMarkedFromSave(dynamic, tag) || GeneratorPreset.isTerraForgedWorld(worldGenSettings)) {
            data.newtf$setTerraforgedWorld(true);
        }
    }

    @Inject(method={"setTagData"}, at={@At(value="RETURN")})
    private void newtf$writeMarker(RegistryAccess access, CompoundTag serverData, CompoundTag playerData, CallbackInfo ci) {
        if (GeneratorPreset.isTerraForgedWorld(this.worldGenSettings)) {
            this.newtf$terraforgedWorld = true;
        }
        if (this.newtf$terraforgedWorld) {
            serverData.putBoolean("newterraforged:NewTerraForgedWorld", true);
        }
    }
}
