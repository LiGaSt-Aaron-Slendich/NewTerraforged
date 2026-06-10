package com.terraforged.mod.worldgen;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class Regenerator {
    private static final Supplier<Field[]> CACHES = Suppliers.memoize(() -> (Field[])Regenerator.getFields(ChunkMap.class, Long2ObjectLinkedOpenHashMap.class).toArray(Field[]::new));

    public static void regenerateChunks(ChunkPos pos, int radius, ServerLevel level, CommandSourceStack source) {
        Regenerator.log(source, "Deleting chunks", ChatFormatting.ITALIC);
        Regenerator.deleteChunks(pos, radius, level);
        Regenerator.log(source, "Regenerating chunks", ChatFormatting.ITALIC);
        Regenerator.regenerateChunks(level);
        Regenerator.log(source, "Regen complete!", ChatFormatting.GREEN);
    }

    private static void log(CommandSourceStack source, String message, ChatFormatting ... formatting) {
        source.sendSuccess((Component)new TextComponent(message).withStyle(formatting), true);
    }

    private static void deleteChunks(ChunkPos pos, int radius, ServerLevel level) {
        ServerChunkCache chunkSource = level.getChunkSource();
        Long2ObjectLinkedOpenHashMap<?>[] caches = Regenerator.getCaches(chunkSource.chunkMap);
        chunkSource.save(true);
        chunkSource.chunkMap.flushWorker();
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int x = pos.x + dx;
                int z = pos.z + dz;
                ChunkPos chunkPos = new ChunkPos(x, z);
                long chunkIndex = chunkPos.toLong();
                chunkSource.chunkMap.write(chunkPos, null);
                for (Long2ObjectLinkedOpenHashMap<?> cache : caches) {
                    cache.remove(chunkIndex);
                }
            }
        }
    }

    private static void regenerateChunks(ServerLevel level) {
        ServerChunkCache chunkSource = level.getChunkSource();
        chunkSource.tick(() -> true, false);
    }

    private static Long2ObjectLinkedOpenHashMap<?>[] getCaches(ChunkMap chunkMap) {
        Field[] fields = CACHES.get();
        Long2ObjectLinkedOpenHashMap[] caches = new Long2ObjectLinkedOpenHashMap[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            caches[i] = Regenerator.get(chunkMap, fields[i], Long2ObjectLinkedOpenHashMap.class, Long2ObjectLinkedOpenHashMap::new);
        }
        return caches;
    }

    private static <T> T get(Object owner, Field field, Class<T> type, Supplier<T> defaultSupplier) {
        try {
            Object t = field.get(owner);
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }

    private static Stream<Field> getFields(Class<?> type, Class<?> fieldType) {
        return Stream.of(type.getDeclaredFields()).filter(f -> f.getType() == fieldType).peek(f -> f.setAccessible(true));
    }
}
