package com.xyp.gtnotgood.client.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;

public final class FragmentWorldCollectionFrontend extends RecipeMapFrontend {

    public FragmentWorldCollectionFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(8 + i % 2 * 18, 26 + i / 2 * 18));
        return positions;
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        List<Pos2d> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(new Pos2d(106 + i % 3 * 18, 8 + i / 3 * 18));
        return positions;
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return count == 0 ? Collections.emptyList() : Collections.singletonList(new Pos2d(8, 80));
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return count == 0 ? Collections.emptyList() : Collections.singletonList(new Pos2d(124, 86));
    }
}
