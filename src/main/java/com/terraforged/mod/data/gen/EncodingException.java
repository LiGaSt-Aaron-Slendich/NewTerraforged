package com.terraforged.mod.data.gen;

import net.minecraft.resources.ResourceKey;

public class EncodingException
extends RuntimeException {
    public EncodingException(ResourceKey<?> key, Throwable t) {
        super("Failed to encode: " + key, t);
    }
}
