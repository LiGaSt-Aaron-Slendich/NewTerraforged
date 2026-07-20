package com.terraforged.mod.client.gui.screen.preview;

/** Preview display modes (0.2.x parity — only BIOME_TYPE is wired in the scaffold). */
public enum RenderMode {
    BIOME_TYPE,
    TEMPERATURE,
    MOISTURE,
    HEIGHT;

    public RenderMode next() {
        RenderMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
