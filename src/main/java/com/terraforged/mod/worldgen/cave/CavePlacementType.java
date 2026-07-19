package com.terraforged.mod.worldgen.cave;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum CavePlacementType {
    FULL_REGION,
    CEILING_PATCH,
    ISLAND_PATCH;

    public static final Codec<CavePlacementType> CODEC;

    public static CavePlacementType fromString(String s) {
        return CavePlacementType.valueOf(s.toUpperCase(Locale.ROOT));
    }

    static {
        CODEC = Codec.STRING.xmap(CavePlacementType::fromString, t -> t.name().toLowerCase(Locale.ROOT));
    }
}
