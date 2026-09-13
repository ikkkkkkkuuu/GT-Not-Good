/* Copyright (c) 2018 Ollie Lansdell. MIT; ported from sonar.flux.common.tileentity.TileFluxPoint. */
package com.xyp.gtnotgood.common.flux;

/** Flux Networks point port: delivers GT EU packets using the owner's GTNH wireless balance. */
public class TileFluxPoint extends TileFluxConnector {

    @Override
    public boolean isPlug() {
        return false;
    }
}
