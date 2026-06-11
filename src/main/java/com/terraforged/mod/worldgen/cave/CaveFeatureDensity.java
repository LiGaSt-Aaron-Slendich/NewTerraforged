package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import net.minecraft.resources.ResourceLocation;

public final class CaveFeatureDensity {
    private CaveFeatureDensity() {
    }

    public static float multiplierFor(FeatureMass mass) {
        return CaveFeatureDensity.multiplierFor(mass, null);
    }

    public static float multiplierFor(FeatureMass mass, ResourceLocation biomeId) {
        float base = switch (mass) {
            case SCATTER -> 1.0f;
            case SMALL -> 1.0f;
            case MEDIUM -> 0.9f;
            case LARGE -> 1.0f;
            default -> 1.0f;
        };
        if (biomeId != null && CaveBiomeIds.isCrystalCaveBiome(biomeId)) {
            return switch (mass) {
                case SCATTER -> 0.92f;
                case SMALL -> 0.88f;
                case MEDIUM -> 0.82f;
                case LARGE -> 0.55f;
                default -> base;
            };
        }
        if (biomeId != null && CaveBiomeIds.isFungalCaveBiome(biomeId)) {
            return switch (mass) {
                case SCATTER -> 0.38f;
                case SMALL -> 0.32f;
                case MEDIUM -> 0.35f;
                case LARGE -> 0.4f;
                default -> base * 0.35f;
            };
        }
        return base;
    }
}
