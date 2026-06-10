package com.terraforged.mod.data;

import com.terraforged.mod.TerraForged;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.NoiseCave;
import com.terraforged.mod.worldgen.cave.CavePlacementType;
import com.terraforged.mod.worldgen.cave.CaveType;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.util.NoiseUtil;

public interface ModCaves {
    public static void register() {
        RandSeed seed = new RandSeed(901246L, 500000);
        TerraForged.register(TerraForged.CAVES, "synapse_high", Factory.synapse(seed.next(), 0.75f, 96, 384));
        TerraForged.register(TerraForged.CAVES, "synapse_mid", Factory.synapse(seed.next(), 1.0f, 0, 256));
        TerraForged.register(TerraForged.CAVES, "synapse_low", Factory.synapse(seed.next(), 1.2f, -32, 128));
        TerraForged.register(TerraForged.CAVES, "mega", Factory.mega(seed.next(), 1.0f, -32, 96));
        TerraForged.register(TerraForged.CAVES, "mega_deep", Factory.mega(seed.next(), 1.2f, -48, 80));
        TerraForged.register(TerraForged.CAVES, "giga", Factory.giga(seed.next(), 1.0f, -40, 112));
    }

    public static class Factory {
        static NoiseCave synapse(int seed, float scale, int minY, int maxY) {
            int elevationScale = NoiseUtil.floor(350.0f * scale);
            int networkScale = NoiseUtil.floor(180.0f * scale);
            int networkWarpScale = NoiseUtil.floor(20.0f * scale);
            int networkWarpStrength = networkWarpScale / 2;
            int floorScale = NoiseUtil.floor(30.0f * scale);
            int size = NoiseUtil.floor(15.0f * scale);
            Module elevation = Source.simplex(++seed, elevationScale, 3).map(0.1, 0.9);
            Module shape = Source.simplexRidge(++seed, networkScale, 3).warp(++seed, networkWarpScale, 1, networkWarpStrength).clamp(0.35, 0.75).map(0.0, 1.0);
            Module floor = Source.simplex(++seed, floorScale, 2).clamp(0.0, 0.15).map(0.0, 1.0);
            return new NoiseCave(seed, CaveType.GLOBAL, CavePlacementType.FULL_REGION, elevation, shape, floor, size, minY, maxY);
        }

        static NoiseCave mega(int seed, float scale, int minY, int maxY) {
            int elevationScale = NoiseUtil.floor(200.0f * scale);
            int networkScale = NoiseUtil.floor(250.0f * scale);
            int floorScale = NoiseUtil.floor(50.0f * scale);
            int size = NoiseUtil.floor(30.0f * scale);
            Module elevation = Source.simplex(++seed, elevationScale, 2).map(0.3, 0.7);
            Module shape = Source.simplex(++seed, networkScale, 3).bias(-0.5).abs().scale(2.0).invert().clamp(0.75, 1.0).map(0.0, 1.0);
            Module floor = Source.simplex(++seed, floorScale, 2).clamp(0.0, 0.3).map(0.0, 1.0);
            return new NoiseCave(seed, CaveType.MEGA, CavePlacementType.FULL_REGION, elevation, shape, floor, size, minY, maxY);
        }

        static NoiseCave giga(int seed, float scale, int minY, int maxY) {
            float geom = 1.35f * scale;
            int elevationScale = NoiseUtil.floor(200.0f * geom);
            int networkScale = NoiseUtil.floor(250.0f * geom);
            int floorScale = NoiseUtil.floor(28.0f * geom);
            int floorDetailScale = NoiseUtil.floor(12.0f * geom);
            int size = NoiseUtil.floor(34.0f * geom);
            Module elevation = Source.simplex(++seed, elevationScale, 3).map(0.22, 0.78);
            Module shape = Source.simplex(++seed, networkScale, 3).bias(-0.5).abs().scale(2.0).invert().clamp(0.72, 1.0).map(0.0, 1.0);
            Module floor = Source.simplex(++seed, floorScale, 3).clamp(0.0, 0.42).map(0.0, 1.0);
            Module floorDetail = Source.simplex(++seed, floorDetailScale, 2).clamp(0.0, 0.28).map(0.0, 1.0);
            floor = floor.add(floorDetail).clamp(0.0, 1.0);
            return new NoiseCave(seed, CaveType.GIGA, CavePlacementType.FULL_REGION, elevation, shape, floor, size, minY, maxY);
        }

        static NoiseCave[] getDefaults() {
            RandSeed seed = new RandSeed(901246L, 500000);
            return new NoiseCave[]{Factory.synapse(seed.next(), 0.75f, 96, 384), Factory.synapse(seed.next(), 1.0f, 0, 256), Factory.synapse(seed.next(), 1.2f, -32, 128), Factory.mega(seed.next(), 1.0f, -32, 96), Factory.mega(seed.next(), 1.2f, -48, 80), Factory.giga(seed.next(), 1.0f, -40, 112)};
        }
    }
}
