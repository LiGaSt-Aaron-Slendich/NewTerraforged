package com.terraforged.mod.worldgen.util;

import com.google.common.base.Suppliers;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource.XoroshiroPositionalRandomFactory;

public class NoopNoise {
   public static final NoiseRouter ROUTER = new NoiseRouter(
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      new XoroshiroPositionalRandomFactory(0L, 0L),
      new XoroshiroPositionalRandomFactory(0L, 0L),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      DensityFunctions.zero(),
      List.of()
   );
   public static final Supplier<BeardifierOrMarker> BEARDIFIER = Suppliers.ofInstance(new BeardifierOrMarker() {
      public double compute(FunctionContext ctx) {
         return 0.0;
      }

      public void fillArray(double[] array, ContextProvider ctx) {
         Arrays.fill(array, 0.0);
      }

      public double minValue() {
         return 0.0;
      }

      public double maxValue() {
         return 0.0;
      }
   });
}
