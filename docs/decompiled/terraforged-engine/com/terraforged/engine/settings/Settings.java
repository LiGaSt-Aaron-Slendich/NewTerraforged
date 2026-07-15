/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.settings;

import com.terraforged.engine.serialization.annotation.Serializable;
import com.terraforged.engine.settings.ClimateSettings;
import com.terraforged.engine.settings.FilterSettings;
import com.terraforged.engine.settings.RiverSettings;
import com.terraforged.engine.settings.TerrainSettings;
import com.terraforged.engine.settings.WorldSettings;

@Serializable
public class Settings {
    public WorldSettings world = new WorldSettings();
    public ClimateSettings climate = new ClimateSettings();
    public TerrainSettings terrain = new TerrainSettings();
    public RiverSettings rivers = new RiverSettings();
    public FilterSettings filters = new FilterSettings();
}

