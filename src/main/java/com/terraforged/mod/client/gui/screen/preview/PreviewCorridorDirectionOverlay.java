package com.terraforged.mod.client.gui.screen.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.terraforged.engine.settings.Settings;
import com.terraforged.engine.settings.WorldSettings;
import com.terraforged.engine.util.pos.PosUtil;
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

    public static boolean shouldApply(boolean previewToggle) {
        return previewToggle && TFNoiseVariantFlags.corridorDirectionOverlayEnabled();
    }

    public static void apply(
            NativeImage image,
            Settings settings,
            int seed,
            int centerX,
            int centerZ,
            int zoom,
            int tileSize,
            boolean previewToggle
    ) {
        if (!shouldApply(previewToggle) || image == null || settings == null || settings.world == null) {
            return;
        }
        try {
            paint(image, settings, seed, centerX, centerZ, zoom, tileSize);
        } catch (Throwable ignored) {
            // Never crash Customize if overlay math fails.
        }
    }

    /** @deprecated use {@link #apply(NativeImage, Settings, int, int, int, int, int, boolean)} */
    @Deprecated
    public static void apply(
            NativeImage image,
            Settings settings,
            int seed,
            int centerX,
            int centerZ,
            int zoom,
            int tileSize
    ) {
        apply(image, settings, seed, centerX, centerZ, zoom, tileSize, false);
    }

    private static void paint(
            NativeImage image,
            Settings settings,
            int seed,
            int centerX,
            int centerZ,
            int zoom,
            int tileSize
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
        float frequency = 1.0F / continentScale;
        int half = tileSize / 2;

        ContinentGenerator continent = buildPreviewContinent(settings, seed);
        OceanCorridorGraph graph = OceanCorridorGraph.build(continent, partners, maxDist);
        if (graph == null || !graph.active()) {
            return;
        }

        for (long key : graph.landKeys()) {
            int cx = PosUtil.unpackLeft(key);
            int cy = PosUtil.unpackRight(key);
            var cell = continent.getCell(cx, cy);
            int px = shapeToPixel(cell.px, frequency, centerX, zoom, half);
            int pz = shapeToPixel(cell.py, frequency, centerZ, zoom, half);
            fillDisk(image, px, pz, 3, NODE, tileSize);
        }

        for (OceanCorridorGraph.DirectedEdge edge : graph.edges()) {
            int x0 = shapeToPixel(edge.fromX(), frequency, centerX, zoom, half);
            int y0 = shapeToPixel(edge.fromY(), frequency, centerZ, zoom, half);
            int x1 = shapeToPixel(edge.toX(), frequency, centerX, zoom, half);
            int y1 = shapeToPixel(edge.toY(), frequency, centerZ, zoom, half);
            drawArrow(image, x0, y0, x1, y1, tileSize);
        }
    }

    private static int shapeToPixel(float shape, float frequency, int center, int zoom, int half) {
        float world = shape / Math.max(1.0E-6F, frequency);
        if (!Float.isFinite(world)) {
            return Integer.MIN_VALUE / 4;
        }
        return NoiseUtil.round((world - center) / (float) Math.max(1, zoom) + half);
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
        // Reject absurd endpoints (would hang Bresenham / flood the image).
        int lim = 1 << 20;
        return Math.abs(x0) < lim && Math.abs(y0) < lim && Math.abs(x1) < lim && Math.abs(y1) < lim;
    }

    private static void drawLineClipped(NativeImage image, int x0, int y0, int x1, int y1, int color, int size) {
        // Quick reject if both ends outside with no chance to cross the image.
        int margin = size + 8;
        boolean aOut = x0 < -margin || y0 < -margin || x0 >= size + margin || y0 >= size + margin;
        boolean bOut = x1 < -margin || y1 < -margin || x1 >= size + margin || y1 >= size + margin;
        if (aOut && bOut) {
            // Still may cross the image — allow but hard-cap steps.
        }
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
