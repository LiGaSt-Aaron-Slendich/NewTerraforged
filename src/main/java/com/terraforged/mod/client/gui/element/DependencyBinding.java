package com.terraforged.mod.client.gui.element;

import com.terraforged.engine.serialization.serializer.Serializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Enables/disables a widget based on Serializer {@code @Restricted} meta. */
public final class DependencyBinding {
    private final String name;
    private final CompoundTag value;
    private final String restrictedField;
    private final String[] options;

    private DependencyBinding(String name, CompoundTag value, String restrictedField, String[] options) {
        this.name = name;
        this.value = value;
        this.restrictedField = restrictedField;
        this.options = options;
    }

    public static DependencyBinding of(String name, CompoundTag value) {
        CompoundTag meta = value.getCompound(Serializer.META_PREFIX + name);
        if (!meta.contains(Serializer.RESTRICTED)) {
            return new DependencyBinding(name, value, null, null);
        }
        CompoundTag restricted = meta.getCompound(Serializer.RESTRICTED);
        String field = restricted.getString(Serializer.RESTRICTED_NAME);
        ListTag list = restricted.getList(Serializer.RESTRICTED_OPTIONS, Tag.TAG_STRING);
        String[] options = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            options[i] = list.getString(i);
        }
        return new DependencyBinding(name, value, field, options);
    }

    public boolean isValid() {
        if (this.restrictedField == null || this.options == null) {
            return true;
        }
        String current = this.value.getString(this.restrictedField);
        if (current.isEmpty() && this.value.contains(this.restrictedField)) {
            // enum stored as string already handled; try raw
            current = String.valueOf(this.value.get(this.restrictedField));
        }
        for (String option : this.options) {
            if (option.equals(current)) {
                return true;
            }
        }
        return false;
    }

    public String name() {
        return this.name;
    }
}
