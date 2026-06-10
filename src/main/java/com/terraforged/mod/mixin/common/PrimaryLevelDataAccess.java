package com.terraforged.mod.mixin.common;

import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={PrimaryLevelData.class})
public interface PrimaryLevelDataAccess {
    @Mutable
    @Accessor(value="worldGenSettings")
    public void newtf$setWorldGenSettings(WorldGenSettings var1);
}
