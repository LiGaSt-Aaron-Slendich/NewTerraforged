package com.terraforged.mod.util;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public class TranslationUtil {
    public static String key(String type, ResourceLocation location) {
        return Util.makeDescriptionId((String)type, (ResourceLocation)location);
    }
}
