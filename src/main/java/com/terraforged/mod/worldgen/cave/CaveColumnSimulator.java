package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.noise.Module;

public final class CaveColumnSimulator {
    private static final int MEGA_GIGA_ROOF_BUFFER = 26;
    private static final int VERTICAL_SCAN_STEP = 8;

    private CaveColumnSimulator() {
    }

    public record Sample(int floorY, int ceilingY, int midY) {
    }

    public static Sample sampleMegaGiga(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z) {
        return CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, x, z, 3, true);
    }

    public static Sample sampleMegaGigaForCartography(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z) {
        return CaveColumnSimulator.sampleMegaGiga(generator, config, seed, modifier, x, z, 1, false);
    }

    private static Sample sampleMegaGiga(Generator generator, NoiseCave config, int seed, Module modifier, int x, int z, int minCavern, boolean applyOceanFilter) {
        if (config == null || !config.getType().isMegaOrGiga()) {
            return null;
        }
        if (applyOceanFilter && CaveOceanFilter.isBlockedForMegaGiga(generator, config.getType(), x, z)) {
            return null;
        }
        int surface = generator.getOceanFloorHeight(x, z);
        int minY = generator.getMinY();
        float value = CaveNoise.sample(modifier, seed, x, z);
        int cavern = config.getCavernSize(seed, x, z, value);
        if (cavern < minCavern) {
            return null;
        }
        int ceilingCap = surface - MEGA_GIGA_ROOF_BUFFER;
        if (ceilingCap <= minY) {
            return null;
        }
        int nominalY = config.getHeight(seed, x, z);
        int bestBottom = Integer.MAX_VALUE;
        int bestTop = Integer.MIN_VALUE;
        int bestSpan = -1;
        int scanLow = Math.max(minY + 3, nominalY - cavern * 2);
        int scanHigh = Math.min(ceilingCap - 3, nominalY + cavern * 2);
        scanLow = Math.max(minY + 3, Math.min(scanLow, ceilingCap - 3));
        scanHigh = Math.max(scanLow, Math.min(scanHigh, ceilingCap - 3));
        for (int tryY = scanLow; tryY <= scanHigh; tryY += VERTICAL_SCAN_STEP) {
            Sample candidate = CaveColumnSimulator.chamberAtY(minY, ceilingCap, cavern, tryY);
            if (candidate == null) {
                continue;
            }
            int span = candidate.ceilingY() - candidate.floorY();
            if (span <= bestSpan) {
                continue;
            }
            bestSpan = span;
            bestBottom = candidate.floorY();
            bestTop = candidate.ceilingY();
        }
        Sample nominal = CaveColumnSimulator.chamberAtY(minY, ceilingCap, cavern, nominalY);
        if (nominal != null) {
            int span = nominal.ceilingY() - nominal.floorY();
            if (span > bestSpan) {
                bestSpan = span;
                bestBottom = nominal.floorY();
                bestTop = nominal.ceilingY();
            }
        }
        if (bestSpan < 3) {
            return null;
        }
        return new Sample(bestBottom, bestTop, bestBottom + bestTop >> 1);
    }

    private static Sample chamberAtY(int minY, int ceilingCap, int cavern, int y) {
        int maxUp = Math.max(0, ceilingCap - y);
        int maxDown = Math.max(0, y - minY);
        int vertRadius = Math.min(cavern, Math.min(maxUp, maxDown));
        if (vertRadius < 3) {
            return null;
        }
        int top = Math.min(y + vertRadius, ceilingCap);
        int bottom = Math.max(y - vertRadius, minY);
        if (top - bottom < 3) {
            return null;
        }
        return new Sample(bottom, top, bottom + top >> 1);
    }
}
