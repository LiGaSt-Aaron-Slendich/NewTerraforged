package com.terraforged.mod.worldgen.util;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class NoopNoise {
    public static final DensityFunction ZERO = new DensityFunction(){

        public double compute(DensityFunction.FunctionContext ctx) {
            return 0.0;
        }

        public void fillArray(double[] array, DensityFunction.ContextProvider ctx) {
            Arrays.fill(array, 0.0);
        }

        public double minValue() {
            return 0.0;
        }

        public double maxValue() {
            return 0.0;
        }

        public Codec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }

        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return this;
        }
    };
    public static final DensityFunctions.BeardifierOrMarker BEARDIFIER = new DensityFunctions.BeardifierOrMarker(){

        public double compute(DensityFunction.FunctionContext ctx) {
            return 0.0;
        }

        public void fillArray(double[] array, DensityFunction.ContextProvider ctx) {
            Arrays.fill(array, 0.0);
        }

        public double minValue() {
            return 0.0;
        }

        public double maxValue() {
            return 0.0;
        }
    };
}
