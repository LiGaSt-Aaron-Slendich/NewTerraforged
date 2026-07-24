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
        // Customize → Done stash: World Type cycle may have replaced the generator briefly.
        if (AppliedCustomizeState.present() && AppliedCustomizeState.settings() != null) {
            long stashSeed = AppliedCustomizeState.seed();
            SettingsDraft draft = new SettingsDraft(stashSeed != -1L ? stashSeed : seed);
            draft.loadGeneratorSettings(AppliedCustomizeState.settings());
            return draft;
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
        normalizeVolcanoWeight(this.settings);
        com.terraforged.mod.worldgen.settings.ContinentGuarantee.syncIslandsMirror(this.settings.world);
        this.syncLevelsFromSettings();
        // Keep NBT in sync with clamped TerrainLevels (slider may have out-of-range values).
        this.settingsData.getCompound("world").getCompound("properties").putInt("seaLevel", this.levels.seaLevel);
        this.settingsData.getCompound("world").getCompound("properties").putInt("worldHeight", this.levels.maxY);
        // Mirror clamped volcano weight back into NBT so the Terrain slider shows the new value.
        this.settingsData.getCompound("terrain").getCompound("volcano").putFloat("weight", this.settings.terrain.volcano.weight);
    }

    public void resetDefaults() {
        this.levels = TerrainLevels.DEFAULT.get().copy();
        this.settings = createFactorySettings(this.seed, this.levels);
        this.settingsData = DataUtils.toNBT(this.settings);
        patchWorldPropertyRanges(this.settingsData, this.levels);
    }

    public void loadGeneratorSettings(GeneratorSettings generatorSettings) {
        if (generatorSettings.seed != -1L) {
            this.seed = generatorSettings.seed;
        }
        this.settings = generatorSettings.toEngine(this.seed, null);
        this.settings.world.seed = this.seed;
        normalizeVolcanoWeight(this.settings);
        com.terraforged.mod.worldgen.settings.ContinentGuarantee.migrateFromLegacyIslands(
                this.settings.world, generatorSettings.engineSettings);
        com.terraforged.mod.worldgen.settings.ContinentGuarantee.syncIslandsMirror(this.settings.world);
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
     * Engine {@code @Range} used to cap worldHeight at 256, but NewTF overworld is
     * height=1024 (min_y=-64) and terrain max_y defaults to 640. Widen slider metadata.
     * Also raises continent / climate scale caps beyond stock TerraForged.
     */
    private static void patchWorldPropertyRanges(CompoundTag root, TerrainLevels levels) {
        CompoundTag world = root.getCompound("world");
        if (world.isEmpty()) {
            return;
        }
        CompoundTag props = world.getCompound("properties");
        if (!props.isEmpty()) {
            int maxY = Math.max(Math.max(levels.maxY, props.getInt("worldHeight")), 640);
            int maxSea = Math.max(32, maxY >> 1);
            putBoundMax(props, "worldHeight", Math.max(maxY, 1024));
            putBoundMax(props, "seaLevel", maxSea);
            // Ensure values themselves are not silently clamped by a 0–256 slider.
            if (props.getInt("worldHeight") < 640) {
                props.putInt("worldHeight", Math.max(levels.maxY, 640));
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
            putBoundMax(climate.getCompound("temperature"), "scale", 100);
            putBoundMax(climate.getCompound("moisture"), "scale", 100);
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
        settings.world.properties.worldHeight = Math.max(levels.maxY, 640);
        settings.filters.erosion.dropletsPerChunk = 350;
        // Stock TF default volcano weight=5 floods continents. Moderate mainland presence;
        // ocean volcanic islands come from Islands.volcanicIslandsChance.
        settings.terrain.volcano.weight = 0.85F;
        settings.terrain.general.globalVerticalScale = 1.12F;
        settings.terrain.mountains.verticalScale = 1.35F;
        settings.terrain.hills.verticalScale = 1.15F;
        settings.terrain.torridonian.verticalScale = 1.25F;
        return settings;
    }

    /** Cap legacy / stock volcano spam when opening Customize. */
    public static void normalizeVolcanoWeight(Settings settings) {
        if (settings != null && settings.terrain != null && settings.terrain.volcano.weight > 1.5F) {
            settings.terrain.volcano.weight = 0.85F;
        }
    }
}
