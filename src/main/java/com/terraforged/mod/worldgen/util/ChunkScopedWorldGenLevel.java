package com.terraforged.mod.worldgen.util;

import com.terraforged.mod.worldgen.cave.CarverChunk;
import com.terraforged.mod.worldgen.cave.CaveUndergroundGuard;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;

public final class ChunkScopedWorldGenLevel
implements WorldGenLevel {
    private final WorldGenLevel delegate;
    private final int chunkX;
    private final int chunkZ;
    private final int radius;
    private final ChunkAccess undergroundGuardChunk;
    private final Holder<Biome> boundBiome;
    private final CarverChunk biomeGuardCarver;

    private ChunkScopedWorldGenLevel(WorldGenLevel delegate, ChunkPos pos, int radius, ChunkAccess undergroundGuardChunk, Holder<Biome> boundBiome, CarverChunk biomeGuardCarver) {
        this.delegate = delegate;
        this.chunkX = pos.x;
        this.chunkZ = pos.z;
        this.radius = radius;
        this.undergroundGuardChunk = undergroundGuardChunk;
        this.boundBiome = boundBiome;
        this.biomeGuardCarver = biomeGuardCarver;
    }

    public static WorldGenLevel wrap(WorldGenLevel level, ChunkAccess chunk) {
        return ChunkScopedWorldGenLevel.wrap(level, chunk, 0);
    }

    public static WorldGenLevel wrap(WorldGenLevel level, ChunkAccess chunk, int radius) {
        return ChunkScopedWorldGenLevel.wrap(level, chunk, radius, null, null, null);
    }

    public static WorldGenLevel wrapWithUndergroundGuard(WorldGenLevel level, ChunkAccess chunk) {
        return ChunkScopedWorldGenLevel.wrapWithUndergroundGuard(level, chunk, null);
    }

    public static WorldGenLevel wrapWithUndergroundGuard(WorldGenLevel level, ChunkAccess chunk, CarverChunk carver) {
        return ChunkScopedWorldGenLevel.wrap(level, chunk, 1, chunk, null, carver);
    }

    public static WorldGenLevel wrapWithBiomeGuard(WorldGenLevel level, ChunkAccess chunk, Holder<Biome> boundBiome, CarverChunk carver) {
        return ChunkScopedWorldGenLevel.wrap(level, chunk, 1, chunk, boundBiome, carver);
    }

    private static WorldGenLevel wrap(WorldGenLevel level, ChunkAccess chunk, int radius, ChunkAccess undergroundGuardChunk, Holder<Biome> boundBiome, CarverChunk biomeGuardCarver) {
        ChunkAccess mergedGuardChunk = undergroundGuardChunk;
        CarverChunk mergedCarver = biomeGuardCarver;
        Holder<Biome> mergedBiome = boundBiome;
        WorldGenLevel root = level;
        while (root instanceof ChunkScopedWorldGenLevel scoped) {
            if (mergedGuardChunk == null && scoped.undergroundGuardChunk != null) {
                mergedGuardChunk = scoped.undergroundGuardChunk;
            }
            if (mergedCarver == null && scoped.biomeGuardCarver != null) {
                mergedCarver = scoped.biomeGuardCarver;
            }
            if (mergedBiome == null && scoped.boundBiome != null) {
                mergedBiome = scoped.boundBiome;
            }
            root = scoped.delegate;
        }
        if (level instanceof ChunkScopedWorldGenLevel scoped) {
            if (scoped.chunkX == chunk.getPos().x && scoped.chunkZ == chunk.getPos().z && scoped.radius == radius && scoped.delegate == root && scoped.undergroundGuardChunk == mergedGuardChunk && scoped.boundBiome == mergedBiome && scoped.biomeGuardCarver == mergedCarver) {
                return scoped;
            }
        }
        return new ChunkScopedWorldGenLevel(root, chunk.getPos(), radius, mergedGuardChunk, mergedBiome, mergedCarver);
    }

    private boolean passesUndergroundGuard(BlockPos pos) {
        if (this.undergroundGuardChunk == null) {
            return true;
        }
        if (this.boundBiome != null) {
            return CaveUndergroundGuard.mayWriteBlockForBiome(this, this.undergroundGuardChunk, pos, this.boundBiome, this.biomeGuardCarver);
        }
        return CaveUndergroundGuard.mayWriteBlock(this, this.undergroundGuardChunk, pos, this.biomeGuardCarver);
    }

    private boolean inRange(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return Math.abs(cx - this.chunkX) <= this.radius && Math.abs(cz - this.chunkZ) <= this.radius;
    }

    public boolean ensureCanWrite(BlockPos pos) {
        return this.inRange(pos) && this.delegate.ensureCanWrite(pos);
    }

    public void setCurrentlyGenerating(Supplier<String> name) {
        this.delegate.setCurrentlyGenerating(name);
    }

    public long getSeed() {
        return this.delegate.getSeed();
    }

    public ServerLevel getLevel() {
        return this.delegate.getLevel();
    }

    public long nextSubTickCount() {
        return this.delegate.nextSubTickCount();
    }

    public LevelTickAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    public LevelData getLevelData() {
        return this.delegate.getLevelData();
    }

    public Random getRandom() {
        return this.delegate.getRandom();
    }

    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return this.delegate.getCurrentDifficultyAt(pos);
    }

    public MinecraftServer getServer() {
        return this.delegate.getServer();
    }

    public BiomeManager getBiomeManager() {
        return this.delegate.getBiomeManager();
    }

    public float getShade(Direction direction, boolean shade) {
        return this.delegate.getShade(direction, shade);
    }

    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return this.delegate.getBlockTint(pos, colorResolver);
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return this.delegate.getNoiseBiome(x, y, z);
    }

    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return this.delegate.getUncachedNoiseBiome(x, y, z);
    }

    public boolean isClientSide() {
        return this.delegate.isClientSide();
    }

    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    public DimensionType dimensionType() {
        return this.delegate.dimensionType();
    }

    public float getBrightness(BlockPos pos) {
        return this.delegate.getBrightness(pos);
    }

    public int getSkyDarken() {
        return this.delegate.getSkyDarken();
    }

    public ChunkAccess getChunk(int x, int z) {
        return this.delegate.getChunk(x, z);
    }

    public ChunkAccess getChunk(int x, int z, ChunkStatus status) {
        return this.delegate.getChunk(x, z, status);
    }

    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean load) {
        return this.delegate.getChunk(x, z, status, load);
    }

    public boolean hasChunk(int x, int z) {
        return this.delegate.hasChunk(x, z);
    }

    public BlockState getBlockState(BlockPos pos) {
        return this.delegate.getBlockState(pos);
    }

    public FluidState getFluidState(BlockPos pos) {
        return this.delegate.getFluidState(pos);
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.delegate.getBlockEntity(pos);
    }

    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return this.delegate.getBlockEntity(pos, type);
    }

    public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) {
        return this.delegate.getHeightmapPos(type, pos);
    }

    public int getHeight(Heightmap.Types type, int x, int z) {
        return this.delegate.getHeight(type, x, z);
    }

    public int getMinBuildHeight() {
        return this.delegate.getMinBuildHeight();
    }

    public int getHeight() {
        return this.delegate.getHeight();
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.setBlock(pos, state, flags);
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.setBlock(pos, state, flags, recursionLeft);
    }

    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.removeBlock(pos, isMoving);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.destroyBlock(pos, dropBlock);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock, Entity entity) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.destroyBlock(pos, dropBlock, entity);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock, Entity entity, int recursionLeft) {
        if (!this.inRange(pos) || !this.passesUndergroundGuard(pos)) {
            return false;
        }
        return this.delegate.destroyBlock(pos, dropBlock, entity, recursionLeft);
    }

    public boolean addFreshEntity(Entity entity) {
        return this.delegate.addFreshEntity(entity);
    }

    public void playSound(Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.delegate.playSound(player, pos, sound, source, volume, pitch);
    }

    public void addParticle(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.delegate.addParticle(options, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public void levelEvent(Player player, int type, BlockPos pos, int data) {
        this.delegate.levelEvent(player, type, pos, data);
    }

    public void gameEvent(GameEvent event, BlockPos pos) {
        this.delegate.gameEvent(event, pos);
    }

    public void gameEvent(Entity entity, GameEvent event, BlockPos pos) {
        this.delegate.gameEvent(entity, event, pos);
    }

    public RegistryAccess registryAccess() {
        return this.delegate.registryAccess();
    }

    public Difficulty getDifficulty() {
        return this.delegate.getDifficulty();
    }

    public ChunkSource getChunkSource() {
        return this.delegate.getChunkSource();
    }

    public LevelLightEngine getLightEngine() {
        return this.delegate.getLightEngine();
    }

    public WorldBorder getWorldBorder() {
        return this.delegate.getWorldBorder();
    }

    public List<Entity> getEntities(Entity entity, AABB area, Predicate<? super Entity> predicate) {
        return this.delegate.getEntities(entity, area, predicate);
    }

    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB area, Predicate<? super T> predicate) {
        return this.delegate.getEntities(type, area, predicate);
    }

    public List<? extends Player> players() {
        return this.delegate.players();
    }

    public Stream<BlockState> getBlockStatesIfLoaded(AABB area) {
        return this.delegate.getBlockStatesIfLoaded(area);
    }

    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return this.delegate.isStateAtPosition(pos, predicate);
    }

    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return this.delegate.isFluidAtPosition(pos, predicate);
    }

    public BlockHitResult clip(ClipContext context) {
        return this.delegate.clip(context);
    }

    public boolean canSeeSkyFromBelowWater(BlockPos pos) {
        return this.delegate.canSeeSkyFromBelowWater(pos);
    }

    public float getTimeOfDay(float partialTick) {
        return this.delegate.getTimeOfDay(partialTick);
    }

    public int getMoonPhase() {
        return this.delegate.getMoonPhase();
    }

    public float getMoonBrightness() {
        return this.delegate.getMoonBrightness();
    }

    public long dayTime() {
        return this.delegate.dayTime();
    }

    public void scheduleTick(BlockPos pos, Block block, int delay) {
        if (!this.inRange(pos)) {
            return;
        }
        this.delegate.scheduleTick(pos, block, delay);
    }

    public void scheduleTick(BlockPos pos, Fluid fluid, int delay) {
        if (!this.inRange(pos)) {
            return;
        }
        this.delegate.scheduleTick(pos, fluid, delay);
    }

    public void scheduleTick(BlockPos pos, Block block, int delay, TickPriority priority) {
        if (!this.inRange(pos)) {
            return;
        }
        this.delegate.scheduleTick(pos, block, delay, priority);
    }

    public void scheduleTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {
        if (!this.inRange(pos)) {
            return;
        }
        this.delegate.scheduleTick(pos, fluid, delay, priority);
    }

    public void blockUpdated(BlockPos pos, Block block) {
        if (!this.inRange(pos)) {
            return;
        }
        this.delegate.blockUpdated(pos, block);
    }

    public void levelEvent(int type, BlockPos pos, int data) {
        this.delegate.levelEvent(type, pos, data);
    }

    public void gameEvent(GameEvent event, Entity entity) {
        this.delegate.gameEvent(event, entity);
    }

    public void gameEvent(Entity entity, GameEvent event, Entity other) {
        this.delegate.gameEvent(entity, event, other);
    }
}
