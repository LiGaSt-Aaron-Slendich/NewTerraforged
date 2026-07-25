package com.terraforged.mod.worldgen.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.world.continent.SpawnType;
import com.terraforged.mod.util.serialization.DataUtils;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.nbt.CompoundTag;

/**
 * Codec-friendly snapshot of engine {@link Settings} used by NoiseGenerator.
 * Persisted on {@code Generator} as {@code generator_settings}.
 * Typed slices cover the common knobs; {@code engine_settings} keeps the full GUI tree
 * (per-type terrain weights/scales, branch rivers, lakes, wetlands, smoothing, …).
 */
public final class GeneratorSettings {
    public static final GeneratorSettings DEFAULT = factoryDefaults();

    private static final Codec<WorldSlice> WORLD_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.INT.optionalFieldOf("continent_scale", 3000).forGetter(s -> s.continentScale),
                            Codec.FLOAT.optionalFieldOf("deep_ocean", 0.1F).forGetter(s -> s.deepOcean),
                            Codec.FLOAT.optionalFieldOf("shallow_ocean", 0.25F).forGetter(s -> s.shallowOcean),
                            Codec.FLOAT.optionalFieldOf("beach", 0.327F).forGetter(s -> s.beach),
                            Codec.FLOAT.optionalFieldOf("coast", 0.448F).forGetter(s -> s.coast),
                            Codec.FLOAT.optionalFieldOf("inland", 0.502F).forGetter(s -> s.inland),
                            Codec.INT.optionalFieldOf("sea_level", 62).forGetter(s -> s.seaLevel),
                            Codec.INT.optionalFieldOf("world_height", 256).forGetter(s -> s.worldHeight),
                            Codec.STRING.optionalFieldOf("spawn_type", SpawnType.CONTINENT_CENTER.name()).forGetter(s -> s.spawnType)
                    )
                    .apply(i, WorldSlice::new)
    );

    private static final Codec<ClimateSlice> CLIMATE_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.INT.optionalFieldOf("biome_size", 220).forGetter(s -> s.biomeSize),
                            Codec.INT.optionalFieldOf("temperature_falloff", 2).forGetter(s -> s.temperatureFalloff),
                            Codec.FLOAT.optionalFieldOf("temperature_bias", 0.1F).forGetter(s -> s.temperatureBias),
                            Codec.INT.optionalFieldOf("temperature_scale", 100).forGetter(s -> s.temperatureScale),
                            Codec.INT.optionalFieldOf("moisture_falloff", 1).forGetter(s -> s.moistureFalloff),
                            Codec.FLOAT.optionalFieldOf("moisture_bias", -0.05F).forGetter(s -> s.moistureBias),
                            Codec.INT.optionalFieldOf("moisture_scale", 100).forGetter(s -> s.moistureScale)
                    )
                    .apply(i, ClimateSlice::new)
    );

    private static final Codec<TerrainSlice> TERRAIN_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.INT.optionalFieldOf("terrain_region_size", 1200).forGetter(s -> s.terrainRegionSize),
                            Codec.FLOAT.optionalFieldOf("global_vertical_scale", 0.98F).forGetter(s -> s.globalVerticalScale),
                            Codec.FLOAT.optionalFieldOf("global_horizontal_scale", 1.0F).forGetter(s -> s.globalHorizontalScale),
                            Codec.BOOL.optionalFieldOf("fancy_mountains", true).forGetter(s -> s.fancyMountains)
                    )
                    .apply(i, TerrainSlice::new)
    );

    private static final Codec<RiversSlice> RIVERS_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.INT.optionalFieldOf("river_count", 8).forGetter(s -> s.riverCount),
                            Codec.INT.optionalFieldOf("main_river_bed_depth", 5).forGetter(s -> s.mainRiverBedDepth),
                            Codec.INT.optionalFieldOf("main_river_bed_width", 8).forGetter(s -> s.mainRiverBedWidth),
                            Codec.INT.optionalFieldOf("main_river_bank_width", 20).forGetter(s -> s.mainRiverBankWidth)
                    )
                    .apply(i, RiversSlice::new)
    );

    private static final Codec<FiltersSlice> FILTERS_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.INT.optionalFieldOf("erosion_droplets", 350).forGetter(s -> s.erosionDroplets),
                            Codec.INT.optionalFieldOf("erosion_lifetime", 12).forGetter(s -> s.erosionLifetime),
                            Codec.FLOAT.optionalFieldOf("erosion_rate", 0.5F).forGetter(s -> s.erosionRate),
                            Codec.FLOAT.optionalFieldOf("deposit_rate", 0.5F).forGetter(s -> s.depositRate)
                    )
                    .apply(i, FiltersSlice::new)
    );

    public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            WORLD_CODEC.optionalFieldOf("world", DEFAULT.world()).forGetter(GeneratorSettings::world),
                            CLIMATE_CODEC.optionalFieldOf("climate", DEFAULT.climate()).forGetter(GeneratorSettings::climate),
                            TERRAIN_CODEC.optionalFieldOf("terrain", DEFAULT.terrain()).forGetter(GeneratorSettings::terrain),
                            RIVERS_CODEC.optionalFieldOf("rivers", DEFAULT.rivers()).forGetter(GeneratorSettings::rivers),
                            FILTERS_CODEC.optionalFieldOf("filters", DEFAULT.filters()).forGetter(GeneratorSettings::filters),
                            CompoundTag.CODEC.optionalFieldOf("engine_settings", new CompoundTag()).forGetter(g -> g.engineSettings),
                            Codec.LONG.optionalFieldOf("seed", -1L).forGetter(g -> g.seed)
                    )
                    .apply(instance, GeneratorSettings::fromCodecParts)
    );

    public final int continentScale;
    public final float deepOcean;
    public final float shallowOcean;
    public final float beach;
    public final float coast;
    public final float inland;
    public final int seaLevel;
    public final int worldHeight;
    public final String spawnType;
    public final int biomeSize;
    public final int temperatureFalloff;
    public final float temperatureBias;
    public final int temperatureScale;
    public final int moistureFalloff;
    public final float moistureBias;
    public final int moistureScale;
    public final int terrainRegionSize;
    public final float globalVerticalScale;
    public final float globalHorizontalScale;
    public final boolean fancyMountains;
    public final int riverCount;
    public final int mainRiverBedDepth;
    public final int mainRiverBedWidth;
    public final int mainRiverBankWidth;
    public final int erosionDroplets;
    public final int erosionLifetime;
    public final float erosionRate;
    public final float depositRate;
    /** Full engine Settings tree from the customize GUI (may be empty for legacy worlds). */
    public final CompoundTag engineSettings;
    /** Preset world seed; {@code -1} means unset (keep draft/create-world seed). */
    public final long seed;

    public GeneratorSettings(WorldSlice world, ClimateSlice climate, TerrainSlice terrain, RiversSlice rivers, FiltersSlice filters) {
        this(world, climate, terrain, rivers, filters, new CompoundTag(), -1L);
    }

    public GeneratorSettings(
            WorldSlice world,
            ClimateSlice climate,
            TerrainSlice terrain,
            RiversSlice rivers,
            FiltersSlice filters,
            CompoundTag engineSettings
    ) {
        this(world, climate, terrain, rivers, filters, engineSettings, -1L);
    }

    public GeneratorSettings(
            WorldSlice world,
            ClimateSlice climate,
            TerrainSlice terrain,
            RiversSlice rivers,
            FiltersSlice filters,
            CompoundTag engineSettings,
            long seed
    ) {
        this(
                world.continentScale,
                world.deepOcean,
                world.shallowOcean,
                world.beach,
                world.coast,
                world.inland,
                world.seaLevel,
                world.worldHeight,
                world.spawnType,
                climate.biomeSize,
                climate.temperatureFalloff,
                climate.temperatureBias,
                climate.temperatureScale,
                climate.moistureFalloff,
                climate.moistureBias,
                climate.moistureScale,
                terrain.terrainRegionSize,
                terrain.globalVerticalScale,
                terrain.globalHorizontalScale,
                terrain.fancyMountains,
                rivers.riverCount,
                rivers.mainRiverBedDepth,
                rivers.mainRiverBedWidth,
                rivers.mainRiverBankWidth,
                filters.erosionDroplets,
                filters.erosionLifetime,
                filters.erosionRate,
                filters.depositRate,
                engineSettings,
                seed
        );
    }

    private static GeneratorSettings fromCodecParts(
            WorldSlice world,
            ClimateSlice climate,
            TerrainSlice terrain,
            RiversSlice rivers,
            FiltersSlice filters,
            CompoundTag engineSettings,
            long seed
    ) {
        return new GeneratorSettings(world, climate, terrain, rivers, filters, engineSettings, seed);
    }

    public GeneratorSettings(
            int continentScale,
            float deepOcean,
            float shallowOcean,
            float beach,
            float coast,
            float inland,
            int seaLevel,
            int worldHeight,
            String spawnType,
            int biomeSize,
            int temperatureFalloff,
            float temperatureBias,
            int temperatureScale,
            int moistureFalloff,
            float moistureBias,
            int moistureScale,
            int terrainRegionSize,
            float globalVerticalScale,
            float globalHorizontalScale,
            boolean fancyMountains,
            int riverCount,
            int mainRiverBedDepth,
            int mainRiverBedWidth,
            int mainRiverBankWidth,
            int erosionDroplets,
            int erosionLifetime,
            float erosionRate,
            float depositRate
    ) {
        this(
                continentScale,
                deepOcean,
                shallowOcean,
                beach,
                coast,
                inland,
                seaLevel,
                worldHeight,
                spawnType,
                biomeSize,
                temperatureFalloff,
                temperatureBias,
                temperatureScale,
                moistureFalloff,
                moistureBias,
                moistureScale,
                terrainRegionSize,
                globalVerticalScale,
                globalHorizontalScale,
                fancyMountains,
                riverCount,
                mainRiverBedDepth,
                mainRiverBedWidth,
                mainRiverBankWidth,
                erosionDroplets,
                erosionLifetime,
                erosionRate,
                depositRate,
                new CompoundTag(),
                -1L
        );
    }

    public GeneratorSettings(
            int continentScale,
            float deepOcean,
            float shallowOcean,
            float beach,
            float coast,
            float inland,
            int seaLevel,
            int worldHeight,
            String spawnType,
            int biomeSize,
            int temperatureFalloff,
            float temperatureBias,
            int temperatureScale,
            int moistureFalloff,
            float moistureBias,
            int moistureScale,
            int terrainRegionSize,
            float globalVerticalScale,
            float globalHorizontalScale,
            boolean fancyMountains,
            int riverCount,
            int mainRiverBedDepth,
            int mainRiverBedWidth,
            int mainRiverBankWidth,
            int erosionDroplets,
            int erosionLifetime,
            float erosionRate,
            float depositRate,
            CompoundTag engineSettings
    ) {
        this(
                continentScale,
                deepOcean,
                shallowOcean,
                beach,
                coast,
                inland,
                seaLevel,
                worldHeight,
                spawnType,
                biomeSize,
                temperatureFalloff,
                temperatureBias,
                temperatureScale,
                moistureFalloff,
                moistureBias,
                moistureScale,
                terrainRegionSize,
                globalVerticalScale,
                globalHorizontalScale,
                fancyMountains,
                riverCount,
                mainRiverBedDepth,
                mainRiverBedWidth,
                mainRiverBankWidth,
                erosionDroplets,
                erosionLifetime,
                erosionRate,
                depositRate,
                engineSettings,
                -1L
        );
    }

    public GeneratorSettings(
            int continentScale,
            float deepOcean,
            float shallowOcean,
            float beach,
            float coast,
            float inland,
            int seaLevel,
            int worldHeight,
            String spawnType,
            int biomeSize,
            int temperatureFalloff,
            float temperatureBias,
            int temperatureScale,
            int moistureFalloff,
            float moistureBias,
            int moistureScale,
            int terrainRegionSize,
            float globalVerticalScale,
            float globalHorizontalScale,
            boolean fancyMountains,
            int riverCount,
            int mainRiverBedDepth,
            int mainRiverBedWidth,
            int mainRiverBankWidth,
            int erosionDroplets,
            int erosionLifetime,
            float erosionRate,
            float depositRate,
            CompoundTag engineSettings,
            long seed
    ) {
        this.continentScale = continentScale;
        this.deepOcean = deepOcean;
        this.shallowOcean = shallowOcean;
        this.beach = beach;
        this.coast = coast;
        this.inland = inland;
        this.seaLevel = seaLevel;
        this.worldHeight = worldHeight;
        this.spawnType = spawnType;
        this.biomeSize = biomeSize;
        this.temperatureFalloff = temperatureFalloff;
        this.temperatureBias = temperatureBias;
        this.temperatureScale = temperatureScale;
        this.moistureFalloff = moistureFalloff;
        this.moistureBias = moistureBias;
        this.moistureScale = moistureScale;
        this.terrainRegionSize = terrainRegionSize;
        this.globalVerticalScale = globalVerticalScale;
        this.globalHorizontalScale = globalHorizontalScale;
        this.fancyMountains = fancyMountains;
        this.riverCount = riverCount;
        this.mainRiverBedDepth = mainRiverBedDepth;
        this.mainRiverBedWidth = mainRiverBedWidth;
        this.mainRiverBankWidth = mainRiverBankWidth;
        this.erosionDroplets = erosionDroplets;
        this.erosionLifetime = erosionLifetime;
        this.erosionRate = erosionRate;
        this.depositRate = depositRate;
        this.engineSettings = engineSettings == null ? new CompoundTag() : engineSettings.copy();
        this.seed = seed;
    }

    public WorldSlice world() {
        return new WorldSlice(continentScale, deepOcean, shallowOcean, beach, coast, inland, seaLevel, worldHeight, spawnType);
    }

    public ClimateSlice climate() {
        return new ClimateSlice(biomeSize, temperatureFalloff, temperatureBias, temperatureScale, moistureFalloff, moistureBias, moistureScale);
    }

    public TerrainSlice terrain() {
        return new TerrainSlice(terrainRegionSize, globalVerticalScale, globalHorizontalScale, fancyMountains);
    }

    public RiversSlice rivers() {
        return new RiversSlice(riverCount, mainRiverBedDepth, mainRiverBedWidth, mainRiverBankWidth);
    }

    public FiltersSlice filters() {
        return new FiltersSlice(erosionDroplets, erosionLifetime, erosionRate, depositRate);
    }

    /** Engine-aligned defaults (continentScale 3000) + NewTF erosion. */
    public static GeneratorSettings factoryDefaults() {
        TerrainLevels levels = TerrainLevels.forCurrentEgf();
        Settings engine = new Settings();
        engine.world.seed = 0L;
        engine.world.properties.seaLevel = levels.seaLevel;
        engine.world.properties.worldHeight = levels.maxY;
        // Keep classic TF continent feel: large landmasses, not island soup.
        engine.world.continent.continentScale = WorldSettings.DEFAULT_CONTINENT_SCALE;
        engine.filters.erosion.dropletsPerChunk = 250;
        engine.terrain.volcano.weight = 0.85F;
        engine.terrain.general.globalVerticalScale = 1.0F;
        engine.terrain.general.fancyMountains = false;
        // Bias land toward plains / wetlands; trim rocky hill/mountain spam.
        engine.terrain.plains.weight = 3.2F;
        engine.terrain.steppe.weight = 1.2F;
        engine.terrain.dales.weight = 2.6F;
        engine.terrain.hills.weight = 1.6F;
        engine.terrain.torridonian.weight = 1.2F;
        engine.terrain.mountains.weight = 1.8F;
        // Stock TF landform scales — tall 640-column boosts only when EGF Mega Ridges is on.
        engine.terrain.mountains.verticalScale = 1.0F;
        engine.terrain.mountains.horizontalScale = 1.0F;
        engine.terrain.hills.verticalScale = 1.0F;
        engine.terrain.hills.horizontalScale = 1.0F;
        engine.terrain.plateau.horizontalScale = 1.0F;
        engine.terrain.plateau.verticalScale = 1.0F;
        engine.terrain.torridonian.verticalScale = 1.0F;
        engine.terrain.torridonian.horizontalScale = 1.0F;
        engine.terrain.badlands.weight = 0.45F;
        engine.terrain.badlands.horizontalScale = 1.0F;
        engine.terrain.badlands.verticalScale = 1.0F;
        return fromEngine(engine);
    }

    /** Islands-only world type defaults (DS:Shipwrecked). */
    public static GeneratorSettings shipwreckedDefaults() {
        TerrainLevels levels = TerrainLevels.DEFAULT.get();
        Settings engine = factoryDefaults().toEngine(0L, levels);
        engine.world.properties.worldStyle = WorldSettings.WorldStyle.SHIPWRECKED;
        engine.world.properties.spawnType = SpawnType.WORLD_ORIGIN;
        engine.world.continent.guaranteedContinentsEnabled = false;
        engine.world.continent.continentSkipping = 1.0F;
        engine.world.continent.continentScale = 1000;
        engine.world.continent.continentSizeVariance = 0.5F;
        engine.world.continent.continentNoiseGain = 0.36F;
        engine.world.islands.archipelago = true;
        engine.world.islands.archipelagoChance = 0.55F;
        engine.world.islands.scatteredArchipelago = true;
        engine.world.islands.scatteredArchipelagoChance = 0.65F;
        engine.world.islands.volcanicIslandsChance = 0.35F;
        engine.world.islands.coastalIslandsChance = 0.45F;
        ContinentShapeWiring.bakeIslandsIntoEngine(engine);
        return fromEngine(engine);
    }

    public static GeneratorSettings fromEngine(Settings settings) {
        int tempScale = com.terraforged.mod.worldgen.noise.climate.ClimateScaleResolver.migratePercent(
                settings.climate.temperature.scale);
        int moistScale = com.terraforged.mod.worldgen.noise.climate.ClimateScaleResolver.migratePercent(
                settings.climate.moisture.scale);
        settings.climate.temperature.scale = tempScale;
        settings.climate.moisture.scale = moistScale;
        return new GeneratorSettings(
                settings.world.continent.continentScale,
                settings.world.controlPoints.deepOcean,
                settings.world.controlPoints.shallowOcean,
                settings.world.controlPoints.beach,
                settings.world.controlPoints.coast,
                settings.world.controlPoints.inland,
                settings.world.properties.seaLevel,
                settings.world.properties.worldHeight,
                settings.world.properties.spawnType.name(),
                settings.climate.biomeShape.biomeSize,
                settings.climate.temperature.falloff,
                settings.climate.temperature.bias,
                tempScale,
                settings.climate.moisture.falloff,
                settings.climate.moisture.bias,
                moistScale,
                settings.terrain.general.terrainRegionSize,
                settings.terrain.general.globalVerticalScale,
                settings.terrain.general.globalHorizontalScale,
                settings.terrain.general.fancyMountains,
                settings.rivers.riverCount,
                settings.rivers.mainRivers.bedDepth,
                settings.rivers.mainRivers.bedWidth,
                settings.rivers.mainRivers.bankWidth,
                settings.filters.erosion.dropletsPerChunk,
                settings.filters.erosion.dropletLifetime,
                settings.filters.erosion.erosionRate,
                settings.filters.erosion.depositeRate,
                DataUtils.toCompactNBT(settings),
                settings.world.seed
        );
    }

    public Settings toEngine(long seed, TerrainLevels levels) {
        Settings settings = new Settings();
        long resolved = this.seed != -1L ? this.seed : seed;
        applyTo(settings, resolved, levels);
        return settings;
    }

    public void applyTo(Settings settings, long seed, TerrainLevels levels) {
        if (!this.engineSettings.isEmpty()) {
            DataUtils.fromNBT(this.engineSettings, settings);
            ContinentGuarantee.migrateFromLegacyIslands(settings.world, this.engineSettings);
        } else {
            applyTypedFields(settings);
        }
        long resolved = this.seed != -1L ? this.seed : seed;
        settings.world.seed = resolved;
        ContinentGuarantee.syncIslandsMirror(settings.world);
        if (settings.terrain.volcano.weight > 1.5F) {
            settings.terrain.volcano.weight = 0.85F;
        }
        settings.climate.temperature.scale = com.terraforged.mod.worldgen.noise.climate.ClimateScaleResolver.migratePercent(
                settings.climate.temperature.scale);
        settings.climate.moisture.scale = com.terraforged.mod.worldgen.noise.climate.ClimateScaleResolver.migratePercent(
                settings.climate.moisture.scale);
        settings.world.properties.seaLevel = levels != null ? levels.seaLevel : this.seaLevel;
        settings.world.properties.worldHeight = levels != null ? levels.maxY : this.worldHeight;
    }

    private void applyTypedFields(Settings settings) {
        settings.world.continent.continentScale = this.continentScale;
        settings.world.controlPoints.deepOcean = this.deepOcean;
        settings.world.controlPoints.shallowOcean = this.shallowOcean;
        settings.world.controlPoints.beach = this.beach;
        settings.world.controlPoints.coast = this.coast;
        settings.world.controlPoints.inland = this.inland;
        settings.world.properties.seaLevel = this.seaLevel;
        settings.world.properties.worldHeight = this.worldHeight;
        try {
            settings.world.properties.spawnType = SpawnType.valueOf(this.spawnType);
        } catch (IllegalArgumentException ignored) {
            settings.world.properties.spawnType = SpawnType.CONTINENT_CENTER;
        }
        settings.climate.biomeShape.biomeSize = this.biomeSize;
        settings.climate.temperature.falloff = this.temperatureFalloff;
        settings.climate.temperature.bias = this.temperatureBias;
        settings.climate.temperature.scale = this.temperatureScale;
        settings.climate.moisture.falloff = this.moistureFalloff;
        settings.climate.moisture.bias = this.moistureBias;
        settings.climate.moisture.scale = this.moistureScale;
        settings.terrain.general.terrainRegionSize = this.terrainRegionSize;
        settings.terrain.general.globalVerticalScale = this.globalVerticalScale;
        settings.terrain.general.globalHorizontalScale = this.globalHorizontalScale;
        settings.terrain.general.fancyMountains = this.fancyMountains;
        settings.rivers.riverCount = this.riverCount;
        settings.rivers.mainRivers.bedDepth = this.mainRiverBedDepth;
        settings.rivers.mainRivers.bedWidth = this.mainRiverBedWidth;
        settings.rivers.mainRivers.bankWidth = this.mainRiverBankWidth;
        settings.filters.erosion.dropletsPerChunk = this.erosionDroplets;
        settings.filters.erosion.dropletLifetime = this.erosionLifetime;
        settings.filters.erosion.erosionRate = this.erosionRate;
        settings.filters.erosion.depositeRate = this.depositRate;
    }

    public record WorldSlice(
            int continentScale,
            float deepOcean,
            float shallowOcean,
            float beach,
            float coast,
            float inland,
            int seaLevel,
            int worldHeight,
            String spawnType
    ) {
    }

    public record ClimateSlice(
            int biomeSize,
            int temperatureFalloff,
            float temperatureBias,
            int temperatureScale,
            int moistureFalloff,
            float moistureBias,
            int moistureScale
    ) {
    }

    public record TerrainSlice(
            int terrainRegionSize,
            float globalVerticalScale,
            float globalHorizontalScale,
            boolean fancyMountains
    ) {
    }

    public record RiversSlice(int riverCount, int mainRiverBedDepth, int mainRiverBedWidth, int mainRiverBankWidth) {
    }

    public record FiltersSlice(int erosionDroplets, int erosionLifetime, float erosionRate, float depositRate) {
    }
}
