// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.client.render;

public final class KerrNewmanBlackHoleMath {

    public static final float CORE_RADIUS = 2.5F;
    public static final float EVENT_HORIZON_RADIUS = 4.5F;
    public static final float DISTORTION_STRENGTH = 0.12F;
    public static final int GROW_DURATION = 60;

    private KerrNewmanBlackHoleMath() {}

    public static float coreMaskRadius(float innerRadius) {
        return Math.max(innerRadius - clamp(innerRadius * 0.01F, 1.5F, 6), 0.001F);
    }

    public static int detailLevel(float radiusPixels) {
        return radiusPixels >= 220 ? 2 : radiusPixels >= 100 ? 1 : 0;
    }

    public static float[] sphereVertices(int latitudeSegments, int longitudeSegments) {
        if (latitudeSegments < 2 || longitudeSegments < 3) throw new IllegalArgumentException("Invalid sphere detail");
        float[] vertices = new float[latitudeSegments * longitudeSegments * 18];
        int offset = 0;
        for (int latitude = 0; latitude < latitudeSegments; latitude++) {
            float theta0 = (float) (Math.PI * latitude / latitudeSegments);
            float theta1 = (float) (Math.PI * (latitude + 1) / latitudeSegments);
            float y0 = (float) Math.cos(theta0);
            float y1 = (float) Math.cos(theta1);
            float ring0 = (float) Math.sin(theta0);
            float ring1 = (float) Math.sin(theta1);
            for (int longitude = 0; longitude < longitudeSegments; longitude++) {
                float phi0 = (float) (2 * Math.PI * longitude / longitudeSegments);
                float phi1 = (float) (2 * Math.PI * (longitude + 1) / longitudeSegments);
                float cos0 = (float) Math.cos(phi0);
                float sin0 = (float) Math.sin(phi0);
                float cos1 = (float) Math.cos(phi1);
                float sin1 = (float) Math.sin(phi1);
                offset = vertex(vertices, offset, ring0 * cos0, y0, ring0 * sin0);
                offset = vertex(vertices, offset, ring1 * cos1, y1, ring1 * sin1);
                offset = vertex(vertices, offset, ring1 * cos0, y1, ring1 * sin0);
                offset = vertex(vertices, offset, ring0 * cos0, y0, ring0 * sin0);
                offset = vertex(vertices, offset, ring0 * cos1, y0, ring0 * sin1);
                offset = vertex(vertices, offset, ring1 * cos1, y1, ring1 * sin1);
            }
        }
        return vertices;
    }

    public static Projection projectSphere(float[] modelView, float[] projection, int width, int height, float core,
        float eventHorizon) {
        float[] eye = { modelView[12], modelView[13], modelView[14], modelView[15] };
        float[] center = project(projection, eye[0], eye[1], eye[2], eye[3]);
        if (center[3] <= 0 || width <= 0 || height <= 0) return null;
        float ndcX = center[0] / center[3];
        float ndcY = center[1] / center[3];
        float inner = projectedRadius(projection, eye, core, ndcX, ndcY, width, height);
        float outer = projectedRadius(projection, eye, eventHorizon, ndcX, ndcY, width, height);
        if (outer <= 0.001F || inner >= outer) return null;
        return new Projection((ndcX * 0.5F + 0.5F) * width, (ndcY * 0.5F + 0.5F) * height, inner, outer);
    }

    private static float projectedRadius(float[] projection, float[] eye, float radius, float centerX, float centerY,
        int width, int height) {
        float horizontal = axisRadius(project(projection, eye[0] + radius, eye[1], eye[2], eye[3]), centerX, centerY,
            width, height);
        float vertical = axisRadius(project(projection, eye[0], eye[1] + radius, eye[2], eye[3]), centerX, centerY,
            width, height);
        return Math.max(horizontal, vertical);
    }

    private static float axisRadius(float[] clip, float centerX, float centerY, int width, int height) {
        if (clip[3] <= 0) return 0;
        return (float) Math.hypot((clip[0] / clip[3] - centerX) * 0.5F * width,
            (clip[1] / clip[3] - centerY) * 0.5F * height);
    }

    private static float[] project(float[] matrix, float x, float y, float z, float w) {
        return new float[] { matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12] * w,
            matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13] * w,
            matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14] * w,
            matrix[3] * x + matrix[7] * y + matrix[11] * z + matrix[15] * w };
    }

    private static int vertex(float[] vertices, int offset, float x, float y, float z) {
        vertices[offset++] = x;
        vertices[offset++] = y;
        vertices[offset++] = z;
        return offset;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public static final class Projection {

        public final float centerX;
        public final float centerY;
        public final float innerRadius;
        public final float outerRadius;

        private Projection(float centerX, float centerY, float innerRadius, float outerRadius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
        }
    }

    public static final class Animation {

        private boolean working;
        private long started;
        private long ending;
        private long lastTime = Long.MIN_VALUE;
        private float endingFromScale;

        public float update(boolean active, long worldTime, float partialTicks) {
            if (worldTime < lastTime) {
                working = false;
                endingFromScale = 0;
            }
            lastTime = worldTime;
            if (active != working) {
                if (active) {
                    started = worldTime;
                } else {
                    endingFromScale = clamp((worldTime - started) / (float) GROW_DURATION, 0, 1);
                    ending = worldTime;
                }
                working = active;
            }
            if (working) return clamp((worldTime - started + partialTicks) / GROW_DURATION, 0, 1);
            return endingFromScale - clamp((worldTime - ending + partialTicks) / GROW_DURATION, 0, endingFromScale);
        }
    }
}
