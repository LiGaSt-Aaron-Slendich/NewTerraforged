package com.terraforged.mod.worldgen.cave;

import com.terraforged.mod.worldgen.cave.CaveClimateType;
import com.terraforged.mod.worldgen.cave.CaveStatVector;
import com.terraforged.noise.util.NoiseUtil;
import java.util.EnumMap;
import java.util.Map;

public final class CaveBiomeStats {
    public static final CaveBiomeStats EMPTY = new CaveBiomeStats(CaveStatVector.ZERO, CaveStatVector.ZERO, CaveStatVector.ZERO, 1.0f, Map.of());
    private final CaveStatVector conditions;
    private final CaveStatVector global;
    private final CaveStatVector local;
    private final float localFalloffPerHop;
    private final Map<CaveClimateType, CaveStatVector> globalByClimate;

    public CaveBiomeStats(CaveStatVector conditions, CaveStatVector global, CaveStatVector local, float localFalloffPerHop, Map<CaveClimateType, CaveStatVector> globalByClimate) {
        this.conditions = conditions;
        this.global = global;
        this.local = local;
        this.localFalloffPerHop = localFalloffPerHop;
        this.globalByClimate = globalByClimate.isEmpty() ? Map.of() : Map.copyOf(globalByClimate);
    }

    public CaveStatVector conditions() {
        return this.conditions;
    }

    public CaveStatVector global() {
        return this.global;
    }

    public CaveStatVector local() {
        return this.local;
    }

    public float localFalloffPerHop() {
        return this.localFalloffPerHop;
    }

    public Map<CaveClimateType, CaveStatVector> globalByClimate() {
        return this.globalByClimate;
    }

    /** Rebuild with one axis of conditions / global / local nudged. */
    public CaveBiomeStats withAxis(String which, String axis, float value) {
        CaveStatVector c = this.conditions;
        CaveStatVector g = this.global;
        CaveStatVector l = this.local;
        float v = NoiseUtil.clamp(value, CaveStatVector.MIN, CaveStatVector.MAX);
        if ("conditions".equals(which) || "cond".equals(which)) {
            c = setAxis(c, axis, v);
        } else if ("global".equals(which) || "g".equals(which)) {
            g = setAxis(g, axis, v);
        } else if ("local".equals(which) || "gen".equals(which) || "plus".equals(which)) {
            l = setAxis(l, axis, v);
        }
        return new CaveBiomeStats(c, g, l, this.localFalloffPerHop, this.globalByClimate);
    }

    private static CaveStatVector setAxis(CaveStatVector src, String axis, float v) {
        return switch (axis == null ? "" : axis.toLowerCase()) {
            case "moist", "moisture", "m" -> new CaveStatVector(v, src.temperature(), src.fertility());
            case "temp", "temperature", "t" -> new CaveStatVector(src.moisture(), v, src.fertility());
            case "fert", "fertility", "f" -> new CaveStatVector(src.moisture(), src.temperature(), v);
            default -> src;
        };
    }

    public CaveStatVector globalForClimate(CaveClimateType climate) {
        return this.globalByClimate.getOrDefault(climate, this.global);
    }

    public boolean matches(CaveStatVector pool) {
        return this.matches(pool, 0.0f);
    }

    public boolean matches(CaveStatVector pool, float relax) {
        if (relax <= 0.0f) {
            return pool.meetsConditions(this.conditions);
        }
        CaveStatVector relaxed = new CaveStatVector(this.conditions.moisture() - relax, this.conditions.temperature() - relax, this.conditions.fertility() - relax);
        return pool.meetsConditions(relaxed);
    }

    public boolean hasAnyValue() {
        return !this.conditions.equals(new CaveStatVector(-10.0f, -10.0f, -10.0f)) || !this.global.equals(CaveStatVector.ZERO) || !this.local.equals(CaveStatVector.ZERO) || !this.globalByClimate.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private CaveStatVector conditions = new CaveStatVector(-10.0f, -10.0f, -10.0f);
        private CaveStatVector global = CaveStatVector.ZERO;
        private CaveStatVector local = CaveStatVector.ZERO;
        private float localFalloffPerHop = 1.0f;
        private final Map<CaveClimateType, CaveStatVector> globalByClimate = new EnumMap<CaveClimateType, CaveStatVector>(CaveClimateType.class);

        public Builder conditions(CaveStatVector conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder global(CaveStatVector global) {
            this.global = global;
            return this;
        }

        public Builder local(CaveStatVector local) {
            this.local = local;
            return this;
        }

        public Builder localFalloffPerHop(float value) {
            this.localFalloffPerHop = value;
            return this;
        }

        public Builder globalForClimate(CaveClimateType climate, CaveStatVector vector) {
            this.globalByClimate.put(climate, vector);
            return this;
        }

        public CaveBiomeStats build() {
            return new CaveBiomeStats(this.conditions, this.global, this.local, this.localFalloffPerHop, this.globalByClimate);
        }
    }
}
