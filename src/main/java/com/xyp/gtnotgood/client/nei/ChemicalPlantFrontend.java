package com.xyp.gtnotgood.client.nei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.gtnewhorizons.modularui.api.math.Pos2d;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;

public final class ChemicalPlantFrontend extends RecipeMapFrontend {

    public ChemicalPlantFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    @Nonnull
    public List<Pos2d> getItemInputPositions(int count) {
        return grid(count, 8, 8, 3);
    }

    @Override
    @Nonnull
    public List<Pos2d> getItemOutputPositions(int count) {
        return grid(count, 98, 86, 4);
    }

    @Override
    @Nonnull
    public List<Pos2d> getFluidInputPositions(int count) {
        return grid(count, 8, 32, 3);
    }

    @Override
    @Nonnull
    public List<Pos2d> getFluidOutputPositions(int count) {
        return grid(count, 98, 8, 4);
    }

    private static List<Pos2d> grid(int count, int x, int y, int columns) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(x + i % columns * 18, y + i / columns * 18));
        return positions;
    }
}
