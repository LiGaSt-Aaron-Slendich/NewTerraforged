package com.terraforged.mod.platform.forge;

import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.world.ForgeWorldPreset;

public class TFPreset
implements ForgeWorldPreset.IBasicChunkGeneratorFactory {
    public static final String NAME = "newterraforged";
    public static final String TRANSLATION_KEY = "generator.newterraforged.newterraforged";

    public ChunkGenerator createChunkGenerator(RegistryAccess registries, long seed) {
        TerrainLevels levels = TerrainLevels.DEFAULT.get().copy();
        return GeneratorPreset.build(seed, levels, registries);
    }

    public static ForgeWorldPreset create() {
        ForgeWorldPreset preset = new ForgeWorldPreset(new TFPreset()){

            public Component getDisplayName() {
                return new TranslatableComponent(TFPreset.TRANSLATION_KEY).withStyle(style -> style.withColor(ChatFormatting.GREEN));
            }
        };
        preset.setRegistryName(new ResourceLocation(NAME, NAME));
        return preset;
    }
}
