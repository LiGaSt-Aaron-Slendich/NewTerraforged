# Exploration notes — reference worldgen systems

Source zips (do not commit extracts): `../for exploration/`

| Zip | Role | MC target (from metadata) |
|-----|------|---------------------------|
| `TerraForged-0.3.x.zip` | **Primary lineage** — NTFG was rewritten from this | Forge 40+ (1.18.2 loader range; API closer to 1.19 `RandomState` / `StructureManager`) |
| `tectonic-mc1.18.2-1.1.1.zip` | Datapack terrain (density functions) | **1.19–1.20** despite zip name — do not mix into 1.18.2 runtime |
| `TerraBlender-TB-1.18.2-1.x.x.zip` | Region + surface-rule library | 1.18.2 |
| `Terralith-1.18.2-mod.zip` | TB regions + datapack biomes/features | 1.18.2 |
| `DynamicTrees-release-1.18.2.zip` | Tree species / feature cancel / huge mushrooms | 1.18.2 — study for **compat**, not terrain |

Extracted working copies (gitignored locally if present): `../for exploration/_extracted/`

## Documents in this folder

| File | Purpose |
|------|---------|
| [2026-07-16-systems-study.md](2026-07-16-systems-study.md) | How experienced mods structure terrain, biomes, cave decor |
| [ntfg-transfer-map.md](ntfg-transfer-map.md) | What to steal / what to avoid in NewTerraForged |
| [dynamic-trees-compat.md](dynamic-trees-compat.md) | DT API + cave fungal / WWEE / RU compatibility path |
| [2026-07-16-working-state-plan.md](2026-07-16-working-state-plan.md) | Phased plan to working state (river tunnels, TB authority, DT)
| [design-principles.md](design-principles.md) | Short “how to write these systems” checklist |

## Related prior audits

- `docs/audit/2026-07-15-worldgen-research.md`
- `docs/audit/2026-07-15-decoration-systems-audit.md`
- `docs/reference-mods/README.md`
