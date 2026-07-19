# NewTerraForged portable layer (vs TerraForged 1.18.2)

Canonical inventory of **our** systems to hang on stock TF sources (`1.18`, later `1.19` / `1.16`).

| Ref | Value |
|-----|--------|
| Base branch | `from-tf-118-decompile` (stock TF 1.18 `mod/`) |
| Layer source | `main` @ `a26e961` (~213 Java files exclusive to NewTF) |
| Stock cave on base | 7 files: `CarverChunk`, `CarverUtil`, `CaveType`, `NoiseCaveCarver`, `NoiseCaveDecorator`, `NoiseCaveGenerator`, `UniqueCaveDistributor` |
| Do not re-layer | DROP table below (void-fill, integrity, tunnel-river, hybrid/legacy, mass budgets) |

Related audits: `docs/exploration/ntfg-transfer-map.md`, `docs/audit/appendix-ntfg-decoration-ecosystem.md`.

---

## Identity

| Item | NewTF (`main`) | Stock TF (decompile base) |
|------|----------------|----------------------------|
| Primary `modId` | `newterraforged` | `terraforged` |
| Legacy `modId` entry | `terraforged` (compat) | — |
| Mixins JSON | `newterraforged.mixins.json` | `terraforged.mixins.json` |
| Lang | `assets/newterraforged/lang/` | `assets/terraforged/lang/` |
| Config dir | `config/NewTerraForged/` (+ legacy `NewTerraforged`) | `config/terraforged/` |
| Defaultconfigs | `defaultconfigs/NewTerraForged/{Cave_configs,Terrain}/*.toml` | (TF pack under `default/`) |
| World / generator | `newterraforged` preset / NBT | `terraforged:generator` |
| Debug commands | `/newtf`, `/newtf debug cave`, `/newtf ix` | stock TF locate only |

Port rule: product identity stays **NewTerraForged**; packages may remain `com.terraforged.*` for API proximity to upstream TF.

---

## KEEP — re-layer onto every TF base

### Config / gates / Forge platform

| Class / path | Role |
|--------------|------|
| `GenerationFeatureGates` | Feature toggles (DROP gates stay false / absent) |
| `TFConfigs`, `TFConfigLoader`, `TFConfigPaths`, `TFConfigWarnings`, `TFConfigClientNotifier` | Forge config bootstrap |
| `TFCaveBiomeConfig`, `TFCaveSystemConfig`, `TFCaveBiomes` | Cave config |
| `TFSurfaceBiomeConfig`, `TFBiomeTerrainIntegrationConfig` | Surface / integrator config |
| `defaultconfigs/NewTerraForged/**` | Shipped defaults |

### Terrain integrator + surface climate

| Class | Role |
|-------|------|
| `BiomeTerrainIntegration` | Terrain↔biome integration (rewrite later; keep API) |
| `SurfaceBiomeConfigLoader` | TOML load for surface biomes |
| `SurfaceBiomeClimate` | Terrain→climate bias |

Stock `TerrainBlender` is **upstream TF**, not “ours”.

### Cave climate, stats, regions, MEGA/GIGA

| Class | Role |
|-------|------|
| `CaveClimateType`, `CaveTemperatureCalculator` | Cave temperature model |
| `CaveStatVector`, `CaveStatSampler`, `CaveStatInitializer` | Stat vectors |
| `CaveBiomeStats`, `CaveBiomeStatDefaults`, `CaveBiomeClimateAffinity` | Biome↔stat affinity |
| `CaveBiomeRegistry`, `CaveBiomeRegistryLoader`, `CaveBiomeAutoRegistry`, `CaveBiomeEntry`, `CaveBiomeIds`, `CaveBiomeCategory` | Registry |
| `CaveBiomeDecoratorRouter`, `CaveBiomeFeatureRunner`, `CaveBiomeFeaturePresets`, `CaveBiomeVolumeDecorator`, `CaveBiomeVanillaPass`, `CaveBiomeColumnUnifier`, `CaveBiomeVerticalFit` | Official-path decor invoke |
| `CaveMegaGigaLayout`, `MegaGigaChunkCache`, `MegaGigaZoneProbe` | MEGA/GIGA layout |
| `CaveRegionMap`, `CaveSystemGrid`, `CaveLayoutRegionGrid` | Region distribution |
| `TerraForgedOfficialCaveDecorator` | TF-0.3-style feature place |
| `ModCaveBiomeFactories` | Data factories |

Extend stock `CaveType` / `NoiseCaveGenerator` carefully: **keep TF118 river mask** `1 - noise * river`; mega must respect river. Do **not** copy NewTF carver regressions from `main`.

### Debug / probe

| Class | Role |
|-------|------|
| `CaveDebugCommand`, `CaveDebugSession`, `CaveDebugScreen` | `/newtf debug cave` |
| `CaveDebugInfo`, `CaveDebugMaps`, `CaveDebugReport` | Reports |
| `CaveDebugNetwork`, `ProbeNetwork` (Forge) | Network |
| `internal/probe/**`, `InspectorCommands` | Inspector overlay |

### Soft compat (optional mods)

| Class | Role |
|-------|------|
| `TerraBlenderCompat`, `TerraBlenderBiomeAuthority`, `TerraBlenderRegionBridge`, `TerraBlenderSurfaceRules` | TB bridges — **must** no-op if TB absent |
| `DynamicTreesCompat` | DT — **must** no-op if DT absent |

### Modes / naming (preserve)

- Cave types: `GLOBAL`, `UNIQUE`, `MEGA`, `GIGA`
- Decor modes: prefer `official`; do not wire `legacy` / `compromise` / `hybrid`
- Stat roles: `CaveGeneratorKind` HEAT / COLD / MOISTURE / FERTILITY (when ported)

### Also KEEP

- `Regenerator` (chunk re-gen; present on stock tree too — keep NewTF command wiring if any)

---

## DROP — never port to TF 1.18 / 1.19 / 1.16

| Class | Why |
|-------|-----|
| `CaveChunkSurfaceRepair`, `RiverVoidFillAccess`, `RiverVoidFillContext`, `RiverVoidStrataFill` | River void-fill plaster |
| `CaveTunnelRiverDecorator` | Extra tunnels under rivers |
| `CaveChunkIntegrityPass`, `LiGaStIntegrityScan` | Integrity re-decorate stack |
| `CaveDecorationSanitizer` | Sanitizer stack |
| `CaveFloatingCrustStrip` | Post-carve plaster |
| `FeatureDensity`, `FeatureDensityBudget`, `FeatureMass`, `FeatureMassClassifier`, `CaveFeatureDensity` | Density/mass caps |
| `BiomeQuartDecorator`, `BiomeQuartAuthority` | Quart paint grid |
| `CaveBiomeCompromiseDecorator`, `CaveHybridBiomeDecorator` | Dead / gated backends |

“Rollback tunnel fixes” on the decompile base = **do not re-add these from `main`**.

---

## REVIEW — port only with explicit need

~120 main-only files. Packages:

| Package | Notes |
|---------|--------|
| `worldgen.cave` (remainder) | Entrances, grotto, filters, `CaveCarvingGate`, `CarverColumnCache`, `CaveModifiers`, `CaveNoise`, river proximity, shore clip, restorers, density settings, corruption checkers — shrink before port |
| `mixin.*` | NewTF mixins — audit each for standalone boot |
| `lifecycle`, `hooks`, `platform.forge` (`TFMain`, `TFClient`, `TFPreset`, …) | Product bootstrap — needed for identity |
| `util.storage`, `data.codec`, `registry` | Support |
| Borderline near-DROP | `CaveChunkOrderRestorer`, `CaveDensityBudget*`, `CaveChunkCorruption*`, `CaveSurfaceBiomeRestorer`, `RiverShoreBiomeClip`, `WorldGeneratorRestorer` |

---

## Port notes (API touchpoints)

| Area | Upstream TF touch | Layer responsibility |
|------|-------------------|----------------------|
| `ChunkGenerator` / noise chunk | Stock TF generator | Soft hooks only; no WWOO noise replace |
| Cave carve | `NoiseCaveGenerator`, `CarverChunk`, `NoiseCaveCarver` | Stats/regions/MEGA on top; preserve river mask |
| Cave decor | `NoiseCaveDecorator` | Prefer `TerraForgedOfficialCaveDecorator` + biome registry |
| Biome / climate | `ClimateNoise`, biome source | `SurfaceBiomeClimate`, `BiomeTerrainIntegration` |
| Forge-only | — | Config, network, preset, commands |
| Foreign mods | — | Soft `ModList.isLoaded` guards; WWOO gate when TF generator active |

### Standalone boot checklist

1. JAR alone + Forge boots to title.
2. Create world with NewTF/TF generator — no hang / error spam.
3. No hard dep on TerraBlender, Terralith, WWOO, DynamicTrees, BOP, BYG, RU.
4. `mods.toml`: mandatory Forge only; optional entries `mandatory=false`.

### WWOO gate checklist

1. WWOO confirmed cause of river shafts with TF generator in TerraforgedTest.
2. With WWOO disabled, shafts gone.
3. Layer must detect WWOO and, when overworld uses TF/`newterraforged` generator, suppress WWOO noise/carver injection (compat class + docs).
4. Never “fix” WWOO shafts with void-fill.

**Implementation (this branch):** `com.terraforged.mod.compat.WwooCompat` — `ModList.isLoaded("wwoo_forge")`; on common setup / generator-active logs WARN recommending WWOO be disabled with NewTF/TF. Soft warn only (no hard dependency).

---

## Re-layer order (1.18)

1. Identity (`newterraforged` modId, lang, mixins, preset)
2. Config + gates (DROP features absent / false)
3. Debug
4. Cave stats / regions / MEGA-GIGA (carver stays TF118)
5. Climate + `BiomeTerrainIntegration`
6. Soft TB/DT + WWOO gate
7. Smoke: TF-only world, then TF+WWOO A/B

## Later bases

Same KEEP/DROP tables apply when hanging the layer on TerraForged **1.19** and **1.16** sources; only adapters (mappings, registry, generator hooks) change.

---

## Status on from-tf-118-decompile

Branch HEAD after identity + config + climate + WWOO warn + portable `/newtf debug cave`, plus mega/biome/cartography layer hardening.

### Layered (compiled / wired)

| Area | What's on the branch |
|------|----------------------|
| Identity | `newterraforged` modId, mixins, lang, preset, config paths |
| Gates / Forge config | `GenerationFeatureGates`, `TFConfigs` / cave / surface / terrain-integrator TOMLs |
| Soft compat | TerraBlender no-op-if-absent; `WwooCompat` WARN when WWOO loaded with TF/NewTF generator |
| Climate + integrator | `SurfaceBiomeClimate`, `BiomeTerrainIntegration` (+ config) |
| Chunk fill / biome null | `ChunkUtil.fillChunk` + `WeightMap` / biome null guards hardened against render holes |
| Cave stats / MEGA-GIGA types | `CaveType` MEGA/GIGA; `CaveMegaGigaLayout`, registry, stats, region helpers |
| `CaveBiomeSampler` + registry | Mega/region API wired; Source registry `WeightMap` for UNIQUE/GLOBAL/synapse biomes |
| Footprint carve | `CarverColumnCache`, `MegaGigaZoneProbe`, `MegaGigaChunkCache` compiled; `CarverChunk` MEGA biome layout for footprint activation |
| Carver | Stock TF118 `CarverChunk` / river mask **`1 - mask * river`** preserved; debug probes (`debugMaskNoise` / `debugRiverNoise`) |
| Cartography | `CaveCartography` + `CaveColumnSimulator` + `CaveCartographyBounds` restored; `executeMap` uses `CaveCartography` |
| Debug (portable) | `CaveDebugCommand` / `Session` / `Screen` / `Network` / `Info` / `Maps` / `Report` compiled + registered (`/newtf debug cave`, save, menu, live session, stat maps) |

### Deferred (still excluded or soft-stubbed)

| Area | Why deferred |
|------|----------------|
| Full decor pipeline (`CaveFeature*`, `CaveBiomeFeature*`, official decorator, …) | Phase4 gradle excludes; needs carver/decor hooks — feature table / feature map layers stubbed |
| `CaveLocator`, `CaveOceanFilter`, entrances / `CaveSiteTags` / probe package | Excluded KEEP/REVIEW until further carver/decor hooks |
| `CarveDecisionDiagnostics` | Soft-deferred in `/newtf debug cave carve` |
| DROP stack | Void-fill, integrity, tunnel-river, quart paint, hybrid/legacy, mass budgets — **never** |

