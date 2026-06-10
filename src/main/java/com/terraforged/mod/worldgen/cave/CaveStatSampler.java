package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveModifiers;
import com.terraforged.mod.worldgen.cave.CaveNoise;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import com.terraforged.noise.util.NoiseUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class CaveStatSampler {
    private CaveStatSampler() {
    }

    public static float cropGrowthFactor(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel)) {
            return 1.0f;
        }
        ServerLevel server = (ServerLevel)level;
        Generator generator = GeneratorPreset.getGenerator(server);
        if (generator == null) {
            return 1.0f;
        }
        CaveStatVector stats = CaveStatSampler.sample(server, generator, pos.getX(), pos.getY(), pos.getZ());
        if (stats == null) {
            return 1.0f;
        }
        float moistureBoost = 1.0f + stats.moisture() * 0.04f;
        float fertilityBoost = 1.0f + stats.fertility() * 0.05f;
        return NoiseUtil.clamp(moistureBoost * fertilityBoost, 0.25f, 2.5f);
    }

    public static CaveStatVector sample(ServerLevel level, Generator generator, int x, int y, int z) {
        if (!MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, x, y, z)) {
            return null;
        }
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int surfaceY = generator.getOceanFloorHeight(x, z);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        CaveType type = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, x, y, z) && CaveNoise.sample(CaveModifiers.giga(), seed, x, z) > 0.1f ? CaveType.GIGA : CaveType.MEGA;
        int radius = type == CaveType.GIGA ? 400 : 250;
        int cx = Math.floorDiv(x, radius * 2) * radius * 2 + radius;
        int cz = Math.floorDiv(z, radius * 2) * radius * 2 + radius;
        CaveMegaGigaLayout layout = source.getCaveBiomeSampler().getMegaGigaLayout(seed, cx, cz, radius, type, surfaceBiome, y, surfaceY);
        return layout == null ? null : layout.statsAt(x, z);
    }
}
