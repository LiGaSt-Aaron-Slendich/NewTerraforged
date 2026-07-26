package com.terraforged.mod.platform.forge;

public final class TFConfigPaths {
    public static final String FOLDER = "NewTerraForged";
    public static final String LEGACY_FOLDER = "NewTerraforged";
    public static final String CAVE_CONFIGS = "NewTerraForged/Cave_configs";
    public static final String TERRAIN = "NewTerraForged/Terrain";
    public static final String CRITICAL_OPTIONS = "NewTerraForged/Critical Options";
    public static final String CAVES = "NewTerraForged/Cave_configs/caves.toml";
    public static final String CAVE_BIOMES = "NewTerraForged/Cave_configs/cave-biomes.toml";
    /** Per-biome cave rules: {@code Cave_configs/Biomes/{mod}/{biome}.json} */
    public static final String CAVE_RULES_BIOMES = "NewTerraForged/Cave_configs/Biomes";
    public static final String SURFACE_BIOMES = "NewTerraForged/Terrain/surface-biomes.toml";
    /** @deprecated Replaced by per-biome JSON under {@link #TERRAIN_RULES_BIOMES}. Kept for migration only. */
    @Deprecated
    public static final String BIOME_TERRAIN_INTEGRATION = "NewTerraForged/Terrain/biome-terrain-integration.toml";
    /** Per-biome terrain rules: {@code Terrain_rules/Biomes/{mod}/{biome}.json} */
    public static final String TERRAIN_RULES = "NewTerraForged/Terrain/Terrain_rules";
    public static final String TERRAIN_RULES_BIOMES = "NewTerraForged/Terrain/Terrain_rules/Biomes";
    /** Bundled curated defaults (see BiomeRuleDefaults.ENABLED). */
    public static final String TERRAIN_RULES_DEFAULTS = "NewTerraForged/Terrain/Terrain_rules/Defaults";
    public static final String DECORATOR_ROUTING = "NewTerraForged/Critical Options/Hybrid options/decorator-routing.toml";

    private TFConfigPaths() {
    }
}
