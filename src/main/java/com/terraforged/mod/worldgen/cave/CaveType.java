package com.terraforged.mod.worldgen.cave;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum CaveType {
    GLOBAL("global"),
    UNIQUE("unique"),
    MEGA("mega"),
    GIGA("giga");

    public static final Codec<CaveType> CODEC;
    final String name;

    private CaveType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isMegaOrGiga() {
        return this == MEGA || this == GIGA;
    }

    public boolean isGiga() {
        return this == GIGA;
    }

    public static CaveType forName(String name) {
        name = name.toUpperCase(Locale.ROOT);
        return CaveType.valueOf(name);
    }

    static {
        CODEC = Codec.STRING.xmap(CaveType::forName, CaveType::getName);
    }
}
