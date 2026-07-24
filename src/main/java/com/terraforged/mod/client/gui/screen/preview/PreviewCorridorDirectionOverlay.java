package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.world.heightmap.ControlPoints;
import com.terraforged.mod.platform.forge.TFNoiseVariantFlags;
import com.terraforged.mod.worldgen.noise.NoiseLevels;
import com.terraforged.mod.worldgen.noise.continent.ContinentGenerator;
import com.terraforged.mod.worldgen.noise.continent.config.ContinentConfig;
import com.terraforged.mod.worldgen.noise.continent.ocean.OceanCorridorGraph;
import com.terraforged.mod.worldgen.settings.ContinentShapeWiring;
import com.terraforged.noise.util.NoiseUtil;

/**
 * EGF Untested → Corridor Direction Overlay: paints directed A→B corridor arrows
 * onto the Customize preview map. Preview-only — does not affect worldgen.
 */
public final class PreviewCorridorDirectionOverlay {
    /** NativeImage ABGR: bright lime arrows. */
    private static final int LINE = rgba(40, 255, 80);
    private static final int HEAD = rgba(255, 230, 40);
    private static final int NODE = rgba(255, 60, 60);

    private PreviewCorridorDirectionOverlay() {
    }

    public static boolean shouldApply() {
        return TFNoiseVariantFlags.corridorDirectionOverlayEnabled();
    }

    public static void apply(
            NativeImage image,
            Settings settings,
            int seed,
            int centerX,
            int centerZ,
            int zoom,
            int tileSize
    ) {
        if (!shouldApply() || image == null || settings == null || settings.world == null) {
            return;
        }
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
        float frequency = 1.0F / continentScale;
        int half = tileSize / 2;

        ContinentGenerator continent;
        OceanCorridorGraph graph;
        try {
            continent = buildPreviewContinent(settings, seed);
            graph = OceanCorridorGraph.build(continent, partners, maxDist);
        } catch (Throwable t) {
            return;
        }
        if (graph == null || !graph.active()) {
            return;
        }

        // Landmass centres first (red dots).
        for (long key : graph.landKeys()) {
            int cx = com.terraforged.engine.util.pos.PosUtil.unpackLeft(key);
            int cy = com.terraforged.engine.util.pos.PosUtil.unpackRight(key);
            var cell = continent.getCell(cx, cy);
            int[] pxz = shapeToPixel(cell.px, cell.py, frequency, centerX, centerZ, zoom, half);
            fillDisk(image, pxz[0], pxz[1], 3, NODE, tileSize);
        }

        for (OceanCorridorGraph.DirectedEdge edge : graph.edges()) {
            int[] a = shapeToPixel(edge.fromX(), edge.fromY(), frequency, centerX, centerZ, zoom, half);
            int[] b = shapeToPixel(edge.toX(), edge.toY(), frequency, centerX, centerZ, zoom, half);
            drawArrow(image, a[0], a[1], b[0], b[1], tileSize);
        }
    }

    private static int[] shapeToPixel(
            float shapeX,
            float shapeY,
            float frequency,
            int centerX,
            int centerZ,
            int zoom,
            int half
    ) {
        // Inverse of preview world→shape: shape ≈ world * frequency.
        float worldX = shapeX / Math.max(1.0E-6F, frequency);
        float worldZ = shapeY / Math.max(1.0E-6F, frequency);
        int lx = NoiseUtil.round((worldX - centerX) / (float) Math.max(1, zoom) + half);
        int lz = NoiseUtil.round((worldZ - centerZ) / (float) Math.max(1, zoom) + half);
        return new int[]{lx, lz};
    }

    private static void drawArrow(NativeImage image, int x0, int y0, int x1, int y1, int size) {
        drawLine(image, x0, y0, x1, y1, LINE, size, 1);
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = NoiseUtil.sqrt(dx * dx + dy * dy);
        if (len < 4.0F) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        // Arrow head ~8px back from tip, ±5px wings.
        float hx = x1 - ux * 8.0F;
        float hy = y1 - uy * 8.0F;
        float px = -uy;
        float py = ux;
        int ax = NoiseUtil.round(hx + px * 5.0F);
        int ay = NoiseUtil.round(hy + py * 5.0F);
        int bx = NoiseUtil.round(hx - px * 5.0F);
        int by = NoiseUtil.round(hy - py * 5.0F);
        drawLine(image, x1, y1, ax, ay, HEAD, size, 1);
        drawLine(image, x1, y1, bx, by, HEAD, size, 1);
        // Tip highlight.
        plot(image, x1, y1, HEAD, size);
        plot(image, x1 + 1, y1, HEAD, size);
        plot(image, x1, y1 + 1, HEAD, size);
    }

    private static void drawLine(NativeImage image, int x0, int y0, int x1, int y1, int color, int size, int thickness) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            for (int ty = -thickness; ty <= thickness; ty++) {
                for (int tx = -thickness; tx <= thickness; tx++) {
                    plot(image, x + tx, y + ty, color, size);
                }
            }
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
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

    /** NativeImage pixel: A << 24 | B << 16 | G << 8 | R */
    private static int rgba(int r, int g, int b) {
        return r + (g << 8) + (b << 16) + (255 << 24);
    }

    private static ContinentGenerator buildPreviewContinent(Settings settings, int seed) {
        ContinentConfig config = new ContinentConfig();
        config.shape.seed0 = seed ^ 0xC0FFEE;
        config.shape.seed1 = seed ^ 0x51ED51ED;
        ContinentShapeWiring.apply(config, settings);
        NoiseLevels noiseLevels = new NoiseLevels(
                false,
                1.0F,
                settings.world.properties.seaLevel,
                Math.max(0, settings.world.properties.seaLevel - 40),
                settings.world.properties.worldHeight,
                0);
        ControlPoints controlPoints = new ControlPoints(settings.world.controlPoints);
        return new ContinentGenerator(config, noiseLevels, controlPoints);
    }
}
