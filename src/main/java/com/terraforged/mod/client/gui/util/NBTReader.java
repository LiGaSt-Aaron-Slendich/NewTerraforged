package com.terraforged.mod.client.gui.util;

import com.terraforged.engine.serialization.serializer.Reader;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

/** {@link Reader} over Mojang NBT tags for engine {@code Deserializer}. */
public final class NBTReader implements Reader {
    private final Tag tag;

    public NBTReader(Tag tag) {
        this.tag = tag == null ? new CompoundTag() : tag;
    }

    @Override
    public int getSize() {
        if (this.tag instanceof CompoundTag compound) {
            return compound.size();
        }
        if (this.tag instanceof ListTag list) {
            return list.size();
        }
        return 0;
    }

    @Override
    public Reader getChild(String key) {
        if (this.tag instanceof CompoundTag compound) {
            Tag child = compound.get(key);
            return new NBTReader(child == null ? new CompoundTag() : child);
        }
        return new NBTReader(new CompoundTag());
    }

    @Override
    public Reader getChild(int index) {
        if (this.tag instanceof ListTag list && index >= 0 && index < list.size()) {
            return new NBTReader(list.get(index));
        }
        return new NBTReader(new CompoundTag());
    }

    @Override
    public Collection<String> getKeys() {
        if (this.tag instanceof CompoundTag compound) {
            return compound.getAllKeys();
        }
        return Collections.emptySet();
    }

    @Override
    public String getString() {
        return this.tag.getAsString();
    }

    @Override
    public boolean getBool() {
        if (this.tag instanceof NumericTag numeric) {
            return numeric.getAsByte() != 0;
        }
        return Boolean.parseBoolean(this.getString());
    }

    @Override
    public float getFloat() {
        if (this.tag instanceof NumericTag numeric) {
            return numeric.getAsFloat();
        }
        try {
            return Float.parseFloat(this.getString());
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    @Override
    public int getInt() {
        if (this.tag instanceof NumericTag numeric) {
            return numeric.getAsInt();
        }
        try {
            return Integer.parseInt(this.getString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
