package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveStatVector;

public enum CaveClimateType {
    FROST,
    DRY,
    WET,
    NORMAL;


    public static CaveClimateType classify(CaveStatVector initial) {
        if (initial.temperature() <= -3.0f) {
            return FROST;
        }
        if (initial.moisture() >= 4.0f) {
            return WET;
        }
        if (initial.moisture() <= -3.0f && initial.temperature() >= 2.0f) {
            return DRY;
        }
        return NORMAL;
    }
}
