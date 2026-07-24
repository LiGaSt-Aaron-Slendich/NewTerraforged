package com.terraforged.mod.worldgen.noise.continent.ocean;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Rare deep-ocean volcano tips (replacement for IslandScatter.evalOceanVolcano).
 */
public final class DeepVolcano {
    public static final int VOLC_CELL = 10000;
    public static final int SHIP_VOLC_CELL = 5200;

    public record Result(boolean hit, boolean pipe, float heightBoost, float strength) {
        public static final Result NONE = new Result(false, false, 0.0F, 0.0F);
    }

    private DeepVolcano() {
    }

    public static Result eval(
            float worldX,
            float worldZ,
            int seed,
            float deepAllow,
            boolean shipwrecked
    ) {
        return eval(worldX, worldZ, seed, deepAllow, shipwrecked, shipwrecked ? 0.85F : 0.55F);
    }

    public static Result eval(
            float worldX,
            float worldZ,
            int seed,
            float deepAllow,
            boolean shipwrecked,
            float densitySlider
    ) {
        if (deepAllow < 0.08F) {
            return Result.NONE;
        }
        float density = NoiseUtil.clamp(densitySlider, 0.0F, 1.0F) * (shipwrecked ? 0.75F : 0.28F);
        float gate = deepAllow * (shipwrecked ? 1.15F : 1.0F);
        int cell = shipwrecked ? SHIP_VOLC_CELL : VOLC_CELL;
        int cx = NoiseUtil.floor(worldX / (float) cell);
        int cz = NoiseUtil.floor(worldZ / (float) cell);
        float place = hash01(seed ^ 0xB01C, cx, cz);
        float need = 1.0F - density * (0.14F + gate * 0.40F);
        if (place < need) {
            return Result.NONE;
        }
        float centerX = (cx + 0.5F) * cell + (hash01(seed, cx, cz) - 0.5F) * cell * 0.4F;
        float centerZ = (cz + 0.5F) * cell + (hash01(seed ^ 3, cx, cz) - 0.5F) * cell * 0.4F;
        float radius = 55.0F + hash01(seed ^ 9, cx, cz) * 200.0F;
        // Stable per-cell offset only — per-sample jitter moved the crater every block
        // and erased VOLCANO_PIPE (cone looked solid with no pipe).
        float jx = (hash01(seed ^ 11, cx, cz) - 0.5F) * radius * 0.10F;
        float jz = (hash01(seed ^ 12, cx, cz) - 0.5F) * radius * 0.10F;
        return cone(worldX, worldZ, centerX + jx, centerZ + jz, radius, seed);
    }

    public static Result cone(float worldX, float worldZ, float centerX, float centerZ, float radius) {
        return cone(worldX, worldZ, centerX, centerZ, radius, 0);
    }

    public static Result cone(
            float worldX, float worldZ, float centerX, float centerZ, float radius, int seed
    ) {
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        // Mild edge warp only — does not relocate the crater centre.
        if (seed != 0) {
            float edgeWarp = (valueNoise2(seed ^ 0xE061, worldX * 0.035F, worldZ * 0.035F) - 0.5F) * 0.10F;
            dist *= 1.0F + edgeWarp;
        }
        if (dist > radius) {
            return Result.NONE;
        }
        float t = 1.0F - dist / radius;
        float craterR = Math.max(22.0F, radius * 0.36F);
        float rimR = Math.max(craterR + 16.0F, radius * 0.55F);
        if (rimR > radius * 0.92F) {
            rimR = radius * 0.92F;
        }
        if (dist <= craterR) {
            float inner = dist / Math.max(1.0F, craterR);
            // Absolute heightNoise for pipe floor — kept below WeightMap rim so crater reads.
            return new Result(true, true, 0.34F + inner * 0.08F, t);
        }
        if (dist <= rimR) {
            float rim = (dist - craterR) / Math.max(1.0E-3F, rimR - craterR);
            return new Result(true, false, 0.58F + rim * 0.22F, t);
        }
        return new Result(true, false, 0.40F + t * 0.32F, t);
    }

    private static float valueNoise2(int seed, float x, float z) {
        int x0 = NoiseUtil.floor(x);
        int z0 = NoiseUtil.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fz * fz * (3.0F - 2.0F * fz);
        float a = hash01(seed, x0, z0);
        float b = hash01(seed, x0 + 1, z0);
        float c = hash01(seed, x0, z0 + 1);
        float d = hash01(seed, x0 + 1, z0 + 1);
        return NoiseUtil.lerp(NoiseUtil.lerp(a, b, u), NoiseUtil.lerp(c, d, u), v);
    }

    private static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
