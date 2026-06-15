package com.terraforged.mod.worldgen.cave;

/**
 * Per-chunk synapse column budget. Mega/giga carve at full strength and do not consume this budget.
 * {@code cave_percent} scales column count only; carved columns keep their natural vertical size.
 */
public final class CaveDensityBudget {
    private final boolean limitYzBlocks;
    private int xyRemaining;
    private int yzRemaining;

    public CaveDensityBudget(CaveDensitySettings settings) {
        this.xyRemaining = settings.resolveXyBudget();
        this.yzRemaining = settings.resolveYzBudget();
        this.limitYzBlocks = settings.yzLimit() != null;
    }

    public static CaveDensityBudget unlimited() {
        CaveDensityBudget budget = new CaveDensityBudget(CaveDensitySettings.DEFAULT);
        budget.xyRemaining = Integer.MAX_VALUE / 4;
        budget.yzRemaining = Integer.MAX_VALUE / 4;
        return budget;
    }

    public void consumeMegaGiga(int columnCount, int verticalBlocks) {
    }

    public boolean canCarveSecondary(int verticalSpan) {
        if (this.xyRemaining <= 0) {
            return false;
        }
        if (!this.limitYzBlocks) {
            return true;
        }
        return verticalSpan > 0 && this.yzRemaining >= verticalSpan;
    }

    public void consumeSecondary(int verticalSpan) {
        if (this.xyRemaining <= 0) {
            return;
        }
        this.xyRemaining--;
        if (this.limitYzBlocks && verticalSpan > 0) {
            this.yzRemaining = Math.max(0, this.yzRemaining - verticalSpan);
        }
    }

    public int xyRemaining() {
        return this.xyRemaining;
    }

    public int yzRemaining() {
        return this.yzRemaining;
    }
}
