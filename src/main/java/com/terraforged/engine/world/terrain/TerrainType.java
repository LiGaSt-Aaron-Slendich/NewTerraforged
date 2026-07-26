package com.terraforged.engine.world.terrain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Engine TerrainType with {@link #getOrCreate} for TerraForged 0.3.x.
 * Replaces the older jar class that only had {@link #register}/{@link #get}.
 */
public class TerrainType {
    private static final Object lock = new Object();
    private static final List<Terrain> REGISTRY = new CopyOnWriteArrayList<>();

    public static final Terrain NONE = register("none", TerrainCategory.NONE);
    public static final Terrain DEEP_OCEAN = register("deep_ocean", TerrainCategory.DEEP_OCEAN);
    public static final Terrain SHALLOW_OCEAN = register("ocean", TerrainCategory.SHALLOW_OCEAN);
    public static final Terrain COAST = register("coast", TerrainCategory.COAST);
    public static final Terrain BEACH = register("beach", TerrainCategory.BEACH);
    public static final Terrain RIVER = register("river", TerrainCategory.RIVER);
    public static final Terrain LAKE = register("lake", TerrainCategory.LAKE);
    public static final Terrain WETLAND = registerWetlands("wetland", TerrainCategory.WETLAND);
    public static final Terrain FLATS = register("flats", TerrainCategory.FLATLAND);
    public static final Terrain BADLANDS = registerBadlands("badlands", TerrainCategory.FLATLAND);
    public static final Terrain PLATEAU = register("plateau", TerrainCategory.LOWLAND);
    public static final Terrain HILLS = register("hills", TerrainCategory.LOWLAND);
    public static final Terrain MOUNTAINS = registerMountain("mountains", TerrainCategory.HIGHLAND);
    public static final Terrain MOUNTAIN_CHAIN = registerMountain("mountain_chain", TerrainCategory.HIGHLAND);
    public static final Terrain VOLCANO = registerVolcano("volcano", TerrainCategory.HIGHLAND);
    public static final Terrain VOLCANO_PIPE = registerVolcano("volcano_pipe", TerrainCategory.HIGHLAND);

    public static void forEach(Consumer<Terrain> action) {
        REGISTRY.forEach(action);
    }

    public static Optional<Terrain> find(Predicate<Terrain> filter) {
        return REGISTRY.stream().filter(filter).findFirst();
    }

    public static Terrain get(String name) {
        for (Terrain terrain : REGISTRY) {
            if (terrain.getName().equalsIgnoreCase(name)) {
                return terrain;
            }
        }
        return null;
    }

    public static Terrain get(int id) {
        synchronized (lock) {
            if (id >= 0 && id < REGISTRY.size()) {
                return REGISTRY.get(id);
            }
            return NONE;
        }
    }

    /** 0.3.x API: return existing by name or register a child of {@code parent}. */
    public static Terrain getOrCreate(String name, Terrain parent) {
        Terrain existing = get(name);
        if (existing != null) {
            return existing;
        }
        if (parent == null) {
            parent = NONE;
        }
        return register(new Terrain(REGISTRY.size(), name, parent.getCategory()));
    }

    public static Terrain register(Terrain instance) {
        synchronized (lock) {
            Terrain current = get(instance.getName());
            if (current != null) {
                return current;
            }
            Terrain terrain = instance.withId(REGISTRY.size());
            REGISTRY.add(terrain);
            return terrain;
        }
    }

    public static Terrain registerComposite(Terrain a, Terrain b) {
        if (a == b) {
            return a;
        }
        synchronized (lock) {
            Terrain min = a.getId() < b.getId() ? a : b;
            Terrain max = a.getId() > b.getId() ? a : b;
            Terrain current = get(min.getName() + "-" + max.getName());
            if (current != null) {
                return current;
            }
            CompositeTerrain mix = new CompositeTerrain(REGISTRY.size(), min, max);
            REGISTRY.add(mix);
            return mix;
        }
    }

    private static Terrain register(String name, TerrainCategory type) {
        synchronized (lock) {
            Terrain terrain = new Terrain(REGISTRY.size(), name, type);
            REGISTRY.add(terrain);
            return terrain;
        }
    }

    private static Terrain registerWetlands(String name, TerrainCategory type) {
        synchronized (lock) {
            ConfiguredTerrain terrain = new ConfiguredTerrain(REGISTRY.size(), name, type, true);
            REGISTRY.add(terrain);
            return terrain;
        }
    }

    private static Terrain registerBadlands(String name, TerrainCategory type) {
        synchronized (lock) {
            ConfiguredTerrain terrain = new ConfiguredTerrain(REGISTRY.size(), name, type, 0.3f);
            REGISTRY.add(terrain);
            return terrain;
        }
    }

    private static Terrain registerMountain(String name, TerrainCategory type) {
        synchronized (lock) {
            ConfiguredTerrain terrain = new ConfiguredTerrain(REGISTRY.size(), name, type, true, true);
            REGISTRY.add(terrain);
            return terrain;
        }
    }

    private static Terrain registerVolcano(String name, TerrainCategory type) {
        synchronized (lock) {
            ConfiguredTerrain terrain = new ConfiguredTerrain(REGISTRY.size(), name, type, true, true) {
                @Override
                public boolean isVolcano() {
                    return true;
                }

                @Override
                public boolean overridesCoast() {
                    return true;
                }
            };
            REGISTRY.add(terrain);
            return terrain;
        }
    }
}
