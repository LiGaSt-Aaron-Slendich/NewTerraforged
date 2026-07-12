package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Replays {@link NoiseCaveCarver} column decisions at the probe position (post-gen safe).
 */
public final class CarveDecisionDiagnostics {
    private CarveDecisionDiagnostics() {
    }

    public static void append(Generator generator, ChunkAccess chunk, BlockPos pos, CaveDebugReport report) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int lx = x & 0xF;
        int lz = z & 0xF;
        int seed = Seeds.get(generator.getSeed());
        report.add("");
        report.add("[Carve decision replay]");
        CarverChunk live = generator.peekCaveCarver(chunk.getPos());
        if (live != null && live.isColumnCacheReady()) {
            report.add("Column cache source: live carver (chunk still in decorate pipeline)");
            CarveDecisionDiagnostics.appendForCarver(generator, chunk, live, lx, y, lz, x, z, seed, report);
            return;
        }
        CarverChunk rebuilt = generator.buildDiagnosticCarver(seed, chunk);
        if (rebuilt == null || !rebuilt.isColumnCacheReady()) {
            report.add("Column cache source: FAILED to rebuild — terrain/chunk data unavailable");
            return;
        }
        report.add("Column cache source: rebuilt from seed + chunk terrain (live cache expired — normal after gen)");
        CarveDecisionDiagnostics.appendForCarver(generator, chunk, rebuilt, lx, y, lz, x, z, seed, report);
    }

    private static void appendForCarver(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, int x, int z, int seed, CaveDebugReport report) {
        CarverColumnCache columns = carver.columnCache();
        CarveDecisionDiagnostics.appendSurfaceBreakdown(generator, chunk, carver, lx, lz, x, z, report);
        CarveDecisionDiagnostics.appendColumnFlags(columns, lx, lz, report);
        CarveDecisionDiagnostics.appendPostGenTruth(chunk, lx, y, lz, report);
        CarveDecisionDiagnostics.appendPostProcessHint(generator, chunk, carver, lx, lz, y, report);
        List<String> carveLines = CarveDecisionDiagnostics.replayCarvePasses(generator, chunk, carver, lx, y, lz, seed);
        report.add("");
        report.add("Per-pass carve replay at column (probe Y=" + y + "):");
        for (String line : carveLines) {
            report.add("  " + line);
        }
        String summary = CarveDecisionDiagnostics.summarize(carveLines, y);
        report.add("");
        report.add("Verdict: " + summary);
    }

    private static void appendSurfaceBreakdown(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int lz, int x, int z, CaveDebugReport report) {
        int heightmap = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int oceanFloorWg = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
        int cached = carver.cachedSurface(lx, lz);
        int terrainH = -1;
        int rawMax = heightmap;
        TerrainData terrain = carver.terrainData;
        if (terrain != null) {
            terrainH = terrain.getHeight(lx, lz);
            rawMax = Math.max(heightmap, terrainH);
        }
        int capped = rawMax;
        if (terrain != null) {
            capped = Math.max(heightmap, Math.min(terrainH, heightmap + 12));
        }
        if (CaveOceanFilter.isSurfaceWaterColumn(generator, x, z)) {
            capped = Math.min(capped, oceanFloorWg);
        }
        boolean waterCol = CaveOceanFilter.isSurfaceWaterColumn(generator, x, z);
        report.add(String.format(Locale.ROOT,
                "Surface chain: heightmap=%d, terrainData=%s, rawMax=%d, cachedSurface=%d (inflation cap +12%s)",
                heightmap,
                terrainH >= 0 ? Integer.toString(terrainH) : "n/a",
                rawMax,
                cached,
                waterCol ? ", water column clamped to OCEAN_FLOOR_WG=" + oceanFloorWg : ""));
        if (cached != capped) {
            report.add("WARNING: cachedSurface=" + cached + " differs from recomputed cap=" + capped + " — check CarverColumnCache.build");
        }
        if (terrainH >= 0 && terrainH > heightmap + 12) {
            report.add(String.format(Locale.ROOT,
                    "Terrain inflation: terrain %d blocks above heightmap — without cap carve ceiling would be ~%d (void-under-water risk)",
                    terrainH - heightmap, terrainH));
        }
    }

    private static void appendColumnFlags(CarverColumnCache columns, int lx, int lz, CaveDebugReport report) {
        byte zone = columns.megaGigaFlag(lx, lz);
        String zoneLabel = switch (zone) {
            case MegaGigaZoneProbe.MEGA -> "MEGA";
            case MegaGigaZoneProbe.GIGA -> "GIGA";
            default -> "none (synapse/global eligible)";
        };
        report.add("Zone: " + zoneLabel
                + ", globalPassMatch=" + columns.matches(CaveType.GLOBAL, lx, lz)
                + ", oceanBlocked=" + columns.oceanBlocked(lx, lz)
                + ", nearRiver=" + columns.nearRiver(lx, lz)
                + ", suppressBreach=" + columns.suppressSurfaceBreach(lx, lz));
        report.add(String.format(Locale.ROOT,
                "Sample shift: dx=%d dz=%d, extraCenterDrop=%d",
                columns.sampleShiftX(lx, lz), columns.sampleShiftZ(lx, lz), columns.extraCenterDrop(lx, lz)));
    }

    private static void appendPostGenTruth(ChunkAccess chunk, int lx, int y, int lz, CaveDebugReport report) {
        BlockState atFeet = chunk.getBlockState(new BlockPos(lx, y, lz));
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int airSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, y, lz, minY, maxY);
        report.add(String.format(Locale.ROOT,
                "Post-gen blocks: feet=%s, air column span=%d",
                atFeet.isAir() ? "AIR" : atFeet.getBlock().getDescriptionId(), airSpan));
    }

    private static void appendPostProcessHint(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int lz, int y, CaveDebugReport report) {
        int x = chunk.getPos().getMinBlockX() + lx;
        int z = chunk.getPos().getMinBlockZ() + lz;
        if (!CaveOceanFilter.isSurfaceWaterColumn(generator, x, z) && !carver.columnCache().nearRiver(lx, lz)) {
            return;
        }
        int waterY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int bedY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
        boolean airUnderWater = false;
        for (int cy = bedY; cy < waterY; ++cy) {
            if (chunk.getBlockState(new BlockPos(lx, cy, lz)).isAir()) {
                airUnderWater = true;
                break;
            }
        }
        if (airUnderWater) {
            report.add(String.format(Locale.ROOT,
                    "Post-carve repair: restoreRiverDepressions may run refillWaterOnly (waterY=%d bed~%d) — water refill without stone, void can remain",
                    waterY, bedY));
        }
    }

    private static List<String> replayCarvePasses(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, int seed) {
        List<String> lines = new ArrayList<>();
        CarverColumnCache columns = carver.columnCache();
        boolean megaGigaChunk = columns.anyMegaGiga();
        NoiseCave[] configs = generator.orderedCarveConfigs();
        boolean anyCarve = false;
        for (NoiseCave config : configs) {
            if (!generator.isCarveConfigEnabled(config)) {
                continue;
            }
            CaveType type = config.getType();
            if (megaGigaChunk && type == CaveType.GLOBAL) {
                lines.add(CarveDecisionDiagnostics.configLabel(config) + ": SKIPPED (chunk has mega/giga — global synapse pass disabled)");
                continue;
            }
            if (!megaGigaChunk && type.isMegaOrGiga()) {
                lines.add(CarveDecisionDiagnostics.configLabel(config) + ": SKIPPED (no mega/giga zone in chunk)");
                continue;
            }
            if (type == CaveType.GLOBAL && !columns.anySynapseEligible()) {
                lines.add(CarveDecisionDiagnostics.configLabel(config) + ": SKIPPED (chunk anySynapseEligible=false)");
                continue;
            }
            carver.beginCavePass(config);
            carver.modifier = generator.carveModifierFor(config);
            int[][] smoothedCenterY = null;
            int[][] smoothedCavern = null;
            if (type.isMegaOrGiga()) {
                smoothedCenterY = new int[16][16];
                smoothedCavern = new int[16][16];
                NoiseCaveCarver.prepareSmoothedMegaGigaColumnsForProbe(seed, config, carver, columns, type,
                        chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), smoothedCenterY, smoothedCavern);
            }
            NoiseCaveCarver.ColumnProbeResult probe = NoiseCaveCarver.probeColumn(seed, chunk, carver, generator, config,
                    lx, lz, y, smoothedCenterY, smoothedCavern);
            String label = CarveDecisionDiagnostics.configLabel(config);
            if (probe.carvesAtProbe()) {
                anyCarve = true;
                lines.add(label + ": CARVE at probe — " + probe.detailLine());
            } else {
                lines.add(label + ": " + probe.skipReason());
            }
        }
        if (!anyCarve) {
            lines.add("(no pass would carve at probe Y — air below may be from neighbor chunk or post-process)");
        }
        return lines;
    }

    private static String configLabel(NoiseCave config) {
        return config.getType().name() + "(y=" + config.getMinY() + ".." + config.getMaxY() + ",seed=" + config.getSeed() + ")";
    }

    private static String summarize(List<String> lines, int probeY) {
        List<String> carvers = new ArrayList<>();
        for (String line : lines) {
            if (line.contains(": CARVE at probe")) {
                carvers.add(line.substring(0, line.indexOf(':')));
            }
        }
        if (!carvers.isEmpty()) {
            return "carving at Y=" + probeY + " explained by: " + String.join(", ", carvers);
        }
        for (String line : lines) {
            if (line.startsWith("  ")) {
                line = line.trim();
            }
            if (line.contains("SKIPPED") || line.contains(": SKIP")) {
                continue;
            }
        }
        String lastSkip = null;
        for (String line : lines) {
            if (line.contains("SKIPPED") || line.contains("SKIP —")) {
                lastSkip = line;
            }
        }
        if (lastSkip != null) {
            return "no carve at probe Y=" + probeY + "; last relevant pass: " + lastSkip;
        }
        return "no carve pass matched this column at Y=" + probeY;
    }
}
