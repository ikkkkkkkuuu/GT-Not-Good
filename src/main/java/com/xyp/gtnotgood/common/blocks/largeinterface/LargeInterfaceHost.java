package com.xyp.gtnotgood.common.blocks.largeinterface;

import appeng.helpers.IInterfaceHost;

/** Limits the enlarged pattern inventory and disabled stock configuration to this interface variant. */
public interface LargeInterfaceHost extends IInterfaceHost {

    int PATTERN_COUNT = 900;
    int PATTERN_COLUMNS = 9;
    int PATTERN_ROWS = PATTERN_COUNT / PATTERN_COLUMNS;
}
