package com.terraforged.mod.client.gui.element;

import com.terraforged.engine.serialization.serializer.Serializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Shared helpers for settings widgets bound to Serializer meta NBT. */
public interface Element {
    default List<String> getTooltip() {
        return Collections.emptyList();
    }

    static String getDisplayName(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        if (meta.contains(Serializer.DISPLAY)) {
            return meta.getString(Serializer.DISPLAY);
        }
        return name;
    }

    static List<String> getToolTip(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        if (!meta.contains(Serializer.COMMENT)) {
            return Collections.emptyList();
        }
        Tag comment = meta.get(Serializer.COMMENT);
        if (comment instanceof ListTag list) {
            List<String> lines = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                lines.add(list.getString(i));
            }
            return lines;
        }
        return List.of(meta.getString(Serializer.COMMENT));
    }
}
