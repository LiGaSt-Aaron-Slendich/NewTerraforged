package com.terraforged.mod.client.gui.util;

import com.terraforged.engine.serialization.serializer.Deserializer;
import com.terraforged.engine.serialization.serializer.Serializer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Serialize engine {@code @Serializable} settings trees to/from {@link CompoundTag}
 * (official mappings port of 0.2.x DataUtils).
 */
public final class DataUtils {
    private DataUtils() {
    }

    public static CompoundTag toNBT(Object object) {
        return toNBT("", object);
    }

    public static CompoundTag toNBT(String owner, Object object) {
        try {
            NBTWriter writer = new NBTWriter();
            Serializer.serialize(object, writer, owner, true);
            return writer.compound();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    public static CompoundTag toCompactNBT(Object object) {
        try {
            NBTWriter writer = new NBTWriter();
            writer.readFrom(object);
            return stripMetadata(writer.compound());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    public static boolean fromNBT(CompoundTag settings, Object object) {
        try {
            NBTReader reader = new NBTReader(settings);
            return Deserializer.deserialize(reader, object);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Stream<String> streamKeys(CompoundTag compound) {
        return compound.getAllKeys()
                .stream()
                .filter(name -> !name.startsWith("#"))
                .sorted(Comparator.comparingInt(name -> compound.getCompound("#" + name).getInt("order")));
    }

    public static <T extends Tag> T stripMetadata(T tag) {
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new LinkedList<>(compound.getAllKeys());
            for (String key : keys) {
                if (!key.isEmpty() && key.charAt(0) == '#') {
                    compound.remove(key);
                } else {
                    Tag child = compound.get(key);
                    if (child != null) {
                        stripMetadata(child);
                    }
                }
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                stripMetadata(list.get(i));
            }
        }
        return tag;
    }
}
