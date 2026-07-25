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
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class VegetationFeatures {
   public static VegetationFeatures NONE = new VegetationFeatures();
   public static final int STAGE = Decoration.VEGETAL_DECORATION.ordinal();
   private static final MethodHandle FEATURE_GETTER = ReflectionUtil.field(PlacedFeature.class, Holder.class);
   private static final MethodHandle PLACEMENTS_GETTER = ReflectionUtil.field(PlacedFeature.class, List.class);
   private static final Set<PlacementModifierType<?>> BIOME_CHECK = Set.of(PlacementModifierType.BIOME_FILTER);
   private static final Set<PlacementModifierType<?>> EXCLUSIONS = Set.of(
      PlacementModifierType.BIOME_FILTER,
      PlacementModifierType.COUNT,
      PlacementModifierType.COUNT_ON_EVERY_LAYER,
      PlacementModifierType.NOISE_BASED_COUNT,
      PlacementModifierType.NOISE_THRESHOLD_COUNT
   );
   private static final Set<PlacementModifierType<?>> TREE_EXCLUSIONS = ImmutableSet.<PlacementModifierType<?>>builder().addAll(EXCLUSIONS).add(PlacementModifierType.IN_SQUARE).build();
   protected static final String[] TREE_KEYWORDS = new String[]{
         "tree", "spruce", "oak", "birch", "pine", "jungle", "redwood", "palm", "willow", "maple",
         "cypress", "mahogany", "jacaranda", "mangrove", "azalea", "dark_forest_vegetation",
         "forest_vegetation", "mega_jungle", "fancy_oak", "super_birch",
         // BOP fungal jungle toadstools / huge mushrooms / fungi — were falling into placeOther only
         "mushroom", "toadstool", "fungus", "glowshroom", "shroom", "huge_", "giant_mushroom"
   };
   protected static final String[] GRASS_KEYWORDS = new String[]{"grass"};
   private final PlacedFeature[] trees;
   private final PlacedFeature[] grass;
   private final PlacedFeature[] other;

   private VegetationFeatures() {
      this.trees = new PlacedFeature[0];
      this.grass = new PlacedFeature[0];
      this.other = new PlacedFeature[0];
   }

   public VegetationFeatures(List<PlacedFeature> trees, List<PlacedFeature> grass, List<PlacedFeature> other) {
      this.trees = trees.toArray(PlacedFeature[]::new);
      this.grass = grass.toArray(PlacedFeature[]::new);
      this.other = other.toArray(PlacedFeature[]::new);
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
      ArrayList<PlacedFeature> arraylist = new ArrayList<>();
      ArrayList<PlacedFeature> arraylist1 = new ArrayList<>();
      ArrayList<PlacedFeature> arraylist2 = new ArrayList<>();
      boolean flag = config != VegetationConfig.NONE;
      List<HolderSet<PlacedFeature>> list = biome.getGenerationSettings().features();
      if (list.size() > STAGE) {
         HolderSet<PlacedFeature> holderset = list.get(STAGE);
         Registry<PlacedFeature> registry = access.registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);

         for (Holder<PlacedFeature> holder : holderset) {
            ResourceLocation resourcelocation = registry.getKey((PlacedFeature)holder.value());
            if (resourcelocation == null) {
               arraylist2.add((PlacedFeature)holder.value());
            } else {
               String s = resourcelocation.getPath();
               if (matches(s, TREE_KEYWORDS)) {
                  arraylist.add(unwrap(holder, TREE_EXCLUSIONS, flag));
               } else if (matches(s, GRASS_KEYWORDS)) {
                  arraylist1.add((PlacedFeature)holder.value());
               } else {
                  arraylist2.add((PlacedFeature)holder.value());
               }
            }
         }
      }

      return new VegetationFeatures(arraylist, arraylist1, arraylist2);
   }

   protected static boolean matches(String name, String[] keywords) {
      for (String s : keywords) {
         if (name.contains(s)) {
            return true;
         }
      }

      return false;
   }

   public static PlacedFeature unwrap(Holder<PlacedFeature> supplier, Set<PlacementModifierType<?>> exclusions, boolean custom) {
      if (!custom) {
         return (PlacedFeature)supplier.value();
      } else {
         try {
            PlacedFeature placedfeature = (PlacedFeature)supplier.value();
            Holder<ConfiguredFeature<?, ?>> holder = getFeature(placedfeature);
            ArrayList<PlacementModifier> arraylist = new ArrayList<>(getPlacements(placedfeature));
            arraylist.removeIf(placement -> exclusions.contains(placement.type()));
            return new PlacedFeature(holder, arraylist);
         } catch (Throwable throwable) {
            throwable.printStackTrace();
            return (PlacedFeature)supplier.value();
         }
      }
   }

   protected static Holder<ConfiguredFeature<?, ?>> getFeature(PlacedFeature feature) throws Throwable {
      return (Holder)FEATURE_GETTER.invokeExact((PlacedFeature)feature);
   }

   protected static List<PlacementModifier> getPlacements(PlacedFeature feature) throws Throwable {
      return (List)PLACEMENTS_GETTER.invokeExact((PlacedFeature)feature);
   }
}
