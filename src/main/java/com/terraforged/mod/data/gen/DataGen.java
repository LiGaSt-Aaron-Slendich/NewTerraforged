package com.terraforged.mod.data.gen;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.Environment;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.registry.ModRegistries;
import com.terraforged.mod.util.FileUtil;
import com.terraforged.mod.util.Init;
import com.terraforged.mod.util.json.JsonFormatter;
import com.terraforged.mod.worldgen.GeneratorPreset;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Writable;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class DataGen extends Init {
   public static final DataGen INSTANCE = new DataGen();

   @Override
   protected void doInit() {
      if (Environment.DATA_GEN) {
         export(Paths.get("datagen"));
      }
   }

   public static void export(Path dir) {
      FileUtil.delete(dir);
      TerraForged.LOG.info("Generating json data");
      Writable writable = RegistryAccess.builtinCopy();
      RegistryOps<JsonElement> registryops = RegistryOps.create(JsonOps.INSTANCE, writable);
      genGenerator(dir, writable, registryops);
      genBuiltin(dir, writable, registryops);
      genBiomes(dir, writable, registryops);
   }

   private static void genGenerator(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
      LevelStem levelstem = GeneratorPreset.getDefault(registries);
      JsonElement jsonelement = (JsonElement)LevelStem.CODEC.encodeStart(writeOps, levelstem).resultOrPartial(System.err::println).orElseThrow();
      export(dir, Registry.DIMENSION_REGISTRY, Level.OVERWORLD, jsonelement);
   }

   private static void genDimensionType(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
      export(dir, Registry.DIMENSION_TYPE_REGISTRY, DimensionType.DIRECT_CODEC, registries, writeOps);
   }

   private static void genBuiltin(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
      for (ModRegistries.HolderEntry<?> holderentry : ModRegistries.getHolders()) {
         export(dir, holderentry, registries, writeOps);
      }
   }

   private static void genBiomes(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
      Registry<Biome> registry = registries.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);

      for (Entry<ResourceKey<Biome>, Biome> entry : registry.entrySet()) {
         if (entry.getKey().location().getNamespace().equals("terraforged")) {
            JsonElement jsonelement = (JsonElement)Biome.DIRECT_CODEC.encodeStart(writeOps, entry.getValue()).result().orElseThrow();
            export(dir, registry.key(), entry.getKey(), jsonelement);
         }
      }
   }

   private static <T> void export(Path dir, ModRegistries.HolderEntry<T> holder, RegistryAccess access, DynamicOps<JsonElement> ops) {
      export(dir, holder.key(), holder.direct(), access, ops);
   }

   private static <T> void export(Path dir, ResourceKey<? extends Registry<T>> key, Codec<T> codec, RegistryAccess access, DynamicOps<JsonElement> ops) {
      Registry<T> registry = access.ownedRegistryOrThrow(key);
      export(dir, registry, codec, ops);
   }

   private static <T> void export(Path dir, Registry<T> registry, Codec<T> codec, DynamicOps<JsonElement> ops) {
      TerraForged.LOG.info("Exporting registry: {}", registry.key());

      for (Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
         try {
            JsonElement jsonelement = (JsonElement)codec.encodeStart(ops, entry.getValue()).mapError(DataGen::logError).result().orElseThrow();
            export(dir, registry.key(), entry.getKey(), jsonelement);
         } catch (Throwable throwable) {
            new EncodingException(entry.getKey(), throwable).printStackTrace();
         }
      }
   }

   private static <T> void export(Path dir, ResourceKey<Registry<T>> registry, ResourceLocation name, Codec<T> codec, T value, DynamicOps<JsonElement> ops) {
      JsonElement jsonelement = (JsonElement)codec.encodeStart(ops, value).result().orElseThrow();
      Path path = dir.resolve("data").resolve(name.getNamespace()).resolve(registry.location().getPath()).resolve(name.getPath() + ".json");
      FileUtil.write(path, jsonelement, (writer, data) -> new JsonFormatter(writer).write(data));
   }

   private static void export(Path dir, ResourceKey<?> registry, ResourceKey<?> key, JsonElement json) {
      Path path = dir.resolve("data").resolve(key.location().getNamespace()).resolve(registry.location().getPath()).resolve(key.location().getPath() + ".json");
      Path path1 = path.getParent();
      if (!Files.exists(path1)) {
         try {
            Files.createDirectories(path1);
         } catch (IOException ioexception) {
            ioexception.printStackTrace();
            return;
         }
      }

      try {
         CompletableFuture.runAsync(() -> {
            try (BufferedWriter bufferedwriter = Files.newBufferedWriter(path)) {
               JsonFormatter.apply(json, bufferedwriter);
            } catch (IOException ioexception1) {
               ioexception1.printStackTrace();
            }
         }).get(1L, TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException | InterruptedException interruptedexception) {
         interruptedexception.printStackTrace();
      }
   }

   private static String logError(String s) {
      TerraForged.LOG.warn(s);
      return s;
   }
}
