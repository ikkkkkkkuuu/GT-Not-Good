package com.xyp.gtnotgood.client.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;

/** Dedicated NEI layout: one input, a 4x4 item grid and a separate row of four fluid outputs. */
public final class TransmutationFrontend extends RecipeMapFrontend {

    public TransmutationFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        return count == 0 ? Collections.emptyList() : Collections.singletonList(new Pos2d(24, 35));
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(88 + i % 4 * 18, 8 + i / 4 * 18));
        return positions;
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(88 + i * 18, 86));
        return positions;
    }
}
