package com.terraforged.mod.worldgen;

/**
 * Code-level master switches for generation subsystems (audit 2026-07-15).
 * Prefer toggling here over deleting paths — keeps rebuild options open.
 * Forge TOML configs still apply unless a gate explicitly overrides them.
 */
public final class GenerationFeatureGates {
    /** When false, {@link com.terraforged.mod.worldgen.cave.CaveDecorationSettings} never selects legacy volume decor. */
    public static boolean legacyCaveDecoratorsEnabled = false;
    /** When false, compromise decorator path is skipped even if cave-biomes.toml enables it. */
    public static boolean compromiseCaveDecoratorsEnabled = false;
    /** When false, vanilla PlacedFeature cave pass is skipped. */
    public static boolean vanillaCavePassEnabled = true;
    /** Master off for synapse/GLOBAL caves (in addition to caves.toml). */
    public static boolean synapseCavesEnabled = true;
    /** Post-decor floating soil/tree crust strip on mega/giga chunks (off — strip after decor caused cut/floating surface features). */
    public static boolean caveFloatingCrustStripEnabled = false;
    /** MEGA/GIGA NoiseCave block carving. */
    public static boolean megaGigaCavesEnabled = true;
    /** Post-carve river/lake void fill ({@link com.terraforged.mod.worldgen.cave.CaveChunkSurfaceRepair}). */
    public static boolean riverVoidFillEnabled = true;
    /** Dry-shore river biome re-paint ({@link com.terraforged.mod.worldgen.cave.RiverShoreBiomeClip}). */
    public static boolean riverShoreBiomeClipEnabled = true;
    /** Full-chunk integrity re-decorate pass ({@link com.terraforged.mod.worldgen.cave.CaveChunkIntegrityPass}). */
    public static boolean chunkIntegrityPassEnabled = false;
    /** Dispatch Terralith/BOP/etc. surface rules via TerraBlender registry when TB is loaded. */
    public static boolean namespacedModSurfaceRulesEnabled = true;
    /** External mod biomes use biome JSON + vanilla placement; TF grid only for newterraforged/terraforged/minecraft. */
    public static boolean useVanillaVegetationOnlyForModBiomes = true;
    /** P4#21: per-quart TB-style feature placement for pre/post decoration stages. */
    public static boolean terraBlenderChunkBiomeDecorEnabled = true;
    /** P4#22: one surface-quart restore pass after shore clip, before features. */
    public static boolean biomeQuartAuthorityEnabled = true;
    /** P4#23: TF vegetation grid only when biome has explicit VegetationConfig in TOML (not NONE). */
    public static boolean tfVegetationGridOptInOnly = true;
    /** P4#20: boost weight for biomes registered via TerraBlender regions. */
    public static boolean terraBlenderRegionBiomeBoostEnabled = true;

    private GenerationFeatureGates() {
    }
}
