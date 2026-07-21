package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.serialization.annotation.Comment;
import com.terraforged.engine.serialization.annotation.NoName;
import com.terraforged.engine.serialization.annotation.Range;
import com.terraforged.engine.serialization.annotation.Serializable;

/** Port of TerraForged 0.2.x {@code PreviewSettings}. */
@Serializable
public class PreviewSettings {

    public static boolean showTooltips = false;
    public static boolean showCoords = false;

    @Range(min = 1, max = 100)
    @Comment("Controls the zoom level of the preview map (1 = 640000 blocks, 100 = closest)")
    public int zoom = 82;

    @NoName
    @Comment("Controls the rendering mode on the preview map")
    public RenderMode display = RenderMode.BIOME_TYPE;

    /**
     * Highlight filter for terrain names (substring match, empty = off).
     * Cycled from the preview UI (e.g. archipelago, volcano, coastal).
     */
    public String terrainFilter = "";
}
