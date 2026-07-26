package com.terraforged.mod.worldgen.cave;

import java.util.Locale;

/**
 * Regional cave climate. JSON aliases: cold→FROST, humid→WET, hot→HOT, volcanic→VOLCANIC.
 * Near active volcano forces VOLCANIC; near dormant forces HOT.
 */
public enum CaveClimateType {
    FROST,
    HOT,
    VOLCANIC,
    DRY,
    WET,
    NORMAL;

    public static CaveClimateType classify(CaveStatVector initial) {
        if (initial.temperature() <= -3.0f) {
            return FROST;
        }
        if (initial.temperature() >= 6.0f && initial.moisture() <= -1.0f) {
            return VOLCANIC;
        }
        if (initial.temperature() >= 4.0f) {
            return HOT;
        }
        if (initial.moisture() >= 4.0f) {
            return WET;
        }
        if (initial.moisture() <= -3.0f && initial.temperature() >= 2.0f) {
            return DRY;
        }
        return NORMAL;
    }

    /** Parse UI / JSON id (cold, humid, hot, volcanic, frost, wet, dry, normal). */
    public static CaveClimateType fromAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "cold", "frost", "ice", "frozen" -> FROST;
            case "hot", "warm", "heat" -> HOT;
            case "volcanic", "volcano", "magma", "lava" -> VOLCANIC;
            case "dry", "arid", "desert" -> DRY;
            case "humid", "wet", "moist", "lush" -> WET;
            default -> {
                try {
                    yield CaveClimateType.valueOf(s.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield NORMAL;
                }
            }
        };
    }

    /** Stable UI / JSON id (prefer user-facing names). */
    public String alias() {
        return switch (this) {
            case FROST -> "cold";
            case HOT -> "hot";
            case VOLCANIC -> "volcanic";
            case DRY -> "dry";
            case WET -> "humid";
            case NORMAL -> "normal";
        };
    }
}
