/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.cell;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainType;

public class Reorder {
    public float value;
    public float erosion;
    public float sediment;
    public float gradient;
    public float moisture = 0.5f;
    public float temperature = 0.5f;
    public float continentEdge;
    public float continentIdentity;
    public float terrainRegionEdge;
    public float terrainRegionIdentity;
    public float biomeEdge = 1.0f;
    public float biomeIdentity;
    public float macroNoise;
    public float riverMask = 1.0f;
    public int continentX;
    public int continentZ;
    public boolean erosionMask = false;
    public Terrain terrain = TerrainType.NONE;
    public BiomeType biomeType = BiomeType.GRASSLAND;
}

