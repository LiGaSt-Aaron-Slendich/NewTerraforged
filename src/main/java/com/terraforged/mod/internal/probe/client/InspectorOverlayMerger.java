package com.terraforged.mod.internal.probe.client;

import com.terraforged.mod.internal.probe.TfOverlayColumn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Merges adjacent columns with the same biome into wider boxes (monotonic regions). */
final class InspectorOverlayMerger {
    private InspectorOverlayMerger() {
    }

    static List<TfOverlayColumn> mergeCaveVolumes(List<TfOverlayColumn> columns) {
        ArrayList<TfOverlayColumn> cave = new ArrayList<>();
        for (TfOverlayColumn column : columns) {
            if (!column.surface()) {
                cave.add(column);
            }
        }
        cave.sort(Comparator.comparing(TfOverlayColumn::label)
                .thenComparingInt(TfOverlayColumn::yMin)
                .thenComparingInt(TfOverlayColumn::yMax)
                .thenComparingInt(TfOverlayColumn::z)
                .thenComparingInt(TfOverlayColumn::x));
        ArrayList<TfOverlayColumn> mergedX = InspectorOverlayMerger.mergeAlongX(cave);
        mergedX.sort(Comparator.comparing(TfOverlayColumn::label)
                .thenComparingInt(TfOverlayColumn::yMin)
                .thenComparingInt(TfOverlayColumn::yMax)
                .thenComparingInt(TfOverlayColumn::x)
                .thenComparingInt(TfOverlayColumn::z));
        return InspectorOverlayMerger.mergeAlongZ(mergedX);
    }

    private static ArrayList<TfOverlayColumn> mergeAlongX(List<TfOverlayColumn> cave) {
        ArrayList<TfOverlayColumn> merged = new ArrayList<>();
        int i = 0;
        while (i < cave.size()) {
            TfOverlayColumn seed = cave.get(i);
            int x1 = seed.x();
            int x2 = seed.xEnd();
            int z = seed.z();
            int yMin = seed.yMin();
            int yMax = seed.yMax();
            String label = seed.label();
            int rgb = seed.rgb();
            int zSize = 1;
            ++i;
            while (i < cave.size()) {
                TfOverlayColumn next = cave.get(i);
                if (next.z() != z || !label.equals(next.label()) || next.yMin() != yMin || next.yMax() != yMax || next.x() != x2) {
                    break;
                }
                x2 = next.xEnd();
                ++i;
            }
            merged.add(new TfOverlayColumn(x1, z, yMin, yMax, rgb, label, false, x2 - x1, zSize));
        }
        return merged;
    }

    private static ArrayList<TfOverlayColumn> mergeAlongZ(List<TfOverlayColumn> cave) {
        ArrayList<TfOverlayColumn> merged = new ArrayList<>();
        int i = 0;
        while (i < cave.size()) {
            TfOverlayColumn seed = cave.get(i);
            int x1 = seed.x();
            int x2 = seed.xEnd();
            int z1 = seed.z();
            int z2 = seed.z() + Math.max(1, seed.zSize());
            int yMin = seed.yMin();
            int yMax = seed.yMax();
            String label = seed.label();
            int rgb = seed.rgb();
            ++i;
            while (i < cave.size()) {
                TfOverlayColumn next = cave.get(i);
                if (next.x() != x1 || next.xEnd() != x2 || !label.equals(next.label()) || next.yMin() != yMin || next.yMax() != yMax || next.z() != z2) {
                    break;
                }
                z2 = next.z() + Math.max(1, next.zSize());
                ++i;
            }
            merged.add(new TfOverlayColumn(x1, z1, yMin, yMax, rgb, label, false, x2 - x1, z2 - z1));
        }
        return merged;
    }
}
