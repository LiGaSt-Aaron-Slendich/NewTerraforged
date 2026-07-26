package com.terraforged.mod.command;

/** Minimal stub — full background search UI deferred on 1.19 port. */
public final class BackgroundSearchTasks {
    private BackgroundSearchTasks() {}

    public static boolean isCancelRequested() {
        return false;
    }

    public static boolean pollCancel(int iterations, int every) {
        return false;
    }

    public static void requestCancel() {}

    public static void clearCancel() {}
}
