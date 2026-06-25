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
                case SCATTER -> 2.4f;
                case SMALL -> 2.2f;
                case MEDIUM -> 1.9f;
                case LARGE -> 1.5f;
                default -> base * 2.0f;
            };
        }
        if (biomeId != null && CaveBiomeIds.isFungalCaveBiome(biomeId)) {
            return switch (mass) {
                case SCATTER -> 1.1f;
                case SMALL -> 1.05f;
                case MEDIUM -> 1.0f;
                case LARGE -> 0.95f;
                default -> base;
            };
        }
        if (biomeId != null && (CaveBiomeIds.isScorchingCaveBiome(biomeId) || CaveBiomeIds.isVolcanicCaveBiome(biomeId))) {
            return switch (mass) {
                case SCATTER -> 2.6f;
                case SMALL -> 2.35f;
                case MEDIUM -> 2.0f;
                case LARGE -> 1.65f;
                default -> base * 2.1f;
            };
        }
        if (biomeId != null && (biomeId.getPath().contains("mycotoxic") || biomeId.getPath().contains("prismachasm"))) {
            return switch (mass) {
                case SCATTER -> 1.85f;
                case SMALL -> 1.7f;
                case MEDIUM -> 1.5f;
                case LARGE -> 1.35f;
                default -> base * 1.6f;
            };
        }
        if (biomeId != null && CaveBiomeIds.isFrostfireCaveBiome(biomeId)) {
            return switch (mass) {
                case SCATTER -> 2.5f;
                case SMALL -> 2.3f;
                case MEDIUM -> 2.0f;
                case LARGE -> 1.7f;
                default -> base * 2.2f;
            };
        }
        if (biomeId != null && CaveBiomeIds.isGenericRedstoneTransition(biomeId)) {
            return switch (mass) {
                case SCATTER -> 2.8f;
                case SMALL -> 2.5f;
                case MEDIUM -> 2.2f;
                case LARGE -> 1.9f;
                default -> base * 2.4f;
            };
        }
        return base;
    }
}
