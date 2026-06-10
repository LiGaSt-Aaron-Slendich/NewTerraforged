package com.terraforged.mod.mixin.common;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ChunkMap.class})
public interface ChunkMapAccess {
    @Mutable
    @Accessor(value="generator")
    public void newtf$setGenerator(ChunkGenerator var1);
}
