package com.terraforged.mod.platform.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TFConfigWarnings {
    private static final List<String> PENDING = Collections.synchronizedList(new ArrayList());

    private TFConfigWarnings() {
    }

    public static void recordFallback(String relativePath, String errorSummary) {
        String message = "[NewTerraforged] " + relativePath + " - syntax error: " + errorSummary + ". Restored bundled default.";
        PENDING.add(message);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<String> drain() {
        List<String> list = PENDING;
        synchronized (list) {
            if (PENDING.isEmpty()) {
                return List.of();
            }
            List<String> copy = List.copyOf(PENDING);
            PENDING.clear();
            return copy;
        }
    }
}
