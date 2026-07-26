package com.terraforged.mod.platform.forge.client;

import com.terraforged.mod.platform.forge.util.ForgeUtil;
import com.terraforged.mod.util.TranslationUtil;
import com.terraforged.mod.worldgen.GeneratorPreset;
import com.terraforged.mod.worldgen.settings.GeneratorSettings;
import com.terraforged.mod.worldgen.terrain.TerrainLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.world.ForgeWorldPreset;
import net.minecraftforge.common.world.ForgeWorldPreset.IBasicChunkGeneratorFactory;

/**
 * Separate create-world map type: islands / archipelagos / volcanic only (DS:Shipwrecked homage).
 * Same Customize UI as NewTerraForged, without continent knobs.
 */
public class ShipwreckedPreset implements IBasicChunkGeneratorFactory {
    public static final ResourceLocation PRESET_NAME = new ResourceLocation("newterraforged", "shipwrecked");
    public static final String TRANSLATION_KEY = TranslationUtil.key("generator", PRESET_NAME);

    public static ForgeWorldPreset INSTANCE;

    @Override
    public ChunkGenerator createChunkGenerator(RegistryAccess registryAccess, long seed) {
        return GeneratorPreset.build(seed, TerrainLevels.DEFAULT.get(), GeneratorSettings.shipwreckedDefaults(), registryAccess);
    }

    public static ForgeWorldPreset create() {
        INSTANCE = ForgeUtil.withName(new ForgeWorldPreset(new ShipwreckedPreset()) {
            @Override
            public String getTranslationKey() {
                return TRANSLATION_KEY;
            }

            @Override
            public Component getDisplayName() {
                return new TranslatableComponent(this.getTranslationKey()).withStyle(s -> s.withColor(ChatFormatting.AQUA));
            }
        }, "shipwrecked");
        return INSTANCE;
    }
}
