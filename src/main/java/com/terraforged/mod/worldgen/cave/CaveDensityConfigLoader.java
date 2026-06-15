package com.terraforged.mod.worldgen.cave;

import com.electronwill.nightconfig.core.Config;
import java.util.Locale;

public final class CaveDensityConfigLoader {
    private CaveDensityConfigLoader() {
    }

    public static CaveDensitySettings read(Config section) {
        if (section == null || section.isEmpty()) {
            return CaveDensitySettings.DEFAULT;
        }
        Integer xy = CaveDensityConfigLoader.parseOptionalLimit(section, "xy_limit");
        Integer yz = CaveDensityConfigLoader.parseOptionalLimit(section, "yz_limit");
        float percent = CaveDensityConfigLoader.parsePercent(CaveDensityConfigLoader.get(section, "cave_percent", "cave%"), 100.0f);
        return new CaveDensitySettings(xy, yz, percent);
    }

    private static Object get(Config cfg, String... keys) {
        for (String key : keys) {
            Object value = cfg.get(key);
            if (value != null) {
                return value;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            for (String k : cfg.valueMap().keySet()) {
                if (k.equalsIgnoreCase(lower)) {
                    return cfg.get(k);
                }
            }
        }
        return null;
    }

    private static Integer parseOptionalLimit(Config cfg, String key) {
        Object value = CaveDensityConfigLoader.get(cfg, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean)value ? 1 : null;
        }
        if (value instanceof Number) {
            return Math.max(0, ((Number)value).intValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "false".equalsIgnoreCase(text) || "null".equalsIgnoreCase(text) || "none".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(text));
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }

    static float parsePercent(Object value, float def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number) {
            float n = ((Number)value).floatValue();
            if (n <= 1.0f && n >= 0.0f) {
                return n * 100.0f;
            }
            return n;
        }
        String text = String.valueOf(value).trim();
        if (text.endsWith("%")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        if (text.isEmpty()) {
            return def;
        }
        try {
            float n = Float.parseFloat(text);
            if (n <= 1.0f && n >= 0.0f) {
                return n * 100.0f;
            }
            return n;
        }
        catch (NumberFormatException ignored) {
            return def;
        }
    }
}
