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
            switch (cell.terrain.getCategory()) {
                case DEEP_OCEAN:
                    return rgba(0.63F, 0.65F, 0.8F);
                case SHALLOW_OCEAN:
                    return rgba(0.6F, 0.6F, 0.8F);
                case BEACH:
                    return rgba(0.2F, 0.4F, 0.75F);
                default:
                    if (cell.value < levels.water) {
                        return getWaterColor();
                    }
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
