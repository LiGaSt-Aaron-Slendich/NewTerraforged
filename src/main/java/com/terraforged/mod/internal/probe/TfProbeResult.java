package com.terraforged.mod.internal.probe;

import java.util.List;

/** Server probe payload for inspector UI (future). */
public record TfProbeResult(int x, int y, int z, List<String> lines) {
}
