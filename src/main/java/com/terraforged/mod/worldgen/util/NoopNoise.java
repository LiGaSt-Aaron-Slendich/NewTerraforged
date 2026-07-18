package com.terraforged.mod.worldgen.util;

import com.google.common.base.Suppliers;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/**
 * Matches official TerraForged-1.18.2-0.3.1-alpha-2: surface uses a zeroed
 * {@link NoiseRouter}, not the vanilla generator's router.
 */
public class NoopNoise {
    private static final DensityFunction ZERO_DF = DensityFunctions.zero();
    private static final PositionalRandomFactory ZERO_RANDOM =
            new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(0L, 0L);

    public static final NoiseRouter ROUTER = new NoiseRouter(
            ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF,
            ZERO_RANDOM, ZERO_RANDOM,
            ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF, ZERO_DF,
            List.of()
    );

    public static final Supplier<DensityFunctions.BeardifierOrMarker> BEARDIFIER = Suppliers.ofInstance(
            new DensityFunctions.BeardifierOrMarker() {
                @Override
                public double compute(DensityFunction.FunctionContext ctx) {
                    return 0.0;
                }

                @Override
                public void fillArray(double[] array, DensityFunction.ContextProvider ctx) {
                    Arrays.fill(array, 0.0);
                }

                @Override
                public double minValue() {
                    return 0.0;
                }

                @Override
                public double maxValue() {
                    return 0.0;
                }
            }
    );
}
