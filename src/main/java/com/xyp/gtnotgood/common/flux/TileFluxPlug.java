/* Copyright (c) 2018 Ollie Lansdell. MIT; ported from sonar.flux.common.tileentity.TileFluxPlug. */
package com.xyp.gtnotgood.common.flux;

/** Flux Networks plug port: accepts GT EU packets and credits the owner's GTNH wireless account. */
public class TileFluxPlug extends TileFluxConnector {

    @Override
    public boolean isPlug() {
        return true;
    }
}
