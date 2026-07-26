package com.terraforged.mod.command;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cooperative cancellation for background locate searches ({@code /locateterrain}, {@code /locatecave}).
 * Long-running loops poll {@link #isCancelRequested()} and exit early when {@code /stopprocess} is used.
 */
public final class BackgroundSearchTasks {
    private static final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private static final AtomicInteger activeSearches = new AtomicInteger(0);

    private BackgroundSearchTasks() {
    }

    public static void beginSearch() {
        cancelRequested.set(false);
        activeSearches.incrementAndGet();
    }

    public static void endSearch() {
        activeSearches.updateAndGet(n -> Math.max(0, n - 1));
    }

    public static boolean requestCancel() {
        if (activeSearches.get() <= 0) {
            return false;
        }
        cancelRequested.set(true);
        return true;
    }

    public static boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public static boolean isSearching() {
        return activeSearches.get() > 0;
    }

    /** Poll every N loop iterations to avoid atomic overhead on tight inner loops. */
    public static boolean pollCancel(int iteration, int interval) {
        return iteration % interval == 0 && BackgroundSearchTasks.isCancelRequested();
    }
}
