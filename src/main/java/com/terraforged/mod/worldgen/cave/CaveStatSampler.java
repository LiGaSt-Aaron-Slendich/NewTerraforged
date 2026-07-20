package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Samples cave climate stats (KEEP). Stats are layout/debug only —
 * they must not scale feature placement or crop growth.
 */
public final class CaveStatSampler {
    private CaveStatSampler() {
    }

    public static float cropGrowthFactor(Level level, BlockPos pos) {
        return 1.0f;
    }

    public static CaveStatVector sample(ServerLevel level, Generator generator, int x, int y, int z) {
        if (generator == null) {
            return null;
        }
        int seed = Seeds.get(generator.getSeed());
        float mega = CaveNoise.sample(CaveModifiers.mega(), seed, x, z);
        float giga = CaveNoise.sample(CaveModifiers.giga(), seed, x, z);
        if (mega <= 0.05f && giga <= 0.05f) {
            return null;
        }
        return new CaveStatVector(NoiseUtil.clamp(mega, 0f, 1f), NoiseUtil.clamp(giga, 0f, 1f), NoiseUtil.clamp((mega + giga) * 0.5f, 0f, 1f));
    }
}
