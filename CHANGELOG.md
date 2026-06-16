# Changelog

## [0.4.3] — 2026-06-16

### Bug fixes

- Fewer solid pillars and clipped walls in mega/giga caves
- River columns shift downward under water instead of leaving a flat cut ceiling
- Less vertical scarring on hillsides from bad surface clipping
- Fungal and Mycotoxic caves no longer stay empty
- Bioshroom / glowshroom keep proper cover with the hybrid decorator
- Features and geodes appear on the surface much less often
- Too many geodes reduced
- Embur bog removed from the Overworld cave registry
- Dead leaves and dead logs no longer generate in caves
- Floating trees and mushrooms in caves fixed
- Leaves and shrubs no longer bleed into scorching / heat cave biomes
- Surface holes and wrong surface biomes at cave entrances reduced
- Sulfur river no longer generates in mega/giga caves
- Mushroom and stalactite overlap in fungal halls reduced
- Leftover cave fragments after flattening trimmed (default cave density settings)

### Performance improvements

- Hybrid decorator — each biome uses only the decoration path that works for it
- Geode and crystal accent density tuned down in mega caves
- Much faster chunk decoration on large modpacks (official TerraForged path for stone/dripstone/karst biomes)

### Other changes

- Hybrid decoration system — official TerraForged, vanilla pass, legacy scatter, and compromise modes per biome
- More cave biome variety in mega/giga systems
- Default configs updated from playtesting (`caves.toml`, `cave-biomes.toml`)
- `cave-biomes.toml` — documentation for the `[decoration]` section and biome routing

### Known issues (0.4.4)

- Fungal caves: Terralith mushrooms can still be sparse; RU mushrooms may show up at biome edges
- Frostfire caves often look empty
- BYG Brimstone can look sparse
- Some areas under sky islands may still lack decoration
