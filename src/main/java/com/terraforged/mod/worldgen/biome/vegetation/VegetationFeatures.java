package com.terraforged.mod.worldgen.biome.vegetation;

import com.google.common.collect.ImmutableSet;
import com.terraforged.mod.util.ReflectionUtil;
import com.terraforged.mod.worldgen.asset.VegetationConfig;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class VegetationFeatures {
    public static VegetationFeatures NONE = new VegetationFeatures();
    public static final int STAGE = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
    private static volatile MethodHandle featureGetter;
    private static volatile MethodHandle placementsGetter;
    private static final Set<PlacementModifierType<?>> BIOME_CHECK;
    private static final Set<PlacementModifierType<?>> EXCLUSIONS;
    private static final Set<PlacementModifierType<?>> TREE_EXCLUSIONS;
    protected static final String[] TREE_KEYWORDS;
    protected static final String[] GRASS_KEYWORDS;
    protected static final String[] COVER_KEYWORDS;
    private final PlacedFeature[] trees;
    private final PlacedFeature[] grass;
    private final PlacedFeature[] other;

    private VegetationFeatures() {
        this.trees = new PlacedFeature[0];
        this.grass = new PlacedFeature[0];
        this.other = new PlacedFeature[0];
    }

    public VegetationFeatures(List<PlacedFeature> trees, List<PlacedFeature> grass, List<PlacedFeature> other) {
        this.trees = (PlacedFeature[])trees.toArray(PlacedFeature[]::new);
        this.grass = (PlacedFeature[])grass.toArray(PlacedFeature[]::new);
        this.other = (PlacedFeature[])other.toArray(PlacedFeature[]::new);
    }

    public PlacedFeature[] trees() {
        return this.trees;
    }

    public PlacedFeature[] grass() {
        return this.grass;
    }

    public PlacedFeature[] other() {
        return this.other;
    }

    public static VegetationFeatures create(Biome biome, RegistryAccess access, VegetationConfig config) {
        ArrayList<PlacedFeature> trees = new ArrayList<PlacedFeature>();
        ArrayList<PlacedFeature> grass = new ArrayList<PlacedFeature>();
        ArrayList<PlacedFeature> other = new ArrayList<PlacedFeature>();
        boolean custom = config != VegetationConfig.NONE;
        List features = biome.getGenerationSettings().features();
        if (features.size() > STAGE) {
            HolderSet vegetation = (HolderSet)features.get(STAGE);
            Registry featureRegistry = access.registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            for (Holder<PlacedFeature> feature : (Iterable<Holder<PlacedFeature>>) vegetation) {
                ResourceLocation featureKey = featureRegistry.getKey(((PlacedFeature)feature.value()));
                if (featureKey == null) {
                    other.add((PlacedFeature)feature.value());
                    continue;
                }
                String path = featureKey.getPath();
                if (VegetationFeatures.matches(path, TREE_KEYWORDS)) {
                    trees.add(VegetationFeatures.unwrap((Holder<PlacedFeature>)feature, TREE_EXCLUSIONS, custom));
                    continue;
                }
                if (VegetationFeatures.matches(path, GRASS_KEYWORDS) || VegetationFeatures.matches(path, COVER_KEYWORDS)) {
                    grass.add((PlacedFeature)feature.value());
                    continue;
                }
                other.add((PlacedFeature)feature.value());
            }
        }
        return new VegetationFeatures(trees, grass, other);
    }

    protected static boolean matches(String name, String[] keywords) {
        for (String keyword : keywords) {
            if (!name.contains(keyword)) continue;
            return true;
        }
        return false;
    }

    public static PlacedFeature unwrap(Holder<PlacedFeature> supplier, Set<PlacementModifierType<?>> exclusions, boolean custom) {
        if (!custom) {
            return (PlacedFeature)supplier.value();
        }
        try {
            PlacedFeature placed = (PlacedFeature)supplier.value();
            Holder<ConfiguredFeature<?, ?>> feature = VegetationFeatures.getFeature(placed);
            ArrayList<PlacementModifier> placements = new ArrayList<PlacementModifier>(VegetationFeatures.getPlacements(placed));
            placements.removeIf(placement -> exclusions.contains(placement.type()));
            return new PlacedFeature(feature, placements);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return (PlacedFeature)supplier.value();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static MethodHandle featureGetter() {
        MethodHandle handle = featureGetter;
        if (handle != null) return handle;
        Class<VegetationFeatures> clazz = VegetationFeatures.class;
        synchronized (VegetationFeatures.class) {
            handle = featureGetter;
            if (handle != null) return handle;
            featureGetter = handle = ReflectionUtil.field(PlacedFeature.class, Holder.class, new String[0]);
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return handle;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static MethodHandle placementsGetter() {
        MethodHandle handle = placementsGetter;
        if (handle != null) return handle;
        Class<VegetationFeatures> clazz = VegetationFeatures.class;
        synchronized (VegetationFeatures.class) {
            handle = placementsGetter;
            if (handle != null) return handle;
            placementsGetter = handle = ReflectionUtil.field(PlacedFeature.class, List.class, new String[0]);
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return handle;
        }
    }

    protected static Holder<ConfiguredFeature<?, ?>> getFeature(PlacedFeature feature) throws Throwable {
        return (Holder<ConfiguredFeature<?, ?>>) VegetationFeatures.featureGetter().invoke(feature);
    }

    protected static List<PlacementModifier> getPlacements(PlacedFeature feature) throws Throwable {
        return (List<PlacementModifier>) VegetationFeatures.placementsGetter().invoke(feature);
    }

    static {
        BIOME_CHECK = Set.of(PlacementModifierType.BIOME_FILTER);
        EXCLUSIONS = Set.of(PlacementModifierType.BIOME_FILTER, PlacementModifierType.COUNT, PlacementModifierType.COUNT_ON_EVERY_LAYER, PlacementModifierType.NOISE_BASED_COUNT, PlacementModifierType.NOISE_THRESHOLD_COUNT);
        TREE_EXCLUSIONS = ImmutableSet.<PlacementModifierType<?>>builder().addAll(EXCLUSIONS).add(PlacementModifierType.IN_SQUARE).build();
        TREE_KEYWORDS = new String[]{"tree", "spruce", "oak", "birch", "pine", "dark_forest_vegetation", "jungle", "mega", "redwood", "palm", "willow", "maple", "cypress", "mahogany"};
        GRASS_KEYWORDS = new String[]{"grass"};
        COVER_KEYWORDS = new String[]{"flower", "lavender", "patch", "fern", "bush", "petal", "clover", "shrub", "flora", "ground", "moss", "vine", "leaf_litter", "vegetation"};
    }
}
