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
 *
 * <p>Highland relief is raised for the 640 column <em>and</em> widened so peaks stay
 * massifs, not knife-edge walls.
 */
public final class TerrainNoiseBuilder {
    /** Amplitude boost for mountains / torridonian (stock TF tuned for ~256-era height). */
    private static final float HIGHLAND_RELIEF_BOOST = 1.40F;
    /**
     * Multiplies highland {@code horizontalScale} before LandForms so wavelengths grow with height.
     * Vertical-only stretch → walls; keep this ≥ relief boost ratio.
     */
    private static final float HIGHLAND_HORIZONTAL_BOOST = 1.85F;

    private TerrainNoiseBuilder() {
    }

    public static TerrainNoise[] build(RegistryAccess access, Settings settings) {
        TerrainSettings terrain = copyTerrain(settings.terrain);
        // Widen before LandForms — mountains/hills bake horizontalScale into Source wavelengths.
        terrain.mountains.horizontalScale = Math.max(0.25F, terrain.mountains.horizontalScale) * HIGHLAND_HORIZONTAL_BOOST;
        terrain.torridonian.horizontalScale = Math.max(0.25F, terrain.torridonian.horizontalScale) * HIGHLAND_HORIZONTAL_BOOST;
        terrain.hills.horizontalScale = Math.max(0.25F, terrain.hills.horizontalScale) * 1.45F;
        terrain.plateau.horizontalScale = Math.max(0.25F, terrain.plateau.horizontalScale) * 1.40F;
        terrain.badlands.horizontalScale = Math.max(0.25F, terrain.badlands.horizontalScale) * 1.50F;

        Seed seed = new RandSeed(9712416L + (long) terrain.general.terrainSeedOffset, 500000);
        LandForms forms = new LandForms(terrain, new Levels(settings.world), Source.ZERO);

        float hillsV = Math.max(0.01F, terrain.hills.verticalScale);
        float dalesV = Math.max(0.01F, terrain.dales.verticalScale);
        float plateauV = Math.max(0.01F, terrain.plateau.verticalScale);
        float torridonV = Math.max(0.01F, terrain.torridonian.verticalScale) * HIGHLAND_RELIEF_BOOST;
        float mountainsV = Math.max(0.01F, terrain.mountains.verticalScale) * HIGHLAND_RELIEF_BOOST;
        float badlandsV = Math.max(0.01F, terrain.badlands.verticalScale);
        // Torridonian / mountains2/3 / badlands ignore settings.horizontalScale internally — freq-widen after.
        // Couple width to height: taller verticalScale → lower frequency (wider massifs).
        float torridonFreq = 1.0F / Math.max(0.25F, terrain.torridonian.horizontalScale);
        float mountainsH = Math.max(0.25F, terrain.mountains.horizontalScale);
        float heightWidth = Math.max(1.0F, mountainsV / (1.0F * HIGHLAND_RELIEF_BOOST));
        float mountainsFreq = 1.0F / (mountainsH * heightWidth);
        float hillsFreq = 1.0F / Math.max(0.25F, terrain.hills.horizontalScale);
        float plateauFreq = 1.0F / Math.max(0.25F, terrain.plateau.horizontalScale);
        float badlandsFreq = 1.0F / Math.max(0.25F, terrain.badlands.horizontalScale);
        // Soften mesa weight further — canyon landform digs mountain trenches at region edges.
        float badlandsW = Math.max(0.0F, terrain.badlands.weight) * 0.55F;

        return new TerrainNoise[]{
                of(access, com.terraforged.engine.world.terrain.TerrainType.FLATS, terrain.steppe.weight, 1.0F, 1.0F, forms::steppe, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.FLATS, terrain.plains.weight, 1.0F, 1.0F, forms::plains, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.hills.weight, hillsV, hillsFreq, forms::hills1, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.hills.weight, hillsV, hillsFreq, forms::hills2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.HILLS, terrain.dales.weight, dalesV, 1.0F, forms::dales, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.PLATEAU, terrain.plateau.weight, plateauV, plateauFreq, forms::plateau, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.BADLANDS, badlandsW, badlandsV, badlandsFreq, forms::badlands, seed),
                of(access, ModTerrainTypes.TORRIDONIAN, terrain.torridonian.weight, torridonV, torridonFreq, forms::torridonian, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, mountainsV, 1.0F, forms::mountains, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, mountainsV, mountainsFreq, forms::mountains2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, mountainsV, mountainsFreq, forms::mountains3, seed),
                dolomite(access, seed, terrain.mountains.weight, mountainsV, mountainsH * heightWidth),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, mountainsV, mountainsFreq, forms::mountains2, seed),
                of(access, com.terraforged.engine.world.terrain.TerrainType.MOUNTAINS, terrain.mountains.weight, mountainsV, mountainsFreq, forms::mountains3, seed)
        };
    }

    private static TerrainNoise of(
            RegistryAccess access,
            Terrain type,
            float weight,
            float verticalScale,
            float freqScale,
            Function<Seed, Module> factory,
            Seed seed
    ) {
        Module module = factory.apply(seed);
        if (freqScale > 0.0F && (freqScale < 0.999F || freqScale > 1.001F)) {
            module = module.freq(freqScale, freqScale);
        }
        if (verticalScale > 1.001F || verticalScale < 0.999F) {
            module = module.scale(verticalScale);
        }
        return new TerrainNoise(typeHolder(access, type), Math.max(0.0F, weight), module);
    }

    private static TerrainNoise dolomite(
            RegistryAccess access, Seed seed, float weight, float verticalScale, float horizontalScale
    ) {
        float freq = 1.0F / Math.max(0.25F, horizontalScale);
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
        Module module = shape.mult(ridge).max(base).warp(seed.next(), 800, 3, 300.0)
                .freq(freq, freq)
                .scale(0.75 * verticalScale);
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
