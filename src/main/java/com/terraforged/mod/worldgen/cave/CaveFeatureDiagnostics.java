package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
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
    private static final int SYNAPSE_SURFACE_BIOME_SKIP = 8;

    private CaveFeatureDiagnostics() {
    }

    public static void append(Generator generator, LevelReader level, BlockPos pos, List<String> lines) {
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
        lines.add("");
        lines.add("[Biome column — decor pipeline]");
        f3Biome.unwrapKey().ifPresent(key -> lines.add("F3 biome (quart): " + key.location()));
        if (quartPainted != null) {
            quartPainted.unwrapKey().ifPresent(key -> lines.add("Painted cave biome (quart): " + key.location()));
        } else {
            lines.add("Painted cave biome (quart): (none)");
            samplerBiome.unwrapKey().ifPresent(key -> lines.add("Sampler only (debug reference, decor ignores): " + key.location()));
        }
        lines.add(String.format(Locale.ROOT, "Local heightmap surface Y=%d, ocean floor Y=%d, depth below heightmap=%d",
                localSurface, oceanFloorY, localSurface - y));
        lines.add(String.format(Locale.ROOT, "Carve paint skip band: y>=%d (mega/giga skip=%d)", carveSkipFromY, carveSkipBand));
        if (carveWouldSkipPaint && quartPainted == null) {
            lines.add("Likely paint gap: floor inside surfaceBiomeSkip — decor needs quart paint, not sampler");
        }
        if (carver == null) {
            lines.add("Carver cache: expired (normal after chunk gen) — anchor/write guards are partial");
        } else {
            lines.add("Carver cache: active for this chunk");
        }
        Holder<Biome> decorBiome = quartPainted != null ? quartPainted : samplerBiome;
        String blockReason = CaveFeatureDiagnostics.evaluateDecorBlockReason(chunk, level, pos, carver, decorBiome, quartPainted, megaGiga, entranceColumn, lx, lz, y);
        boolean canFeatures = blockReason.startsWith("ok:");
        lines.add("");
        lines.add(String.format(Locale.ROOT, "Decor at feet: %s due to '%s'", canFeatures ? "eligible" : "blocked", canFeatures ? blockReason.substring(3) : blockReason));
        CaveFeatureDiagnostics.appendAnchorGridDiagnostics(generator, chunk, carver, decorBiome, quartPainted, megaGiga, entranceColumn, x, z, y, lines);
        if (quartPainted != null) {
            int maxY = chunk.getHighestSectionPosition() + 15;
            int minY = chunk.getMinBuildHeight();
            int chamberSpan = CaveColumnScan.measureAirColumnSpan(chunk, lx, y, lz, minY, maxY);
            boolean nearSurfaceCrust = CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga);
            CaveFeatureDiagnostics.appendBiomeFeatureVerdict(quartPainted, chamberSpan, nearSurfaceCrust, lines);
        }
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveFeatureDiagnostics.appendTunnelDiagnostics(generator, seed, x, z, caveSystem, lines);
        }
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
        if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, pos)) {
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
            case "official" -> "official/" + CaveBiomeDecoratorRouter.resolve(decorBiome).name().toLowerCase();
            case "compromise" -> "compromise decorator";
            case "vanilla" -> "vanilla pass";
            case "legacy" -> "legacy volume decorator";
            default -> "no decoration mode enabled";
        };
    }

    private static void appendAnchorGridDiagnostics(Generator generator, ChunkAccess chunk, CarverChunk carver, Holder<Biome> decorBiome, Holder<Biome> quartPainted, boolean megaGiga, boolean entranceColumn, int x, int z, int y, List<String> lines) {
        if (carver == null || quartPainted == null) {
            return;
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
        for (int glx = 0; glx < 16; glx += grid) {
            for (int glz = 0; glz < 16; glz += grid) {
                int floorY = CaveBiomeVolumeDecorator.findFloorAirPublic(chunk, carver, decorBiome, glx, glz, minY, maxY, generator, chunkX + glx, chunkZ + glz);
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
        lines.add("");
        lines.add("[Decor anchor grid]");
        lines.add(String.format(Locale.ROOT, "Probe grid step=%d (mega/giga decor uses grid anchors, not every block)", grid));
        if (bestFloorY < 0) {
            lines.add("Nearest grid anchor: none in chunk (findFloorAir failed for all probe columns)");
            return;
        }
        boolean anchorOk = CaveUndergroundGuard.mayPlaceAnchorForBiome(chunk, carver, bestLx, bestFloorY, bestLz, quartPainted, megaGiga, carver.isEntranceColumn(bestLx, bestLz));
        lines.add(String.format(Locale.ROOT, "Nearest grid anchor: local (%d, %d, %d), dist=%d blocks", bestLx, bestFloorY, bestLz, bestDist));
        lines.add(String.format(Locale.ROOT, "Anchor mayPlaceAnchorForBiome: %s", anchorOk ? "pass" : "FAIL"));
        if (!anchorOk) {
            Holder<Biome> resolved = carver.resolveBiome(chunk, bestLx, bestFloorY, bestLz);
            resolved.unwrapKey().ifPresent(key -> lines.add("Anchor resolved biome: " + key.location()));
        }
        if (bestDist > 0) {
            lines.add("Note: you are not on a decor grid column — empty patches between anchors are expected");
        }
    }

    private static void appendBiomeFeatureVerdict(Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust, List<String> lines) {
        if (!CaveBiomeIds.isUndergroundBiome(biome)) {
            lines.add("Feature pool: none (surface/non-cave biome)");
            return;
        }
        biome.unwrapKey().ifPresent(key -> {
            lines.add("Feature biome: " + key.location());
            CaveFeatureDiagnostics.appendSampleFeatures(biome, chamberSpan, nearSurfaceCrust, lines);
        });
    }

    private static void appendSampleFeatures(Holder<Biome> biome, int chamberSpan, boolean nearSurfaceCrust, List<String> lines) {
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        int shown = 0;
        int allowed = 0;
        int rejected = 0;
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int i = 0; i < stage.size(); ++i) {
                Holder placed = stage.get(i);
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null || CaveFeatureFilters.isDeferredOrGlobalFeature((Holder<PlacedFeature>)placed)) continue;
                String verdict = CaveFeatureDiagnostics.classifyFeature((Holder<PlacedFeature>)placed, biome, chamberSpan, nearSurfaceCrust);
                if (verdict.startsWith("allowed")) {
                    ++allowed;
                } else {
                    ++rejected;
                }
                if (shown >= 6) continue;
                lines.add(String.format(Locale.ROOT, "Feature type: %s due to \"%s\"", id, verdict));
                ++shown;
            }
        }
        lines.add(String.format(Locale.ROOT, "Feature candidates: %d allowed, %d rejected (%s)",
                allowed, rejected, CaveFeatureDiagnostics.featurePoolLabel()));
        if (shown == 0) {
            lines.add("Feature type: (none) due to \"no biome-native decoration stages\"");
        }
    }

    private static String featurePoolLabel() {
        return switch (CaveDecorationSettings.activeModeLabel()) {
            case "official" -> "official router rules; deferred/global skipped";
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
        if (CaveDecorationSettings.useOfficialTfDecorator()) {
            return switch (CaveBiomeDecoratorRouter.resolve(biome)) {
                case VANILLA -> "allowed - vanilla pass (predicates decide placement)";
                case LEGACY, COMPROMISE -> CaveFeatureDiagnostics.classifyLegacyFeature(placed, biome);
                default -> TerraForgedOfficialCaveDecorator.decorFeatureVerdict(placed, biome, chamberSpan, nearSurfaceCrust);
            };
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
        if (CaveFeatureFilters.isAnchorOnlyFeature(placed)) {
            return "anchor-only (needs legacy volume pass)";
        }
        return "allowed - may place when anchor/budget pass";
    }

    private static String classifyLegacyFeature(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isDeferredOrGlobalFeature(placed)) {
            return "foreign/deferred feature (not for this cave biome)";
        }
        if (!CaveFeatureFilters.isModCaveFeatureAllowed(placed, biome)) {
            return "filtered by cave feature rules";
        }
        if (!CaveFeatureFilters.belongsToModCaveBiome(placed, biome)) {
            return "feature theme does not match biome (legacy filter)";
        }
        return "allowed - legacy scatter pass will attempt placement";
    }

    private static void appendTunnelDiagnostics(Generator generator, int seed, int x, int z, String caveSystem, List<String> lines) {
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
        lines.add("");
        lines.add("[OGPM(T)]");
        lines.add("Tunnel subtype (both openings carved): " + completed);
        lines.add("Prospective tunnel pair (validated mouth+exit): " + prospective);
        lines.add("System mouth claimed: " + mouthClaimed + ", exit claimed: " + exitClaimed);
        if (axis != null) {
            lines.add(String.format(Locale.ROOT, "Tunnel axis: mouth (%d,%d) -> exit (%d,%d)", axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ()));
        } else {
            lines.add("Tunnel axis: none");
        }
        if (prospective && !mouthClaimed) {
            lines.add("Note: validated pair; mouth carve runs at anchor column during chunk generation");
        } else if (mouthClaimed && !exitClaimed) {
            lines.add("Note: mouth carved; exit carve pending at opposite anchor");
        } else if (completed) {
            lines.add("Note: both tunnel openings carved — true through-cave");
        }
    }
}
