package com.xyp.gtnotgood.client.radialmenu.core.geometry;

import java.util.Arrays;
import java.util.Comparator;

public final class ArcLayout {

    private static final double FULL = 360.0;

    private ArcLayout() {}

    public static final class Arc {

        public final double start;

        public final double span;

        public Arc(double start, double span) {
            this.start = start;
            this.span = span;
        }

        public double center() {
            return start + span / 2.0;
        }

        public double end() {
            return start + span;
        }
    }

    public static Arc[] place(double[] centers, double[] spans) {
        int count = centers.length;
        Arc[] placed = new Arc[count];
        if (count == 0) {
            return placed;
        }

        Integer[] order = sortedByCenter(centers);

        double wanted = 0.0;
        for (double span : spans) {
            wanted += span;
        }

        if (wanted >= FULL) {
            double scale = FULL / wanted;
            double cursor = RadialGeometry.normalizeDegrees(centers[order[0]]) - spans[order[0]] * scale / 2.0;
            for (int i = 0; i < count; i++) {
                int index = order[i];
                double span = spans[index] * scale;
                placed[index] = new Arc(cursor, span);
                cursor += span;
            }
            return placed;
        }

        double[] positions = relax(centers, spans, order);
        for (int i = 0; i < count; i++) {
            int index = order[i];
            placed[index] = new Arc(positions[i] - spans[index] / 2.0, spans[index]);
        }
        return placed;
    }

    private static Integer[] sortedByCenter(final double[] centers) {
        Integer[] order = new Integer[centers.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {

            @Override
            public int compare(Integer left, Integer right) {
                return Double.compare(RadialGeometry.normalizeDegrees(centers[left]),
                    RadialGeometry.normalizeDegrees(centers[right]));
            }
        });
        return order;
    }

    private static double[] relax(double[] centers, double[] spans, Integer[] order) {
        int count = order.length;
        double[] positions = new double[count];
        for (int i = 0; i < count; i++) {
            positions[i] = RadialGeometry.normalizeDegrees(centers[order[i]]);
        }
        if (count == 1) {
            return positions;
        }

        int passes = 32 + count * 8;
        for (int pass = 0; pass < passes; pass++) {
            boolean moved = false;
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                double gap = positions[next] - positions[i];
                if (next == 0) {
                    gap += FULL;
                }
                double required = (spans[order[i]] + spans[order[next]]) / 2.0;
                double overlap = required - gap;
                if (overlap <= 1.0e-9) {
                    continue;
                }
                positions[i] -= overlap / 2.0;
                positions[next] += overlap / 2.0;
                moved = true;
            }
            if (!moved) {
                break;
            }
        }
        return positions;
    }
}
