/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.engine.world.biome.type;

import com.terraforged.engine.world.biome.type.BiomeType;
import com.terraforged.noise.util.NoiseUtil;
import com.terraforged.noise.util.Vec2f;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BiomeTypeLoader {
    private static BiomeTypeLoader instance;
    private final BiomeType[][] map = new BiomeType[256][256];

    public BiomeTypeLoader() {
        this.generateTypeMap();
    }

    public BiomeType[][] getTypeMap() {
        return this.map;
    }

    public Vec2f[] getRanges(BiomeType type) {
        float minTemp = 1.0f;
        float maxTemp = 0.0f;
        float minMoist = 1.0f;
        float maxMoist = 0.0f;
        for (int moist = 0; moist < this.map.length; ++moist) {
            BiomeType[] row = this.map[moist];
            for (int temp = 0; temp < row.length; ++temp) {
                BiomeType t = row[temp];
                if (t != type) continue;
                float temperature = (float)temp / (float)(row.length - 1);
                float moisture = (float)moist / (float)(this.map.length - 1);
                minTemp = Math.min(minTemp, temperature);
                maxTemp = Math.max(maxTemp, temperature);
                minMoist = Math.min(minMoist, moisture);
                maxMoist = Math.max(maxMoist, moisture);
            }
        }
        return new Vec2f[]{new Vec2f(minTemp, maxTemp), new Vec2f(minMoist, maxMoist)};
    }

    private BiomeType getType(int x, int y) {
        return this.map[y][x];
    }

    private void generateTypeMap() {
        try {
            BufferedImage image = ImageIO.read(BiomeType.class.getResourceAsStream("/biomes.png"));
            float xf = (float)image.getWidth() / 256.0f;
            float yf = (float)image.getHeight() / 256.0f;
            for (int y = 0; y < 256; ++y) {
                for (int x = 0; x < 256; ++x) {
                    if (255 - y > x) {
                        this.map[255 - y][x] = BiomeType.ALPINE;
                        continue;
                    }
                    int ix = NoiseUtil.round((float)x * xf);
                    int iy = NoiseUtil.round((float)y * yf);
                    int argb = image.getRGB(ix, iy);
                    Color color = BiomeTypeLoader.fromARGB(argb);
                    this.map[255 - y][x] = BiomeTypeLoader.forColor(color);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BiomeType forColor(Color color) {
        BiomeType type = null;
        int closest = Integer.MAX_VALUE;
        for (BiomeType t : BiomeType.values()) {
            int distance2 = BiomeTypeLoader.getDistance2(color, t.getLookup());
            if (distance2 >= closest) continue;
            closest = distance2;
            type = t;
        }
        if (type == null) {
            return BiomeType.GRASSLAND;
        }
        return type;
    }

    private static int getDistance2(Color a, Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return dr * dr + dg * dg + db * db;
    }

    private static Color fromARGB(int argb) {
        int b = argb & 0xFF;
        int g = argb >> 8 & 0xFF;
        int r = argb >> 16 & 0xFF;
        return new Color(r, g, b);
    }

    private static int dist2(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public static BiomeTypeLoader getInstance() {
        if (instance == null) {
            instance = new BiomeTypeLoader();
        }
        return instance;
    }
}

