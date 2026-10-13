// SPDX-License-Identifier: LGPL-3.0-only

package com.xyp.gtnotgood.common.wirelessfrequency;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

public interface IBlockStateListener {

    void onBlockChanged(BlockPos pos);
}
