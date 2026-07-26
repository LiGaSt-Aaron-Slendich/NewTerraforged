package com.terraforged.mod.platform.forge.client;

import com.terraforged.mod.util.TranslationUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * Stub constants for create-world Shipwrecked map type (ForgeWorldPreset removed in 1.19).
 * Full preset registration is deferred until WorldPreset datapack wiring is ported.
 */
public final class ShipwreckedPreset {
    public static final ResourceLocation PRESET_NAME = new ResourceLocation("newterraforged", "shipwrecked");
    public static final String TRANSLATION_KEY = TranslationUtil.key("generator", PRESET_NAME);

    private ShipwreckedPreset() {}
}
