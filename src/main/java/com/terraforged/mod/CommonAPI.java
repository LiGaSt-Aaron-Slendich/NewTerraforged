package com.terraforged.mod;

import com.terraforged.mod.registry.RegistryManager;
import com.terraforged.mod.util.ApiHolder;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeMatcher;
import com.terraforged.mod.worldgen.biome.util.matcher.BiomeTagMatcher;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.tags.TagKey;

public interface CommonAPI {
    public static final ApiHolder<CommonAPI> HOLDER = new ApiHolder<CommonAPI>(new CommonAPI(){});

    default public Path getContainer() {
        return Paths.get(".", new String[0]);
    }

    default public RegistryManager getRegistryManager() {
        return RegistryManager.DEFAULT;
    }

    default public BiomeMatcher getOverworldMatcher() {
        return new BiomeTagMatcher.Overworld(new TagKey[0]);
    }

    public static CommonAPI get() {
        return HOLDER.get();
    }
}
