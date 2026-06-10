package com.terraforged.mod.mixin.common;

import com.terraforged.mod.hooks.WorldGeneratorRestorer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MinecraftServer.class})
public class MixinMinecraftServer {
    @Inject(method={"createLevels"}, at={@At(value="HEAD")})
    private void newtf$patchOverworldGeneratorHead(ChunkProgressListener progressListener, CallbackInfo ci) {
        WorldGeneratorRestorer.patchSettingsBeforeLevels((MinecraftServer)(Object)this);
    }

    @Inject(method={"createLevels"}, at={@At(value="RETURN")})
    private void newtf$ensureOverworldGenerator(ChunkProgressListener progressListener, CallbackInfo ci) {
        WorldGeneratorRestorer.ensureOverworldGenerator((MinecraftServer)(Object)this);
    }
}
