package com.terraforged.mod.worldgen.settings;

import com.terraforged.engine.Seed;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.TerrainSettings;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.engine.world.terrain.LandForms;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.ModTerrainTypes;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
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
 * Builds weighted {@link TerrainNoise} from live engine {@link TerrainSettings}.
 *
 * <p>Tall highland relief / width boosts apply only when EGF Mega Ridges is on;
 * otherwise stock TerraForged landform amplitude is used.
 */
public final class TerrainNoiseBuilder {
    /** Amplitude boost for mountains / torridonian when Mega Ridges EGF is on. */
    private static final float HIGHLAND_RELIEF_BOOST = 1.40F;
    private static final float HIGHLAND_HORIZONTAL_BOOST = 1.85F;

    private TerrainNoiseBuilder() {
    }

    public static TerrainNoise[] build(RegistryAccess access, Settings settings) {
        TerrainSettings terrain = copyTerrain(settings.terrain);
        boolean mega = TFNoiseVariantFlags.megaRidgesEnabled();

        if (mega) {
            // Experimental tall package (pre-EGF defaults) when knobs are still stock.
            terrain.mountains.verticalScale = Math.max(terrain.mountains.verticalScale, 2.0F);
            terrain.mountains.horizontalScale = Math.max(terrain.mountains.horizontalScale, 2.05F);
            terrain.hills.verticalScale = Math.max(terrain.hills.verticalScale, 1.30F);
            terrain.hills.horizontalScale = Math.max(terrain.hills.horizontalScale, 1.40F);
            terrain.plateau.horizontalScale = Math.max(terrain.plateau.horizontalScale, 1.55F);
            terrain.torridonian.verticalScale = Math.max(terrain.torridonian.verticalScale, 1.75F);
            terrain.torridonian.horizontalScale = Math.max(terrain.torridonian.horizontalScale, 1.70F);
            if (terrain.general.globalVerticalScale < 1.15F) {
                terrain.general.globalVerticalScale = 1.20F;
            }
            terrain.mountains.horizontalScale = Math.max(0.25F, terrain.mountains.horizontalScale) * HIGHLAND_HORIZONTAL_BOOST;
            terrain.torridonian.horizontalScale = Math.max(0.25F, terrain.torridonian.horizontalScale) * HIGHLAND_HORIZONTAL_BOOST;
            terrain.hills.horizontalScale = Math.max(0.25F, terrain.hills.horizontalScale) * 1.45F;
            terrain.plateau.horizontalScale = Math.max(0.25F, terrain.plateau.horizontalScale) * 1.40F;
            terrain.badlands.horizontalScale = Math.max(0.25F, terrain.badlands.horizontalScale) * 1.50F;
        }

        Seed seed = new RandSeed(9712416L + (long) terrain.general.terrainSeedOffset, 500000);
        LandForms forms = new LandForms(terrain, new Levels(settings.world), Source.ZERO);

        float hillsV;
        float dalesV = Math.max(0.01F, terrain.dales.verticalScale);
        float plateauV;
        float torridonV;
        float mountainsV;
        float badlandsV = Math.max(0.01F, terrain.badlands.verticalScale);
        float torridonFreq;
        float mountainsFreq;
        float hillsFreq;
        float plateauFreq;
        float badlandsFreq;
        float mountainsH;
        float heightWidth;

        if (mega) {
            hillsV = Math.max(0.01F, terrain.hills.verticalScale);
            plateauV = Math.max(0.01F, terrain.plateau.verticalScale);
            torridonV = Math.max(0.01F, terrain.torridonian.verticalScale) * HIGHLAND_RELIEF_BOOST;
            mountainsV = Math.max(0.01F, terrain.mountains.verticalScale) * HIGHLAND_RELIEF_BOOST;
            torridonFreq = 1.0F / Math.max(0.25F, terrain.torridonian.horizontalScale);
            mountainsH = Math.max(0.25F, terrain.mountains.horizontalScale);
            heightWidth = Math.max(1.0F, mountainsV / (1.0F * HIGHLAND_RELIEF_BOOST));
            mountainsFreq = 1.0F / (mountainsH * heightWidth);
            hillsFreq = 1.0F / Math.max(0.25F, terrain.hills.horizontalScale);
            plateauFreq = 1.0F / Math.max(0.25F, terrain.plateau.horizontalScale);
            badlandsFreq = 1.0F / Math.max(0.25F, terrain.badlands.horizontalScale);
        } else {
            // Stock TerraForged landform amplitude (pre mega-ridge experiments).
            hillsV = 1.0F;
            plateauV = 1.0F;
            torridonV = 1.0F;
            mountainsV = 1.0F;
            torridonFreq = 1.0F;
            mountainsH = 1.0F;
            heightWidth = 1.0F;
            mountainsFreq = 1.0F;
            hillsFreq = 1.0F;
            plateauFreq = 1.0F;
            badlandsFreq = 1.0F;
        }
        float badlandsW = Math.max(0.0F, terrain.badlands.weight) * (mega ? 0.55F : 1.0F);

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

    private static Holder<TerrainType> typeHolder(RegistryAccess access, Terrain terrain) {
        return TerraForged.TERRAIN_TYPES.holder(terrain.getName(), access, () -> TerrainType.of(terrain));
    }

    private static TerrainSettings copyTerrain(TerrainSettings src) {
        TerrainSettings copy = new TerrainSettings();
        DataUtils.fromNBT(DataUtils.toCompactNBT(src), copy);
        return copy;
    }
}
