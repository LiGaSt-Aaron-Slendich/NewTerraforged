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
 * Local placement context for zone flags (near volcano, etc.).
 * Probes continent-painted volcano cells within {@link #DEFAULT_PROBE_RADIUS} when needed.
 */
public final class ZoneContext {
    /** Default probe / suggested zone radius for near_active_volcano. */
    public static final float DEFAULT_PROBE_RADIUS = 640.0F;

    public final boolean nearActiveVolcano;
    /** Distance to nearest volcano cell in blocks; 0 when standing on volcano; {@link Float#MAX_VALUE} if none. */
    public final float volcanoDistanceBlocks;

    public ZoneContext(boolean nearActiveVolcano) {
        this(nearActiveVolcano, nearActiveVolcano ? 0.0F : Float.MAX_VALUE);
    }

    public ZoneContext(boolean nearActiveVolcano, float volcanoDistanceBlocks) {
        this.nearActiveVolcano = nearActiveVolcano;
        this.volcanoDistanceBlocks = volcanoDistanceBlocks;
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
        if (sample != null && isVolcanoTerrain(sample.terrainType)) {
            return new ZoneContext(true, 0.0F);
        }
        float radius = Math.max(64.0F, probeRadius);
        if (noise == null || noise.getContinent() == null) {
            return new ZoneContext(false, Float.MAX_VALUE);
        }
        float dist = findVolcanoDistance(noise, blockX, blockZ, radius);
        if (dist <= radius) {
            return new ZoneContext(true, dist);
        }
        return new ZoneContext(false, Float.MAX_VALUE);
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
    private static float findVolcanoDistance(INoiseGenerator noise, int x, int z, float maxRadius) {
        int step = 48;
        int maxCell = Math.max(1, NoiseUtil.floor(maxRadius / step));
        SpiralIterator spiral = new SpiralIterator(NoiseUtil.floor(x / (float) step), NoiseUtil.floor(z / (float) step), 0, maxCell);
        NoiseSample sample = new NoiseSample().reset();
        float best = Float.MAX_VALUE;
        while (spiral.hasNext()) {
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
            noise.getContinent().sampleContinent(nx, nz, sample);
            if (isVolcanoTerrain(sample.terrainType)) {
                best = dist;
                if (best <= step) {
                    return best;
                }
            }
        }
        return best;
    }
}
