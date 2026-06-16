package com.terraforged.mod.platform.forge;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.terraforged.mod.platform.forge.TFConfigLoader;
import com.terraforged.mod.worldgen.cave.CaveDensityConfigLoader;
import com.terraforged.mod.worldgen.cave.CaveDensitySettings;
import com.terraforged.mod.worldgen.cave.CaveSystemConfig;

public final class TFCaveSystemConfig {
    public static TFCaveSystemConfig INSTANCE;
    public int megaRegionCountMin = 7;
    public int megaRegionCountMax = 10;
    public int megaTransitionPerRegion = 3;
    public double megaScale = 1.0;
    public int gigaRegionCountMin = 7;
    public int gigaRegionCountMax = 10;
    public int gigaTransitionPerRegion = 6;
    public double gigaScale = 2.0;
    public int normalMaxBiomes = 2;
    public int transitionMaxWidth = 15;
    public double islandMaxRadius = 1.5;
    public boolean enableSynapseCaves = true;
    /** Blocks of stone kept below heightmap before cave ceiling. */
    public int surfaceRoofBufferMegaGiga = 26;
    public int surfaceRoofBufferSynapse = 22;
    public CaveDensitySettings caveDensity = CaveDensitySettings.DEFAULT;

    public static void load() {
        CommentedFileConfig cfg = TFConfigLoader.open("NewTerraForged/caves.toml");
        INSTANCE = new TFCaveSystemConfig();
        INSTANCE.read(cfg);
        cfg.close();
    }

    private void read(CommentedFileConfig root) {
        Config mega = TFConfigLoader.section((Config)root, "mega");
        this.megaRegionCountMin = TFConfigLoader.getInt(mega, "region_count_min", this.megaRegionCountMin);
        this.megaRegionCountMax = TFConfigLoader.getInt(mega, "region_count_max", this.megaRegionCountMax);
        this.megaTransitionPerRegion = TFConfigLoader.getInt(mega, "transition_per_region", this.megaTransitionPerRegion);
        this.megaScale = TFConfigLoader.getDouble(mega, "scale", this.megaScale);
        Config giga = TFConfigLoader.section((Config)root, "giga");
        this.gigaRegionCountMin = TFConfigLoader.getInt(giga, "region_count_min", this.gigaRegionCountMin);
        this.gigaRegionCountMax = TFConfigLoader.getInt(giga, "region_count_max", this.gigaRegionCountMax);
        this.gigaTransitionPerRegion = TFConfigLoader.getInt(giga, "transition_per_region", this.gigaTransitionPerRegion);
        this.gigaScale = TFConfigLoader.getDouble(giga, "scale", this.gigaScale);
        Config normal = TFConfigLoader.section((Config)root, "normal");
        this.normalMaxBiomes = TFConfigLoader.getInt(normal, "max_biomes_per_system", this.normalMaxBiomes);
        this.transitionMaxWidth = TFConfigLoader.getInt(normal, "transition_max_width_blocks", this.transitionMaxWidth);
        this.islandMaxRadius = TFConfigLoader.getDouble(normal, "island_max_radius_chunks", this.islandMaxRadius);
        this.enableSynapseCaves = TFConfigLoader.getBool(normal, "enable_synapse_caves", this.enableSynapseCaves);
        Config caves = TFConfigLoader.section((Config)root, "caves");
        this.surfaceRoofBufferMegaGiga = TFConfigLoader.getInt(caves, "surface_roof_buffer_mega_giga", TFConfigLoader.getInt(normal, "surface_roof_buffer_mega_giga", this.surfaceRoofBufferMegaGiga));
        this.surfaceRoofBufferSynapse = TFConfigLoader.getInt(caves, "surface_roof_buffer_synapse", TFConfigLoader.getInt(normal, "surface_roof_buffer_synapse", this.surfaceRoofBufferSynapse));
        this.caveDensity = CaveDensityConfigLoader.read(caves);
    }

    public CaveSystemConfig toSystemConfig() {
        return new CaveSystemConfig(this.megaRegionCountMin, this.megaRegionCountMax, this.megaTransitionPerRegion, (float)this.megaScale, this.gigaRegionCountMin, this.gigaRegionCountMax, this.gigaTransitionPerRegion, (float)this.gigaScale, this.normalMaxBiomes, this.transitionMaxWidth, (float)this.islandMaxRadius);
    }
}
