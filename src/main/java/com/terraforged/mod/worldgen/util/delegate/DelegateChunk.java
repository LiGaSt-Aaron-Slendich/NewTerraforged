package com.terraforged.mod.worldgen.util.delegate;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.ChunkAccess.TicksToSave;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickContainerAccess;

public abstract class DelegateChunk extends ProtoChunk {
   private static final LevelHeightAccessor ZERO_HEIGHT = new DelegateChunk.ZeroHeight();
   private static final Registry<Biome> EMPTY_REGISTRY = new MappedRegistry(Registry.BIOME_REGISTRY, Lifecycle.stable(), null);
   protected ChunkAccess delegate;

   protected DelegateChunk() {
      super(ChunkPos.ZERO, UpgradeData.EMPTY, ZERO_HEIGHT, EMPTY_REGISTRY, null);
   }

   protected void set(ChunkAccess chunk) {
      this.delegate = chunk;
   }

   public GameEventDispatcher getEventDispatcher(int p_156113_) {
      return this.delegate.getEventDispatcher(p_156113_);
   }

   public BlockState setBlockState(BlockPos p_62087_, BlockState p_62088_, boolean p_62089_) {
      return this.delegate.setBlockState(p_62087_, p_62088_, p_62089_);
   }

   public void setBlockEntity(BlockEntity p_156114_) {
      this.delegate.setBlockEntity(p_156114_);
   }

   public void addEntity(Entity p_62078_) {
      this.delegate.addEntity(p_62078_);
   }

   public LevelChunkSection getHighestSection() {
      return this.delegate.getHighestSection();
   }

   public int getHighestSectionPosition() {
      return this.delegate.getHighestSectionPosition();
   }

   public Set<BlockPos> getBlockEntitiesPos() {
      return this.delegate.getBlockEntitiesPos();
   }

   public LevelChunkSection[] getSections() {
      return this.delegate.getSections();
   }

   public LevelChunkSection getSection(int p_187657_) {
      return this.delegate.getSection(p_187657_);
   }

   public Collection<Entry<Types, Heightmap>> getHeightmaps() {
      return this.delegate.getHeightmaps();
   }

   public void setHeightmap(Types p_62083_, long[] p_62084_) {
      this.delegate.setHeightmap(p_62083_, p_62084_);
   }

   public Heightmap getOrCreateHeightmapUnprimed(Types p_62079_) {
      return this.delegate.getOrCreateHeightmapUnprimed(p_62079_);
   }

   public boolean hasPrimedHeightmap(Types p_187659_) {
      return this.delegate.hasPrimedHeightmap(p_187659_);
   }

   public int getHeight(Types p_62080_, int p_62081_, int p_62082_) {
      return this.delegate.getHeight(p_62080_, p_62081_, p_62082_);
   }

   public ChunkPos getPos() {
      return this.delegate.getPos();
   }

   public StructureStart getStartForFeature(Structure p_207944_) {
      return this.delegate.getStartForFeature(p_207944_);
   }

   public void setStartForFeature(Structure p_207949_, StructureStart p_207950_) {
      this.delegate.setStartForFeature(p_207949_, p_207950_);
   }

   public Map<Structure, StructureStart> getAllStarts() {
      return this.delegate.getAllStarts();
   }

   public void setAllStarts(Map<Structure, StructureStart> p_62090_) {
      this.delegate.setAllStarts(p_62090_);
   }

   public LongSet getReferencesForFeature(Structure p_207952_) {
      return this.delegate.getReferencesForFeature(p_207952_);
   }

   public void addReferenceForFeature(Structure p_207946_, long p_207947_) {
      this.delegate.addReferenceForFeature(p_207946_, p_207947_);
   }

   public Map<Structure, LongSet> getAllReferences() {
      return this.delegate.getAllReferences();
   }

   public void setAllReferences(Map<Structure, LongSet> p_187663_) {
      this.delegate.setAllReferences(p_187663_);
   }

   public boolean isYSpaceEmpty(int p_62075_, int p_62076_) {
      return this.delegate.isYSpaceEmpty(p_62075_, p_62076_);
   }

   public void setUnsaved(boolean p_62094_) {
      this.delegate.setUnsaved(p_62094_);
   }

   public boolean isUnsaved() {
      return this.delegate.isUnsaved();
   }

   public ChunkStatus getStatus() {
      return this.delegate.getStatus();
   }

   public void removeBlockEntity(BlockPos p_62101_) {
      this.delegate.removeBlockEntity(p_62101_);
   }

   public void markPosForPostprocessing(BlockPos p_62102_) {
      this.delegate.markPosForPostprocessing(p_62102_);
   }

   public ShortList[] getPostProcessing() {
      return this.delegate.getPostProcessing();
   }

   public void addPackedPostProcess(short p_62092_, int p_62093_) {
      this.delegate.addPackedPostProcess(p_62092_, p_62093_);
   }

   public void setBlockEntityNbt(CompoundTag p_62091_) {
      this.delegate.setBlockEntityNbt(p_62091_);
   }

   public CompoundTag getBlockEntityNbt(BlockPos p_62103_) {
      return this.delegate.getBlockEntityNbt(p_62103_);
   }

   public CompoundTag getBlockEntityNbtForSaving(BlockPos p_62104_) {
      return this.delegate.getBlockEntityNbtForSaving(p_62104_);
   }

   public Stream<BlockPos> getLights() {
      return this.delegate.getLights();
   }

   public TickContainerAccess<Block> getBlockTicks() {
      return this.delegate.getBlockTicks();
   }

   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.delegate.getFluidTicks();
   }

   public TicksToSave getTicksForSerialization() {
      return this.delegate.getTicksForSerialization();
   }

   public UpgradeData getUpgradeData() {
      return this.delegate.getUpgradeData();
   }

   public boolean isOldNoiseGeneration() {
      return this.delegate.isOldNoiseGeneration();
   }

   public BlendingData getBlendingData() {
      return this.delegate.getBlendingData();
   }

   public void setBlendingData(BlendingData p_187646_) {
      this.delegate.setBlendingData(p_187646_);
   }

   public long getInhabitedTime() {
      return this.delegate.getInhabitedTime();
   }

   public void incrementInhabitedTime(long p_187633_) {
      this.delegate.incrementInhabitedTime(p_187633_);
   }

   public void setInhabitedTime(long p_62099_) {
      this.delegate.setInhabitedTime(p_62099_);
   }

   public boolean isLightCorrect() {
      return this.delegate.isLightCorrect();
   }

   public void setLightCorrect(boolean p_62100_) {
      this.delegate.setLightCorrect(p_62100_);
   }

   public int getMinBuildHeight() {
      return this.delegate.getMinBuildHeight();
   }

   public int getHeight() {
      return this.delegate.getHeight();
   }

   public NoiseChunk getOrCreateNoiseChunk(
      NoiseRouter p_207938_, Supplier<BeardifierOrMarker> p_207939_, NoiseGeneratorSettings p_207940_, FluidPicker p_207941_, Blender p_207942_
   ) {
      return this.delegate.getOrCreateNoiseChunk(p_207938_, p_207939_, p_207940_, p_207941_, p_207942_);
   }

   @Deprecated
   public Holder<Biome> carverBiome(Supplier<Holder<Biome>> p_204345_) {
      return this.delegate.carverBiome(p_204345_);
   }

   public Holder<Biome> getNoiseBiome(int p_204347_, int p_204348_, int p_204349_) {
      return this.delegate.getNoiseBiome(p_204347_, p_204348_, p_204349_);
   }

   public void fillBiomesFromNoise(BiomeResolver p_187638_, Sampler p_187639_) {
      this.delegate.fillBiomesFromNoise(p_187638_, p_187639_);
   }

   public boolean hasAnyStructureReferences() {
      return this.delegate.hasAnyStructureReferences();
   }

   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return this.delegate.getBelowZeroRetrogen();
   }

   public boolean isUpgrading() {
      return this.delegate.isUpgrading();
   }

   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this.delegate.getHeightAccessorForGeneration();
   }

   public BlockEntity getBlockEntity(BlockPos p_45570_) {
      return this.delegate.getBlockEntity(p_45570_);
   }

   public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos p_151367_, BlockEntityType<T> p_151368_) {
      return this.delegate.getBlockEntity(p_151367_, p_151368_);
   }

   public BlockState getBlockState(BlockPos p_45571_) {
      return this.delegate.getBlockState(p_45571_);
   }

   public FluidState getFluidState(BlockPos p_45569_) {
      return this.delegate.getFluidState(p_45569_);
   }

   public int getLightEmission(BlockPos p_45572_) {
      return this.delegate.getLightEmission(p_45572_);
   }

   public int getMaxLightLevel() {
      return this.delegate.getMaxLightLevel();
   }

   public Stream<BlockState> getBlockStates(AABB p_45557_) {
      return this.delegate.getBlockStates(p_45557_);
   }

   public BlockHitResult isBlockInLine(ClipBlockStateContext p_151354_) {
      return this.delegate.isBlockInLine(p_151354_);
   }

   public BlockHitResult clip(ClipContext p_45548_) {
      return this.delegate.clip(p_45548_);
   }

   public BlockHitResult clipWithInteractionOverride(Vec3 p_45559_, Vec3 p_45560_, BlockPos p_45561_, VoxelShape p_45562_, BlockState p_45563_) {
      return this.delegate.clipWithInteractionOverride(p_45559_, p_45560_, p_45561_, p_45562_, p_45563_);
   }

   public double getBlockFloorHeight(VoxelShape p_45565_, Supplier<VoxelShape> p_45566_) {
      return this.delegate.getBlockFloorHeight(p_45565_, p_45566_);
   }

   public double getBlockFloorHeight(BlockPos p_45574_) {
      return this.delegate.getBlockFloorHeight(p_45574_);
   }

   public int getMaxBuildHeight() {
      return this.delegate.getMaxBuildHeight();
   }

   public int getSectionsCount() {
      return this.delegate.getSectionsCount();
   }

   public int getMinSection() {
      return this.delegate.getMinSection();
   }

   public int getMaxSection() {
      return this.delegate.getMaxSection();
   }

   public boolean isOutsideBuildHeight(BlockPos p_151571_) {
      return this.delegate.isOutsideBuildHeight(p_151571_);
   }

   public boolean isOutsideBuildHeight(int p_151563_) {
      return this.delegate.isOutsideBuildHeight(p_151563_);
   }

   public int getSectionIndex(int p_151565_) {
      return this.delegate.getSectionIndex(p_151565_);
   }

   public int getSectionIndexFromSectionY(int p_151567_) {
      return this.delegate.getSectionIndexFromSectionY(p_151567_);
   }

   public int getSectionYFromSectionIndex(int p_151569_) {
      return this.delegate.getSectionYFromSectionIndex(p_151569_);
   }

   private static class ZeroHeight implements LevelHeightAccessor {
      public int getHeight() {
         return 0;
      }

      public int getMinBuildHeight() {
         return 0;
      }
   }
}
