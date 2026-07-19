package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Samples cave climate stats (KEEP). MEGA/GIGA presence via {@link CaveModifiers} noise
 * until MegaCaveStructureFilter / layout sampler hooks are layered on HEAD carver.
 */
public final class CaveStatSampler {
    private CaveStatSampler() {
    }

    public static float cropGrowthFactor(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel)) {
            return 1.0f;
        }
        ServerLevel server = (ServerLevel) level;
        Generator generator = GeneratorPreset.getGenerator(server);
        if (generator == null) {
            return 1.0f;
        }
        CaveStatVector stats = sample(server, generator, pos.getX(), pos.getY(), pos.getZ());
        if (stats == null) {
            return 1.0f;
        }
        float moistureBoost = 1.0f + stats.moisture() * 0.04f;
        float fertilityBoost = 1.0f + stats.fertility() * 0.05f;
        return NoiseUtil.clamp(moistureBoost * fertilityBoost, 0.25f, 2.5f);
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
        // Lightweight placeholder until full MEGA/GIGA layout sampler is wired on TF118 carver.
        return new CaveStatVector(NoiseUtil.clamp(mega, 0f, 1f), NoiseUtil.clamp(giga, 0f, 1f), NoiseUtil.clamp((mega + giga) * 0.5f, 0f, 1f));
    }
}
