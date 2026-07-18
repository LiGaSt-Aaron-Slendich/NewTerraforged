package com.terraforged.mod.worldgen;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class Regenerator {
   private static final Supplier<Field[]> CACHES = Suppliers.memoize(() -> getFields(ChunkMap.class, Long2ObjectLinkedOpenHashMap.class).toArray(Field[]::new));

   public static void regenerateChunks(ChunkPos pos, int radius, ServerLevel level, CommandSourceStack source) {
      log(source, "Deleting chunks", ChatFormatting.ITALIC);
      deleteChunks(pos, radius, level);
      log(source, "Regenerating chunks", ChatFormatting.ITALIC);
      regenerateChunks(level);
      log(source, "Regen complete!", ChatFormatting.GREEN);
   }

   private static void log(CommandSourceStack source, String message, ChatFormatting... formatting) {
      source.sendSuccess(new TextComponent(message).withStyle(formatting), true);
   }

   private static void deleteChunks(ChunkPos pos, int radius, ServerLevel level) {
      ServerChunkCache serverchunkcache = level.getChunkSource();
      Long2ObjectLinkedOpenHashMap<?>[] long2objectlinkedopenhashmap = getCaches(serverchunkcache.chunkMap);
      serverchunkcache.save(true);
      serverchunkcache.chunkMap.flushWorker();

      for (int i = -radius; i <= radius; i++) {
         for (int j = -radius; j <= radius; j++) {
            int k = pos.x + j;
            int l = pos.z + i;
            ChunkPos chunkpos = new ChunkPos(k, l);
            long i1 = chunkpos.toLong();
            serverchunkcache.chunkMap.write(chunkpos, null);

            for (Long2ObjectLinkedOpenHashMap<?> long2objectlinkedopenhashmap1 : long2objectlinkedopenhashmap) {
               long2objectlinkedopenhashmap1.remove(i1);
            }
         }
      }
   }

   private static void regenerateChunks(ServerLevel level) {
      ServerChunkCache serverchunkcache = level.getChunkSource();
      serverchunkcache.tick(() -> true, false);
   }

   private static Long2ObjectLinkedOpenHashMap<?>[] getCaches(ChunkMap chunkMap) {
      Field[] afield = CACHES.get();
      Long2ObjectLinkedOpenHashMap<?>[] long2objectlinkedopenhashmap = new Long2ObjectLinkedOpenHashMap[afield.length];

      for (int i = 0; i < afield.length; i++) {
         long2objectlinkedopenhashmap[i] = get(chunkMap, afield[i], Long2ObjectLinkedOpenHashMap.class, Long2ObjectLinkedOpenHashMap::new);
      }

      return long2objectlinkedopenhashmap;
   }

   private static <T> T get(Object owner, Field field, Class<T> type, Supplier<T> defaultSupplier) {
      try {
         Object object = field.get(owner);
         if (type.isInstance(object)) {
            return type.cast(object);
         }
      } catch (IllegalAccessException illegalaccessexception) {
         illegalaccessexception.printStackTrace();
      }

      return defaultSupplier.get();
   }

   private static Stream<Field> getFields(Class<?> type, Class<?> fieldType) {
      return Stream.of(type.getDeclaredFields()).filter(f -> f.getType() == fieldType).peek(f -> f.setAccessible(true));
   }
}
