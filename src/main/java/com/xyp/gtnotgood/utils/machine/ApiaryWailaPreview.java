package com.xyp.gtnotgood.utils.machine;

import java.util.Arrays;

public final class ApiaryWailaPreview<T> {

    private static final int PREVIEW_LIMIT = 3;

    private final T[] outputs;
    private final int totalCount;

    private ApiaryWailaPreview(T[] outputs, int totalCount) {
        this.outputs = outputs;
        this.totalCount = totalCount;
    }

    public static <T> ApiaryWailaPreview<T> of(T[] outputs) {
        int count = 0;
        if (outputs == null) return new ApiaryWailaPreview<>(null, 0);
        T[] visible = outputs.length <= PREVIEW_LIMIT ? outputs : Arrays.copyOf(outputs, PREVIEW_LIMIT);
        for (T output : outputs) {
            if (output == null) continue;
            if (outputs.length > PREVIEW_LIMIT && count < PREVIEW_LIMIT) visible[count] = output;
            count++;
        }
        if (outputs.length > PREVIEW_LIMIT && count < PREVIEW_LIMIT) visible = Arrays.copyOf(visible, count);
        return new ApiaryWailaPreview<>(visible, count);
    }

    public T[] outputs() {
        return outputs;
    }

    public int totalCount() {
        return totalCount;
    }
}
