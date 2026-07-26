package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.tile.Tile;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.noise.util.NoiseUtil;

/**
 * EGF access gate + preview Corridors toggle: paints directed A→B arrows between
 * large continent centroids extracted from the preview tile (not islands).
 */
public final class PreviewCorridorDirectionOverlay {
    private static final int LINE = rgba(40, 255, 80);
    private static final int HEAD = rgba(255, 230, 40);
    private static final int NODE = rgba(255, 60, 60);

    private PreviewCorridorDirectionOverlay() {
    }

    public static boolean shouldApply(boolean previewToggle) {
        return previewToggle && TFNoiseVariantFlags.corridorDirectionOverlayEnabled();
    }

    public static void apply(
            NativeImage image,
            Tile tile,
            Settings settings,
            int centerX,
            int centerZ,
            int zoom,
            boolean previewToggle
    ) {
        if (!shouldApply(previewToggle) || image == null || tile == null || settings == null || settings.world == null) {
            return;
        }
        try {
            paint(image, tile, settings, centerX, centerZ, zoom);
        } catch (Throwable ignored) {
        }
    }

    private static void paint(
            NativeImage image,
            Tile tile,
            Settings settings,
            int centerX,
            int centerZ,
            int zoom
    ) {
        if (settings.world.properties != null
                && settings.world.properties.worldStyle == WorldSettings.WorldStyle.SHIPWRECKED) {
            return;
        }
        WorldSettings.OceanLandscape ol = settings.world.oceanLandscape != null
                ? settings.world.oceanLandscape
                : new WorldSettings.OceanLandscape();
        int partners = Math.max(1, Math.min(4, ol.corridorPartners));
        float maxDist = NoiseUtil.clamp(ol.corridorMaxDistance, 2.0F, 24.0F);
        int continentScale = Math.max(100, settings.world.continent != null
                ? settings.world.continent.continentScale
                : 3000);
        Levels levels = new Levels(settings.world);
        int size = tile.getBlockSize().size;

        PreviewTileContinentGraph.Graph graph = PreviewTileContinentGraph.build(
                tile, levels, centerX, centerZ, zoom, partners, maxDist, continentScale);
        if (!graph.active()) {
            return;
        }

        for (PreviewTileContinentGraph.Node node : graph.nodes()) {
            fillDisk(image, node.px(), node.pz(), 3, NODE, size);
        }
        for (PreviewTileContinentGraph.Edge edge : graph.edges()) {
            drawArrow(image, edge.x0(), edge.y0(), edge.x1(), edge.y1(), size);
        }
    }

    private static void drawArrow(NativeImage image, int x0, int y0, int x1, int y1, int size) {
        if (!finiteSegment(x0, y0, x1, y1)) {
            return;
        }
        drawLineClipped(image, x0, y0, x1, y1, LINE, size);
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = NoiseUtil.sqrt(dx * dx + dy * dy);
        if (len < 4.0F || !Float.isFinite(len)) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        float hx = x1 - ux * 8.0F;
        float hy = y1 - uy * 8.0F;
        float px = -uy;
        float py = ux;
        int ax = NoiseUtil.round(hx + px * 5.0F);
        int ay = NoiseUtil.round(hy + py * 5.0F);
        int bx = NoiseUtil.round(hx - px * 5.0F);
        int by = NoiseUtil.round(hy - py * 5.0F);
        drawLineClipped(image, x1, y1, ax, ay, HEAD, size);
        drawLineClipped(image, x1, y1, bx, by, HEAD, size);
        plot(image, x1, y1, HEAD, size);
    }

    private static boolean finiteSegment(int x0, int y0, int x1, int y1) {
        int lim = 1 << 20;
        return Math.abs(x0) < lim && Math.abs(y0) < lim && Math.abs(x1) < lim && Math.abs(y1) < lim;
    }

    private static void drawLineClipped(NativeImage image, int x0, int y0, int x1, int y1, int color, int size) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        long err = dx - dy;
        int x = x0;
        int y = y0;
        int maxSteps = size * 4 + 16;
        for (int step = 0; step < maxSteps; step++) {
            plot(image, x, y, color, size);
            plot(image, x + 1, y, color, size);
            plot(image, x, y + 1, color, size);
            if (x == x1 && y == y1) {
                return;
            }
            long e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static void fillDisk(NativeImage image, int cx, int cy, int r, int color, int size) {
        if (cx < -r || cy < -r || cx >= size + r || cy >= size + r) {
            return;
        }
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r2) {
                    plot(image, cx + dx, cy + dy, color, size);
                }
            }
        }
    }

    private static void plot(NativeImage image, int x, int y, int abgr, int size) {
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return;
        }
        image.setPixelRGBA(x, y, abgr);
    }

    private static int rgba(int r, int g, int b) {
        return r + (g << 8) + (b << 16) + (255 << 24);
    }
}
