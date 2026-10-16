package com.xyp.gtnotgood.ae2thing.quickterminal.client;

public interface InterfacePatternRows {

    int getVisibleRowCount();

    int getVisibleHeight();

    int getActualSlot(int visibleSlot);

    int getVisibleSlot(int actualSlot);
}
