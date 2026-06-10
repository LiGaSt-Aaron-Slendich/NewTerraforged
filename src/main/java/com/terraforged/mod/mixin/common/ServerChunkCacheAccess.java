package com.terraforged.mod.mixin.common;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ServerChunkCache.class})
public interface ServerChunkCacheAccess {
    @Accessor(value="chunkMap")
    public ChunkMap newtf$getChunkMap();
}
