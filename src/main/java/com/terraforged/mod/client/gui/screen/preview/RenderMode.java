package com.terraforged.mod.client.gui.screen.preview;

import com.terraforged.engine.cell.Cell;
import com.terraforged.engine.world.heightmap.Levels;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;

/** Port of TerraForged 0.2.x {@code RenderMode} (NativeImage ABGR colors). */
public enum RenderMode {
    BIOME_TYPE {
        @Override
        public boolean handlesWater() {
            return true;
        }

        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            // Flood by absolute water line first — otherwise DEEP/SHALLOW_OCEAN
            // categories hide sea-level changes (heights are water-relative).
            Integer flooded = floodColor(cell, levels);
            if (flooded != null) {
                return flooded;
            }
            switch (cell.terrain.getCategory()) {
                case DEEP_OCEAN:
                    return rgba(0.63F, 0.65F, 0.8F);
                case SHALLOW_OCEAN:
                    return rgba(0.6F, 0.6F, 0.8F);
                case BEACH:
                    return rgba(0.2F, 0.4F, 0.75F);
                default:
                    Color color = cell.biome.getColor();
                    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), new float[3]);
                    return rgba(hsb[0], hsb[1], hsb[2] * scale + bias);
            }
        }
    },
    TRANSITION_POINTS {
        @Override
        public boolean handlesWater() {
            return true;
        }

        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            Integer flooded = floodColor(cell, levels);
            if (flooded != null) {
                return flooded;
            }
            switch (cell.terrain.getCategory()) {
                case DEEP_OCEAN:
                    return rgba(0.63F, 0.65F, 0.8F);
                case SHALLOW_OCEAN:
                    return rgba(0.6F, 0.6F, 0.8F);
                case BEACH:
                    return rgba(0.2F, 0.4F, 0.75F);
                case COAST:
                    return rgba(0.35F, 0.75F, 0.65F);
                default:
                    if (cell.terrain.isRiver() || cell.terrain.isWetland()) {
                        return rgba(0.6F, 0.6F, 0.8F);
                    }
                    return rgba(0.3F, 0.7F, 0.5F);
            }
        }
    },
    TEMPERATURE {
        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            return rgba(step(1.0F - cell.temperature, 8) * 0.65F, 0.7F, 0.8F);
        }
    },
    MOISTURE {
        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            return rgba(step(cell.moisture, 8) * 0.65F, 0.7F, 0.8F);
        }
    },
    BIOME {
        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            return rgba(cell.biomeRegionId, 0.7F, 0.8F);
        }
    },
    MACRO_NOISE {
        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            return rgba(cell.macroBiomeId, 0.7F, 0.8F);
        }
    },
    TERRAIN_REGION {
        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            return rgba(cell.terrain.getRenderHue(), 0.7F, 0.8F);
        }
    },
    /**
     * Approximate landscape elevation only (no trees/buildings). Includes seafloor /
     * underwater relief so deep basins and submarine ridges are visible.
     */
    HEIGHT {
        @Override
        public boolean handlesWater() {
            return true;
        }

        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            // cell.value is water-relative height in 0..1; map full column incl. below sea.
            float h = NoiseUtil.clamp(cell.value, 0.0F, 1.0F);
            float water = levels.water;
            if (h < water) {
                // Deep navy → cyan toward the surface.
                float t = NoiseUtil.clamp(h / Math.max(1.0E-4F, water), 0.0F, 1.0F);
                return lerpRgb(8, 18, 48, 40, 140, 200, t);
            }
            // Shore green → highland yellow → peak white.
            float land = NoiseUtil.clamp((h - water) / Math.max(1.0E-4F, 1.0F - water), 0.0F, 1.0F);
            land = (float) NoiseUtil.round(land * 12.0F) / 12.0F; // light banding
            if (land < 0.35F) {
                return lerpRgb(48, 120, 52, 160, 170, 70, land / 0.35F);
            }
            if (land < 0.70F) {
                return lerpRgb(160, 170, 70, 190, 140, 70, (land - 0.35F) / 0.35F);
            }
            return lerpRgb(190, 140, 70, 235, 235, 230, (land - 0.70F) / 0.30F);
        }

        private static int lerpRgb(int r0, int g0, int b0, int r1, int g1, int b1, float t) {
            t = NoiseUtil.clamp(t, 0.0F, 1.0F);
            int r = NoiseUtil.round(r0 + (r1 - r0) * t);
            int g = NoiseUtil.round(g0 + (g1 - g0) * t);
            int b = NoiseUtil.round(b0 + (b1 - b0) * t);
            return rgba(r, g, b);
        }
    };

    public int getColor(Cell cell, Levels levels) {
        if (!handlesWater() && cell.value < levels.water) {
            return getWaterColor();
        }
        float bands = 10.0F;
        float alpha = 0.2F;
        float elevation = (cell.value - levels.water) / (1.0F - levels.water);
        int band = NoiseUtil.round(elevation * bands);
        float scale = 1.0F - alpha;
        float bias = alpha * (band / bands);
        return getColor(cell, levels, scale, bias);
    }

    public abstract int getColor(Cell cell, Levels levels, float scale, float bias);

    public boolean handlesWater() {
        return false;
    }

    public RenderMode next() {
        RenderMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    /** Water when cell height is below the current sea line; deep vs shallow by depth. */
    private static Integer floodColor(Cell cell, Levels levels) {
        if (cell.value >= levels.water) {
            return null;
        }
        float depth = levels.water - cell.value;
        if (depth > 0.05F) {
            return rgba(0.63F, 0.65F, 0.8F);
        }
        if (depth > 0.015F) {
            return rgba(0.6F, 0.6F, 0.8F);
        }
        return getWaterColor();
    }

    private static int getWaterColor() {
        return rgba(40, 140, 200);
    }

    private static float step(float value, int steps) {
        return (float) NoiseUtil.round(value * steps) / (float) steps;
    }

    private static int rgba(float h, float s, float b) {
        int argb = Color.HSBtoRGB(h, s, b);
        int red = argb >> 16 & 0xFF;
        int green = argb >> 8 & 0xFF;
        int blue = argb & 0xFF;
        return rgba(red, green, blue);
    }

    /** NativeImage pixel format: A << 24 | B << 16 | G << 8 | R */
    private static int rgba(int r, int g, int b) {
        return r + (g << 8) + (b << 16) + (255 << 24);
    }
}
