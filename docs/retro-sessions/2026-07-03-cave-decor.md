# Retro: cave decor arc (Jun 2026)

**Scope:** `db28035` → Concept_1–20 → `concept_root2_*` → Jun 30 fixes → uncommitted debug/paint work.  
**Next retro:** after bioshroom/mega/giga playtest on **new chunks** with latest JAR.

## Timeline (what happened)

| Phase | Commits | Intent | Outcome |
|-------|---------|--------|---------|
| Baseline | `db28035` | Hybrid decor, mega/giga carve, official TF for fungal | **Good reference** — quart paint + routing worked |
| Hotfix spiral | `Concept_1`–`14` | Trees, sanctizers, split fungal decor, integrity passes | **Mixed** — fixes stacked; some helped trees, some broke decor |
| Revert | `668e2f2` (Concept_15) | Undo Concept_14 split decor | **Better** — leaf bleed / missing features from over-split |
| Rollback | `53bf426` (Concept_20) | Remove ~2.5k lines of restorers/sanitizers | **Much better** — complexity was fighting itself |
| Root2 | `concept_root2_1`–`7` | Paint bands, narrow slits, cover, tree purge | **Better direction** — biome fit + carve/paint alignment |
| Paint restore | `65255f9` | Re-anchor to `db28035` quart paint | **Critical** — drift from baseline caused empty/wrong biomes |
| Stabilize | `8286157`–`53707d1` | Stop purge slicing mushrooms; unify official decor; bioshroom paint | **Improving** — fewer post-decor amputations |
| In flight | uncommitted | `CaveDecoratePaint`, carve paint, debug save/menu/table | **Diagnostics ↑**; gameplay needs new-chunk verify |

## Symptom → likely root (learned)

| Symptom | Often NOT the cause | Likely cause |
|---------|---------------------|--------------|
| Empty bioshroom / “allowed” but nothing | Feature filter / mod not loaded | **No quart paint**, **not on decor grid anchor**, feet not air |
| Sparse mega/giga / missing cover | “Too few features in biome JSON” | **Paint gap** (surfaceBiomeSkip), **sparse anchor grid**, cover pass skipped |
| Dripstone spam | Single decorator bug | **Vanilla dripstone** + weak throttle; same paint/grid family |
| Floating trees | Tree feature alone | **Integrity/sanitizer stack** (removed in Concept_20); carve air band |
| 13% gen hang | One cave type | **Structure + carve interaction** — needs isolated repro |
| “Dead zones” between decor | Biome picker | **Grid step 2–3** — empty between anchors is expected |

## Better / worse

**Better after arc:** unified quart paint as truth; fewer post-decor purges; official decor path consolidated; terrain integrator kept separate; deploy/commit cursor rules; debug separates filter vs feet vs anchor.

**Worse / regressed temporarily:** Concept_14 split decor; sanitizer/restorer pile (Concept_8–19); dual paint paths; debug that implied “will place” from filter only; testing without new chunks.

## Mistakes to avoid

### Agent (AI)
1. Treat **filter allowed** as **placement success**.
2. Add **new pass** (sanitizer/restorer) before proving **paint + anchor + carve** chain.
3. Fix **symptoms in parallel** (dripstone + bioshroom + mega) without one **unified hypothesis** (paint/grid).
4. Ship **half-wired** tools (debug classes without command/network/register).
5. **Overwrite JAR** without `.disabled` backup.
6. Assume **sampler biome** matters for decor (decor uses **quart paint**).
7. **Large diffs** without checkpoint commits → hard rollback.

### Human / process
1. Test **old chunks** after carve/decor changes.
2. Stand on **solid block** when judging floor decor (feet column not air).
3. Bundle many unrelated symptoms in one batch → obscures which fix worked.
4. Skip **save/menu debug** when chat overflow hides the table.

### Both
1. **Drift from `db28035`** paint/decor semantics without explicit diff against baseline.
2. **Revert commits** (`668e2f2`, `53bf426`) are signals — read *why* before re-adding similar code.

## Misreadings (agent counted wrong)

- Bioshroom empty = “features blocked” → actually **decor blocked at feet** + **anchor grid**.
- Empty caves = “decorator not called” → often **paint gap** near surface skip band.
- Concept_14 “more control” → **split routing** broke density and bleed.
- More sanitizers = safer → **Concept_20** proved opposite (deleted 2.5k LOC).

## Working agreement (next sessions)

1. **Hypothesis first:** paint → carve → anchor → filter → place (in that order).
2. **One axis per deploy** when possible; note commit SHA in playtest.
3. **New chunks only** for decor/carve验证.
4. Use `/newtf debug cave save` + **menu**; read **At feet / At anchor** columns.
5. **Periodic retro** (this file pattern): `docs/retro-sessions/YYYY-MM-DD-<topic>.md` + update `.cursor/rules/cave-work-retro.mdc`.
6. Before new decor pass: `git diff db28035 -- <cave files>` sanity check.

## Open risks

- Uncommitted work not in git history.
- Anchor sparsity still feels “empty” even when correct.
- Carver cache **expired** after gen → live debug partial.
- `compileTestJava` fails; use `compileJava` + `reobfJar` for deploy until tests fixed.

## Playtest 2026-07-03 evening (user)

| # | Symptom | Root read from debug/screens |
|---|---------|------------------------------|
| 1+6 | Lake under + snow ground holes | Surface quart leak (`minecraft:river` underground); surface cover skip when not air above; PLAINS fallback in restorer |
| 2 | Plains patches at rivers/lakes | `BiomeSampler` RIVER only when `riverNoise==0`; shores get climate biome |
| 3 | No cover in debug | Fixed: `[Floor cover]` section |
| 4 | Carve "why cut" missing | Post-gen cannot replay carve; added span/edge/paint verdict |
| 5 | Wythers fungal empty, other OK | Paint mismatch region vs quart; mushroom_caves quart under fungal/giga layout |
| 7 | Mycotoxic anchor n/a | Bug: carver==null skipped anchor grid; fixed offline probe |
