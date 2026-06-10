package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import com.terraforged.mod.worldgen.biome.decorator.FeatureMassClassifier;
import com.terraforged.mod.worldgen.cave.CaveBiomeIds;
import com.terraforged.mod.worldgen.cave.CaveFeatureFilters;
import com.terraforged.mod.worldgen.cave.CaveFeaturePlacement;
import com.terraforged.mod.worldgen.cave.CaveFeatureRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class CaveFeaturePlan {
    private final StageFeature[] floor;
    private final StageFeature[] ceiling;

    private CaveFeaturePlan(StageFeature[] floor, StageFeature[] ceiling) {
        this.floor = floor;
        this.ceiling = ceiling;
    }

    public StageFeature[] forAnchor(CaveFeatureRules.Anchor anchor) {
        return anchor == CaveFeatureRules.Anchor.FLOOR ? this.floor : this.ceiling;
    }

    public static CaveFeaturePlan build(BiomeGenerationSettings settings) {
        return CaveFeaturePlan.build(settings, false);
    }

    public static CaveFeaturePlan build(BiomeGenerationSettings settings, boolean modCaveBiome) {
        ArrayList<StageFeature> floor = new ArrayList<StageFeature>();
        ArrayList<StageFeature> ceiling = new ArrayList<StageFeature>();
        int topLayerStage = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
        List stages = settings.features();
        for (int stageIndex = 0; stageIndex < stages.size(); ++stageIndex) {
            HolderSet stage;
            if (!CaveFeaturePlan.isCaveDecorationStage(stageIndex, modCaveBiome) || (stage = (HolderSet)stages.get(stageIndex)) == null || stage.size() == 0) continue;
            boolean topLayer = stageIndex == topLayerStage;
            for (int featureIndex = 0; featureIndex < stage.size(); ++featureIndex) {
                boolean floorFeature;
                FeatureMass mass;
                Holder placed = stage.get(featureIndex);
                if (!CaveFeatureFilters.isAllowedFeature((Holder<PlacedFeature>)placed, stageIndex) || FeatureMassClassifier.isTree((Holder<PlacedFeature>)placed) || FeatureMassClassifier.spawnsSurfaceVegetation((Holder<PlacedFeature>)placed) || (mass = FeatureMassClassifier.classify((Holder<PlacedFeature>)placed)) == FeatureMass.BLOCKED) continue;
                StageFeature entry = new StageFeature(stageIndex, featureIndex, (Holder<PlacedFeature>)placed, mass, topLayer);
                boolean ceilingFeature = FeatureMassClassifier.isCaveCeilingFeature((Holder<PlacedFeature>)placed) || FeatureMassClassifier.isCeilingScatter((Holder<PlacedFeature>)placed);
                boolean bl = floorFeature = FeatureMassClassifier.isCaveFloorLarge((Holder<PlacedFeature>)placed) || !ceilingFeature || FeatureMassClassifier.isDualSurfaceFeature((Holder<PlacedFeature>)placed);
                if (floorFeature) {
                    floor.add(entry);
                }
                if (!ceilingFeature && !topLayer) continue;
                ceiling.add(entry);
            }
        }
        Comparator<StageFeature> byMass = Comparator.comparingInt(e -> CaveFeaturePlacement.massPriority(e.mass()));
        floor.sort(byMass);
        ceiling.sort(byMass);
        return new CaveFeaturePlan((StageFeature[])floor.toArray(StageFeature[]::new), (StageFeature[])ceiling.toArray(StageFeature[]::new));
    }

    private static boolean isCaveDecorationStage(int stageIndex, boolean modCaveBiome) {
        return CaveFeatureFilters.isAllowedStage(stageIndex, modCaveBiome);
    }

    private static boolean isAnchorFeature(Holder<PlacedFeature> placed, FeatureMass mass) {
        if (CaveFeatureFilters.isAnchorOnlyFeature(placed)) {
            return true;
        }
        if (FeatureMassClassifier.isCaveFloorLarge(placed) || FeatureMassClassifier.isCaveCeilingFeature(placed)) {
            return true;
        }
        return mass == FeatureMass.LARGE;
    }

    public record StageFeature(int stageIndex, int featureIndex, Holder<PlacedFeature> feature, FeatureMass mass, boolean topLayer) {
    }

    public static final class Cache {
        private final Map<Holder<Biome>, CaveFeaturePlan> plans = new IdentityHashMap<Holder<Biome>, CaveFeaturePlan>();

        public CaveFeaturePlan get(Holder<Biome> biome) {
            return this.plans.computeIfAbsent(biome, b -> {
                boolean modCave = CaveBiomeIds.isModCaveBiome((Holder<Biome>)b);
                return CaveFeaturePlan.build(((Biome)b.value()).getGenerationSettings(), modCave);
            });
        }

        public void clear() {
            this.plans.clear();
        }
    }
}
