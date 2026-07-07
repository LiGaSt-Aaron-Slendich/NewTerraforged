package com.terraforged.mod.internal.probe;

import com.terraforged.mod.command.CaveDebugCommand;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Read-only probe API — worldgen never imports inspector client code. */
public final class TfProbeService {
    private TfProbeService() {
    }

    public static TfProbeResult probe(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.getLevel();
        Generator generator = GeneratorPreset.getGenerator(level);
        if (generator == null) {
            return new TfProbeResult(pos.getX(), pos.getY(), pos.getZ(), List.of("Not a NewTerraForged world"));
        }
        return new TfProbeResult(pos.getX(), pos.getY(), pos.getZ(), CaveDebugCommand.collectReport(generator, level, pos, CaveDebugCommand.Mode.LOCAL).lines());
    }
}
