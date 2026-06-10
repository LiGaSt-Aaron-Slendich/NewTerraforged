package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.biome.CaveBiomeSampler;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveMegaGigaLayout;
import com.terraforged.mod.worldgen.cave.CaveRegionMap;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.CaveUndergroundJungleDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class CaveJungleStreamDecorator {
    private static final int MAX_LENGTH = 48;
    private static final int STEP = 2;

    private CaveJungleStreamDecorator() {
    }

    public static void carveStream(ChunkAccess chunk, CarverChunk carver, WorldGenLevel region, Generator generator, WorldgenRandom random, int startX, int startY, int startZ, long seed) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        float[] dir = CaveJungleStreamDecorator.resolveFlowDirection(generator, random, startX, startZ, seed);
        int x = startX;
        int z = startZ;
        int y = startY;
        boolean towardThermal = CaveJungleStreamDecorator.hasNearbyThermalRegion(generator, startX, startZ);
        for (int step = 0; step < 48; step += 2) {
            int lx = x & 0xF;
            int lz = z & 0xF;
            if (!CaveUndergroundJungleDecorator.isJungleBiome(carver, chunk, lx, y, lz)) {
                if (!towardThermal || !CaveUndergroundJungleDecorator.isThermalBiome(carver, chunk, lx, y, lz)) break;
                CaveUndergroundJungleDecorator.setWater(region, new BlockPos(x, y, z));
                break;
            }
            int prevY = y;
            y = CaveUndergroundJungleDecorator.findFloorAt(region, x, z, minY, maxY);
            if (y < 0) break;
            CaveUndergroundJungleDecorator.setWater(region, new BlockPos(x, y, z));
            if (prevY > 0 && prevY - y >= 3) {
                for (int drop = prevY - 1; drop > y; --drop) {
                    BlockPos fall = new BlockPos(x, drop, z);
                    if (!CaveUndergroundJungleDecorator.canPlaceWater(region, fall)) continue;
                    CaveUndergroundJungleDecorator.setFallingWater(region, fall);
                }
            }
            if (random.nextFloat() < 0.12f) {
                dir[0] = dir[0] + (random.nextFloat() - 0.5f) * 0.35f;
                dir[1] = dir[1] + (random.nextFloat() - 0.5f) * 0.35f;
                CaveJungleStreamDecorator.normalize(dir);
            }
            if ((x += Math.round(dir[0] * 2.0f)) >> 4 != chunk.getPos().x || (z += Math.round(dir[1] * 2.0f)) >> 4 != chunk.getPos().z) break;
        }
    }

    private static float[] resolveFlowDirection(Generator generator, WorldgenRandom random, int wx, int wz, long seed) {
        float dz;
        float dx;
        float len;
        float tx = 0.0f;
        float tz = 0.0f;
        int count = 0;
        CaveBiomeSampler sampler = generator.getBiomeSource().getCaveBiomeSampler();
        CaveRegionMap map = sampler.getRegionMap((int)generator.getSeed(), wx, wz, CaveType.MEGA);
        if (map == null) {
            map = sampler.getRegionMap((int)generator.getSeed(), wx, wz, CaveType.GIGA);
        }
        if (map != null) {
            for (CaveMegaGigaLayout.GeneratorNode region : map.layout().generators()) {
                if (!CaveBiomeIds.isThermalThemedBiome(region.biome().biome()) && !CaveBiomeIds.isVolcanicCaveBiome(region.biome().biome())) continue;
                tx += region.x();
                tz += region.z();
                ++count;
            }
        }
        if (count > 0 && (len = (float)Math.sqrt((dx = tx / (float)count - (float)wx) * dx + (dz = tz / (float)count - (float)wz) * dz)) > 4.0f) {
            return new float[]{dx / len, dz / len};
        }
        random.setSeed(seed ^ 0x5EEDBEEFL);
        float angle = random.nextFloat() * ((float)Math.PI * 2);
        return new float[]{(float)Math.cos(angle), (float)Math.sin(angle)};
    }

    private static boolean hasNearbyThermalRegion(Generator generator, int wx, int wz) {
        CaveBiomeSampler sampler = generator.getBiomeSource().getCaveBiomeSampler();
        CaveRegionMap map = sampler.getRegionMap((int)generator.getSeed(), wx, wz, CaveType.MEGA);
        if (map == null) {
            map = sampler.getRegionMap((int)generator.getSeed(), wx, wz, CaveType.GIGA);
        }
        if (map == null) {
            return false;
        }
        for (CaveMegaGigaLayout.GeneratorNode region : map.layout().generators()) {
            float dz;
            float dx;
            if (!CaveBiomeIds.isThermalSpringsBiome(region.biome().biome()) && !CaveBiomeIds.isVolcanicCaveBiome(region.biome().biome()) || !((dx = region.x() - (float)wx) * dx + (dz = region.z() - (float)wz) * dz < 48400.0f)) continue;
            return true;
        }
        return false;
    }

    private static void normalize(float[] dir) {
        float len = (float)Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
        if (len < 0.001f) {
            dir[0] = 1.0f;
            dir[1] = 0.0f;
            return;
        }
        dir[0] = dir[0] / len;
        dir[1] = dir[1] / len;
    }
}
