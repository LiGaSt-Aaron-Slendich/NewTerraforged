package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.cave.CaveFeatureClassifier;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Post-gen feature diagnostics aligned with the real decor pipeline (not sampler-only shortcuts).
 */
public final class CaveFeatureDiagnostics {
    private static final int MEGA_GIGA_SURFACE_BIOME_SKIP = 10;
    private static final int GIGA_SURFACE_BIOME_SKIP = 14;
    private static final int SYNAPSE_SURFACE_BIOME_SKIP = 8;

    private CaveFeatureDiagnostics() {
    }

    public static void append(Generator generator, LevelReader level, BlockPos pos, List<String> lines) {
        CaveDebugReport report = new CaveDebugReport();
        CaveFeatureDiagnostics.append(generator, level, pos, report);
        report.appendFeatureTable();
        lines.addAll(report.lines());
    }

    public static void append(Generator generator, LevelReader level, BlockPos pos, CaveDebugReport report) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int lx = x & 0xF;
        int lz = z & 0xF;
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int oceanFloorY = generator.getOceanFloorHeight(x, z);
        int localSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, x, y, z);
        boolean megaGiga = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, x, y, z);
        Holder<Biome> f3Biome = chunk.getNoiseBiome(lx >> 2, y >> 2, lz >> 2);
        Holder<Biome> quartPainted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        Holder<Biome> samplerBiome = source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome, y, oceanFloorY, x, z, 256);
        CarverChunk carver = generator.peekCaveCarver(chunk.getPos());
        boolean entranceColumn = carver != null && carver.isEntranceColumn(lx, lz);
        int carveSkipBand = megaGiga ? MEGA_GIGA_SURFACE_BIOME_SKIP : SYNAPSE_SURFACE_BIOME_SKIP;
        int carveSkipFromY = localSurface - carveSkipBand;
        boolean carveWouldSkipPaint = y >= carveSkipFromY;
        report.add("");
        report.add("[Carve / column]");
        CarveDecisionDiagnostics.append(generator, level, pos, report);
        CaveFeatureDiagnostics.appendCarveDiagnostics(generator, chunk, carver, lx, y, lz, x, z, localSurface, oceanFloorY, carveSkipFromY, carveSkipBand, megaGiga, caveSystem, quartPainted, report);
        report.add("");
        report.add("[Biome column — decor pipeline]");
        f3Biome.unwrapKey().ifPresent(key -> report.add("F3 biome (quart): " + key.location()));
        if (quartPainted != null) {
            quartPainted.unwrapKey().ifPresent(key -> report.add("Painted cave biome (quart): " + key.location()));
        } else {
            report.add("Painted cave biome (quart): (none)");
            samplerBiome.unwrapKey().ifPresent(key -> report.add("Sampler only (debug reference, decor ignores): " + key.location()));
        }
        report.add(String.format(Locale.ROOT, "Local heightmap surface Y=%d, ocean floor Y=%d, depth below heightmap=%d",
                localSurface, oceanFloorY, localSurface - y));
        report.add(String.format(Locale.ROOT, "Carve paint skip band: y>=%d (mega/giga skip=%d)", carveSkipFromY, carveSkipBand));
        if (carveWouldSkipPaint && quartPainted == null) {
            report.add("Likely paint gap: floor inside surfaceBiomeSkip — decor needs quart paint, not sampler");
        }
        if (carver == null) {
            report.add("Carver cache: expired (normal after chunk gen) — anchor/write guards are partial");
        } else {
            report.add("Carver cache: active for this chunk");
        }
        Holder<Biome> decorBiome = quartPainted != null ? quartPainted : samplerBiome;
        String blockReason = CaveFeatureDiagnostics.evaluateDecorBlockReason(chunk, level, pos, carver, decorBiome, quartPainted, megaGiga, entranceColumn, lx, lz, y);
        boolean canFeatures = blockReason.startsWith("ok:");
        report.add("");
        report.add(String.format(Locale.ROOT, "Decor at feet: %s due to '%s'", canFeatures ? "eligible" : "blocked", canFeatures ? blockReason.substring(3) : blockReason));
        AnchorInfo anchor = CaveFeatureDiagnostics.appendAnchorGridDiagnostics(generator, chunk, carver, decorBiome, quartPainted, megaGiga, entranceColumn, x, z, y, report);
        if (quartPainted != null) {
            int maxY = chunk.getHighestSectionPosition() + 15;
            int minY = chunk.getMinBuildHeight();
            int chamberSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, y, lz, minY, maxY);
            boolean nearSurfaceCrust = CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga);
            Holder<Biome> featureBiome = CaveBiomeIds.holderForDecoration(quartPainted, generator.getBiomeSource().getRegistry());
            CaveFeatureDiagnostics.appendBiomeFeatureVerdict(generator, chunk, level, carver, featureBiome, quartPainted, megaGiga, entranceColumn, chamberSpan, nearSurfaceCrust, blockReason, anchor, lx, y, lz, report);
        }
        if (quartPainted != null && CaveBiomeIds.isUndergroundBiome(quartPainted)) {
            CaveFeatureDiagnostics.appendCoverDiagnostics(chunk, carver, quartPainted, lx, y, lz, megaGiga, report);
        }
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveFeatureDiagnostics.appendTunnelDiagnostics(generator, seed, x, z, caveSystem, report);
        }
    }

    private record AnchorInfo(int lx, int floorY, int lz, int dist, boolean anchorOk) {}

    private static void appendCarveDiagnostics(Generator generator, ChunkAccess chunk, CarverChunk carver, int lx, int y, int lz, int x, int z, int localSurface, int oceanFloorY, int carveSkipFromY, int carveSkipBand, boolean megaGiga, String caveSystem, Holder<Biome> quartPainted, CaveDebugReport report) {
        boolean columnAir = chunk.getBlockState(new BlockPos(lx, y, lz)).isAir();
        boolean hasSolidFloor = y > chunk.getMinBuildHeight() && !chunk.getBlockState(new BlockPos(lx, y - 1, lz)).isAir();
        boolean carvedAir = columnAir && hasSolidFloor;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int airSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, y, lz, minY, maxY);
        report.add(String.format(Locale.ROOT, "Column at feet: air=%s, solid floor below=%s -> carved chamber floor=%s",
                columnAir, hasSolidFloor, carvedAir));
        report.add(String.format(Locale.ROOT, "Air column span at feet: %d blocks (wide flat chambers = synapse/mega horizontal carve)", airSpan));
        if (carver != null && carver.isColumnCacheReady()) {
            boolean forbidsWrite = carver.columnCache().forbidsUndergroundWrite(lx, y, lz, chunk, carver.isEntranceColumn(lx, lz));
            report.add("Column cache forbidsUndergroundWrite at feet: " + forbidsWrite);
            report.add("Carve cache at gen time: available (live column cache)");
        } else {
            report.add("Carve cache at gen time: EXPIRED — cannot replay exact carve decision; block/paint state below is post-gen truth");
        }
        if (lx <= 1 || lx >= 14 || lz <= 1 || lz >= 14) {
            report.add("Chunk edge column — horizontal caves often truncate at chunk boundary (looks 'cut flat')");
        }
        boolean inMegaGigaZone = megaGiga || "Mega".equals(caveSystem) || "Giga".equals(caveSystem);
        if (inMegaGigaZone) {
            report.add("Mega/giga cave zone: yes (" + caveSystem + ")");
        } else {
            report.add("Mega/giga cave zone: no (synapse/global cave column)");
        }
        if (y >= carveSkipFromY) {
            report.add(String.format(Locale.ROOT, "Carve paint skipped at gen: yes (y=%d >= skip band y>=%d) — quart paint may be missing even if stone was carved", y, carveSkipFromY));
        } else {
            report.add(String.format(Locale.ROOT, "Carve paint skipped at gen: no (y=%d below skip band y>=%d)", y, carveSkipFromY));
        }
        if (quartPainted != null) {
            if (!CaveBiomeIds.isUndergroundBiome(quartPainted)) {
                quartPainted.unwrapKey().ifPresent(key -> report.add("Quart biome paint at feet: " + key.location() + " (SURFACE biome leaked underground — decor/cover skipped)"));
            } else {
                quartPainted.unwrapKey().ifPresent(key -> report.add("Quart biome paint at feet: " + key.location()));
            }
        } else {
            report.add("Quart biome paint at feet: (none) — decor will not run without paint");
            if (carvedAir || airSpan >= 4) {
                report.add("Carve verdict: chamber air exists but paint missing — likely surfaceBiomeSkip band or restore overwrote cave paint");
            } else if (!columnAir) {
                report.add("Carve verdict: no air at probe — column was not carved here (solid rock) or player not on chamber floor");
            }
        }
        report.add(String.format(Locale.ROOT, "Ocean floor Y=%d, depth below ocean floor=%d", oceanFloorY, oceanFloorY - y));
    }

    private static void appendCoverDiagnostics(ChunkAccess chunk, CarverChunk carver, Holder<Biome> biome, int lx, int y, int lz, boolean megaGiga, CaveDebugReport report) {
        report.add("");
        report.add("[Floor cover]");
        boolean applies = CaveFloorCover.appliesTo(biome);
        report.add("Cover pass applies: " + applies);
        if (!applies) {
            return;
        }
        report.add("Themed cover block: " + CaveFloorCover.describeCover(biome));
        int floorY = CaveFeatureDiagnostics.resolveFloorY(chunk, lx, y, lz);
        BlockState floor = chunk.getBlockState(new BlockPos(lx, floorY, lz));
        boolean wouldPaint = floorY >= chunk.getMinBuildHeight() && CaveFloorCover.wouldPaintAt(chunk, carver, lx, floorY + 1, lz);
        report.add(String.format(Locale.ROOT, "Cover at feet floor Y=%d: block=%s, wouldPaintNow=%s", floorY, floor.getBlock().getDescriptionId(), wouldPaint));
        if (carver == null) {
            report.add("Note: cover runs at decor anchors during chunk gen — expired carver cannot replay anchor list");
        }
        if (!wouldPaint && applies) {
            if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, floorY, lz, megaGiga)) {
                report.add("Cover blocked: surface-forbidden zone");
            } else if (floorY >= chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz) - 4) {
                report.add("Cover blocked: within 4 blocks of heightmap surface");
            } else if (floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.MYCELIUM) || floor.is(Blocks.PODZOL)) {
                report.add("Cover blocked: floor already has surface soil");
            } else {
                report.add("Cover blocked: floor not paintable stone/dirt type");
            }
        }
    }

    private static int resolveFloorY(ChunkAccess chunk, int lx, int y, int lz) {
        int minY = chunk.getMinBuildHeight();
        if (chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
            for (int fy = y - 1; fy >= minY; --fy) {
                if (!chunk.getBlockState(new BlockPos(lx, fy, lz)).isAir()) {
                    return fy;
                }
            }
            return y;
        }
        return y > minY ? y - 1 : y;
    }

    private static String evaluateDecorBlockReason(ChunkAccess chunk, LevelReader level, BlockPos pos, CarverChunk carver, Holder<Biome> decorBiome, Holder<Biome> quartPainted, boolean megaGiga, boolean entranceColumn, int lx, int lz, int y) {
        if (quartPainted == null) {
            return "no painted cave biome at floor (quart empty)";
        }
        if (!CaveBiomeIds.isUndergroundBiome(quartPainted)) {
            return "painted quart is not an underground/cave biome";
        }
        if (CaveBiomeIds.isBlockedCaveBiome(quartPainted)) {
            return "biome is blocked for decoration";
        }
        if (CaveBiomeIds.isNetherThemedBiome(quartPainted)) {
            return "nether-themed biome blocked in overworld caves";
        }
        if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(quartPainted)) {
            return "dedicated decorator path is not wired (no runtime decorator call)";
        }
        if (carver != null && !CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, lx, y, lz, quartPainted, megaGiga, entranceColumn)) {
            return "mayPlaceAnchorForBiome failed (quart/biome mismatch vs decor expected biome)";
        }
        if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            return "surface-forbidden zone";
        }
        if (!CaveUndergroundGuard.mayPlaceAnchor(chunk, lx, y, lz, megaGiga)) {
            int depth = megaGiga ? CaveUndergroundGuard.MEGA_GIGA_ANCHOR_DEPTH : CaveUndergroundGuard.MIN_ANCHOR_DEPTH;
            return "above anchor depth (need y < heightmap surface - " + depth + ")";
        }
        if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
            return "feet column is not air";
        }
        if (y <= chunk.getMinBuildHeight() || chunk.getBlockState(new BlockPos(lx, y - 1, lz)).isAir()) {
            return "no solid floor under feet";
        }
        if (carver != null && carver.isColumnCacheReady() && carver.columnCache().forbidsUndergroundWrite(lx, y, lz, chunk, entranceColumn)) {
            return "forbidsUndergroundWrite (open-air column band)";
        }
        if (carver != null && level instanceof WorldGenLevel worldGen && !CaveUndergroundGuard.mayWriteBlockForBiome(worldGen, chunk, pos, quartPainted, carver)) {
            return "mayWriteBlockForBiome failed (placement write guard)";
        }
        return "ok:" + switch (CaveDecorationSettings.activeModeLabel()) {
            case "hybrid" -> "hybrid/" + CaveBiomeDecoratorRouter.resolve(decorBiome).name().toLowerCase();
            case "official" -> "official TF decorator";
            case "compromise" -> "compromise decorator";
            case "vanilla" -> "vanilla pass";
            case "legacy" -> "legacy volume decorator";
            default -> "no decoration mode enabled";
        };
    }

    private static AnchorInfo appendAnchorGridDiagnostics(Generator generator, ChunkAccess chunk, CarverChunk carver, Holder<Biome> decorBiome, Holder<Biome> quartPainted, boolean megaGiga, boolean entranceColumn, int x, int z, int y, CaveDebugReport report) {
        if (quartPainted == null) {
            return new AnchorInfo(-1, -1, -1, Integer.MAX_VALUE, false);
        }
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getHighestSectionPosition() + 15;
        int bestDist = Integer.MAX_VALUE;
        int bestFloorY = -1;
        int bestLx = -1;
        int bestLz = -1;
        int grid = megaGiga ? (CaveBiomeIds.isFungalCaveBiome(decorBiome) ? 2 : 3) : 2;
        boolean offline = carver == null;
        for (int glx = 0; glx < 16; glx += grid) {
            for (int glz = 0; glz < 16; glz += grid) {
                int floorY = offline
                        ? CaveFeatureDiagnostics.findOfflineFloorAir(chunk, glx, glz, minY, maxY, megaGiga)
                        : CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, decorBiome, glx, glz, minY, maxY, generator, chunkX + glx, chunkZ + glz);
                if (floorY < 0) {
                    continue;
                }
                int dist = Math.abs(glx - (x & 0xF)) + Math.abs(glz - (z & 0xF));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestFloorY = floorY;
                    bestLx = glx;
                    bestLz = glz;
                }
            }
        }
        report.add("");
        report.add("[Decor anchor grid]");
        report.add(String.format(Locale.ROOT, "Probe grid step=%d (decor runs at grid anchors, not every block)", grid));
        if (offline) {
            report.add("Anchor probe mode: OFFLINE (carver cache expired — block scan only, mayPlaceAnchorForBiome not replayed)");
        }
        if (bestFloorY < 0) {
            report.add("Nearest grid anchor: none in chunk (no air+solid floor on grid columns)");
            return new AnchorInfo(-1, -1, -1, Integer.MAX_VALUE, false);
        }
        boolean anchorOk = !offline && CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, bestLx, bestFloorY, bestLz, quartPainted, megaGiga, carver.isEntranceColumn(bestLx, bestLz));
        report.add(String.format(Locale.ROOT, "Nearest grid anchor: local (%d, %d, %d), dist=%d blocks", bestLx, bestFloorY, bestLz, bestDist));
        report.add(String.format(Locale.ROOT, "Anchor mayPlaceAnchorForBiome: %s", offline ? "unknown (carver expired)" : anchorOk ? "pass" : "FAIL"));
        if (!offline && !anchorOk) {
            Holder<Biome> resolved = carver.resolveBiome(chunk, bestLx, bestFloorY, bestLz);
            resolved.unwrapKey().ifPresent(key -> report.add("Anchor resolved biome: " + key.location()));
        }
        if (bestDist > 0) {
            report.add("Note: you are not on a decor grid column — empty patches between anchors are expected");
        }
        return new AnchorInfo(bestLx, bestFloorY, bestLz, bestDist, !offline && anchorOk);
    }

    private static int findOfflineFloorAir(ChunkAccess chunk, int lx, int lz, int minY, int maxY, boolean megaGiga) {
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        int minDepth = megaGiga ? CaveUndergroundGuard.MEGA_GIGA_ANCHOR_DEPTH : CaveUndergroundGuard.MIN_ANCHOR_DEPTH;
        int scanDepth = megaGiga ? 220 : 72;
        int scanTop = Math.min(maxY, surface - minDepth);
        int scanBottom = Math.max(minY, surface - scanDepth);
        for (int fy = scanTop; fy >= scanBottom; --fy) {
            if (!chunk.getBlockState(new BlockPos(lx, fy, lz)).isAir()) {
                continue;
            }
            if (fy <= minY || chunk.getBlockState(new BlockPos(lx, fy - 1, lz)).isAir()) {
                continue;
            }
            return fy;
        }
        return -1;
    }

    private static void appendBiomeFeatureVerdict(Generator generator, ChunkAccess chunk, LevelReader level, CarverChunk carver, Holder<Biome> biome, Holder<Biome> quartPainted, boolean megaGiga, boolean entranceColumn, int chamberSpan, boolean nearSurfaceCrust, String feetBlockReason, AnchorInfo anchor, int feetLx, int feetY, int feetLz, CaveDebugReport report) {
        if (!CaveBiomeIds.isUndergroundBiome(biome)) {
            report.add("Feature pool: none (surface/non-cave biome)");
            return;
        }
        biome.unwrapKey().ifPresent(key -> {
            report.add("Feature biome: " + key.location());
            CaveFeatureDiagnostics.appendAllFeatures(chunk, level, carver, biome, quartPainted, megaGiga, entranceColumn, chamberSpan, nearSurfaceCrust, feetBlockReason, anchor, report);
        });
    }

    private static void appendAllFeatures(ChunkAccess chunk, LevelReader level, CarverChunk carver, Holder<Biome> biome, Holder<Biome> quartPainted, boolean megaGiga, boolean entranceColumn, int chamberSpan, boolean nearSurfaceCrust, String feetBlockReason, AnchorInfo anchor, CaveDebugReport report) {
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        int floorAllowed = 0;
        int floorRejected = 0;
        int ceilAllowed = 0;
        int ceilRejected = 0;
        int injectedSkipped = 0;
        boolean feetOk = feetBlockReason.startsWith("ok:");
        String anchorDecorReason = "";
        boolean anchorDecorOk = false;
        boolean offlineAnchor = carver == null;
        if (anchor.floorY() >= 0) {
            BlockPos anchorPos = new BlockPos(chunk.getPos().getMinBlockX() + anchor.lx(), anchor.floorY(), chunk.getPos().getMinBlockZ() + anchor.lz());
            anchorDecorReason = CaveFeatureDiagnostics.evaluateDecorBlockReason(chunk, level, anchorPos, carver, biome, quartPainted, megaGiga, entranceColumn, anchor.lx(), anchor.lz(), anchor.floorY());
            anchorDecorOk = anchorDecorReason.startsWith("ok:");
        }
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) {
                continue;
            }
            for (int i = 0; i < stage.size(); ++i) {
                Holder placed = stage.get(i);
                ResourceLocation id = CaveFeatureClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null) {
                    continue;
                }
                if (CaveFeatureFilters.isDeferredOrGlobalFeature((Holder<PlacedFeature>)placed)) {
                    ++injectedSkipped;
                    continue;
                }
                String floorVerdict = CaveFeatureDiagnostics.classifyFeature((Holder<PlacedFeature>)placed, biome, chamberSpan, nearSurfaceCrust);
                boolean filterOk = floorVerdict.startsWith("allowed");
                boolean ceilingOnly = floorVerdict.contains("ceiling-only");
                String chamberStatus = CaveFeatureDiagnostics.chamberStatusFor((Holder<PlacedFeature>)placed, biome, chamberSpan, nearSurfaceCrust);
                if (filterOk) {
                    ++floorAllowed;
                } else if (ceilingOnly) {
                    ++ceilRejected;
                } else {
                    ++floorRejected;
                }
                String feetPlace = CaveFeatureDiagnostics.placementVerdict(feetOk, filterOk, ceilingOnly, feetBlockReason);
                String anchorPlace = anchor.floorY() < 0
                        ? "n/a"
                        : offlineAnchor
                        ? (anchorDecorOk ? "likely (offline probe)" : "no (offline)")
                        : CaveFeatureDiagnostics.placementVerdict(anchorDecorOk && anchor.anchorOk(), filterOk, ceilingOnly, anchorDecorReason);
                String placeAtAnchor = CaveFeatureDiagnostics.placeAtAnchorVerdict(filterOk, ceilingOnly, chamberStatus, anchorPlace, anchorDecorOk, anchor.anchorOk());
                String why = floorVerdict;
                if (filterOk && !"ok".equals(chamberStatus) && !"legacy".equals(chamberStatus) && !"n/a".equals(chamberStatus) && !chamberStatus.startsWith("legacy")) {
                    why = why + "; chamber: " + chamberStatus;
                }
                if (filterOk && !feetOk) {
                    why = floorVerdict + "; feet blocked: " + (feetBlockReason.startsWith("ok:") ? feetBlockReason.substring(3) : feetBlockReason);
                }
                if (filterOk && anchor.floorY() >= 0 && "no".equals(anchorPlace) && anchorDecorOk) {
                    why = why + "; anchor: mayPlaceAnchorForBiome=" + anchor.anchorOk();
                }
                if (filterOk && "no".equals(placeAtAnchor) && "yes".equals(anchorPlace)) {
                    why = why + "; placement may still fail (RNG/collision)";
                }
                report.addFeature(new CaveDebugReport.FeatureRow("[floor] " + id, filterOk ? "allowed" : "blocked", chamberStatus, feetPlace, anchorPlace, placeAtAnchor, why));
                if (TerraForgedOfficialCaveDecorator.isCeilingFeaturePublic((Holder<PlacedFeature>)placed)) {
                    String ceilVerdict = CaveFeatureDiagnostics.classifyCeilingFeature((Holder<PlacedFeature>)placed, biome);
                    boolean ceilFilterOk = ceilVerdict.startsWith("allowed");
                    if (ceilFilterOk) {
                        ++ceilAllowed;
                    } else {
                        ++ceilRejected;
                    }
                    String ceilFeet = CaveFeatureDiagnostics.placementVerdict(feetOk, ceilFilterOk, false, feetBlockReason);
                    String ceilAnchor = anchor.floorY() < 0
                            ? "n/a"
                            : offlineAnchor
                            ? (anchorDecorOk ? "likely (offline)" : "no (offline)")
                            : CaveFeatureDiagnostics.placementVerdict(anchorDecorOk && anchor.anchorOk(), ceilFilterOk, false, anchorDecorReason);
                    String ceilPlace = CaveFeatureDiagnostics.placeAtAnchorVerdict(ceilFilterOk, false, "n/a", ceilAnchor, anchorDecorOk, anchor.anchorOk());
                    report.addFeature(new CaveDebugReport.FeatureRow("[ceiling] " + id, ceilFilterOk ? "allowed" : "blocked", "n/a", ceilFeet, ceilAnchor, ceilPlace, ceilVerdict));
                }
            }
        }
        report.add(String.format(Locale.ROOT, "Feature candidates: floor %d allowed, %d rejected; ceiling %d allowed, %d rejected (%s)",
                floorAllowed, floorRejected, ceilAllowed, ceilRejected, CaveFeatureDiagnostics.featurePoolLabel()));
        if (injectedSkipped > 0) {
            report.add(String.format(Locale.ROOT, "Injected globals skipped from pool: %d (BYG/TerraBlender deferred)", injectedSkipped));
        }
        if (report.features().isEmpty()) {
            report.add("Feature type: (none) due to \"no biome-native decoration stages\"");
        }
        report.add("Note: filter 'allowed' = rules pass; Place? = anchor+chamber OK (RNG/collision may still fail); decor runs at grid anchors during chunk gen");
    }

    private static String chamberStatusFor(Holder<PlacedFeature> placed, Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust) {
        CaveDecoratorKind kind = CaveBiomeDecoratorRouter.resolve(biome);
        if (CaveDecorationSettings.useOfficialTfDecorator() || CaveDecorationSettings.usePerBiomeDecorators()) {
            return TerraForgedOfficialCaveDecorator.chamberGuardVerdict(placed, biome, chamberSpan, nearSurfaceCrust);
        }
        if (kind == CaveDecoratorKind.LEGACY && TerraForgedOfficialCaveDecorator.isFungalLargeFloorFeature(placed)) {
            return "legacy (large-first)";
        }
        return kind == CaveDecoratorKind.LEGACY ? "legacy" : "n/a";
    }

    private static String placeAtAnchorVerdict(boolean filterOk, boolean ceilingOnly, String chamberStatus, String anchorPlace, boolean anchorDecorOk, boolean anchorGridOk) {
        if (ceilingOnly || !filterOk) {
            return "no";
        }
        if ("n/a".equals(anchorPlace)) {
            return "n/a";
        }
        if (!anchorDecorOk || !anchorGridOk || "no".equals(anchorPlace) || anchorPlace.startsWith("no")) {
            return "no";
        }
        if (chamberStatus != null && chamberStatus.startsWith("skip")) {
            return "no";
        }
        if (anchorPlace.startsWith("likely")) {
            return "likely";
        }
        return "yes";
    }

    private static String placementVerdict(boolean decorOk, boolean filterOk, boolean ceilingOnly, String decorReason) {
        if (ceilingOnly) {
            return "no";
        }
        if (!filterOk) {
            return "no";
        }
        if (!decorOk) {
            return "no";
        }
        return "yes";
    }

    private static String classifyCeilingFeature(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return "forbidden for this cave biome";
        }
        if (TerraForgedOfficialCaveDecorator.shouldSkipFeaturePublic(placed, biome)) {
            return "blocked hazard/tree/ore (official skip list)";
        }
        if (TerraForgedOfficialCaveDecorator.shouldSkipForeignFeaturePublic(placed, biome)) {
            return "foreign/deferred feature (not for this cave biome)";
        }
        return "allowed - official TF ceiling pass will attempt placement";
    }

    private static String featurePoolLabel() {
        return switch (CaveDecorationSettings.activeModeLabel()) {
            case "official" -> "official TF rules; deferred/global skipped";
            case "hybrid" -> "hybrid router rules; deferred/global skipped";
            case "compromise" -> "compromise pass rules; deferred/global skipped";
            case "vanilla" -> "vanilla pass rules; deferred/global skipped";
            case "legacy" -> "legacy filter rules; deferred/global skipped";
            default -> "deferred/global skipped";
        };
    }

    private static String classifyFeature(Holder<PlacedFeature> placed, Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust) {
        if (CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return "forbidden for this cave biome";
        }
        CaveDecoratorKind kind = CaveBiomeDecoratorRouter.resolve(biome);
        if (CaveDecorationSettings.useOfficialTfDecorator() || CaveDecorationSettings.usePerBiomeDecorators()) {
            return TerraForgedOfficialCaveDecorator.decorFeatureVerdict(placed, biome, chamberSpan, nearSurfaceCrust);
        }
        if (CaveDecorationSettings.useCompromiseDecorator()) {
            return "allowed - compromise pass (cover/scatter at floor anchors)";
        }
        if (CaveDecorationSettings.useVanillaPass()) {
            return "allowed - vanilla pass (predicates decide placement)";
        }
        if (!CaveFeatureFilters.isModCaveFeatureAllowed(placed, biome)) {
            return "filtered by cave feature rules";
        }
        if (!CaveFeatureFilters.belongsToModCaveBiome(placed, biome)) {
            return "feature theme does not match biome (legacy filter)";
        }
        if (kind == CaveDecoratorKind.LEGACY && TerraForgedOfficialCaveDecorator.isFungalLargeFloorFeature(placed)) {
            return "allowed - legacy pass (large features before scatter)";
        }
        if (CaveFeatureFilters.isAnchorOnlyFeature(placed)) {
            return "anchor-only (needs legacy volume pass)";
        }
        return "allowed - may place when anchor/budget pass";
    }

    private static void appendTunnelDiagnostics(Generator generator, int seed, int x, int z, String caveSystem, CaveDebugReport report) {
        CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
        boolean prospective = CaveSiteTags.qualifiesProspectiveTunnel(generator, seed, x, z);
        boolean completed = CaveSiteTags.qualifiesTunnelMegaGiga(generator, seed, x, z);
        long systemKey = CaveSystemGrid.systemKey(x, z, type);
        CaveEntranceClaims claims = generator.getCaveEntranceClaims();
        boolean mouthClaimed = claims.isClaimed(systemKey);
        boolean exitClaimed = claims.hasExit(systemKey);
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(systemKey);
        if (axis == null) {
            axis = CaveSiteTags.prospectiveTunnelAxis(seed, type, x, z);
        }
        report.add("");
        report.add("[OGPM(T)]");
        report.add("Tunnel subtype (both openings carved): " + completed);
        report.add("Prospective tunnel pair (validated mouth+exit): " + prospective);
        report.add("System mouth claimed: " + mouthClaimed + ", exit claimed: " + exitClaimed);
        if (axis != null) {
            report.add(String.format(Locale.ROOT, "Tunnel axis: mouth (%d,%d) -> exit (%d,%d)", axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ()));
        } else {
            report.add("Tunnel axis: none");
        }
        if (prospective && !mouthClaimed) {
            report.add("Note: validated pair; mouth carve runs at anchor column during chunk generation");
        } else if (mouthClaimed && !exitClaimed) {
            report.add("Note: mouth carved; exit carve pending at opposite anchor");
        } else if (completed) {
            report.add("Note: both tunnel openings carved — true through-cave");
        }
    }
}
