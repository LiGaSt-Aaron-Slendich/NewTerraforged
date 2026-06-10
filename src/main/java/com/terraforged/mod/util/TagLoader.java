package com.terraforged.mod.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.resource.ResourcePackLoader;

public class TagLoader {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    public static void bindTags(RegistryAccess access) {
        ArrayList<net.minecraft.server.packs.PackResources> sources = new ArrayList<>();
        sources.add(new VanillaPackResources(ServerPacksSource.BUILT_IN_METADATA, new String[]{"minecraft"}));
        for (IModInfo mod : ModList.get().getMods()) {
            if (mod.getOwningFile() == null) continue;
            sources.add(ResourcePackLoader.createPackForMod((IModFileInfo)mod.getOwningFile()));
        }
        try (MultiPackResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, sources);){
            TagLoader.bindTags(access, (ResourceManager)resourceManager);
        }
    }

    public static void bindTags(RegistryAccess access, ResourceManager resources) {
        TagManager tagManager = new TagManager(access);
        tagManager.reload(TagLoader::complete, resources, (ProfilerFiller)InactiveProfiler.INSTANCE, (ProfilerFiller)InactiveProfiler.INSTANCE, DIRECT_EXECUTOR, DIRECT_EXECUTOR);
        List<TagManager.LoadResult<?>> result = tagManager.getResult();
        for (TagManager.LoadResult entry : result) {
            TagLoader.bind(entry, access);
        }
    }

    private static <T> void bind(TagManager.LoadResult<T> result, RegistryAccess access) {
        Optional registry = access.ownedRegistry(result.key());
        if (registry.isEmpty()) {
            return;
        }
        HashMap tags = new HashMap();
        result.tags().forEach((id, tag) -> tags.put(TagKey.create(result.key(), (ResourceLocation)id), tag.getValues()));
        ((Registry)registry.get()).bindTags(tags);
    }

    @Nonnull
    private static <T> CompletableFuture<T> complete(T t) {
        return CompletableFuture.completedFuture(t);
    }
}
