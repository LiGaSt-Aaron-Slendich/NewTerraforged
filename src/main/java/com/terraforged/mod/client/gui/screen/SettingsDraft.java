package com.terraforged.mod.client.gui.screen;

/**
 * Mutable draft for the Generator Settings scaffold.
 * Full TerraSettings / TOML binding comes later — seed drives the live preview for now.
 */
public final class SettingsDraft {
    private int seed;

    public SettingsDraft(int seed) {
        this.seed = seed == -1 ? (int)System.currentTimeMillis() : seed;
    }

    public int seed() {
        return this.seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void randomizeSeed() {
        this.seed = java.util.concurrent.ThreadLocalRandom.current().nextInt();
    }
}
