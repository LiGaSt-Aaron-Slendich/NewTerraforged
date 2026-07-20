package com.terraforged.mod.client.gui.screen;

import com.terraforged.engine.settings.Settings;
import com.terraforged.mod.client.gui.util.DataUtils;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.nbt.CompoundTag;

/**
 * Mutable create-world draft: engine {@link Settings} + {@link TerrainLevels},
 * with an NBT mirror for slider binding.
 */
public final class SettingsDraft {
    private int seed;
    private Settings settings;
    private TerrainLevels levels;
    private CompoundTag settingsData;

    public SettingsDraft(int seed) {
        this.seed = seed == -1 ? (int)System.currentTimeMillis() : seed;
        this.levels = TerrainLevels.DEFAULT.get().copy();
        this.settings = createFactorySettings(this.seed, this.levels);
        this.settingsData = DataUtils.toNBT(this.settings);
    }

    public int seed() {
        return this.seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
        this.settings.world.seed = seed;
    }

    public void randomizeSeed() {
        this.setSeed(java.util.concurrent.ThreadLocalRandom.current().nextInt());
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
    }

    public void resetDefaults() {
        this.levels = TerrainLevels.DEFAULT.get().copy();
        this.settings = createFactorySettings(this.seed, this.levels);
        this.settingsData = DataUtils.toNBT(this.settings);
    }

    public void refreshNbt() {
        this.settingsData = DataUtils.toNBT(this.settings);
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
