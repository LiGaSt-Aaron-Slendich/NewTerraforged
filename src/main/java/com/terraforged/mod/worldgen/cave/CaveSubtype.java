package com.terraforged.mod.worldgen.cave;

import java.util.Locale;

public enum CaveSubtype {
    ANY("any"),
    COASTAL("coastal"),
    TUNNEL("tunnel");

    private final String name;

    private CaveSubtype(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isCoastal() {
        return this == COASTAL;
    }

    public boolean isTunnel() {
        return this == TUNNEL;
    }

    public static CaveSubtype forName(String raw) {
        String name;
        return switch (name = raw.toLowerCase(Locale.ROOT).trim()) {
            case "coastal", "coast", "grotto", "shore" -> COASTAL;
            case "tunnel", "ogpm", "ogpm_t", "through", "karst" -> TUNNEL;
            case "any", "all", "*", "" -> ANY;
            default -> throw new IllegalArgumentException("Unknown cave subtype: " + raw);
        };
    }
}
