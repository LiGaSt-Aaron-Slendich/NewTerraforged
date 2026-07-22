package com.terraforged.mod.worldgen.noise.continent.island;

import com.terraforged.mod.util.MathUtil;
import com.terraforged.noise.util.NoiseUtil;

/**
 * Shared island / archipelago placement math.
 * <ul>
 *   <li>{@link ArchipelagoStyle#ARCHIPELAGO} — 2–5 islands (incl. main), each 50–300 wide</li>
 *   <li>{@link ArchipelagoStyle#SCATTERED} — many islands, each 15–500 wide</li>
 * </ul>
 */
public final class IslandScatter {
    public static final int LAGUNA_MAX_DEPTH = 15;
    /** Cluster anchor spacing. */
    public static final int ARCH_CELL = 9000;
    public static final int ARCH_CELL_COMPACT = 6500;
    public static final int VOLC_CELL = 10000;

    /**
     * ContinentNoise at/above this = shallow/coast/land — island overlays are culled so they
     * are not absorbed into the mainland (no leftover archipelago_mountains on continents).
     */
    public static final float SHORE_CULL_CN = 0.28F;
    /** Extra buffer: high shore proximity + not mid-ocean ⇒ delete cluster islands. */
    public static final float SHORE_CULL_PROXIMITY = 0.55F;

    /** Width = 2 * radius. Scattered: 15–500. Archipelago: 50–300. */
    public static final float SCATTERED_MIN_RADIUS = 7.5F;
    public static final float SCATTERED_MAX_RADIUS = 250.0F;
    public static final float ARCH_MIN_RADIUS = 25.0F;
    public static final float ARCH_MAX_RADIUS = 150.0F;

    /** Water gap between island footprints so members do not fuse into one blob. */
    private static final float ARCH_MIN_GAP = 40.0F;
    private static final float SCATTERED_MIN_GAP = 28.0F;

    /** @deprecated use {@link #SCATTERED_MIN_RADIUS} */
    @Deprecated
    public static final float SAT_MIN_RADIUS = SCATTERED_MIN_RADIUS;
    /** @deprecated use {@link #SCATTERED_MAX_RADIUS} */
    @Deprecated
    public static final float SAT_MAX_RADIUS = SCATTERED_MAX_RADIUS;
    /** @deprecated mother size is style-specific now */
    @Deprecated
    public static final float MOTHER_MIN_RADIUS = 100.0F;
    /** @deprecated mother size is style-specific now */
    @Deprecated
    public static final float MOTHER_MAX_RADIUS = 250.0F;

    /** Scattered: many islets, but not so many they pack into a fake continent. */
    private static final int SCATTERED_MIN_SATS = 12;
    private static final int SCATTERED_MAX_SATS = 36;
    /** Total islands 2–5 ⇒ satellites 1–4. */
    private static final int ARCH_MIN_SATS = 1;
    private static final int ARCH_MAX_SATS = 4;

    public enum ArchipelagoStyle {
        /** 2–5 islands total, 50–300 blocks across. */
        ARCHIPELAGO,
        /** Many islands, 15–500 blocks across. */
        SCATTERED
    }

    private IslandScatter() {
    }

    public static float shoreProximity(float continentNoise, int continentScale) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        float scale = Math.max(500.0F, continentScale);
        float width = 0.10F + 0.22F * NoiseUtil.clamp(scale / 3000.0F, 0.35F, 2.5F);
        float peak = 0.30F;
        float d = (cn - peak) / width;
        float bell = (float) Math.exp(-0.5 * d * d);
        float channel = cn > 0.12F && cn < 0.42F ? 0.25F * (1.0F - Math.abs(cn - 0.28F) / 0.20F) : 0.0F;
        return NoiseUtil.clamp(bell + Math.max(0.0F, channel), 0.0F, 1.0F);
    }

    public static float midOceanAllow(float continentNoise) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        if (cn >= 0.18F) {
            return 0.0F;
        }
        return (0.18F - cn) / 0.18F;
    }

    /**
     * True when this sample is on/near mainland — island land/laguna must not paint here.
     */
    public static boolean tooCloseToShore(float continentNoise, float proximity, float midOcean) {
        float cn = NoiseUtil.clamp(continentNoise, 0.0F, 1.0F);
        if (cn >= SHORE_CULL_CN) {
            return true;
        }
        // Right against the coast belt: delete even if still "deep" enough by cn alone.
        return proximity >= SHORE_CULL_PROXIMITY && midOcean < 0.20F;
    }

    public static float hash01(int seed, int x, int z) {
        int h = MathUtil.hash(seed, x, z);
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    public static float valueNoise2(int seed, float x, float z) {
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
        float ab = a + (b - a) * u;
        float cd = c + (d - c) * u;
        return ab + (cd - ab) * v;
    }

    /** Spacing for lone islands (not archipelago clusters). */
    public static final int SOLO_CELL = 2600;

    public enum ShapeKind {
        ELLIPSE,
        BANANA,
        LOBED,
        CRESCENT,
        RIBBON,
        IRREGULAR
    }

    /**
     * Organic island mask with several shape families. Returns &lt;0 outside, 0..1 inside.
     */
    public static float organicIslandMask(float dx, float dz, float rx, float rz, float angle, int seed, int id) {
        return shapedIslandMask(dx, dz, rx, rz, angle, seed, id, pickShape(seed, id), SAT_MIN_RADIUS, SAT_MIN_RADIUS);
    }

    public static float organicIslandMask(
            float dx, float dz, float rx, float rz, float angle, int seed, int id, float minRx, float minRz
    ) {
        return shapedIslandMask(dx, dz, rx, rz, angle, seed, id, pickShape(seed, id), minRx, minRz);
    }

    public static ShapeKind pickShape(int seed, int id) {
        float r = hash01(seed ^ 0x51A9E, id * 17, id * 31);
        if (r < 0.18F) {
            return ShapeKind.ELLIPSE;
        }
        if (r < 0.36F) {
            return ShapeKind.BANANA;
        }
        if (r < 0.54F) {
            return ShapeKind.LOBED;
        }
        if (r < 0.70F) {
            return ShapeKind.CRESCENT;
        }
        if (r < 0.84F) {
            return ShapeKind.RIBBON;
        }
        return ShapeKind.IRREGULAR;
    }

    public static float shapedIslandMask(
            float dx, float dz, float rx, float rz, float angle, int seed, int id, ShapeKind kind, float minRx, float minRz
    ) {
        float c = NoiseUtil.cos(angle);
        float s = NoiseUtil.sin(angle);
        float u = dx * c - dz * s;
        float v = dx * s + dz * c;
        float warpFreq = id < 0 ? 0.0038F : 0.014F;
        float warpAmp = id < 0 ? 0.85F : 0.70F;
        float warp = (valueNoise2(seed ^ (id * 31), u * warpFreq, v * warpFreq) - 0.5F) * warpAmp;
        float warp2 = (valueNoise2(seed ^ (id * 17 + 3), u * warpFreq * 2.4F, v * warpFreq * 2.4F) - 0.5F) * warpAmp * 0.55F;
        float detail = (valueNoise2(seed ^ (id * 53 + 7), u * warpFreq * 5.0F, v * warpFreq * 5.0F) - 0.5F) * 0.22F;
        float rxw = Math.max(minRx, rx * (1.0F + warp + detail));
        float rzw = Math.max(minRz, rz * (1.0F + warp2 - detail * 0.5F));

        float e;
        switch (kind) {
            case BANANA -> {
                float bend = 0.55F + hash01(seed ^ 44, id, 1) * 0.75F;
                float uu = u + bend * (v * v) / Math.max(1.0F, rzw);
                e = (uu * uu) / (rxw * rxw) + (v * v) / (rzw * rzw * 0.72F);
            }
            case LOBED -> {
                float ang = (float) Math.atan2(v, u);
                float lobes = 2.5F + hash01(seed ^ 55, id, 2) * 3.5F;
                float lobe = 1.0F + 0.38F * NoiseUtil.cos(ang * lobes);
                e = ((u * u) / (rxw * rxw) + (v * v) / (rzw * rzw)) / Math.max(0.45F, lobe);
            }
            case CRESCENT -> {
                float gap = 0.35F + hash01(seed ^ 66, id, 3) * 0.35F;
                float base = (u * u) / (rxw * rxw) + (v * v) / (rzw * rzw);
                float biteRx = rxw * (0.55F + gap * 0.25F);
                float biteRz = rzw * (0.55F + gap * 0.25F);
                float bite = ((u - rxw * 0.35F) * (u - rxw * 0.35F)) / (biteRx * biteRx)
                        + (v * v) / (biteRz * biteRz);
                if (bite < 1.0F) {
                    return -1.0F;
                }
                e = base;
            }
            case RIBBON -> {
                float thin = 0.22F + hash01(seed ^ 77, id, 4) * 0.28F;
                e = (u * u) / (rxw * rxw) + (v * v) / (rzw * rzw * thin * thin);
            }
            case IRREGULAR -> {
                float n = valueNoise2(seed ^ 88, u * 0.009F, v * 0.009F);
                float n2 = valueNoise2(seed ^ 89, u * 0.021F, v * 0.021F);
                float scale = 0.70F + n * 0.55F + n2 * 0.25F;
                e = ((u * u) / (rxw * rxw) + (v * v) / (rzw * rzw)) / Math.max(0.4F, scale);
            }
            default -> e = (u * u) / (rxw * rxw) + (v * v) / (rzw * rzw);
        }
        if (e >= 1.0F) {
            return -1.0F;
        }
        return 1.0F - e;
    }

    public record ClusterEval(
            boolean land,
            boolean laguna,
            float heightBoost,
            boolean volcano,
            boolean pipe,
            Landform landform,
            Hydrology hydrology,
            ArchipelagoStyle style
    ) {
        public static final ClusterEval NONE = new ClusterEval(
                false, false, 0.0F, false, false, Landform.FLATS, Hydrology.NONE, null);

        public static ClusterEval land(float boost, Landform form) {
            return land(boost, form, null);
        }

        public static ClusterEval land(float boost, Landform form, ArchipelagoStyle style) {
            return new ClusterEval(true, false, boost, false, false, form, Hydrology.NONE, style);
        }

        public static ClusterEval landHydrology(float boost, Landform form, Hydrology hydro, float hydroBoost) {
            return landHydrology(boost, form, hydro, hydroBoost, null);
        }

        public static ClusterEval landHydrology(
                float boost, Landform form, Hydrology hydro, float hydroBoost, ArchipelagoStyle style
        ) {
            float h = hydro == Hydrology.NONE ? boost : hydroBoost;
            return new ClusterEval(true, false, h, false, false, form, hydro, style);
        }

        public static ClusterEval laguna(float shelfStrength) {
            return laguna(shelfStrength, null);
        }

        public static ClusterEval laguna(float shelfStrength, ArchipelagoStyle style) {
            return new ClusterEval(false, true, shelfStrength * 0.15F, false, false, Landform.FLATS, Hydrology.NONE, style);
        }

        public static ClusterEval volcano(float boost, boolean pipe) {
            return new ClusterEval(true, false, boost, true, pipe, Landform.MOUNTAINS, Hydrology.NONE, null);
        }

        public boolean scattered() {
            return style == ArchipelagoStyle.SCATTERED;
        }
    }

    public enum Landform {
        FLATS,
        HILLS,
        PLATEAU,
        MOUNTAINS
    }

    /** Inland water on island land — rivers and lakes, not laguna. */
    public enum Hydrology {
        NONE,
        RIVER,
        LAKE
    }

    /**
     * Archipelago / Scattered Archipelago: shared shallow shelf, mother island, satellites, interior laguna.
     */
    public static ClusterEval evalArchipelago(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float proximity,
            float midOcean,
            ArchipelagoStyle style,
            float continentNoise
    ) {
        // Delete clusters that sit on / against the mainland.
        if (tooCloseToShore(continentNoise, proximity, midOcean)) {
            return ClusterEval.NONE;
        }
        // Prefer open water; proximity alone must not spawn clusters into the beach.
        float gate = midOcean * 0.85F + proximity * 0.15F;
        if (gate < 0.10F) {
            return ClusterEval.NONE;
        }

        int cell = style == ArchipelagoStyle.ARCHIPELAGO ? ARCH_CELL_COMPACT : ARCH_CELL;
        int styleSeed = style == ArchipelagoStyle.ARCHIPELAGO ? seed ^ 0xA11A : seed ^ 0x5CA7;
        int cx0 = NoiseUtil.floor(worldX / (float) cell);
        int cz0 = NoiseUtil.floor(worldZ / (float) cell);
        ClusterEval best = ClusterEval.NONE;
        for (int oz = -1; oz <= 1; oz++) {
            for (int ox = -1; ox <= 1; ox++) {
                ClusterEval eval = evalArchipelagoCell(
                        worldX, worldZ, cx0 + ox, cz0 + oz, styleSeed, archipelagoChance,
                        proximity, midOcean, gate, style, cell);
                best = mergeArchipelago(best, eval);
            }
        }
        return best;
    }

    /** @deprecated prefer overload with continentNoise */
    @Deprecated
    public static ClusterEval evalArchipelago(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float proximity,
            float midOcean,
            ArchipelagoStyle style
    ) {
        return evalArchipelago(worldX, worldZ, seed, archipelagoChance, proximity, midOcean, style, 0.0F);
    }

    /** @deprecated prefer {@link #evalArchipelago(float, float, int, float, float, float, ArchipelagoStyle, float)} */
    @Deprecated
    public static ClusterEval evalArchipelago(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float proximity,
            float midOcean
    ) {
        return evalArchipelago(worldX, worldZ, seed, archipelagoChance, proximity, midOcean, ArchipelagoStyle.SCATTERED, 0.0F);
    }

    private static ClusterEval mergeArchipelago(ClusterEval a, ClusterEval b) {
        if (a.land() && b.land()) {
            if (a.hydrology() != Hydrology.NONE && b.hydrology() == Hydrology.NONE) {
                return a;
            }
            if (b.hydrology() != Hydrology.NONE && a.hydrology() == Hydrology.NONE) {
                return b;
            }
            return a.heightBoost() >= b.heightBoost() ? a : b;
        }
        if (a.land()) {
            return a;
        }
        if (b.land()) {
            return b;
        }
        if (a.laguna()) {
            return a;
        }
        if (b.laguna()) {
            return b;
        }
        return ClusterEval.NONE;
    }

    private static ClusterEval evalArchipelagoCell(
            float worldX,
            float worldZ,
            int cx,
            int cz,
            int seed,
            float archipelagoChance,
            float proximity,
            float midOcean,
            float gate,
            ArchipelagoStyle style,
            int cell
    ) {
        boolean scattered = style == ArchipelagoStyle.SCATTERED;
        // Compact archipelagos are less dense; scattered fills more ocean.
        float clusterRate = scattered
                ? 0.18F + archipelagoChance * 0.32F
                : 0.12F + archipelagoChance * 0.28F;
        float place = hash01(seed ^ 99, cx, cz);
        float need = 1.0F - clusterRate * NoiseUtil.clamp(0.35F + gate * 0.85F, 0.0F, 1.0F);
        if (place < need) {
            return ClusterEval.NONE;
        }
        if (hash01(seed ^ 77, cx, cz) > 0.20F + midOcean * 0.80F) {
            return ClusterEval.NONE;
        }

        float jx = (hash01(seed ^ 11, cx, cz) - 0.5F) * cell * 0.42F;
        float jz = (hash01(seed ^ 13, cx, cz) - 0.5F) * cell * 0.42F;
        float centerX = (cx + 0.5F) * cell + jx;
        float centerZ = (cz + 0.5F) * cell + jz;
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;

        float minR = scattered ? SCATTERED_MIN_RADIUS : ARCH_MIN_RADIUS;
        float maxR = scattered ? SCATTERED_MAX_RADIUS : ARCH_MAX_RADIUS;

        float sizeRoll = hash01(seed ^ 2, cx, cz);
        float sizeMul = sizeRoll < 0.25F ? 0.55F + sizeRoll * 1.0F
                : sizeRoll < 0.75F ? 0.85F + (sizeRoll - 0.25F) * 0.9F
                : 1.15F + (sizeRoll - 0.75F) * 1.4F;
        float shelfBase = scattered ? 900.0F : 420.0F;
        float shelfSpan = scattered ? 4200.0F : 1600.0F;
        float shelfRx = (shelfBase + hash01(seed ^ 3, cx, cz) * shelfSpan) * sizeMul;
        float shelfRz = ((scattered ? 700.0F : 320.0F) + hash01(seed ^ 4, cx, cz) * (scattered ? 3800.0F : 1400.0F)) * sizeMul;
        float aspectShelf = 0.35F + hash01(seed ^ 5, cx, cz) * 1.4F;
        shelfRz = Math.min(shelfRx * aspectShelf, shelfRx * 1.8F);
        float shelfAngle = hash01(seed ^ 6, cx, cz) * NoiseUtil.PI2;
        float shelfMin = scattered ? 400.0F : 180.0F;
        float shelf = shapedIslandMask(
                dx, dz, shelfRx, shelfRz, shelfAngle, seed, -1, pickShape(seed ^ 101, cx + cz), shelfMin, shelfMin);
        if (shelf < 0.0F) {
            return ClusterEval.NONE;
        }

        // Mother biased to the large end of the style's width range.
        float motherMin = scattered ? maxR * 0.40F : maxR * 0.45F;
        float motherMax = maxR;
        float motherRx = motherMin + hash01(seed ^ 8, cx, cz) * (motherMax - motherMin);
        float motherRz = motherRx * (0.45F + hash01(seed ^ 9, cx, cz) * 0.95F);
        motherRz = NoiseUtil.clamp(motherRz, minR, maxR);
        motherRx = NoiseUtil.clamp(motherRx, minR, maxR);
        float motherAngle = hash01(seed ^ 19, cx, cz) * NoiseUtil.PI2;
        float motherOx = (hash01(seed ^ 21, cx, cz) - 0.5F) * shelfRx * 0.28F;
        float motherOz = (hash01(seed ^ 25, cx, cz) - 0.5F) * shelfRz * 0.28F;
        float mdx = dx - motherOx;
        float mdz = dz - motherOz;
        ShapeKind motherKind = pickShape(seed ^ 202, cx * 3 + cz);
        float mother = shapedIslandMask(
                mdx, mdz, motherRx, motherRz, motherAngle, seed, 0, motherKind, minR * 0.5F, minR * 0.35F);
        if (mother >= 0.0F) {
            Landform motherForm = pickMotherLandform(seed, cx, cz);
            Hydrology hydro = evalIslandHydrology(mdx, mdz, motherRx, motherRz, motherAngle, seed, cx, cz, true);
            float boost = 0.38F + mother * 0.42F;
            if (motherForm == Landform.MOUNTAINS) {
                boost += 0.12F;
            } else if (motherForm == Landform.PLATEAU) {
                boost += 0.06F;
            }
            if (hydro == Hydrology.LAKE) {
                return ClusterEval.landHydrology(boost, motherForm, hydro, 0.34F, style);
            }
            if (hydro == Hydrology.RIVER) {
                return ClusterEval.landHydrology(boost, motherForm, hydro, 0.36F, style);
            }
            return ClusterEval.land(boost, motherForm, style);
        }

        int minSats = scattered ? SCATTERED_MIN_SATS : ARCH_MIN_SATS;
        int maxSats = scattered ? SCATTERED_MAX_SATS : ARCH_MAX_SATS;
        int sats = minSats + NoiseUtil.floor(hash01(seed ^ 23, cx, cz) * (maxSats - minSats + 1));
        float bestSat = -1.0F;
        Landform bestForm = Landform.FLATS;
        float bestOx = 0.0F;
        float bestOz = 0.0F;
        float bestRx = minR;
        float bestRz = minR;
        float bestAngle = 0.0F;
        for (int i = 0; i < sats; i++) {
            float ox = (hash01(seed ^ (100 + i * 3), cx, cz) - 0.5F) * shelfRx * (scattered ? 1.75F : 1.35F);
            float oz = (hash01(seed ^ (200 + i * 3), cx, cz) - 0.5F) * shelfRz * (scattered ? 1.75F : 1.35F);
            float satRx;
            float satRz;
            float tier = hash01(seed ^ (300 + i), cx, cz);
            Landform satForm;
            float range = maxR - minR;
            if (scattered) {
                if (tier < 0.42F) {
                    satRx = minR + hash01(seed ^ (400 + i), cx, cz) * Math.min(42.0F, range * 0.25F);
                    satForm = Landform.FLATS;
                } else if (tier < 0.82F) {
                    satRx = minR + range * 0.15F + hash01(seed ^ (400 + i), cx, cz) * range * 0.45F;
                    satForm = hash01(seed ^ (700 + i), cx, cz) < 0.55F ? Landform.HILLS : Landform.FLATS;
                } else {
                    satRx = minR + range * 0.45F + hash01(seed ^ (400 + i), cx, cz) * range * 0.55F;
                    float f = hash01(seed ^ (700 + i), cx, cz);
                    satForm = f < 0.35F ? Landform.PLATEAU : (f < 0.65F ? Landform.HILLS : Landform.MOUNTAINS);
                }
            } else {
                // Archipelago: all members 50–300 wide; fewer, fuller islands.
                if (tier < 0.35F) {
                    satRx = minR + hash01(seed ^ (400 + i), cx, cz) * range * 0.40F;
                    satForm = Landform.FLATS;
                } else if (tier < 0.75F) {
                    satRx = minR + range * 0.25F + hash01(seed ^ (400 + i), cx, cz) * range * 0.45F;
                    satForm = hash01(seed ^ (700 + i), cx, cz) < 0.50F ? Landform.HILLS : Landform.FLATS;
                } else {
                    satRx = minR + range * 0.50F + hash01(seed ^ (400 + i), cx, cz) * range * 0.50F;
                    float f = hash01(seed ^ (700 + i), cx, cz);
                    satForm = f < 0.40F ? Landform.PLATEAU : (f < 0.70F ? Landform.HILLS : Landform.MOUNTAINS);
                }
            }
            float aspect = 0.45F + hash01(seed ^ (500 + i), cx, cz) * 0.95F;
            satRz = satRx * aspect;
            satRx = NoiseUtil.clamp(satRx, minR, maxR);
            satRz = NoiseUtil.clamp(satRz, minR, maxR);
            float sang = hash01(seed ^ (600 + i), cx, cz) * NoiseUtil.PI2;
            float sdx = dx - ox;
            float sdz = dz - oz;
            ShapeKind satKind = pickShape(seed ^ (800 + i), i + cx);
            float mask = shapedIslandMask(sdx, sdz, satRx, satRz, sang, seed, i + 1, satKind, minR, minR * 0.6F);
            if (mask > bestSat) {
                bestSat = mask;
                bestForm = satForm;
                bestOx = sdx;
                bestOz = sdz;
                bestRx = satRx;
                bestRz = satRz;
                bestAngle = sang;
            }
        }
        if (bestSat >= 0.0F) {
            Hydrology hydro = evalIslandHydrology(bestOx, bestOz, bestRx, bestRz, bestAngle, seed, cx, cz, false);
            float boost = bestSat < 0.35F ? 0.22F + bestSat * 0.28F : 0.30F + bestSat * 0.32F;
            if (bestForm == Landform.MOUNTAINS) {
                boost += 0.08F;
            } else if (bestForm == Landform.PLATEAU) {
                boost += 0.04F;
            }
            if (hydro == Hydrology.LAKE) {
                return ClusterEval.landHydrology(boost, bestForm, hydro, 0.34F, style);
            }
            if (hydro == Hydrology.RIVER) {
                return ClusterEval.landHydrology(boost, bestForm, hydro, 0.36F, style);
            }
            return ClusterEval.land(boost, bestForm, style);
        }

        // Interior of shelf with no land = laguna (shallow water between islands).
        return ClusterEval.laguna(shelf, style);
    }

    public static ClusterEval evalOceanVolcano(
            float worldX,
            float worldZ,
            int seed,
            float volcanicChance,
            float proximity,
            float midOcean,
            float archipelagoChance,
            float continentNoise
    ) {
        if (tooCloseToShore(continentNoise, proximity, midOcean)) {
            return ClusterEval.NONE;
        }
        float gate = midOcean * (0.55F + archipelagoChance * 0.40F) + proximity * 0.20F;
        if (gate < 0.08F || volcanicChance <= 0.0F) {
            return ClusterEval.NONE;
        }
        float effective = NoiseUtil.clamp(volcanicChance * (1.0F + archipelagoChance * 0.65F), 0.0F, 1.0F);
        int cell = Math.max(4200, (int) (VOLC_CELL / (1.0F + archipelagoChance * 0.85F)));
        int cx = NoiseUtil.floor(worldX / cell);
        int cz = NoiseUtil.floor(worldZ / cell);
        float place = hash01(seed ^ 0xB01C, cx, cz);
        float need = 1.0F - effective * (0.14F + gate * 0.40F);
        if (place < need) {
            return ClusterEval.NONE;
        }
        float centerX = (cx + 0.5F) * cell + (hash01(seed, cx, cz) - 0.5F) * cell * 0.4F;
        float centerZ = (cz + 0.5F) * cell + (hash01(seed ^ 3, cx, cz) - 0.5F) * cell * 0.4F;
        float radius = 55.0F + hash01(seed ^ 9, cx, cz) * 200.0F;
        return evalVolcanoCone(worldX, worldZ, centerX, centerZ, radius);
    }

    /** @deprecated prefer overload with continentNoise */
    @Deprecated
    public static ClusterEval evalOceanVolcano(
            float worldX,
            float worldZ,
            int seed,
            float volcanicChance,
            float proximity,
            float midOcean,
            float archipelagoChance
    ) {
        return evalOceanVolcano(worldX, worldZ, seed, volcanicChance, proximity, midOcean, archipelagoChance, 0.0F);
    }

    /**
     * Lone islands outside archipelago shelves. Density rises with archipelago chance
     * so high settings mix solos into mid-ocean instead of only clusters.
     */
    public static ClusterEval evalIndependentIsland(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float coastalChance,
            float proximity,
            float midOcean,
            boolean shipwrecked,
            float continentNoise
    ) {
        if (!shipwrecked && tooCloseToShore(continentNoise, proximity, midOcean)) {
            return ClusterEval.NONE;
        }
        float pressure = archipelagoChance * 0.60F + coastalChance * 0.20F + (shipwrecked ? 0.25F : 0.0F);
        if (pressure < 0.05F) {
            return ClusterEval.NONE;
        }
        float gate = midOcean * (0.45F + archipelagoChance * 0.50F) + proximity * 0.15F + (shipwrecked ? 0.35F : 0.0F);
        if (gate < 0.08F) {
            return ClusterEval.NONE;
        }
        int cell = Math.max(1400, (int) (SOLO_CELL / (0.75F + pressure)));
        int cx = NoiseUtil.floor(worldX / (float) cell);
        int cz = NoiseUtil.floor(worldZ / (float) cell);
        float place = hash01(seed ^ 0x5010, cx, cz);
        float need = 1.0F - pressure * (0.20F + gate * 0.55F);
        if (place < need) {
            return ClusterEval.NONE;
        }
        float centerX = (cx + 0.5F) * cell + (hash01(seed ^ 1, cx, cz) - 0.5F) * cell * 0.55F;
        float centerZ = (cz + 0.5F) * cell + (hash01(seed ^ 2, cx, cz) - 0.5F) * cell * 0.55F;
        float sizeRoll = hash01(seed ^ 3, cx, cz);
        // Width 15–500 (radius 7.5–250).
        float rx;
        if (sizeRoll < 0.45F) {
            rx = SCATTERED_MIN_RADIUS + sizeRoll * 80.0F;
        } else if (sizeRoll < 0.85F) {
            rx = 40.0F + (sizeRoll - 0.45F) * 280.0F;
        } else {
            rx = 160.0F + (sizeRoll - 0.85F) * (SCATTERED_MAX_RADIUS - 160.0F) / 0.15F;
        }
        rx = NoiseUtil.clamp(rx, SCATTERED_MIN_RADIUS, SCATTERED_MAX_RADIUS);
        float aspect = 0.35F + hash01(seed ^ 4, cx, cz) * 1.25F;
        float rz = NoiseUtil.clamp(rx * aspect, SCATTERED_MIN_RADIUS, SCATTERED_MAX_RADIUS);
        float ang = hash01(seed ^ 5, cx, cz) * NoiseUtil.PI2;
        ShapeKind kind = pickShape(seed ^ 6, cx * 13 + cz);
        float mask = shapedIslandMask(
                worldX - centerX, worldZ - centerZ, rx, rz, ang, seed, 42 + (cx & 15), kind,
                SCATTERED_MIN_RADIUS, SCATTERED_MIN_RADIUS * 0.5F);
        if (mask < 0.0F) {
            return ClusterEval.NONE;
        }
        Landform form = rx > 140.0F
                ? (hash01(seed ^ 7, cx, cz) < 0.45F ? Landform.HILLS : Landform.PLATEAU)
                : (rx > 60.0F && hash01(seed ^ 8, cx, cz) < 0.35F ? Landform.HILLS : Landform.FLATS);
        return ClusterEval.land(0.28F + mask * 0.34F, form);
    }

    /** @deprecated prefer overload with continentNoise */
    @Deprecated
    public static ClusterEval evalIndependentIsland(
            float worldX,
            float worldZ,
            int seed,
            float archipelagoChance,
            float coastalChance,
            float proximity,
            float midOcean,
            boolean shipwrecked
    ) {
        return evalIndependentIsland(
                worldX, worldZ, seed, archipelagoChance, coastalChance, proximity, midOcean, shipwrecked, 0.0F);
    }

    public static ClusterEval evalCoastalFreckle(
            float worldX,
            float worldZ,
            int seed,
            float coastalChance,
            float volcanicChance,
            float continentNoise
    ) {
        // Near-shore ocean only — never on mainland (avoids archipelago terrains on continents).
        if (continentNoise < 0.12F || continentNoise >= SHORE_CULL_CN) {
            return ClusterEval.NONE;
        }
        int ix = NoiseUtil.floor(worldX);
        int iz = NoiseUtil.floor(worldZ);
        float roll = hash01(seed, ix >> 2, iz >> 2);
        float volcanicBand = volcanicChance * 0.10F;
        if (volcanicChance > 0.0F && roll < volcanicBand) {
            float local = hash01(seed ^ 31, ix >> 1, iz >> 1);
            if (local < 0.18F) {
                float centerX = (ix >> 4 << 4) + 8.0F;
                float centerZ = (iz >> 4 << 4) + 8.0F;
                float radius = 50.0F + local * 80.0F;
                return evalVolcanoCone(worldX, worldZ, centerX, centerZ, radius);
            }
            return ClusterEval.NONE;
        }
        if (coastalChance > 0.0F && roll < volcanicBand + coastalChance * 0.45F) {
            float local = hash01(seed ^ 17, ix >> 1, iz >> 1);
            if (local < 0.28F) {
                float ang = hash01(seed ^ 41, ix >> 3, iz >> 3) * NoiseUtil.PI2;
                float rx = SAT_MIN_RADIUS + local * 40.0F;
                float rz = SAT_MIN_RADIUS * 0.7F + local * 28.0F;
                float centerX = (ix >> 3 << 3) + 4.0F;
                float centerZ = (iz >> 3 << 3) + 4.0F;
                float mask = organicIslandMask(worldX - centerX, worldZ - centerZ, rx, rz, ang, seed, 99);
                if (mask >= 0.0F) {
                    return ClusterEval.land(0.25F + mask * 0.25F, Landform.FLATS);
                }
            }
        }
        return ClusterEval.NONE;
    }

    public static ClusterEval evalVolcanoCone(float worldX, float worldZ, float centerX, float centerZ, float radius) {
        float dx = worldX - centerX;
        float dz = worldZ - centerZ;
        float dist = NoiseUtil.sqrt(dx * dx + dz * dz);
        if (dist > radius) {
            return ClusterEval.NONE;
        }
        float t = 1.0F - dist / radius;
        // Pipe must be findable in-world: floor size scales with cone, with a solid minimum.
        float craterR = Math.max(18.0F, radius * 0.34F);
        float rimR = Math.max(craterR + 14.0F, radius * 0.52F);
        if (rimR > radius * 0.92F) {
            rimR = radius * 0.92F;
        }
        if (dist <= craterR) {
            // Distinct crater floor — lower than rim so pipe isn't filled by Math.max land paints.
            float inner = dist / Math.max(1.0F, craterR);
            return ClusterEval.volcano(0.30F + inner * 0.08F, true);
        }
        if (dist <= rimR) {
            float rim = (dist - craterR) / Math.max(1.0E-3F, rimR - craterR);
            return ClusterEval.volcano(0.58F + rim * 0.22F, false);
        }
        return ClusterEval.volcano(0.40F + t * 0.32F, false);
    }

    private static Landform pickMotherLandform(int seed, int cx, int cz) {
        float roll = hash01(seed ^ 701, cx, cz);
        if (roll < 0.38F) {
            return Landform.MOUNTAINS;
        }
        if (roll < 0.68F) {
            return Landform.PLATEAU;
        }
        return Landform.HILLS;
    }

    /**
     * Rivers and lakes on island land (blue in sketch). Laguna is separate shallow ocean between islands.
     */
    private static Hydrology evalIslandHydrology(
            float localX,
            float localZ,
            float islandRx,
            float islandRz,
            float angle,
            int seed,
            int cx,
            int cz,
            boolean mother
    ) {
        float c = NoiseUtil.cos(angle);
        float s = NoiseUtil.sin(angle);
        float u = localX * c - localZ * s;
        float v = localX * s + localZ * c;
        float el = NoiseUtil.sqrt((u * u) / (islandRx * islandRx) + (v * v) / (islandRz * islandRz));
        if (el > 0.96F) {
            return Hydrology.NONE;
        }

        float lakeFrac = mother ? 0.05F + hash01(seed ^ 801, cx, cz) * 0.05F : 0.03F;
        if (el < lakeFrac) {
            float rim = el / Math.max(1.0E-3F, lakeFrac);
            if (rim > 0.5F || valueNoise2(seed ^ 802, u * 0.05F, v * 0.05F) > 0.62F) {
                return Hydrology.LAKE;
            }
        }

        int channels = mother ? 2 + NoiseUtil.floor(hash01(seed ^ 803, cx, cz) * 3.0F) : 1;
        float ang = (float) Math.atan2(v, u);
        if (ang < 0.0F) {
            ang += NoiseUtil.PI2;
        }
        for (int i = 0; i < channels; i++) {
            float ca = hash01(seed ^ (900 + i), cx, cz) * NoiseUtil.PI2;
            float diff = Math.abs(wrapAngle(ang - ca));
            float width = (mother ? 0.055F : 0.04F) + hash01(seed ^ (910 + i), cx, cz) * 0.025F;
            float wobble = (valueNoise2(seed ^ (920 + i), u * 0.012F, v * 0.012F) - 0.5F) * 0.02F;
            if (diff < width * 0.4F + Math.abs(wobble) && el > lakeFrac * 0.75F && el < 0.9F) {
                return Hydrology.RIVER;
            }
        }
        return Hydrology.NONE;
    }

    private static float wrapAngle(float a) {
        float pi = (float) Math.PI;
        while (a > pi) {
            a -= NoiseUtil.PI2;
        }
        while (a < -pi) {
            a += NoiseUtil.PI2;
        }
        return a;
    }
}
