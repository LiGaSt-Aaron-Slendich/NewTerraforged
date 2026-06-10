package com.terraforged.mod.data;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.TerrainSettings;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.LandForms;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

public interface ModTerrains {
    public static void register() {
        Seed seed = Factory.createSeed();
        TerraForged.register(TerraForged.TERRAINS, "steppe", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 1.5f, LandForms::steppe));
        TerraForged.register(TerraForged.TERRAINS, "plains", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 2.5f, LandForms::plains));
        TerraForged.register(TerraForged.TERRAINS, "hills_1", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0f, LandForms::hills1));
        TerraForged.register(TerraForged.TERRAINS, "hills_2", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0f, LandForms::hills2));
        TerraForged.register(TerraForged.TERRAINS, "dales", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 1.5f, LandForms::dales));
        TerraForged.register(TerraForged.TERRAINS, "plateau", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, 2.0f, LandForms::plateau));
        TerraForged.register(TerraForged.TERRAINS, "badlands", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, 1.75f, LandForms::badlands));
        TerraForged.register(TerraForged.TERRAINS, "torridonian", Factory.create(null, seed, ModTerrainTypes.TORRIDONIAN, 2.5f, LandForms::torridonian));
        TerraForged.register(TerraForged.TERRAINS, "mountains_1", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains));
        TerraForged.register(TerraForged.TERRAINS, "mountains_2", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains2));
        TerraForged.register(TerraForged.TERRAINS, "mountains_3", Factory.create(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains3));
        TerraForged.register(TerraForged.TERRAINS, "dolomites", Factory.createDolomite(null, seed, ModTerrainTypes.DOLOMITES, 1.25f));
        TerraForged.register(TerraForged.TERRAINS, "mountains_ridge_1", Factory.createNF(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains2));
        TerraForged.register(TerraForged.TERRAINS, "mountains_ridge_2", Factory.createNF(null, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains3));
    }

    public static class Factory {
        static final LandForms LAND_FORMS = new LandForms(Factory.settings(), new Levels(63, 255), Source.ZERO);
        static final LandForms LAND_FORMS_NF = new LandForms(Factory.nonFancy(), new Levels(63, 255), Source.ZERO);

        static Seed createSeed() {
            return new RandSeed(9712416L, 500000);
        }

        static TerrainNoise create(RegistryAccess access, Seed seed, Terrain type, float weight, BiFunction<LandForms, Seed, Module> factory) {
            return new TerrainNoise(Factory.getType(access, type), weight, factory.apply(LAND_FORMS, seed));
        }

        static TerrainNoise createNF(RegistryAccess access, Seed seed, Terrain type, float weight, BiFunction<LandForms, Seed, Module> factory) {
            return new TerrainNoise(Factory.getType(access, type), weight, factory.apply(LAND_FORMS_NF, seed));
        }

        static TerrainNoise createDolomite(RegistryAccess access, Seed seed, Terrain type, float weight) {
            Module base = Source.simplex(seed.next(), 80, 4).scale(0.1);
            Module shape = Source.simplex(seed.next(), 475, 4).clamp(0.3, 1.0).map(0.0, 1.0).warp(seed.next(), 10, 2, 8.0);
            Module slopes = shape.pow(2.2).scale(0.65).add(base);
            Module peaks = Source.build(seed.next(), 400, 5).lacunarity(2.7).gain(0.6).simplexRidge().clamp(0.0, 0.675).map(0.0, 1.0).warp(Domain.warp(Source.SIMPLEX, seed.next(), 40, 5, 30.0)).alpha(0.875);
            Module noise = shape.mult(peaks).max(slopes).warp(seed.next(), 800, 3, 300.0).scale(0.75);
            return new TerrainNoise(Factory.getType(access, type), weight, noise);
        }

        static Holder<TerrainType> getType(RegistryAccess access, Terrain terrain) {
            return TerraForged.TERRAIN_TYPES.holder(terrain.getName(), access, () -> TerrainType.of(terrain));
        }

        static TerrainSettings settings() {
            TerrainSettings settings = new TerrainSettings();
            settings.general.globalVerticalScale = 1.0f;
            return settings;
        }

        static TerrainSettings nonFancy() {
            TerrainSettings settings = Factory.settings();
            settings.general.fancyMountains = false;
            return settings;
        }

        public static TerrainNoise[] getDefault(RegistryAccess access) {
            Seed seed = Factory.createSeed();
            return new TerrainNoise[]{Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 1.5f, LandForms::steppe), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.FLATS, 2.5f, LandForms::plains), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0f, LandForms::hills1), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 2.0f, LandForms::hills2), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.HILLS, 1.5f, LandForms::dales), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, 2.0f, LandForms::plateau), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, 1.75f, LandForms::badlands), Factory.create(access, seed, ModTerrainTypes.TORRIDONIAN, 2.5f, LandForms::torridonian), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains2), Factory.create(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains3), Factory.createDolomite(access, seed, ModTerrainTypes.DOLOMITES, 1.25f), Factory.createNF(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains2), Factory.createNF(access, seed, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, 1.25f, LandForms::mountains3)};
        }
    }

    public static interface Weights {
        public static final float STEPPE = 1.5f;
        public static final float PLAINS = 2.5f;
        public static final float HILLS = 2.0f;
        public static final float DALES = 1.5f;
        public static final float PLATEAU = 2.0f;
        public static final float BADLANDS = 1.75f;
        public static final float TORRIDONIAN = 2.5f;
        public static final float MOUNTAINS = 1.25f;
    }
}
