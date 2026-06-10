package com.terraforged.mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public final class TextComponents {
    private TextComponents() {
    }

    public static Component literal(String text) {
        return new TextComponent(text);
    }
}
