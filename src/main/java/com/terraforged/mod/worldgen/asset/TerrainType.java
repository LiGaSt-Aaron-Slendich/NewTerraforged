package com.terraforged.mod.worldgen.asset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.terraforged.engine.world.terrain.Terrain;
import com.terraforged.engine.world.terrain.TerrainHelper;
import com.terraforged.mod.codec.LazyCodec;
import com.terraforged.mod.registry.ModRegistry;
import net.minecraft.core.Holder;

public class TerrainType {
   public static final TerrainType NONE = new TerrainType("none", com.terraforged.engine.world.terrain.TerrainType.NONE);
   public static final Codec<TerrainType> DIRECT = LazyCodec.record(
      instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(TerrainType::getName),
            Codec.STRING.fieldOf("parent").xmap(TerrainType::forName, Terrain::getName).forGetter(TerrainType::getParentType)
         )
         .apply(instance, TerrainType::new)
   );
   public static final Codec<Holder<TerrainType>> CODEC = LazyCodec.registry(DIRECT, ModRegistry.TERRAIN_TYPE);
   private final String name;
   private final Terrain parentType;
   private final Terrain terrain;

   public TerrainType(String name, Terrain type) {
      this.name = name;
      this.parentType = type;
      this.terrain = TerrainHelper.getOrCreate(name, type);
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
      return terrain.getDelegate() instanceof Terrain terrainx ? new TerrainType(terrain.getName(), terrainx) : new TerrainType(terrain.getName(), terrain);
   }
}
