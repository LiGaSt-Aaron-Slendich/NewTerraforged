package com.terraforged.mod.client.gui.screen;

import com.terraforged.engine.serialization.serializer.Serializer;
import com.terraforged.engine.settings.Settings;
import com.terraforged.mod.client.gui.util.DataUtils;
import com.terraforged.mod.worldgen.Generator;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenSettings;

/**
 * Mutable create-world draft: engine {@link Settings} + {@link TerrainLevels},
 * with an NBT mirror for slider binding.
 */
public final class SettingsDraft {
    private long seed;
    private Settings settings;
    private TerrainLevels levels;
    private CompoundTag settingsData;

    /** Fresh draft with factory defaults. */
    public SettingsDraft(long seed) {
        this.seed = seed == -1L ? System.currentTimeMillis() : seed;
        this.levels = TerrainLevels.DEFAULT.get().copy();
        this.settings = createFactorySettings(this.seed, this.levels);
        this.settingsData = DataUtils.toNBT(this.settings);
        patchWorldPropertyRanges(this.settingsData, this.levels);
    }

    /**
     * Draft restored from a previously applied {@link Generator}.
     * Keeps all Customize settings so re-opening the screen shows what was last applied.
     */
    public SettingsDraft(long seed, Generator generator) {
        this.seed = seed == -1L ? System.currentTimeMillis() : seed;
        this.levels = generator.getTerrainLevels().copy();
        GeneratorSettings gs = generator.getGeneratorSettings();
        this.settings = gs.toEngine(this.seed, this.levels);
        this.settings.world.seed = this.seed;
        this.settingsData = DataUtils.toNBT(this.settings);
        patchWorldPropertyRanges(this.settingsData, this.levels);
    }

    /**
     * Try to restore from the current {@link WorldGenSettings}. Falls back to factory defaults
     * when the overworld generator is not a NewTF {@link Generator}.
     */
    public static SettingsDraft fromWorldSettings(long seed, WorldGenSettings wgs) {
        if (wgs != null) {
            ChunkGenerator cg = wgs.overworld();
            if (cg instanceof Generator gen) {
                return new SettingsDraft(seed, gen);
            }
        }
        return new SettingsDraft(seed);
    }

    public long seed() {
        return this.seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        this.settings.world.seed = seed;
    }

    public void randomizeSeed() {
        this.setSeed(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }

    public Settings settings() {
        return this.settings;
    }

    public TerrainLevels levels() {
        return this.levels;
    }

    public CompoundTag settingsData() {
        return this.settingsData;
    }

    public CompoundTag section(String key) {
        return this.settingsData.getCompound(key);
    }

    /** Push NBT edits back into the live {@link Settings} object. */
    public void applyToSettings() {
        DataUtils.fromNBT(this.settingsData, this.settings);
        this.settings.world.seed = this.seed;
        this.syncLevelsFromSettings();
        // Keep NBT in sync with clamped TerrainLevels (slider may have out-of-range values).
        this.settingsData.getCompound("world").getCompound("properties").putInt("seaLevel", this.levels.seaLevel);
        this.settingsData.getCompound("world").getCompound("properties").putInt("worldHeight", this.levels.maxY);
    }

    public void resetDefaults() {
        this.levels = TerrainLevels.DEFAULT.get().copy();
        this.settings = createFactorySettings(this.seed, this.levels);
        this.settingsData = DataUtils.toNBT(this.settings);
        patchWorldPropertyRanges(this.settingsData, this.levels);
    }

    public void loadGeneratorSettings(GeneratorSettings generatorSettings) {
        this.settings = generatorSettings.toEngine(this.seed, null);
        this.settings.world.seed = this.seed;
        this.syncLevelsFromSettings();
        this.refreshNbt();
    }

    public void refreshNbt() {
        this.settingsData = DataUtils.toNBT(this.settings);
        patchWorldPropertyRanges(this.settingsData, this.levels);
    }

    public GeneratorSettings toGeneratorSettings() {
        this.applyToSettings();
        return GeneratorSettings.fromEngine(this.settings);
    }

    private void syncLevelsFromSettings() {
        int sea = this.settings.world.properties.seaLevel;
        int height = this.settings.world.properties.worldHeight;
        this.levels = new TerrainLevels(
                this.levels.noiseLevels.auto,
                this.levels.noiseLevels.scale,
                this.levels.minY,
                Math.max(height, 128),
                this.levels.baseHeight,
                sea,
                Math.min(this.levels.seaFloor, sea - 1)
        );
        this.settings.world.properties.seaLevel = this.levels.seaLevel;
        this.settings.world.properties.worldHeight = this.levels.maxY;
    }

    /**
     * Engine {@code @Range} caps worldHeight at 256 / seaLevel at 255, but NewTF uses
     * maxY=480 and sea up to maxY/2. Widen slider metadata so the UI can express that.
     * Also raises continent / climate scale caps beyond stock TerraForged.
     */
    private static void patchWorldPropertyRanges(CompoundTag root, TerrainLevels levels) {
        CompoundTag world = root.getCompound("world");
        if (world.isEmpty()) {
            return;
        }
        CompoundTag props = world.getCompound("properties");
        if (!props.isEmpty()) {
            int maxY = Math.max(levels.maxY, props.getInt("worldHeight"));
            int maxSea = Math.max(32, maxY >> 1);
            putBoundMax(props, "worldHeight", maxY);
            putBoundMax(props, "seaLevel", maxSea);
            // Ensure values themselves are not silently clamped by a 0–256 slider.
            if (props.getInt("worldHeight") < levels.maxY) {
                props.putInt("worldHeight", levels.maxY);
            }
            if (props.contains("seaLevel")) {
                int sea = props.getInt("seaLevel");
                if (sea < 32) {
                    props.putInt("seaLevel", 32);
                } else if (sea > maxSea) {
                    props.putInt("seaLevel", maxSea);
                }
            }
        }
        CompoundTag continent = world.getCompound("continent");
        if (!continent.isEmpty()) {
            putBoundMax(continent, "continentScale", 50000);
        }

        CompoundTag climate = root.getCompound("climate");
        if (!climate.isEmpty()) {
            putBoundMax(climate.getCompound("temperature"), "scale", 80);
            putBoundMax(climate.getCompound("moisture"), "scale", 80);
            putBoundMax(climate.getCompound("biomeShape"), "biomeSize", 8000);
            putBoundMax(climate.getCompound("biomeShape"), "macroNoiseSize", 40);
        }
    }

    private static void putBoundMax(CompoundTag props, String field, int max) {
        if (props == null || props.isEmpty()) {
            return;
        }
        CompoundTag meta = props.getCompound(Serializer.META_PREFIX + field);
        if (meta.isEmpty()) {
            return;
        }
        meta.putInt(Serializer.BOUND_MAX, max);
        props.put(Serializer.META_PREFIX + field, meta);
    }

    /** Engine WorldSettings defaults + NewTF sea/height + erosion 350. */
    public static Settings createFactorySettings(long seed, TerrainLevels levels) {
        Settings settings = new Settings();
        settings.world.seed = seed;
        settings.world.continent.continentScale = com.terraforged.engine.settings.WorldSettings.DEFAULT_CONTINENT_SCALE;
        settings.world.properties.seaLevel = levels.seaLevel;
        settings.world.properties.worldHeight = levels.maxY;
        settings.filters.erosion.dropletsPerChunk = 350;
        return settings;
    }
}
