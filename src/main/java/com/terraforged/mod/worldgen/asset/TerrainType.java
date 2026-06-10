package com.terraforged.mod.worldgen.asset;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.terraforged.engine.world.terrain.ITerrain;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.codec.LazyCodec;
import com.terraforged.mod.util.TerrainTypes;
import net.minecraft.core.Holder;

public class TerrainType {
    public static final TerrainType NONE = new TerrainType("none", com.terraforged.engine.world.terrain.TerrainType.NONE);
    public static final Codec<TerrainType> DIRECT = LazyCodec.record(instance -> instance.group(Codec.STRING.fieldOf("name").forGetter(TerrainType::getName), Codec.STRING.fieldOf("parent").xmap(TerrainType::forName, Terrain::getName).forGetter(TerrainType::getParentType)).apply(instance, TerrainType::new));
    public static final Codec<Holder<TerrainType>> CODEC = LazyCodec.registry(DIRECT, TerraForged.TERRAIN_TYPES);
    private final String name;
    private final Terrain parentType;
    private final Terrain terrain;

    public TerrainType(String name, Terrain type) {
        this.name = name;
        this.parentType = type;
        this.terrain = TerrainTypes.getOrCreate(name, type);
    }

    public String getName() {
        return this.name;
    }

    public Terrain getTerrain() {
        return this.terrain;
    }

    public Terrain getParentType() {
        return this.parentType;
    }

    private static Terrain forName(String name) {
        return com.terraforged.engine.world.terrain.TerrainType.get(name);
    }

    public static TerrainType of(Terrain terrain) {
        ITerrain iTerrain = terrain.getDelegate();
        if (iTerrain instanceof Terrain) {
            Terrain parent = (Terrain)iTerrain;
            return new TerrainType(terrain.getName(), parent);
        }
        return new TerrainType(terrain.getName(), terrain);
    }
}
