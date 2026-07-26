package com.terraforged.mod.internal.probe;

public enum InspectorOverlayMode {
    BIOMES(0),
    FEATURES(1),
    TERRAIN(2);

    public final byte id;

    InspectorOverlayMode(int id) {
        this.id = (byte)id;
    }

    public static InspectorOverlayMode byId(byte id) {
        for (InspectorOverlayMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return BIOMES;
    }

    public InspectorOverlayMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
