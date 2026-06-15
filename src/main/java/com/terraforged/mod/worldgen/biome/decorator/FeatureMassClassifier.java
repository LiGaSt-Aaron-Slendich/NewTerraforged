package com.terraforged.mod.worldgen.biome.decorator;

import com.terraforged.mod.worldgen.biome.decorator.FeatureMass;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FeatureMassClassifier {
    private static final String[] BLOCKED = new String[]{"geode", "amethyst_geode", "mineshaft", "monster_room", "fossil", "dungeon", "ancient_city", "trial_chambers", "trial_chamber", "dead_leaves", "dead_log", "dead_leaf", "fallen_log", "deadwood", "dead_fungus", "dead_tree"};
    private static final String[] TREE = new String[]{"tree", "spruce", "oak", "birch", "pine", "jungle", "redwood", "palm", "willow", "maple", "cypress", "mahogany", "dark_forest_vegetation", "forest_vegetation", "mangrove", "azalea_tree", "mega_jungle", "jungle_tree", "fancy_oak", "super_birch", "pine_tree"};
    private static final String[] VEGETATION_SPAWN = new String[]{"plain_vegetation", "meadow_vegetation", "flower_default", "flower_warm", "flower_forest", "flower_plains", "flower_swamp", "patch_tall_grass", "patch_grass", "bamboo_vegetation", "savanna_vegetation"};
    private static final String[] CEILING_SCATTER = new String[]{"hanging", "roots", "vine", "icicle", "stalactite", "spore_blossom", "glow_lichen", "lichen", "dripstone", "prismoss", "prisma", "rainbow", "ceiling", "crystal", "cluster", "columns", "frostfire", "amethyst", "bud"};
    private static final String[] DUAL_SURFACE = new String[]{"mushroom", "fungus", "glow_", "moss", "lichen", "patch"};
    private static final String[] CAVE_FLOOR_LARGE = new String[]{"column", "pillar", "large_dripstone", "large_", "huge_", "giant_", "big_", "spire", "spike", "stalagmite", "dripstone_cluster", "mushroom_huge", "huge_mushroom", "big_mushroom", "big_shroom", "mega_shroom", "glowshroom", "shroom_cap", "fungal_colony", "fungal_tree"};
    private static final String[] CAVE_CEILING_LARGE = new String[]{"stalactite", "icicle", "geyser", "thermal", "suspended", "hanging", "dripstone", "spore_blossom", "glow_lichen", "big_shroom", "huge_"};
    private static final String[] MEDIUM = new String[]{"soul_fire", "spore_blossom", "fallen_log", "dead_bush", "boulder", "geyser", "rock"};
    private static final String[] LARGE = new String[]{"column", "pillar", "large_dripstone", "large_", "huge_", "mega_", "giant_", "big_", "spire", "spike", "stalagmite", "dripstone_cluster", "mushroom_huge", "huge_mushroom", "big_mushroom", "big_shroom", "mega_shroom", "glowshroom", "fungal_colony", "chorus"};
    private static final String[] SMALL = new String[]{"shrub", "bush", "fern", "clover", "small_", "minor_", "tuff_", "pebble", "mini_"};
    private static final String[] SCATTER_HINT = new String[]{"mushroom", "fungus", "flower", "grass", "moss", "vine", "lichen", "petal", "flora", "ground", "leaf_litter", "vegetation", "cover", "speckle", "pointed_dripstone", "icicle", "snow_layer", "carpet", "patch", "glow_", "disk", "log_", "cluster"};
    private static final String[] LARGE_BEFORE_SCATTER = new String[]{"big_", "huge_", "large_", "mega_", "giant_", "whole_", "full_"};

    private FeatureMassClassifier() {
    }

    public static boolean isTree(Holder<PlacedFeature> placed) {
        return FeatureMassClassifier.isTree(FeatureMassClassifier.featurePath(placed));
    }

    public static boolean isTree(PlacedFeature placed) {
        return FeatureMassClassifier.isTree(FeatureMassClassifier.featurePath((Holder<PlacedFeature>)Holder.direct(placed)));
    }

    public static boolean isTree(ResourceLocation id) {
        return id != null && FeatureMassClassifier.isTree(id.getPath());
    }

    public static boolean isTree(String path) {
        return path != null && FeatureMassClassifier.containsAny(path, TREE);
    }

    public static boolean spawnsSurfaceVegetation(Holder<PlacedFeature> placed) {
        return FeatureMassClassifier.containsAny(FeatureMassClassifier.featurePath(placed).getPath(), VEGETATION_SPAWN);
    }

    public static boolean isCeilingScatter(Holder<PlacedFeature> placed) {
        return FeatureMassClassifier.containsAny(FeatureMassClassifier.featurePath(placed).getPath(), CEILING_SCATTER);
    }

    public static boolean isDualSurfaceFeature(Holder<PlacedFeature> placed) {
        String path = FeatureMassClassifier.featurePath(placed).getPath();
        return FeatureMassClassifier.containsAny(path, DUAL_SURFACE) && !FeatureMassClassifier.containsAny(path, CAVE_FLOOR_LARGE);
    }

    public static boolean isCaveFloorLarge(Holder<PlacedFeature> placed) {
        String path = FeatureMassClassifier.featurePath(placed).getPath();
        return FeatureMassClassifier.containsAny(path, CAVE_FLOOR_LARGE);
    }

    public static boolean isCaveCeilingFeature(Holder<PlacedFeature> placed) {
        String path = FeatureMassClassifier.featurePath(placed).getPath();
        return FeatureMassClassifier.containsAny(path, CAVE_CEILING_LARGE);
    }

    public static FeatureMass classify(Holder<PlacedFeature> placed) {
        return FeatureMassClassifier.classify(FeatureMassClassifier.featurePath(placed));
    }

    public static FeatureMass classify(PlacedFeature placed) {
        return FeatureMassClassifier.classify(FeatureMassClassifier.featurePath((Holder<PlacedFeature>)Holder.direct(placed)));
    }

    public static FeatureMass classify(ResourceLocation id) {
        if (id == null) {
            return FeatureMass.SCATTER;
        }
        return FeatureMassClassifier.classify(id.getPath());
    }

    public static FeatureMass classify(String path) {
        if (path == null || path.isEmpty()) {
            return FeatureMass.SCATTER;
        }
        if (FeatureMassClassifier.containsAny(path, BLOCKED)) {
            return FeatureMass.BLOCKED;
        }
        if (FeatureMassClassifier.containsAny(path, TREE)) {
            return FeatureMass.LARGE;
        }
        if (FeatureMassClassifier.containsAny(path, CAVE_CEILING_LARGE)) {
            return FeatureMass.MEDIUM;
        }
        if (FeatureMassClassifier.containsAny(path, LARGE)) {
            return FeatureMass.LARGE;
        }
        if (FeatureMassClassifier.containsAny(path, LARGE_BEFORE_SCATTER)) {
            return FeatureMass.LARGE;
        }
        if (FeatureMassClassifier.containsAny(path, SCATTER_HINT)) {
            return FeatureMass.SCATTER;
        }
        if (FeatureMassClassifier.containsAny(path, MEDIUM)) {
            return FeatureMass.MEDIUM;
        }
        if (FeatureMassClassifier.containsAny(path, SMALL)) {
            return FeatureMass.SMALL;
        }
        return FeatureMass.SMALL;
    }

    public static ResourceLocation featurePath(Holder<PlacedFeature> placed) {
        return placed.unwrapKey()
                .map(k -> k.location())
                .orElseGet(() -> placed.value().feature().unwrapKey()
                        .map(k -> k.location())
                        .orElse(null));
    }

    private static boolean containsAny(String path, String[] keywords) {
        for (String keyword : keywords) {
            if (!path.contains(keyword)) continue;
            return true;
        }
        return false;
    }
}
