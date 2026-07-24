package com.terraforged.engine.settings;

import com.terraforged.engine.serialization.annotation.Comment;
import com.terraforged.engine.serialization.annotation.Limit;
import com.terraforged.engine.serialization.annotation.Range;
import com.terraforged.engine.serialization.annotation.Restricted;
import com.terraforged.engine.serialization.annotation.Serializable;
import com.terraforged.engine.serialization.annotation.legacy.LegacyFloat;
import com.terraforged.engine.world.continent.ContinentType;
import com.terraforged.engine.world.continent.SpawnType;
import com.terraforged.noise.func.DistanceFunc;

/**
 * NewTF override of engine {@code WorldSettings}: wider continent scale + Islands section
 * (placed before Continent so it appears between island-style knobs and continent knobs).
 */
@Serializable
public class WorldSettings {
    public static final int DEFAULT_CONTINENT_SCALE = 3000;
    public transient long seed = 0L;
    /** Coastal / volcanic islands and landmass distribution (UI section before Continent). */
    public Islands islands = new Islands();
    /**
     * EGF Ocean Landscape tunables. Customize page is only reachable when
     * Experimental → Ocean Landscape is ON.
     */
    public OceanLandscape oceanLandscape = new OceanLandscape();
    public Continent continent = new Continent();
    public ControlPoints controlPoints = new ControlPoints();
    public Properties properties = new Properties();

    /** Surface world layout style (continents vs islands-only). */
    public enum WorldStyle {
        CONTINENTS,
        /** Don't Starve: Shipwrecked inspired — no continents, only islands / archipelagos / volcanic islands. */
        SHIPWRECKED
    }

    @Serializable
    public static class OceanLandscape {
        @Range(min = 0.25f, max = 3.0f)
        @Comment(value = {
                "Seafloor noise scale for Ocean Landscape corridors.",
                "Lower = smoother banks / larger features. Higher = noisier / finer detail.",
                "Requires EGF Untested → Ocean Landscape ON."
        })
        public float noiseScale = 1.0f;

        @Range(min = 1.0f, max = 4.0f)
        @Comment(value = {
                "Outgoing corridor limit: each continent may send seafloor corridors toward",
                "this many nearest continents (2–3 recommended). Incoming corridors are unlimited.",
                "Requires EGF Untested → Ocean Landscape ON."
        })
        public int corridorPartners = 2;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Strength of inter-continent seafloor corridors / banks.",
                "0 = almost none, 1 = strong. Requires EGF Ocean Landscape ON."
        })
        public float corridorStrength = 0.85f;

        @Range(min = 2.0f, max = 24.0f)
        @Comment(value = {
                "Maximum distance (× Continent Scale) between landmasses that may form a corridor.",
                "Pairs farther than this are never linked — no long deep-ocean bridges.",
                "Default 8 ≈ eight continent scales. Requires EGF Ocean Landscape ON."
        })
        public float corridorMaxDistance = 8.0f;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Coastal shelf strength along landmasses that participate in corridors.",
                "0 = corridor ridge only, 1 = broader shallow shelf hugging linked coasts.",
                "Requires EGF Ocean Landscape ON."
        })
        public float shelfStrength = 0.55f;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Deep-ocean volcano density for Ocean Landscape.",
                "0 = rare/none, 1 = denser. Requires EGF Ocean Landscape ON."
        })
        public float volcanoDensity = 0.55f;
    }

    @Serializable
    public static class Islands {
        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Chance to form coastal islands near mainland shores",
                "(distance a bit larger than a river). 0 = never, 1 = always when eligible."
        })
        public float coastalIslandsChance = 0.45f;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Chance to form volcanic islands in the ocean (cone + crater). Also keep Terrain→Volcano weight moderate.",
                "0 = never, 1 = denser when eligible — default is sparse, not spam."
        })
        public float volcanicIslandsChance = 0.15f;

        @Comment(value = {
                "EXPERIMENTAL (EGF): Enable Archipelago clusters: 2–5 islands total (including the main one),",
                "each 50–300 blocks across. Water between them is Laguna (max 15 deep).",
                "Requires Experimental Generation Features → Archipelago ON (title screen U→I→0→1, /EGF open)."
        })
        public boolean archipelago = false;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Chance to place an Archipelago cluster (2–5 islands, 50–300 wide) where eligible.",
                "Only applies when EGF Archipelago is enabled."
        })
        public float archipelagoChance = 0.30f;

        @Comment(value = {
                "EXPERIMENTAL (EGF): Enable Scattered Archipelago: many more islands than Archipelago,",
                "each 15–500 blocks across. Laguna between members (max 15 deep).",
                "Requires Experimental Generation Features → Scattered Archipelago ON."
        })
        public boolean scatteredArchipelago = false;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Chance to place a Scattered Archipelago cluster (many islands, 15–500 wide).",
                "Only applies when EGF Scattered Archipelago is enabled."
        })
        public float scatteredArchipelagoChance = 0.35f;

        /** @deprecated kept for NBT compat; prefer {@link #coastalIslandsChance}. */
        @Deprecated
        public boolean coastalIslands = true;
        /** @deprecated kept for NBT compat; prefer {@link #volcanicIslandsChance}. */
        @Deprecated
        public boolean volcanicIslands = true;
    }

    @Serializable
    public static class Properties {
        @Comment(value = {
                "NTF world style.",
                "CONTINENTS = normal NewTF landmasses.",
                "SHIPWRECKED = no continents; only islands, archipelagos and volcanic islands (DS:Shipwrecked homage)."
        })
        public WorldStyle worldStyle = WorldStyle.CONTINENTS;

        @Comment(value = {"Set whether spawn should be close to x=0,z=0 or the centre of the nearest continent"})
        public SpawnType spawnType = SpawnType.CONTINENT_CENTER;
        @Range(min = 0.0f, max = 256.0f)
        @Comment(value = {"Controls the world height"})
        public int worldHeight = 256;
        @Range(min = 0.0f, max = 255.0f)
        @Comment(value = {"Controls the sea level"})
        public int seaLevel = 63;
    }

    @Serializable
    public static class ControlPoints {
        @Range(min = 0.0f, max = 1.0f)
        @Limit(upper = "shallowOcean")
        @Comment(value = {
                "Controls the point above which deep oceans transition into shallow oceans.",
                "The greater the gap to the shallow ocean slider, the more gradual the transition."
        })
        public float deepOcean = 0.1f;
        @Range(min = 0.0f, max = 1.0f)
        @Limit(lower = "deepOcean", upper = "beach")
        @Comment(value = {
                "Controls the point above which shallow oceans transition into coastal terrain.",
                "The greater the gap to the coast slider, the more gradual the transition."
        })
        public float shallowOcean = 0.25f;
        @Range(min = 0.0f, max = 1.0f)
        @Limit(lower = "shallowOcean", upper = "coast")
        @Comment(value = {"Controls how much of the coastal terrain is assigned to beach biomes."})
        public float beach = 0.327f;
        @Range(min = 0.0f, max = 1.0f)
        @Limit(lower = "beach", upper = "inland")
        @Comment(value = {
                "Controls the size of coastal regions and is also the point below",
                "which inland terrain transitions into oceans. Certain biomes such",
                "as Mushroom Fields only generate in coastal areas."
        })
        public float coast = 0.448f;
        @Range(min = 0.0f, max = 1.0f)
        @Limit(lower = "coast")
        @Comment(value = {"Controls the overall transition from ocean to inland terrain."})
        public float inland = 0.502f;
    }

    @Serializable
    public static class Continent {
        @Comment(value = {
                "EXPERIMENTAL (EGF Untested → Guaranteed Continents): When ON, about Guaranteed Continents (±1)",
                "landmasses are forced inside the 640000x640000 window.",
                "Cut zones stay mostly ocean but keep islands / archipelago freckles (soft cut).",
                "When OFF (or EGF off), continent count follows normal noise / skipping (no guarantee)."
        })
        public boolean guaranteedContinentsEnabled = true;

        @Range(min = 1.0f, max = 16.0f)
        @Comment(value = {
                "EXPERIMENTAL (EGF Untested → Guaranteed Continents): Target number of continents",
                "inside the 640000x640000 preview/guarantee window.",
                "Actual count may be target−1, target, or target+1. Only used when Guaranteed Continents Enabled is ON."
        })
        public int guaranteedContinents = 3;

        @Range(min = 0.0f, max = 1.0f)
        @Comment(value = {
                "Increases the distance between continents (ocean gaps grow).",
                "0 = more clustered landmasses, 1 = widely spaced.",
                "Does not shift the whole map in one direction."
        })
        public float continentsSpread = 0.5f;

        @Comment(value = {"Controls the continent generator type"})
        public ContinentType continentType = ContinentType.MULTI_IMPROVED;
        @Restricted(name = "continentType", value = {"MULTI", "SINGLE"})
        @Comment(value = {
                "Controls how continent shapes are calculated.",
                "You may also need to adjust the transition points to ensure beaches etc still form."
        })
        public DistanceFunc continentShape = DistanceFunc.EUCLIDEAN;
        @Range(min = 100.0f, max = 50000.0f)
        @Comment(value = {
                "Controls the size of continents.",
                "You may also need to adjust the transition points to ensure beaches etc still form."
        })
        public int continentScale = 3000;
        @LegacyFloat(value = 0.7f)
        @Range(min = 0.5f, max = 1.0f)
        @Comment(value = {"Controls how much continent centers are offset from the underlying noise grid."})
        public float continentJitter = 0.82f;
        @Range(min = 0.0f, max = 1.0f)
        @Restricted(name = "continentType", value = {"MULTI_IMPROVED"})
        @Comment(value = {"Reduces the number of continents to create more vast oceans."})
        public float continentSkipping = 0.25f;
        @Range(min = 0.0f, max = 0.75f)
        @Restricted(name = "continentType", value = {"MULTI_IMPROVED"})
        @Comment(value = {"Increases the variance of continent sizes."})
        public float continentSizeVariance = 0.42f;
        @Range(min = 1.0f, max = 5.0f)
        @Restricted(name = "continentType", value = {"MULTI_IMPROVED"})
        @Comment(value = {"The number of octaves of noise used to distort the continent."})
        public int continentNoiseOctaves = 6;
        @Range(min = 0.0f, max = 0.5f)
        @Restricted(name = "continentType", value = {"MULTI_IMPROVED"})
        @Comment(value = {"The contribution strength of each noise octave."})
        public float continentNoiseGain = 0.34f;
        @Range(min = 1.0f, max = 10.0f)
        @Restricted(name = "continentType", value = {"MULTI_IMPROVED"})
        @Comment(value = {"The frequency multiplier for each noise octave."})
        public float continentNoiseLacunarity = 4.33f;
    }
}
