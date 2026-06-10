package com.terraforged.mod.worldgen.biome.util.matcher;

import com.terraforged.mod.worldgen.biome.util.matcher.BiomeMatcher;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class BiomeTagMatcher
implements BiomeMatcher {
    private final TagKey<Biome>[] tags;

    @SafeVarargs
    public BiomeTagMatcher(TagKey<Biome> ... tags) {
        this.tags = tags;
    }

    @Override
    public boolean test(Holder<Biome> biome) {
        for (TagKey<Biome> tag : this.tags) {
            if (!biome.is(tag)) continue;
            return true;
        }
        return false;
    }

    public static class Overworld
    extends BiomeTagMatcher {
        @SafeVarargs
        public Overworld(TagKey<Biome> ... tags) {
            super(tags);
        }

        @Override
        public boolean test(Holder<Biome> biome) {
            return super.test(biome) || this.isTerraForged(biome);
        }

        private boolean isTerraForged(Holder<Biome> biome) {
            return biome.unwrapKey().map(key -> {
                String ns = key.location().getNamespace();
                return "newterraforged".equals(ns) || "terraforged".equals(ns);
            }).orElse(false);
        }
    }
}
