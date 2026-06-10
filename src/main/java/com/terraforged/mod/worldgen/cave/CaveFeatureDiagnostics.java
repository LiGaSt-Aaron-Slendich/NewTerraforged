package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.Seeds;
import com.terraforged.mod.worldgen.biome.Source;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveDebugInfo;
import com.terraforged.mod.worldgen.cave.CaveEntranceClaims;
import com.terraforged.mod.worldgen.cave.CaveFeatureFilters;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveOceanFilter;
import com.terraforged.mod.worldgen.cave.CaveOpenAirCheck;
import com.terraforged.mod.worldgen.cave.CaveSystemGrid;
import com.terraforged.mod.worldgen.cave.CaveTunnelRiverDecorator;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import com.terraforged.mod.worldgen.cave.MegaCaveStructureFilter;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveFeatureDiagnostics {
    private CaveFeatureDiagnostics() {
    }

    public static void append(Generator generator, LevelReader level, BlockPos pos, List<String> lines) {
        Object blockReason;
        boolean canFeatures;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int lx = x & 0xF;
        int lz = z & 0xF;
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
        Source source = generator.getBiomeSource();
        int seed = Seeds.get(generator.getSeed());
        int surfaceY = generator.getOceanFloorHeight(x, z);
        int localSurface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
        Holder<Biome> surfaceBiome = source.getNoiseBiome(x >> 2, 0, z >> 2, Source.NOOP_CLIMATE_SAMPLER);
        String caveSystem = CaveDebugInfo.resolveCaveSystem(generator, x, y, z);
        boolean megaGiga = MegaCaveStructureFilter.isInMegaOrGigaCaveAt(generator, x, y, z);
        Holder<Biome> f3Biome = chunk.getNoiseBiome(lx >> 2, y >> 2, lz >> 2);
        Holder<Biome> painted = CarverChunk.readPaintedBiomeAt(chunk, lx, y, lz);
        if (painted == null) {
            painted = source.getUnderGroundBiome(seed, x, z, CaveType.GLOBAL, surfaceBiome, y, surfaceY, x, z, 256);
        }
        lines.add("");
        lines.add("[Biome column]");
        f3Biome.unwrapKey().ifPresent(key -> lines.add("F3 biome (quart): " + key.location()));
        painted.unwrapKey().ifPresent(key -> lines.add("Painted/sampler cave biome: " + key.location()));
        lines.add(String.format(Locale.ROOT, "Local surface Y=%d, depth below surface=%d, forbidden band=%d (mega/giga=%s)", localSurface, localSurface - y, megaGiga ? 4 : 12, megaGiga));
        if (!CaveBiomeIds.isUndergroundBiome(painted)) {
            canFeatures = false;
            blockReason = "not an underground/cave biome at this column";
        } else if (CaveBiomeIds.isBlockedCaveBiome(painted)) {
            canFeatures = false;
            blockReason = "biome is blocked for decoration";
        } else if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(painted)) {
            canFeatures = true;
            blockReason = "uses dedicated decorator (not volume scatter)";
        } else if (CaveOpenAirCheck.isInUndergroundSurfaceForbiddenZone(chunk, lx, y, lz, megaGiga)) {
            canFeatures = false;
            blockReason = "surface-forbidden zone (open sky / too close to local surface)";
        } else if (!CaveUndergroundGuard.mayPlaceAnchor(chunk, lx, y, lz, megaGiga)) {
            canFeatures = false;
            blockReason = "above anchor depth (need deeper than surface - " + (megaGiga ? 6 : 16) + " blocks)";
        } else if (!chunk.getBlockState(new BlockPos(lx, y, lz)).isAir()) {
            canFeatures = false;
            blockReason = "anchor column is not air";
        } else if (!CaveFeaturePlacement.hasSolidFloorBelow(chunk, pos)) {
            canFeatures = false;
            blockReason = "no solid floor under anchor";
        } else {
            canFeatures = true;
            blockReason = "volume/accent decorators eligible";
        }
        lines.add("");
        lines.add(String.format(Locale.ROOT, "Features: %s due to '%s'", canFeatures ? "true" : "false", blockReason));
        CaveFeatureDiagnostics.appendBiomeFeatureVerdict(painted, lines);
        if ("Mega".equals(caveSystem) || "Giga".equals(caveSystem)) {
            CaveFeatureDiagnostics.appendTunnelDiagnostics(generator, seed, x, z, caveSystem, lines);
        }
    }

    private static void appendBiomeFeatureVerdict(Holder<Biome> biome, List<String> lines) {
        if (!CaveBiomeIds.isUndergroundBiome(biome)) {
            lines.add("Features: false due to 'surface/non-cave biome'");
            return;
        }
        if (CaveBiomeIds.isDedicatedDecoratedCaveBiome(biome)) {
            lines.add("Features: true due to 'dedicated cave decorator path'");
        }
        biome.unwrapKey().ifPresent(key -> {
            lines.add("Feature biome: " + key.location());
            CaveFeatureDiagnostics.appendSampleFeatures(biome, lines);
        });
    }

    private static void appendSampleFeatures(Holder<Biome> biome, List<String> lines) {
        BiomeGenerationSettings settings = ((Biome)biome.value()).getGenerationSettings();
        List stages = settings.features();
        int shown = 0;
        for (int stageIndex = 0; stageIndex < stages.size() && shown < 4; ++stageIndex) {
            HolderSet stage;
            if (!CaveFeatureFilters.isModCaveDecorationStage(stageIndex) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            for (int i = 0; i < stage.size() && shown < 4; ++i) {
                Holder placed = stage.get(i);
                ResourceLocation id = FeatureMassClassifier.featurePath((Holder<PlacedFeature>)placed);
                if (id == null) continue;
                String verdict = CaveFeatureDiagnostics.classifyFeature((Holder<PlacedFeature>)placed, biome);
                lines.add(String.format(Locale.ROOT, "Feature type: %s due to \"%s\"", id, verdict));
                ++shown;
            }
        }
        if (shown == 0) {
            lines.add("Feature type: (none) due to \"no mod-cave decoration stages in biome\"");
        }
    }

    private static String classifyFeature(Holder<PlacedFeature> placed, Holder<Biome> biome) {
        if (CaveFeatureFilters.isForbiddenForCaveBiome(placed, biome)) {
            return "forbidden for this cave biome";
        }
        if (!CaveFeatureFilters.isModCaveFeatureAllowed(placed, biome)) {
            return "filtered by cave feature rules";
        }
        if (!CaveFeatureFilters.belongsToModCaveBiome(placed, biome)) {
            return "feature theme does not match biome";
        }
        if (CaveFeatureFilters.isAnchorOnlyFeature(placed)) {
            return "anchor-only (needs volume anchor pass)";
        }
        return "allowed \u0432\u0402\u201d may place when anchor/budget pass";
    }

    private static void appendTunnelDiagnostics(Generator generator, int seed, int x, int z, String caveSystem, List<String> lines) {
        CaveType type = "Giga".equals(caveSystem) ? CaveType.GIGA : CaveType.MEGA;
        boolean nearSea = CaveOceanFilter.isNearSea(generator, x, z);
        boolean massif = CaveTunnelRiverDecorator.qualifiesMountainMassif(generator, seed, x, z);
        boolean tagTunnel = !nearSea && massif;
        long systemKey = CaveSystemGrid.systemKey(x, z, type);
        CaveEntranceClaims claims = generator.getCaveEntranceClaims();
        boolean mouthClaimed = claims.isClaimed(systemKey);
        boolean exitClaimed = claims.hasExit(systemKey);
        CaveEntranceClaims.TunnelAxis axis = claims.tunnelAxis(systemKey);
        lines.add("");
        lines.add("[OGPM(T)]");
        lines.add("Tag subtype tunnel: " + tagTunnel + " (relief massif=" + massif + ", nearSea=" + nearSea + ")");
        lines.add("System mouth claimed: " + mouthClaimed + ", exit claimed: " + exitClaimed);
        if (axis != null) {
            lines.add(String.format(Locale.ROOT, "Tunnel axis: mouth (%d,%d) -> exit (%d,%d)", axis.mouthX(), axis.mouthZ(), axis.exitX(), axis.exitZ()));
        } else {
            lines.add("Tunnel axis: none (river/exit not registered for this system)");
        }
        if (tagTunnel && !mouthClaimed) {
            lines.add("Note: tag only checks relief \u0432\u0402\u201d actual entrance needs carved mouth in this mega/giga cell");
        }
    }
}
