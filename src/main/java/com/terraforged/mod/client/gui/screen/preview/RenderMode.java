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
            Integer flooded = floodColor(cell, levels);
            if (flooded != null) {
                return flooded;
            }
            if (cell.terrain == null) {
                return rgba(0.5F, 0.5F, 0.5F);
            }
            switch (cell.terrain.getCategory()) {
                case DEEP_OCEAN:
                    return rgba(0.63F, 0.65F, 0.8F);
                case SHALLOW_OCEAN:
                    return rgba(0.6F, 0.6F, 0.8F);
                case BEACH:
                    return rgba(0.2F, 0.4F, 0.75F);
                default:
                    Color color = cell.biome != null ? cell.biome.getColor() : Color.GRAY;
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
            if (cell.terrain == null) {
                return rgba(0.5F, 0.5F, 0.5F);
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
            return rgba(cell.terrain != null ? cell.terrain.getRenderHue() : 0.0F, 0.7F, 0.8F);
        }
    },
    /**
     * Tangent-space style normal map for volume (flat ≈ blue 128,128,255).
     * Neighbour heights come from the tile; used by {@link Preview} for HEIGHT mode.
     */
    HEIGHT {
        @Override
        public boolean handlesWater() {
            return true;
        }

        @Override
        public int getColor(Cell cell, Levels levels, float scale, float bias) {
            // Fallback without neighbours — soft elevation tint (rarely used).
            float h = NoiseUtil.clamp(cell.value, 0.0F, 1.0F);
            return heightNormalFromSlope(0.0F, 0.0F, h, levels);
        }
    };

    /** Sobel-ish normal from left/right/down/up neighbour heights (HEIGHT preview). */
    public static int heightNormalColor(float left, float right, float down, float up, float center, Levels levels) {
        float strength = 14.0F;
        float dx = (right - left) * strength;
        float dz = (up - down) * strength;
        return heightNormalFromSlope(dx, dz, center, levels);
    }

    private static int heightNormalFromSlope(float dx, float dz, float height, Levels levels) {
        float invLen = 1.0F / (float) Math.sqrt(dx * dx + dz * dz + 1.0F);
        float nx = -dx * invLen;
        float ny = -dz * invLen;
        float nz = invLen;
        int r = NoiseUtil.round((nx * 0.5F + 0.5F) * 255.0F);
        int g = NoiseUtil.round((ny * 0.5F + 0.5F) * 255.0F);
        int b = NoiseUtil.round((nz * 0.5F + 0.5F) * 255.0F);
        // Slight depth darkening underwater so basins still read as volume.
        float water = levels.water;
        if (height < water) {
            float t = NoiseUtil.clamp(height / Math.max(1.0E-4F, water), 0.0F, 1.0F);
            r = NoiseUtil.round(r * (0.55F + 0.45F * t));
            g = NoiseUtil.round(g * (0.55F + 0.45F * t));
            b = NoiseUtil.round(Math.min(255, b * (0.75F + 0.25F * t)));
        }
        return rgba(r, g, b);
    }

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
