package com.terraforged.mod.data.gen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.CommonAPI;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.data.codec.Codecs;
import com.terraforged.mod.data.gen.EncodingException;
import com.terraforged.mod.data.util.JsonFormatter;
import com.terraforged.mod.registry.DataRegistry;
import com.terraforged.mod.util.FileUtil;
import com.terraforged.mod.util.TagLoader;
import com.terraforged.mod.worldgen.GeneratorPreset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.HashCache;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class DataGen {
    private static final ResourceKey<Registry<LevelStem>> LEVEL_STEM_KEY = ResourceKey.createRegistryKey((ResourceLocation)new ResourceLocation("minecraft", "dimension"));
    private final HashCache cache;
    private final List<CompletableFuture<?>> tasks = new ObjectArrayList();

    public DataGen(HashCache cache) {
        this.cache = cache;
    }

    protected CompletableFuture<?> doExport(Path dir) {
        FileUtil.delete(dir);
        RegistryAccess.Writable registries = RegistryAccess.builtinCopy();
        RegistryOps writeOps = RegistryOps.create((DynamicOps)JsonOps.INSTANCE, (RegistryAccess)registries);
        TagLoader.bindTags((RegistryAccess)registries);
        this.genPreset(dir, (RegistryAccess)registries, (RegistryOps<JsonElement>)writeOps);
        this.genBuiltin(dir, (RegistryAccess)registries, (RegistryOps<JsonElement>)writeOps);
        this.genDimensionType(dir, (RegistryAccess)registries, (RegistryOps<JsonElement>)writeOps);
        return CompletableFuture.allOf((CompletableFuture[])this.tasks.toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture<?> export(Path dir, HashCache cache) {
        return new DataGen(cache).doExport(dir);
    }

    private void genPreset(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
        LevelStem dimension = GeneratorPreset.getDefault(registries);
        JsonElement dimensionJson = Codecs.encode(dimension, LevelStem.CODEC, writeOps);
        this.export(dir, LEVEL_STEM_KEY, new ResourceLocation("minecraft", "overworld"), dimensionJson);
    }

    private void genDimensionType(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
        Registry registry = registries.ownedRegistryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        DimensionType overworld = (DimensionType)registry.get(new ResourceLocation("minecraft", "overworld"));
        JsonObject json = Codecs.encode(overworld, DimensionType.DIRECT_CODEC, writeOps).getAsJsonObject();
        json.addProperty("height", (Number)1024);
        json.addProperty("logical_height", (Number)1024);
        json.addProperty("effects", TerraForged.DIMENSION_EFFECTS.toString());
        this.export(dir, Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("minecraft", "overworld"), (JsonElement)json);
    }

    private void genBuiltin(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
        for (DataRegistry<?> registry : CommonAPI.get().getRegistryManager().getRegistries()) {
            this.export(dir, registry, registries, (DynamicOps<JsonElement>)writeOps);
        }
    }

    private void genTags(Path dir, RegistryAccess registries, RegistryOps<JsonElement> writeOps) {
    }

    private <T> void export(Path dir, DataRegistry<T> builtin, RegistryAccess access, DynamicOps<JsonElement> ops) {
        Registry registry = access.ownedRegistryOrThrow(builtin.key().get());
        TerraForged.LOG.info("Exporting registry: {}", registry.key());
        for (Map.Entry<ResourceKey<T>, T> entry : builtin) {
            try {
                JsonElement json = (JsonElement)builtin.codec().encodeStart(ops, (T) registry.getOrThrow(entry.getKey())).mapError(s -> {
                    DataGen.logError(s);
                    return s;
                }).result().orElseThrow();
                this.export(dir, registry.key(), entry.getKey().location(), json);
            }
            catch (Throwable t) {
                new EncodingException(entry.getKey(), t).printStackTrace();
            }
        }
    }

    private void export(Path dir, ResourceKey<?> registry, ResourceLocation name, JsonElement json) {
        Path file = dir.resolve("data").resolve(name.getNamespace()).resolve(registry.location().getPath()).resolve(name.getPath() + ".json");
        if (this.cache != null) {
            this.writeCached(file, json);
        } else {
            this.tasks.add(CompletableFuture.runAsync(() -> this.writeDirect(file, json)));
        }
    }

    protected void writeDirect(Path path, JsonElement json) {
        Path parent = path.getParent();
        if (!Files.exists(parent, new LinkOption[0])) {
            try {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try (BufferedWriter out = Files.newBufferedWriter(path, new OpenOption[0]);){
            JsonFormatter.format(json, out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void writeCached(Path path, JsonElement json) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            HashingOutputStream hashOut = new HashingOutputStream(Hashing.sha1(), (OutputStream)byteOut);
            JsonFormatter.format(json, (OutputStream)hashOut);
            Path parent = path.getParent();
            if (!Files.exists(parent, new LinkOption[0])) {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            String hashStr = hashOut.hash().toString();
            if (!this.cache.had(path)) {
                Files.write(path, byteOut.toByteArray(), new OpenOption[0]);
            }
            this.cache.putNew(path, hashStr);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String logError(String s) {
        TerraForged.LOG.warn(s);
        return s;
    }
}
