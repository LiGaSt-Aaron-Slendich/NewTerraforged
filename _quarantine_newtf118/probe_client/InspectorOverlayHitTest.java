package com.terraforged.mod.internal.probe.client;

import com.terraforged.mod.internal.probe.TfOverlayColumn;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

final class InspectorOverlayHitTest {
    private InspectorOverlayHitTest() {
    }

    static String hoverLabel(InspectorCamera camera, List<TfOverlayColumn> columns, long window, int screenW, int screenH, float fov) {
        if (columns.isEmpty()) {
            return null;
        }
        double[] mx = new double[1];
        double[] my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);
        Vec3 start = camera.eyePosition();
        Vec3 dir = InspectorInputHelper.lookFromScreen(mx[0], my[0], screenW, screenH, camera.yaw(), camera.pitch(), fov);
        double best = Double.MAX_VALUE;
        String bestLabel = null;
        for (TfOverlayColumn column : columns) {
            AABB box = InspectorOverlayHitTest.boxFor(column);
            Double dist = InspectorOverlayHitTest.intersect(start, dir, box);
            if (dist != null && dist < best) {
                best = dist;
                bestLabel = column.label();
            }
        }
        return bestLabel;
    }

    private static AABB boxFor(TfOverlayColumn column) {
        if (column.surface()) {
            return new AABB(column.x(), column.yMin(), column.z(), column.xEnd(), column.yMin() + 1.05, column.zEnd());
        }
        return new AABB(column.x(), column.yMin(), column.z(), column.xEnd(), column.yMax() + 1, column.zEnd());
    }

    private static Double intersect(Vec3 origin, Vec3 dir, AABB box) {
        double tMin = 0.0;
        double tMax = 256.0;
        for (int axis = 0; axis < 3; ++axis) {
            double o = axis == 0 ? origin.x : (axis == 1 ? origin.y : origin.z);
            double d = axis == 0 ? dir.x : (axis == 1 ? dir.y : dir.z);
            double min = axis == 0 ? box.minX : (axis == 1 ? box.minY : box.minZ);
            double max = axis == 0 ? box.maxX : (axis == 1 ? box.maxY : box.maxZ);
            if (Math.abs(d) < 1.0E-8) {
                if (o < min || o > max) {
                    return null;
                }
                continue;
            }
            double inv = 1.0 / d;
            double t1 = (min - o) * inv;
            double t2 = (max - o) * inv;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMax < tMin) {
                return null;
            }
        }
        return tMin >= 0.0 ? tMin : null;
    }
}
