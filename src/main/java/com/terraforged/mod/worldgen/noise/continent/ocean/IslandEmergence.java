package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.worldgen.noise.NoiseSample;
import com.terraforged.noise.util.NoiseUtil;

/**
 * When seafloor relief would break the surface, boost continentNoise into land bands
 * and stamp island terrain so biomes follow tips (not ocean override).
 */
public final class IslandEmergence {
    public static final float CN_FLOOR = 0.58F;

    private IslandEmergence() {
    }

    public static void emerge(NoiseSample sample, float maskedRelief, SeafloorLandscape.Form form) {
        float boost = NoiseUtil.clamp(maskedRelief, 0.0F, 1.0F);
        sample.continentNoise = Math.max(sample.continentNoise, CN_FLOOR + boost * 0.22F);
        sample.baseNoise = Math.max(sample.baseNoise, 0.14F + boost * 0.40F);
        sample.terrainType = switch (form) {
            case MOUNTAINS -> IslandTerrainLabels.mountains();
            case HILLS -> IslandTerrainLabels.hills();
            case FLATS -> IslandTerrainLabels.flats();
        };
    }

    public static void emergeVolcano(NoiseSample sample, DeepVolcano.Result volcano) {
        if (!volcano.hit()) {
            return;
        }
        if (volcano.pipe()) {
            sample.terrainType = IslandTerrainLabels.volcanoPipe();
            sample.continentNoise = Math.max(sample.continentNoise, CN_FLOOR);
            sample.baseNoise = Math.max(sample.baseNoise, 0.20F);
            sample.heightNoise = volcano.heightBoost();
            return;
        }
        sample.terrainType = IslandTerrainLabels.volcano();
        sample.continentNoise = Math.max(sample.continentNoise, CN_FLOOR + volcano.heightBoost() * 0.25F);
        sample.baseNoise = Math.max(sample.baseNoise, 0.12F + volcano.heightBoost() * 0.45F);
    }
}
