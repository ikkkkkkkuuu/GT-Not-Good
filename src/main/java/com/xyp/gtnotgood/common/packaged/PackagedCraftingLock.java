// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.packaged;

/** AE2 19.2.17 lock order and icons, adapted without changing the five user-visible modes. */
public enum PackagedCraftingLock {

    // #tr gui.packaged.lock.none
    // # Crafting lock: disabled
    // # zh_CN 合成锁定：关闭
    NONE("gui.packaged.lock.none", 160),
    // #tr gui.packaged.lock.pulse
    // # Lock until the next redstone pulse
    // # zh_CN 派单后锁定，等待下一次红石脉冲
    PULSE("gui.packaged.lock.pulse", 32),
    // #tr gui.packaged.lock.high
    // # Lock while redstone signal is high
    // # zh_CN 有红石信号时锁定
    HIGH("gui.packaged.lock.high", 80),
    // #tr gui.packaged.lock.low
    // # Lock while redstone signal is low
    // # zh_CN 无红石信号时锁定
    LOW("gui.packaged.lock.low", 64),
    // #tr gui.packaged.lock.result
    // # Lock until the result returns to the ME network
    // # zh_CN 派单后锁定，等待产物返回 ME 网络
    RESULT("gui.packaged.lock.result", 112);

    public final String key;
    public final int iconX;

    PackagedCraftingLock(String key, int iconX) {
        this.key = key;
        this.iconX = iconX;
    }

    public static PackagedCraftingLock read(int value) {
        return values()[Math.floorMod(value, values().length)];
    }
}
