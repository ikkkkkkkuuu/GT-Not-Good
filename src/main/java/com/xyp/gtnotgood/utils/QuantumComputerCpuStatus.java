// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.utils;

public interface QuantumComputerCpuStatus {

    byte NORMAL_CPU = 0;
    byte VIRTUAL_CPU = 1;
    byte SPLIT_CPU = 2;

    byte gtng$getCPUType();
}
