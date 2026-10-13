// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

public class WirelessCardActions {

    public static final int PREVIOUS_PAGE = 0;
    public static final int NEXT_PAGE = 1;
    public static final int TOGGLE_AUTO = 2;
    public static final int REFRESH = 3;
    public static final int BACK = 4;
    public static final int RENAME = 5;
    public static final int APPLY_FILTER = 6;
    public static final int CYCLE_FILTER = 7;
    public static final int CYCLE_VISUALISATION = 9;

    public static final int TOGGLE_PAUSE = 10;
    public static final int OPEN_DETAILS = 20;
    public static final int COPY_POSITION = 30;
    public static final int HIGHLIGHT = 40;
    public static final int TELEPORT = 50;
    public static final int CLOSE_DETAILS = 60;
    public static final int TOGGLE_GROUPING = 61;
    public static final int OPEN_SOURCES = 62;
    public static final int SELECT_SOURCE = 63;
    public static final int RENAME_SOURCE = 70;
    public static final int DELETE_SOURCE = 80;
    private static final int ROW_STRIDE = 10;

    private WirelessCardActions() {}

    public static int type(int id) {
        return isRowAction(id) ? id - rowIndex(id) : id;
    }

    public static int rowIndex(int id) {
        return id % ROW_STRIDE;
    }

    public static boolean isRowAction(int id) {
        return id >= TOGGLE_PAUSE && id < TELEPORT + ROW_STRIDE
            || id >= RENAME_SOURCE && id < DELETE_SOURCE + ROW_STRIDE;
    }
}
