package com.xyp.gtnotgood.common.mebridge;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.value.sync.StringSyncValue;

final class MEBridgeGuiSync {

    private MEBridgeGuiSync() {}

    static StringSyncValue editableChannel(Supplier<String> getter, Consumer<String> setter) {
        return new StringSyncValue(getter, setter).allowC2S();
    }

    static StringSyncValue readOnly(Supplier<String> getter) {
        return new StringSyncValue(getter, value -> {});
    }
}
