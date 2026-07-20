package com.terraforged.mod.client.gui.util;

import com.terraforged.engine.serialization.serializer.AbstractWriter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ByteTag;

/** {@link AbstractWriter} backed by Mojang {@link CompoundTag} / {@link ListTag}. */
public final class NBTWriter extends AbstractWriter<Tag, CompoundTag, ListTag, NBTWriter> {
    public CompoundTag compound() {
        Tag tag = this.get();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    @Override
    protected NBTWriter self() {
        return this;
    }

    @Override
    protected boolean isObject(Tag value) {
        return value instanceof CompoundTag;
    }

    @Override
    protected boolean isArray(Tag value) {
        return value instanceof ListTag;
    }

    @Override
    protected void add(CompoundTag parent, String key, Tag value) {
        parent.put(key, value);
    }

    @Override
    protected void add(ListTag parent, Tag value) {
        parent.add(value);
    }

    @Override
    protected CompoundTag createObject() {
        return new CompoundTag();
    }

    @Override
    protected ListTag createArray() {
        return new ListTag();
    }

    @Override
    protected Tag closeObject(CompoundTag o) {
        return o;
    }

    @Override
    protected Tag closeArray(ListTag a) {
        return a;
    }

    @Override
    protected Tag create(String value) {
        return StringTag.valueOf(value);
    }

    @Override
    protected Tag create(int value) {
        return IntTag.valueOf(value);
    }

    @Override
    protected Tag create(float value) {
        return FloatTag.valueOf(value);
    }

    @Override
    protected Tag create(boolean value) {
        return ByteTag.valueOf(value);
    }
}
