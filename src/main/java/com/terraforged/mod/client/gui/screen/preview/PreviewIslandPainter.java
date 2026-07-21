package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.TerrainType;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Paints NewTF island / archipelago / volcanic-island freckles onto the engine preview tile.
 * Preview TileGenerator does not run {@code IslandFeatureOverlay}; this fills that gap.
 */
public final class PreviewIslandPainter {
    private static final int ARCH_CELL = 3200;
    private static final int VOLC_CELL = 4200;

    private PreviewIslandPainter() {
    }

    public static void apply(Tile tile, Settings settings, int seed, int centerX, int centerZ, int zoom) {
        if (tile == null || settings == null || settings.world == null) {
            return;
        }
        WorldSettings.Islands islands = settings.world.islands != null ? settings.world.islands : new WorldSettings.Islands();
        float coastal = NoiseUtil.clamp(islands.coastalIslandsChance, 0.0F, 1.0F);
        float volcanic = NoiseUtil.clamp(islands.volcanicIslandsChance, 0.0F, 1.0F);
        boolean archOn = islands.scatteredArchipelago;
        float archChance = NoiseUtil.clamp(islands.scatteredArchipelagoChance, 0.0F, 1.0F);
        Levels levels = new Levels(settings.world);
        float water = levels.water;
        int size = tile.getBlockSize().size;
        int half = size / 2;

        tile.iterate((cell, lx, lz) -> {
            int worldX = centerX + (lx - half) * zoom;
            int worldZ = centerZ + (lz - half) * zoom;
            boolean ocean = cell.continentEdge <= settings.world.controlPoints.shallowOcean
                    || cell.value <= water + 0.002F
                    || cell.terrain != null && (cell.terrain.isDeepOcean() || cell.terrain.isShallowOcean() || cell.terrain.isSubmerged());

            if (ocean) {
                if (archOn && archChance > 0.0F) {
                    paintArchipelago(cell, worldX, worldZ, seed, archChance, water);
                }
                if (cell.continentEdge <= settings.world.controlPoints.shallowOcean && volcanic > 0.0F) {
                    paintOceanVolcano(cell, worldX, worldZ, seed, volcanic, water);
                }
                return;
            }

            // Near-coast freckles only (do not repaint inland mainland volcanoes).
            float edge = cell.continentEdge;
            boolean nearCoast = edge > settings.world.controlPoints.beach
                    && edge < settings.world.controlPoints.inland + 0.08F;
            if (!nearCoast) {
                return;
            }
            float roll = hash01(seed ^ 0x51ED, worldX >> 2, worldZ >> 2);
            float volcanicBand = volcanic * 0.35F;
            if (volcanic > 0.0F && roll < volcanicBand) {
                paintVolcanoCone(cell, worldX, worldZ, seed, water, 70.0F + hash01(seed, worldX >> 4, worldZ >> 4) * 90.0F);
            } else if (coastal > 0.0F && roll < volcanicBand + coastal * 0.50F) {
                float local = hash01(seed ^ 17, worldX >> 1, worldZ >> 1);
                if (local < 0.35F) {
                    cell.terrain = ModTerrainTypes.COASTAL_ISLAND;
                    cell.value = Math.max(cell.value, water + 0.04F + local * 0.06F);
                }
            }
        });
    }

    private static void paintArchipelago(Cell cell, int worldX, int worldZ, int seed, float chance, float water) {
        int cx = NoiseUtil.floor(worldX / (float) ARCH_CELL);
        int cz = NoiseUtil.floor(worldZ / (float) ARCH_CELL);
        if (hash01(seed ^ 99, cx, cz) > chance) {
            return;
        }
        float centerX = (cx + 0.5F) * ARCH_CELL;
        float centerZ = (cz + 0.5F) * ARCH_CELL;
        float coreR = 180.0F + hash01(seed, cx, cz) * 820.0F;
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (dist <= coreR) {
            cell.terrain = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
            cell.continentEdge = Math.max(cell.continentEdge, 0.75F);
            cell.value = Math.max(cell.value, water + 0.06F + (1.0F - dist / coreR) * 0.08F);
            return;
        }
        float ring = coreR * 2.2F;
        if (dist > ring) {
            return;
        }
        float angle = (float) (Math.atan2(dz, dx) / (Math.PI * 2.0) + 0.5);
        int slot = NoiseUtil.floor(angle * 12.0F);
        float slotCenter = (slot + 0.5F) / 12.0F;
        if (Math.abs(angle - slotCenter) > 0.04F) {
            cell.terrain = ModTerrainTypes.LAGUNA;
            cell.continentEdge = Math.max(cell.continentEdge, 0.30F);
            cell.value = Math.min(cell.value, water - 0.01F);
            return;
        }
        float smallR = 15.0F + hash01(seed ^ 7, cx, cz + slot) * 80.0F;
        float radial = Math.abs(dist - (coreR + smallR * 1.6F));
        if (radial <= smallR) {
            cell.terrain = ModTerrainTypes.SCATTERED_ARCHIPELAGO;
            cell.continentEdge = Math.max(cell.continentEdge, 0.70F);
            cell.value = Math.max(cell.value, water + 0.05F);
        } else {
            cell.terrain = ModTerrainTypes.LAGUNA;
            cell.continentEdge = Math.max(cell.continentEdge, 0.28F);
            cell.value = Math.min(cell.value, water - 0.008F);
        }
    }

    private static void paintOceanVolcano(Cell cell, int worldX, int worldZ, int seed, float chance, float water) {
        int cx = NoiseUtil.floor(worldX / (float) VOLC_CELL);
        int cz = NoiseUtil.floor(worldZ / (float) VOLC_CELL);
        if (hash01(seed ^ 0xB01C, cx, cz) > chance * 0.55F) {
            return;
        }
        float centerX = (cx + 0.5F) * VOLC_CELL + (hash01(seed, cx, cz) - 0.5F) * VOLC_CELL * 0.35F;
        float centerZ = (cz + 0.5F) * VOLC_CELL + (hash01(seed ^ 3, cx, cz) - 0.5F) * VOLC_CELL * 0.35F;
        float radius = 90.0F + hash01(seed ^ 9, cx, cz) * 160.0F;
        paintVolcanoAt(cell, worldX, worldZ, centerX, centerZ, radius, water);
    }

    private static void paintVolcanoCone(Cell cell, int worldX, int worldZ, int seed, float water, float radius) {
        float centerX = (worldX >> 4 << 4) + 8.0F;
        float centerZ = (worldZ >> 4 << 4) + 8.0F;
        paintVolcanoAt(cell, worldX, worldZ, centerX, centerZ, radius, water);
    }

    private static void paintVolcanoAt(Cell cell, float worldX, float worldZ, float centerX, float centerZ, float radius, float water) {
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (dist > radius) {
            return;
        }
        float t = 1.0F - dist / radius;
        float craterR = radius * 0.22F;
        float rimR = radius * 0.38F;
        float height;
        if (dist <= craterR) {
            float inner = dist / Math.max(1.0F, craterR);
            height = water + 0.10F + inner * 0.04F;
            cell.terrain = TerrainType.VOLCANO_PIPE;
        } else if (dist <= rimR) {
            float rim = (dist - craterR) / Math.max(1.0E-3F, rimR - craterR);
            height = water + 0.16F + rim * 0.08F;
            cell.terrain = TerrainType.VOLCANO;
        } else {
            height = water + 0.08F + t * 0.14F;
            cell.terrain = ModTerrainTypes.VOLCANIC_ISLAND;
        }
        cell.continentEdge = Math.max(cell.continentEdge, 0.55F + t * 0.35F);
        cell.value = Math.max(cell.value, height);
    }

    private static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
