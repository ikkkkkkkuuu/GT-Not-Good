// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.function.Supplier;

public interface WirelessInterfaceNameOverride {

    <T> T wirelessFrequency$withoutCustomName(Supplier<T> reader);
}
