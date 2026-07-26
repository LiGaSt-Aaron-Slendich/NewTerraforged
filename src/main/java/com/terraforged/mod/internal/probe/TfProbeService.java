package com.terraforged.mod.internal.probe;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Stub — full cave debug report port deferred for 1.19. */
public final class TfProbeService {
    private TfProbeService() {}

    public static TfProbeResult probe(ServerPlayer player, BlockPos pos) {
        return new TfProbeResult(pos.getX(), pos.getY(), pos.getZ(), List.of("Cave debug report not ported to 1.19 yet"));
    }
}
