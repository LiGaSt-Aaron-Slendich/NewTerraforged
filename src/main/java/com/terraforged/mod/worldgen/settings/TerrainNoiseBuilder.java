package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.TerrainSettings;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.LandForms;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.registry.ModRegistry;
import com.terraforged.mod.registry.lazy.LazyHolder;
import com.terraforged.mod.registry.lazy.LazyKey;
import com.terraforged.mod.util.seed.RandSeed;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.mod.worldgen.asset.TerrainNoise;
import com.terraforged.mod.worldgen.asset.TerrainType;
import com.terraforged.noise.Module;
import com.terraforged.noise.Source;
import com.terraforged.noise.domain.Domain;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

/**
 * Builds weighted {@link TerrainNoise} from live engine {@link TerrainSettings}
 * (weights, horizontal/vertical scales, fancy mountains).
 */
public final class TerrainNoiseBuilder {
    private TerrainNoiseBuilder() {
    }

    public static TerrainNoise[] build(RegistryAccess access, Settings settings) {
        TerrainSettings terrain = copyTerrain(settings.terrain);
        Seed seed = new RandSeed(9712416L + (long) terrain.general.terrainSeedOffset, 500000);
        LandForms forms = new LandForms(terrain, new Levels(settings.world), Source.ZERO);

        return new TerrainNoise[]{
                of(access, com.terraforged.engine.world.terrain.TerrainType.FLATS, terrain.steppe.weight, forms::steppe, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.FLATS, terrain.plains.weight, forms::plains, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.hills.weight, forms::hills1, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.hills.weight, forms::hills2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.dales.weight, forms::dales, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, terrain.plateau.weight, forms::plateau, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, terrain.badlands.weight, forms::badlands, seed),
                of(access, ModTerrainTypes.TORRIDONIAN, terrain.torridonian.weight, forms::torridonian, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, forms::mountains, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, forms::mountains2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, forms::mountains3, seed),
                dolomite(access, seed, terrain.mountains.weight),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, forms::mountains2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, forms::mountains3, seed)
        };
    }

    private static TerrainNoise of(RegistryAccess access, Terrain type, float weight, Function<Seed, Module> factory, Seed seed) {
        return new TerrainNoise(typeHolder(access, type), Math.max(0.0F, weight), factory.apply(seed));
    }

    private static TerrainNoise dolomite(RegistryAccess access, Seed seed, float weight) {
        Module detail = Source.simplex(seed.next(), 80, 4).scale(0.1);
        Module shape = Source.simplex(seed.next(), 475, 4).clamp(0.3, 1.0).map(0.0, 1.0).warp(seed.next(), 10, 2, 8.0);
        Module base = shape.pow(2.2).scale(0.65).add(detail);
        Module ridge = Source.build(seed.next(), 400, 5)
                .lacunarity(2.7)
                .gain(0.6)
                .simplexRidge()
                .clamp(0.0, 0.675)
                .map(0.0, 1.0)
                .warp(Domain.warp(Source.SIMPLEX, seed.next(), 40, 5, 30.0))
                .alpha(0.875);
        Module module = shape.mult(ridge).max(base).warp(seed.next(), 800, 3, 300.0).scale(0.75);
        return new TerrainNoise(typeHolder(access, ModTerrainTypes.DOLOMITES), Math.max(0.0F, weight), module);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Holder<TerrainType> typeHolder(RegistryAccess access, Terrain terrain) {
        LazyKey<TerrainType> key = ModRegistry.TERRAIN_TYPE.element(terrain.getName());
        if (access == null) {
            return new LazyHolder<>(TerrainType.of(terrain), key);
        }
        return access.ownedRegistryOrThrow(ModRegistry.TERRAIN_TYPE.get()).getHolderOrThrow(key.get());
    }

    private static TerrainSettings copyTerrain(TerrainSettings src) {
        TerrainSettings copy = new TerrainSettings();
        DataUtils.fromNBT(DataUtils.toCompactNBT(src), copy);
        return copy;
    }
}
