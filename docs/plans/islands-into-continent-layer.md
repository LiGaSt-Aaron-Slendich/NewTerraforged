# Острови в continent-шар + видалення ARCHIPELAGO_*

## Мета

- Архіпелаги/острови живуть у **тому ж** шарі, що материки: `warp + worldOffset` → shape → `continentNoise` → ControlPoints.
- Зміщення материків **зсуває** і острови.
- Висота — звичайний `getInland` / WeightMap (`hills1` тощо), **без** `applyIslandSurface` і без окремих `ARCHIPELAGO_*`.
- Terrain Integrator sub-terrains для відсіву острівних біомів — **не в цій роботі** (follow-up).

## Що видаляємо (типи)

З `ModTerrainTypes` + `ModTerrains`:

| Видалити | Замінити на stock |
|----------|-------------------|
| `ARCHIPELAGO_HILLS` | `TerrainType.HILLS` |
| `ARCHIPELAGO_PLATEAU` | `TerrainType.PLATEAU` |
| `ARCHIPELAGO_MOUNTAINS` | `TerrainType.MOUNTAINS` |
| `SCATTERED_ARCHIPELAGO` | `TerrainType.FLATS` |

Volcanic / coastal лейбли звести до `TerrainType.VOLCANO` / `TerrainType.COAST` (або `HILLS`).  
`LAGUNA` — або лишити, або звести до `SHALLOW_OCEAN`.

Прибрати гілки з `NoiseGenerator`, overlay, preview, locate.

## Як інтегрувати

1. Після `warp + offset` у `ContinentNoise.sampleContinent` — island eval у **тому ж** continent-space (не сирі world blocks без offset).
2. Внесок у `continentNoise` / `baseNoise` (`max`), потім `ContinentPoints.getTerrainType`.
3. Landform stamp = stock `HILLS` / `PLATEAU` / `MOUNTAINS` / `FLATS`; height через звичайний `getInland`.
4. Shipwrecked: материк придушений; острови — той самий шлях, без окремого overlay-фрейму.
5. `IslandScatter` лишається як math; `IslandFeatureOverlay` → contributor або злити; preview sync.
6. Видалити `applyIslandSurface` + island LandForms modules.

Налаштування `WorldSettings.Islands` лишаються. Shelf ~1500 зберегти.

## Порядок (окремі коміти)

1. **Collapse types** — delete ARCHIPELAGO_*, map to stock; build+deploy+commit.
2. **Continent-space bake** — contribution after warp+offset; remove world-space overlay; preview+Shipwrecked; build+deploy+commit.
3. **Height cleanup** — remove applyIslandSurface / island modules; build+deploy+commit.

## Потім (не зараз)

Terrain Integrator: колисні острівні імена як підтерени для фільтра біомів (`BiomeTerrainIntegration`).
