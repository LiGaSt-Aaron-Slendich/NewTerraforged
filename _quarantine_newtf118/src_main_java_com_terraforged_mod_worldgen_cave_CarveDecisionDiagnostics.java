package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.terrain.TerrainData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Replays {@link NoiseCaveCarver} column decisions at the probe position (post-gen safe).
 */
public final class CarveDecisionDiagnostics {
    private static final int NEIGHBOR_SCAN_RADIUS = 7;

    private CarveDecisionDiagnostics() {
    }

    public static void append(Generator generator, LevelReader level, BlockPos pos, CaveDebugReport report) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
        int lx = x & 0xF;
        int lz = z & 0xF;
        int seed = Seeds.get(generator.getSeed());
        report.add("");
        report.add("[Carve decision replay]");
        if (!CaveCarvingGate.isEnabled()) {
            report.add("NoiseCave carving: DISABLED (CaveCarvingGate.enabled=false) — no synapse/mega/giga air should be placed");
        } else if (CaveCarvingGate.deferBlockCarveUntilAfterRiverFill) {
            report.add("NoiseCave block carve: deferred — restoreRiverDepressions first, then applyCarveBlocks, then features");
        } else {
            report.add("NoiseCave block carve: AIR step (full TerraForged pipeline), river fill in decorate after carve");
        }
        CarverChunk live = generator.peekCaveCarver(chunk.getPos());
        if (live != null && live.isColumnCacheReady()) {
            report.add("Column cache source: live carver (chunk still in decorate pipeline)");
            CarveDecisionDiagnostics.appendForCarver(generator, level, chunk, live, lx, y, lz, x, z, seed, report);
            return;
        }
        CarverChunk rebuilt = generator.buildDiagnosticCarver(seed, chunk);
        if (rebuilt == null || !rebuilt.isColumnCacheReady()) {
            report.add("Column cache source: FAILED to rebuild — terrain/chunk data unavailable");
            return;
        }
        report.add("Column cache source: rebuilt from seed + chunk terrain (live cache expired — normal after gen)");
        CarveDecisionDiagnostics.appendForCarver(generator, level, chunk, rebuilt, lx, y, lz, x, z, seed, report);
    }

    private static void appendForCarver(Generator generator, LevelReader level, ChunkAccess chunk, CarverChunk carver,
            int lx, int y, int lz, int x, int z, int seed, CaveDebugReport report) {
        CarverColumnCache columns = carver.columnCache();
        CarveDecisionDiagnostics.appendSurfaceBreakdown(generator, chunk, carver, lx, lz, x, z, report);
        CarveDecisionDiagnostics.appendSynapseGateBreakdown(generator, chunk, columns, lx, lz, x, z, seed, report);
        CarveDecisionDiagnostics.appendColumnFlags(generator, chunk, columns, lx, lz, report);
        boolean airAtFeet = chunk.getBlockState(new BlockPos(lx, y, lz)).isAir();
        CarveDecisionDiagnostics.appendPostGenTruth(chunk, lx, y, lz, report);
        if (airAtFeet) {
            CarveDecisionDiagnostics.appendHorizontalAirLayer(chunk, lx, y, lz, report);
        }
        CarveDecisionDiagnostics.appendPostProcessHint(generator, chunk, carver, lx, lz, y, report);
        List<String> carveLines = CarveDecisionDiagnostics.replayCarvePasses(generator, chunk, carver, lx, y, lz, seed);
        report.add("");
        report.add("Per-pass carve replay at THIS column (probe Y=" + y + "):");
        for (String line : carveLines) {
            report.add("  " + line);
        }
        boolean localCarve = carveLines.stream().anyMatch(line -> line.contains(": CARVE at probe"));
        if (airAtFeet && !localCarve) {
            report.add("");
            report.add("[Air origin — extended search]");
            report.add("Local NoiseCaveCarver passes do not explain air — scanning neighbors / grotto / other chunks");
            CarveDecisionDiagnostics.appendNeighborColumnCarveScan(generator, level, chunk, carver, lx, y, lz, x, z, seed, report);
            CarveDecisionDiagnostics.appendSphereBleedScan(generator, level, chunk, carver, lx, y, lz, x, z, seed, report);
            CarveDecisionDiagnostics.appendEntranceCarverProbe(generator, chunk, carver, lx, y, lz, x, z, seed, report);
            CarveDecisionDiagnostics.appendGrottoProbe(generator, chunk, carver, lx, y, lz, x, z, seed, report);
        }
        String summary = CarveDecisionDiagnostics.summarize(carveLines, y, airAtFeet, localCarve);
        report.add("");
        report.add("Verdict: " + summary);
    }

    private static void appendSynapseGateBreakdown(Generator generator, ChunkAccess chunk, CarverColumnCache columns,
            int lx, int lz, int x, int z, int seed, CaveDebugReport report) {
        NoiseCave synapse = CarveDecisionDiagnostics.primarySynapseConfig(generator);
        report.add("Chunk synapse gate: anySynapseEligible=" + columns.anySynapseEligible()
                + " (informational — GLOBAL pass no longer skipped when false)");
        if (synapse == null) {
            report.add("Synapse config: none/disabled");
            return;
        }
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int maxCavern = 0;
        int maxX = 0;
        int maxZ = 0;
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int wx = startX + dx;
                int wz = startZ + dz;
                int cavern = synapse.getCavernSize(seed, wx, wz, 1.0f);
                if (cavern > maxCavern) {
                    maxCavern = cavern;
                    maxX = wx;
                    maxZ = wz;
                }
            }
        }
        report.add(String.format(Locale.ROOT,
                "Synapse cavern probe (noise=1.0): max=%d at %d,%d (need >=1 for chunk gate)",
                maxCavern, maxX, maxZ));
        if (!columns.anySynapseEligible()) {
            report.add("Synapse border probe: no cavern>=1 inside or outside chunk — per-column carve only (chunk gate removed)");
        } else if (maxCavern < 1) {
            report.add("Synapse border probe: gate OPEN via neighbor-outside cavern (local max=0 but border stitch active)");
        }
        if (columns.isBorderColumn(lx, lz)) {
            CarverColumnCache.SynapseSample stitch = columns.resolveSynapseSample(synapse,
                    generator.carveModifierFor(synapse), seed, x, z, lx, lz);
            if (stitch.stitchedFromNeighbor) {
                report.add(String.format(Locale.ROOT,
                        "Border stitch at probe: using neighbor sample (%d,%d) cavern=%d",
                        stitch.sampleX, stitch.sampleZ, stitch.cavern));
            }
        }
    }

    private static void appendHorizontalAirLayer(ChunkAccess chunk, int lx, int y, int lz, CaveDebugReport report) {
        int airCount = 1;
        for (int ox = -NEIGHBOR_SCAN_RADIUS; ox <= NEIGHBOR_SCAN_RADIUS; ++ox) {
            for (int oz = -NEIGHBOR_SCAN_RADIUS; oz <= NEIGHBOR_SCAN_RADIUS; ++oz) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int px = lx + ox;
                int pz = lz + oz;
                if (px < 0 || px > 15 || pz < 0 || pz > 15) {
                    continue;
                }
                if (chunk.getBlockState(new BlockPos(px, y, pz)).isAir()) {
                    ++airCount;
                }
            }
        }
        report.add(String.format(Locale.ROOT,
                "Horizontal air at Y=%d within chunk (radius %d): %d columns — %s",
                y, NEIGHBOR_SCAN_RADIUS,
                airCount,
                airCount >= 5 ? "tunnel-like layer (likely carved horizontally, not single column)" : "isolated pocket"));
    }

    private static void appendSphereBleedScan(Generator generator, LevelReader level, ChunkAccess chunk,
            CarverChunk carver, int lx, int y, int lz, int x, int z, int seed, CaveDebugReport report) {
        report.add("Mega/giga bleed scan: column carve only — no cross-column sphere bleed (use neighbor column scan)");
    }

    private static void appendEntranceCarverProbe(Generator generator, ChunkAccess chunk, CarverChunk carver,
            int lx, int y, int lz, int x, int z, int seed, CaveDebugReport report) {
        CaveEntranceClaims claims = generator.getCaveEntranceClaims();
        if (claims == null) {
            report.add("Entrance carver: no entrance claims registry");
            return;
        }
        int midX = chunk.getPos().getMinBlockX() + 8;
        int midZ = chunk.getPos().getMinBlockZ() + 8;
        CaveType systemType = CaveSystemGrid.dominantType(generator, seed, midX, midZ);
        long systemKey = CaveSystemGrid.systemKey(midX, midZ, systemType);
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(systemKey);
        if (axis != null) {
            int distMouth = Math.max(Math.abs(x - axis.mouthX()), Math.abs(z - axis.mouthZ()));
            int distExit = Math.max(Math.abs(x - axis.exitX()), Math.abs(z - axis.exitZ()));
            report.add(String.format(Locale.ROOT,
                    "Massif tunnel axis: mouth=%d,%d exit=%d,%d — probe dist mouth=%d exit=%d (CaveEntranceCarver blobs/connectors)",
                    axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ(), distMouth, distExit));
            if (distMouth <= 14 || distExit <= 14) {
                report.add("  ^ within ramp-blob radius (~14) — air may be from carveRampBlob / carveSynapseConnector");
            }
        } else {
            report.add("Entrance carver: no massif tunnel axis registered for this chunk system");
        }
        if (carver.columnCache().riverSurfaceSuppressed(lx, lz)) {
            report.add("Entrance carver: riverSurfaceSuppressed=true at probe — entrance/tunnel carve blocked here");
        }
    }

    private static void appendNeighborColumnCarveScan(Generator generator, LevelReader level, ChunkAccess chunk,
            CarverChunk carver, int lx, int y, int lz, int x, int z, int seed, CaveDebugReport report) {
        List<String> hits = new ArrayList<>();
        CarveDecisionDiagnostics.scanWorldColumns(generator, level, chunk, carver, lx, y, lz, x, z, seed, NEIGHBOR_SCAN_RADIUS, hits, false);
        if (hits.isEmpty()) {
            report.add("Neighbor column carve scan (radius " + NEIGHBOR_SCAN_RADIUS + "): no NoiseCaveCarver pass covers probe Y");
            return;
        }
        report.add("Neighbor column carve scan — these columns WOULD carve at probe Y (air may bleed from sphere/column):");
        for (String hit : hits) {
            report.add("  " + hit);
        }
    }

    private static void scanWorldColumns(Generator generator, LevelReader level, ChunkAccess probeChunk,
            CarverChunk probeCarver, int probeLx, int probeLy, int probeLz, int probeX, int probeZ, int seed,
            int radius, List<String> hits, boolean includeProbeColumn) {
        for (int ox = -radius; ox <= radius; ++ox) {
            for (int oz = -radius; oz <= radius; ++oz) {
                if (!includeProbeColumn && ox == 0 && oz == 0) {
                    continue;
                }
                int wx = probeX + ox;
                int wz = probeZ + oz;
                ChunkAccess sourceChunk = level.getChunk(wx >> 4, wz >> 4);
                if (sourceChunk == null) {
                    continue;
                }
                int sourceLx = wx & 0xF;
                int sourceLz = wz & 0xF;
                CarverChunk sourceCarver = generator.peekCaveCarver(sourceChunk.getPos());
                if (sourceCarver == null || !sourceCarver.isColumnCacheReady()) {
                    sourceCarver = generator.buildDiagnosticCarver(seed, sourceChunk);
                }
                if (sourceCarver == null || !sourceCarver.isColumnCacheReady()) {
                    continue;
                }
                String hit = CarveDecisionDiagnostics.findCarveHit(generator, probeChunk, probeCarver, probeLx,
                        probeLy, probeLz, sourceChunk, sourceCarver, sourceLx, sourceLz, probeLy, wx, wz, seed, ox, oz);
                if (hit != null) {
                    hits.add(hit);
                }
            }
        }
    }

    private static String findCarveHit(Generator generator, ChunkAccess probeChunk, CarverChunk probeCarver,
            int probeLx, int probeLy, int probeLz, ChunkAccess sourceChunk, CarverChunk sourceCarver, int sourceLx,
            int sourceLz, int probeY, int wx, int wz, int seed, int ox, int oz) {
        CarverColumnCache columns = sourceCarver.columnCache();
        for (NoiseCave config : generator.orderedCarveConfigs()) {
            if (!generator.isCarveConfigEnabled(config)) {
                continue;
            }
            CaveType type = config.getType();
            if (!columns.matches(type, sourceLx, sourceLz)) {
                continue;
            }
            sourceCarver.beginCavePass(config);
            sourceCarver.modifier = generator.carveModifierFor(config);
            if (ox != 0 || oz != 0) {
                continue;
            }
            NoiseCaveCarver.ColumnProbeResult probe = NoiseCaveCarver.probeColumn(seed, sourceChunk, sourceCarver,
                    generator, config, sourceLx, sourceLz, probeY);
            if (!probe.carvesAtProbe()) {
                continue;
            }
            return String.format(Locale.ROOT, "offset (%+d,%+d) world %d,%d via %s COLUMN — %s",
                    ox, oz, wx, wz, type.name(), probe.detailLine());
        }
        return null;
    }

    private static void appendGrottoProbe(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int y,
            int lz, int x, int z, int seed, CaveDebugReport report) {
        CarverColumnCache columns = carver.columnCache();
        if (!columns.nearSea() || !columns.mayHaveRiver()) {
            report.add("Grotto carver: chunk not nearSea/mayHaveRiver — grotto entrance path unlikely");
            return;
        }
        NoiseCave synapse = CarveDecisionDiagnostics.primarySynapseConfig(generator);
        if (synapse == null) {
            report.add("Grotto carver: synapse config disabled");
            return;
        }
        List<String> candidates = new ArrayList<>();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int minY = generator.getMinY();
        for (int dx = 0; dx < 16; dx += 4) {
            for (int dz = 0; dz < 16; dz += 4) {
                int gx = startX + dx;
                int gz = startZ + dz;
                if (!CaveGrottoCarver.isGrottoCandidate(generator, columns, seed, gx, gz, dx, dz)) {
                    continue;
                }
                int surface = carver.cachedSurface(dx, dz);
                int chamberY = Math.max(minY + 6, Math.min(surface - 14, surface - 10));
                int dist = Math.max(Math.abs(gx - x), Math.abs(gz - z));
                boolean yMatch = Math.abs(y - chamberY) <= 8;
                candidates.add(String.format(Locale.ROOT,
                        "grotto candidate at %d,%d chamberY~%d dist=%d Ymatch=%s",
                        gx, gz, chamberY, dist, yMatch));
            }
        }
        if (candidates.isEmpty()) {
            report.add("Grotto carver: no hillside grotto candidates in chunk (CaveGrottoCarver — NOT in NoiseCaveCarver replay)");
            return;
        }
        report.add("Grotto carver (separate from synapse pass — horizontal connectors at chamberY):");
        for (String line : candidates) {
            report.add("  " + line);
        }
        report.add("If Ymatch=true: air may be from carveSynapseConnector / carveRampBlob, not column carve replay");
    }

    private static NoiseCave primarySynapseConfig(Generator generator) {
        for (NoiseCave config : generator.orderedCarveConfigs()) {
            if (config.getType() == CaveType.GLOBAL && generator.isCarveConfigEnabled(config)) {
                return config;
            }
        }
        return null;
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

    private static void appendColumnFlags(Generator generator, ChunkAccess chunk, CarverColumnCache columns, int lx, int lz, CaveDebugReport report) {
        report.add(String.format(Locale.ROOT,
                "Sea level (TerraForged default): Y=%d — hydrator/guards clamp near this height",
                generator.getSeaLevel()));
        int waterTop = CaveOceanFilter.findWaterSurfaceY(chunk, lx, lz);
        if (waterTop >= 0) {
            report.add(String.format(Locale.ROOT, "Chunk water surface at probe column: Y=%d", waterTop));
        }
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
                + ", riverCarveBlocked=" + columns.riverCarveBlocked(lx, lz)
                + ", riverSurfaceSuppressed=" + columns.riverSurfaceSuppressed(lx, lz)
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
        if (!CaveChunkSurfaceRepair.isRiverDepressionRestoreEnabled()) {
            report.add("Post-carve repair: restoreRiverDepressions DISABLED (riverDepressionRestoreEnabled=false)");
        }
        if (!CaveRiverEntranceHydrator.isRiverEntranceHydratorEnabled()) {
            report.add("Post-carve repair: CaveRiverEntranceHydrator DISABLED — old version filled y>=sea-1 only, leaving air shaft to Y="
                    + generator.getSeaLevel() + " on high rivers");
        }
        if (!CaveOceanFilter.isSurfaceWaterColumn(generator, x, z) && !carver.columnCache().nearRiver(lx, lz)) {
            return;
        }
        if (CaveChunkSurfaceRepair.isRiverDepressionRestoreEnabled()
                && CaveRiverEntranceHydrator.isRiverEntranceHydratorEnabled()) {
            // fall through to legacy hints below when both enabled
        } else if (!CaveChunkSurfaceRepair.isRiverDepressionRestoreEnabled()
                && !CaveRiverEntranceHydrator.isRiverEntranceHydratorEnabled()) {
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
                    "Post-carve repair: river channel has air under water (waterY=%d bed~%d) — refillWaterOnly patches gaps only (no carve/sync/plug)",
                    waterY, bedY));
        }
        TerrainData terrain = generator.getChunkDataIfReady(chunk.getPos());
        if (terrain != null && CaveChunkSurfaceRepair.isRiverBedColumn(terrain, lx, lz)) {
            int sea = generator.getSeaLevel();
            int bedYTerrain = terrain.getHeight(lx, lz);
            int waterYTerrain = com.terraforged.mod.worldgen.terrain.TerrainLevels.getWaterLevel(lx, lz, sea, terrain);
            if (CaveChunkSurfaceRepair.riverDepressionSkippedDueToCaveAir(chunk, lx, lz, bedYTerrain, waterYTerrain)) {
                report.add(String.format(Locale.ROOT,
                        "River bed column: cave air below bed~%d..%d — restoreRiverDepressions no longer plugs (fix carve root cause)",
                        bedYTerrain, waterYTerrain));
            }
        }
        report.add("River/lake: bank fill targets terrain.getHeight() per column (yellow-line slope), not flat refWaterY shelf");
        if (carver.hasTunnelRiver()) {
            report.add("Tunnel river: CaveTunnelRiverDecorator may carve punch/channel along massif tunnel axis (separate from restoreRiverDepressions)");
        }
    }

    private static List<String> replayCarvePasses(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, int seed) {
        List<String> lines = new ArrayList<>();
        CarverColumnCache columns = carver.columnCache();
        NoiseCave[] configs = generator.orderedCarveConfigs();
        boolean anyCarve = false;
        for (NoiseCave config : configs) {
            if (!generator.isCarveConfigEnabled(config)) {
                continue;
            }
            CaveType type = config.getType();
            if (!columns.matches(type, lx, lz)) {
                lines.add(CarveDecisionDiagnostics.configLabel(config) + ": SKIPPED (zone mismatch at probe column)");
                continue;
            }
            if (columns.riverCarveBlocked(lx, lz)) {
                lines.add(CarveDecisionDiagnostics.configLabel(config) + ": SKIPPED (riverCarveBlocked at probe)");
                continue;
            }
            carver.beginCavePass(config);
            carver.modifier = generator.carveModifierFor(config);
            NoiseCaveCarver.ColumnProbeResult probe = NoiseCaveCarver.probeColumn(seed, chunk, carver, generator, config,
                    lx, lz, y);
            String label = CarveDecisionDiagnostics.configLabel(config);
            if (probe.carvesAtProbe()) {
                anyCarve = true;
                lines.add(label + ": CARVE at probe — " + probe.detailLine());
            } else {
                lines.add(label + ": " + probe.skipReason());
            }
        }
        if (!anyCarve) {
            lines.add("(no NoiseCaveCarver pass at THIS column — see [Air origin] if feet=AIR)");
        }
        return lines;
    }

    private static String configLabel(NoiseCave config) {
        return config.getType().name() + "(y=" + config.getMinY() + ".." + config.getMaxY() + ",seed=" + config.getSeed() + ")";
    }

    private static String summarize(List<String> lines, int probeY, boolean airAtFeet, boolean localCarve) {
        if (localCarve) {
            List<String> carvers = new ArrayList<>();
            for (String line : lines) {
                if (line.contains(": CARVE at probe")) {
                    carvers.add(line.substring(0, line.indexOf(':')));
                }
            }
            return "NoiseCaveCarver explains Y=" + probeY + ": " + String.join(", ", carvers);
        }
        if (airAtFeet) {
            return "AIR at Y=" + probeY + " but no local NoiseCaveCarver pass — check [Air origin] (neighbor column / grotto / cross-chunk)";
        }
        String lastSkip = null;
        for (String line : lines) {
            if (line.contains("SKIPPED") || line.contains("SKIP —")) {
                lastSkip = line;
            }
        }
        if (lastSkip != null) {
            return "solid/no local carve at Y=" + probeY + "; " + lastSkip;
        }
        return "no carve pass matched this column at Y=" + probeY;
    }
}
