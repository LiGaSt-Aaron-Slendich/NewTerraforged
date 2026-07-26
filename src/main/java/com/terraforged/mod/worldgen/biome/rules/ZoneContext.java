package com.terraforged.mod.worldgen.biome.rules;

import com.terraforged.engine.util.pos.PosUtil;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.util.SpiralIterator;
import com.terraforged.mod.worldgen.noise.INoiseGenerator;
import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.mod.worldgen.noise.climate.ClimateSample;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Local placement context for zone flags (near active / dormant volcano, etc.).
 * Probes continent-painted volcano cells within {@link #DEFAULT_PROBE_RADIUS} when needed.
 * Active volcano rings use a noise-ragged radius; dormant rings are circular.
 */
public final class ZoneContext {
    /** Default probe / suggested zone radius for near_*_volcano flags. */
    public static final float DEFAULT_PROBE_RADIUS = 640.0F;

    public final boolean nearActiveVolcano;
    public final boolean nearDormantVolcano;
    public final boolean onActiveVolcano;
    public final boolean onDormantVolcano;
    /** Distance to nearest volcano cell in blocks; 0 when standing on volcano; {@link Float#MAX_VALUE} if none. */
    public final float volcanoDistanceBlocks;
    /**
     * Effective radius scale at this sample (ragged for active, 1.0 for dormant / none).
     * Used when comparing a rule's {@code radius_blocks} to {@link #volcanoDistanceBlocks}.
     */
    public final float edgeScale;

    public ZoneContext(boolean nearActiveVolcano) {
        this(nearActiveVolcano, false, false, false, nearActiveVolcano ? 0.0F : Float.MAX_VALUE, 1.0F);
    }

    public ZoneContext(boolean nearActiveVolcano, float volcanoDistanceBlocks) {
        this(nearActiveVolcano, false, false, false, volcanoDistanceBlocks, 1.0F);
    }

    public ZoneContext(
            boolean nearActiveVolcano,
            boolean nearDormantVolcano,
            boolean onActiveVolcano,
            boolean onDormantVolcano,
            float volcanoDistanceBlocks,
            float edgeScale
    ) {
        this.nearActiveVolcano = nearActiveVolcano;
        this.nearDormantVolcano = nearDormantVolcano;
        this.onActiveVolcano = onActiveVolcano;
        this.onDormantVolcano = onDormantVolcano;
        this.volcanoDistanceBlocks = volcanoDistanceBlocks;
        this.edgeScale = Math.max(0.5F, edgeScale);
    }

    public boolean nearAnyVolcano() {
        return nearActiveVolcano || nearDormantVolcano || onActiveVolcano || onDormantVolcano;
    }

    public boolean onAnyVolcano() {
        return onActiveVolcano || onDormantVolcano;
    }

    public static ZoneContext from(ClimateSample sample) {
        return from(sample, null, 0, 0, DEFAULT_PROBE_RADIUS);
    }

    public static ZoneContext from(ClimateSample sample, INoiseGenerator noise, int blockX, int blockZ) {
        return from(sample, noise, blockX, blockZ, DEFAULT_PROBE_RADIUS);
    }

    public static ZoneContext from(
            ClimateSample sample, INoiseGenerator noise, int blockX, int blockZ, float probeRadius
    ) {
        int seed = VolcanoActivity.resolveSeed(noise);
        float radius = Math.max(64.0F, probeRadius);
        if (sample != null && isVolcanoTerrain(sample.terrainType)) {
            boolean active = VolcanoActivity.isActive(seed, blockX, blockZ);
            float edge = active ? VolcanoActivity.raggedRadiusScale(seed, blockX, blockZ) : 1.0F;
            return new ZoneContext(active, !active, active, !active, 0.0F, edge);
        }
        // Ocean / shelf: volcano rings are irrelevant — never spiral-sample here.
        if (sample != null && sample.continentNoise < 0.55F) {
            return empty();
        }
        if (noise == null || noise.getContinent() == null) {
            return empty();
        }
        VolcanoHit hit = findNearestVolcano(noise, blockX, blockZ, radius);
        if (hit == null) {
            return empty();
        }
        boolean active = VolcanoActivity.isActive(seed, hit.wx, hit.wz);
        float edge = active ? VolcanoActivity.raggedRadiusScale(seed, blockX, blockZ) : 1.0F;
        float limit = radius * edge;
        if (hit.dist > limit) {
            return empty();
        }
        if (active) {
            return new ZoneContext(true, false, false, false, hit.dist, edge);
        }
        return new ZoneContext(false, true, false, false, hit.dist, 1.0F);
    }

    private static ZoneContext empty() {
        return new ZoneContext(false, false, false, false, Float.MAX_VALUE, 1.0F);
    }

    public static boolean isVolcanoTerrain(Terrain t) {
        if (t == null) {
            return false;
        }
        if (t == TerrainType.VOLCANO
                || t == TerrainType.VOLCANO_PIPE
                || t == ModTerrainTypes.ISLAND_VOLCANO
                || t.isVolcano()) {
            return true;
        }
        String n = t.getName();
        return "volcano".equalsIgnoreCase(n)
                || "volcano_pipe".equalsIgnoreCase(n)
                || "island_volcano".equalsIgnoreCase(n);
    }

    public static boolean isPipeTerrain(Terrain t) {
        if (t == null) {
            return false;
        }
        return t == TerrainType.VOLCANO_PIPE || "volcano_pipe".equalsIgnoreCase(t.getName());
    }

    public static boolean isConeTerrain(Terrain t) {
        if (t == null) {
            return false;
        }
        if (isPipeTerrain(t)) {
            return false;
        }
        return isVolcanoTerrain(t);
    }

    /**
     * Coarse spiral search for painted volcano / pipe cells. Step 48 keeps biome sampling affordable.
     */
    private static VolcanoHit findNearestVolcano(INoiseGenerator noise, int x, int z, float maxRadius) {
        int step = 80;
        // Cap cells — uncapped r=640 spiral (~700 samples) × biome quart grid freezes world load.
        int maxCell = Math.min(3, Math.max(1, NoiseUtil.floor(maxRadius / step)));
        SpiralIterator spiral = new SpiralIterator(NoiseUtil.floor(x / (float) step), NoiseUtil.floor(z / (float) step), 0, maxCell);
        NoiseSample sample = new NoiseSample().reset();
        float best = Float.MAX_VALUE;
        int bestWx = 0;
        int bestWz = 0;
        boolean found = false;
        int guard = 0;
        while (spiral.hasNext() && guard++ < 96) {
            long packed = spiral.next();
            int cx = PosUtil.unpackLeft(packed);
            int cz = PosUtil.unpackRight(packed);
            int wx = cx * step + step / 2;
            int wz = cz * step + step / 2;
            float dx = wx - x;
            float dz = wz - z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist > maxRadius || dist >= best) {
                continue;
            }
            float nx = noise.getNoiseCoord(wx);
            float nz = noise.getNoiseCoord(wz);
            noise.getContinent().sampleContinent(VolcanoActivity.resolveSeed(noise), nx, nz, sample);
            if (isVolcanoTerrain(sample.terrainType)) {
                best = dist;
                bestWx = wx;
                bestWz = wz;
                found = true;
                if (best <= step) {
                    return new VolcanoHit(bestWx, bestWz, best);
                }
            }
        }
        return found ? new VolcanoHit(bestWx, bestWz, best) : null;
    }

    private record VolcanoHit(int wx, int wz, float dist) {
    }
}
