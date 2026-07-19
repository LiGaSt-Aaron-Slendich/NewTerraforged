package com.terraforged.mod.worldgen.cave;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Feature path / kind helpers for cave decor (KEEP). Replaces DROP CaveFeatureClassifier
 * without bringing FeatureMass / mass budgets.
 */
public final class CaveFeatureClassifier {
    private static final String[] TREE = new String[]{"tree", "spruce", "oak", "birch", "pine", "jungle", "redwood", "palm", "willow", "maple", "cypress", "mahogany", "dark_forest_vegetation", "forest_vegetation", "mangrove", "azalea_tree", "mega_jungle", "jungle_tree", "fancy_oak", "super_birch", "pine_tree"};
    private static final String[] VEGETATION_SPAWN = new String[]{"plain_vegetation", "meadow_vegetation", "flower_default", "flower_warm", "flower_forest", "flower_plains", "flower_swamp", "patch_tall_grass", "patch_grass", "bamboo_vegetation", "savanna_vegetation"};
    private static final String[] CEILING_SCATTER = new String[]{"hanging", "roots", "vine", "icicle", "stalactite", "spore_blossom", "glow_lichen", "lichen", "dripstone", "prismoss", "prisma", "rainbow", "ceiling", "crystal", "cluster", "columns", "frostfire", "amethyst", "bud"};
    private static final String[] DUAL_SURFACE = new String[]{"mushroom", "fungus", "glow_", "moss", "lichen", "patch"};
    private static final String[] CAVE_FLOOR_LARGE = new String[]{"column", "pillar", "large_dripstone", "large_", "huge_", "giant_", "big_", "spire", "spike", "stalagmite", "dripstone_cluster", "mushroom_huge", "huge_mushroom", "big_mushroom", "big_shroom", "mega_shroom", "glowshroom", "shroom_cap", "fungal_colony", "fungal_tree"};
    private static final String[] BLOCKED = new String[]{"geode", "amethyst_geode", "mineshaft", "monster_room", "fossil", "dungeon", "ancient_city", "trial_chambers", "trial_chamber", "dead_leaves", "dead_log", "dead_leaf", "fallen_log", "deadwood", "dead_fungus", "dead_tree"};
    private static final String[] MEDIUM = new String[]{"soul_fire", "spore_blossom", "fallen_log", "dead_bush", "boulder", "geyser", "rock"};
    private static final String[] LARGE = new String[]{"column", "pillar", "large_dripstone", "large_", "huge_", "mega_", "giant_", "big_", "spire", "spike", "stalagmite", "dripstone_cluster", "mushroom_huge", "huge_mushroom", "big_mushroom", "big_shroom", "mega_shroom", "glowshroom", "fungal_colony", "chorus"};
    private static final String[] SMALL = new String[]{"shrub", "bush", "fern", "clover", "small_", "minor_", "tuff_", "pebble", "mini_"};
    private static final String[] SCATTER_HINT = new String[]{"mushroom", "fungus", "flower", "grass", "moss", "vine", "lichen", "petal", "flora", "ground", "leaf_litter", "vegetation", "cover", "speckle", "pointed_dripstone", "icicle", "snow_layer", "carpet", "patch", "glow_", "disk", "log_", "cluster"};
    private static final String[] LARGE_BEFORE_SCATTER = new String[]{"big_", "huge_", "large_", "mega_", "giant_", "whole_", "full_"};
    private static final String[] CAVE_CEILING_LARGE = new String[]{"stalactite", "icicle", "geyser", "thermal", "suspended", "hanging", "dripstone", "spore_blossom", "glow_lichen", "big_shroom", "huge_"};

    private CaveFeatureClassifier() {
    }

    public static boolean isTree(Holder<PlacedFeature> placed) {
        return isTree(featurePath(placed));
    }

    public static boolean isTree(PlacedFeature placed) {
        return isTree(featurePath(Holder.direct(placed)));
    }

    public static boolean isTree(ResourceLocation id) {
        return id != null && isTree(id.getPath());
    }

    public static boolean isTree(String path) {
        return path != null && containsAny(path, TREE);
    }

    public static boolean spawnsSurfaceVegetation(Holder<PlacedFeature> placed) {
        ResourceLocation id = featurePath(placed);
        return id != null && containsAny(id.getPath(), VEGETATION_SPAWN);
    }

    public static boolean isCeilingScatter(Holder<PlacedFeature> placed) {
        ResourceLocation id = featurePath(placed);
        return id != null && containsAny(id.getPath(), CEILING_SCATTER);
    }

    public static boolean isDualSurfaceFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = featurePath(placed);
        if (id == null) return false;
        String path = id.getPath();
        return containsAny(path, DUAL_SURFACE) && !containsAny(path, CAVE_FLOOR_LARGE);
    }

    public static boolean isCaveFloorLarge(Holder<PlacedFeature> placed) {
        ResourceLocation id = featurePath(placed);
        return id != null && containsAny(id.getPath(), CAVE_FLOOR_LARGE);
    }

    public static boolean isCaveCeilingFeature(Holder<PlacedFeature> placed) {
        ResourceLocation id = featurePath(placed);
        return id != null && containsAny(id.getPath(), CAVE_CEILING_LARGE);
    }

    public static ResourceLocation featurePath(Holder<PlacedFeature> placed) {
        if (placed == null) return null;
        return placed.unwrapKey()
                .map(k -> k.location())
                .orElseGet(() -> placed.value().feature().unwrapKey()
                        .map(k -> k.location())
                        .orElse(null));
    }


    public static CaveFeatureMass classify(Holder<PlacedFeature> placed) {
        return classify(featurePath(placed));
    }

    public static CaveFeatureMass classify(ResourceLocation id) {
        if (id == null) return CaveFeatureMass.SCATTER;
        return classify(id.getPath());
    }

    public static CaveFeatureMass classify(String path) {
        if (path == null || path.isEmpty()) return CaveFeatureMass.SCATTER;
        if (containsAny(path, BLOCKED)) return CaveFeatureMass.BLOCKED;
        if (containsAny(path, TREE)) return CaveFeatureMass.LARGE;
        if (containsAny(path, CAVE_CEILING_LARGE)) return CaveFeatureMass.MEDIUM;
        if (containsAny(path, LARGE)) return CaveFeatureMass.LARGE;
        if (containsAny(path, LARGE_BEFORE_SCATTER)) return CaveFeatureMass.LARGE;
        if (containsAny(path, SCATTER_HINT)) return CaveFeatureMass.SCATTER;
        if (containsAny(path, MEDIUM)) return CaveFeatureMass.MEDIUM;
        if (containsAny(path, SMALL)) return CaveFeatureMass.SMALL;
        return CaveFeatureMass.SMALL;
    }
    private static boolean containsAny(String path, String[] keywords) {
        for (String keyword : keywords) {
            if (path.contains(keyword)) return true;
        }
        return false;
    }
}
