package com.terraforged.mod.worldgen.cave;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum CaveBiomeCategory {
    PRIMARY,
    TRANSITION,
    SPECIAL,
    COASTAL;

    public static final Codec<CaveBiomeCategory> CODEC;

    public static CaveBiomeCategory fromString(String s) {
        return CaveBiomeCategory.valueOf(s.toUpperCase(Locale.ROOT));
    }

    static {
        CODEC = Codec.STRING.xmap(CaveBiomeCategory::fromString, c -> c.name().toLowerCase(Locale.ROOT));
    }
}
