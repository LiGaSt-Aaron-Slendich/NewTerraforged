# Claude chat summary — bc573447

**Source:** https://claude.ai/share/bc573447-7f92-46fc-b469-896ea5edd41f  
**Export:** `bc573447-megastructure-chat.json` + `.txt` (80 messages)  
**Note:** This is **not** NewTerraForged — a separate Minecraft mod project (working title **Ruthenium** / megastructure generator).

---

## About what

Procedural generation of **megastructures** (cities, citadels) from **NBT tiles** with a strict naming convention, multi-floor layouts, corridors, and terrain adaptation (cliffs, rivers, bridges).

Claude iteratively asked ~64 clarifying questions; you answered; then Java files were generated one-by-one with confirmation before each next file.

---

## Core systems

### 1. Tile naming (NBT files)

- Separator between fields: `_`
- Multiple values inside one field: `-`
- Example (legacy): `Hall_10x12_East_West_AdditionalExit1:North_3_2_1`
- Fields include: room type, size, shape, entrance/exit sides (8 directions + diagonals), additional exits, style, floor count, tile ID, angle
- `3_2_1` = Style / floorCount (physical floors occupied) / unique tile ID
- `entrance=none` / `exit=none` — avoid if possible, use only if no alternative

### 2. Room types (citadel)

- `room`, `corridor`, `stairroom`, `grandroom`, `throneroom`
- Corridors: NBT tile **or** algorithmic fill between rooms
- Corridor path: first place stairs/throne/entrances, build broken line between them, then branches off the line

### 3. Shapes

- circle, square, hexagon, pentagon, octagon, star, triangle (+ subtypes: pria, rivn, prav, host, tupo)
- **Formula shapes** via `FormulaShapeEvaluator` (expressions, sin/cos in degrees)
- **MultiShapeHandler**: multiple sub-figures per floor
  - `displacement` = surface-level parameter (center offset before building figures)
  - `position` = coordinates or shortcuts (`corner`, `center`, `edge`)
  - `connectToBelow` alignment overrides coordinates
  - Per-subfigure and per-surface rotation

### 4. TerrainAdapter (cities on terrain)

- Full perimeter scan → find max height drop (cliff) and direction
- City center placed flush against cliff
- Cliff threshold ~**30 blocks** (configurable in `TerrainConfig`)
- ≤30: cliff stairs along road gap (Bresenham path, tile-based)
- \>30: **Erebor-style** — part of citadel/circle goes underground into rock (not just cut off)
- River detection: biome first, then water blocks (>5 in row, width >6 → river grate even without river biome)
- Bridge segments from settlement tile list (not room tiles); waterlogging on supported blocks; water level = highest water block under bridge

### 5. Planned / generated Java files (order)

1. `src/main/resources/META-INF/mods.toml`
2. `RutheniumLib.java`
3. `config/ModConfig.java`
4. `templates/logic/TileDecoder.java`
5. `templates/logic/FormulaShapeEvaluator.java`
6. `templates/logic/MultiShapeHandler.java`
7. `templates/logic/TerrainAdapter.java`
8. `templates/logic/CustomMazeFiller.java` ← next at end of chat

---

## Limitations of this export

- Shared Claude chats **hide attachments** (prompt parts, generated code)
- Export is from page accessibility snapshot — **text only**, no full Java sources
- Some long user replies are truncated in the UI (`…`)

For full code, re-export from Claude or copy generated files from that project repo.
