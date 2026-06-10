package com.terraforged.mod.util.seed;

import com.terraforged.engine.Seed;
import java.util.SplittableRandom;

public class RandSeed
extends Seed {
    private final int range;
    private final SplittableRandom random;

    public RandSeed(long value, int range) {
        super(value);
        this.range = range;
        this.random = new SplittableRandom(value);
    }

    @Override
    public int next() {
        return this.random.nextInt(this.range);
    }
}
