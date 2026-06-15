package com.terraforged.mod.worldgen.cave;

/**
 * Cave decoration backend — different biomes render best with different passes.
 */
public enum CaveDecoratorKind {
    /** Original TerraForged 0.3.x placeWithBiomeCheck at cave height. */
    OFFICIAL,
    /** TerraLith-style vanilla pass with multiple floor/ceiling origins. */
    VANILLA,
    /** NewTerraForged anchor cover + scatter ({@link CaveBiomeVolumeDecorator}). */
    LEGACY,
    /** Compromise cover + scatter without the heavy noise-cave column loop. */
    COMPROMISE
}
