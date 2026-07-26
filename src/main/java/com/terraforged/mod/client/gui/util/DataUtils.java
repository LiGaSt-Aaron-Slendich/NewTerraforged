package com.terraforged.mod.client.gui.util;

import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Client re-export of shared engine settings NBT helpers. */
public final class DataUtils {
    private DataUtils() {
    }

    public static CompoundTag toNBT(Object object) {
        return com.terraforged.mod.util.serialization.DataUtils.toNBT(object);
    }

    public static CompoundTag toNBT(String owner, Object object) {
        return com.terraforged.mod.util.serialization.DataUtils.toNBT(owner, object);
    }

    public static CompoundTag toCompactNBT(Object object) {
        return com.terraforged.mod.util.serialization.DataUtils.toCompactNBT(object);
    }

    public static boolean fromNBT(CompoundTag settings, Object object) {
        return com.terraforged.mod.util.serialization.DataUtils.fromNBT(settings, object);
    }

    public static Stream<String> streamKeys(CompoundTag compound) {
        return com.terraforged.mod.util.serialization.DataUtils.streamKeys(compound);
    }

    public static <T extends Tag> T stripMetadata(T tag) {
        return com.terraforged.mod.util.serialization.DataUtils.stripMetadata(tag);
    }
}
