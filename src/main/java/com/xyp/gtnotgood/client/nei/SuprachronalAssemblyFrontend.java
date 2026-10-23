// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.client.nei;

import java.util.List;
import java.util.function.Supplier;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.maps.AssemblyLineFrontend;
import gregtech.nei.GTNEIDefaultHandler;

public final class SuprachronalAssemblyFrontend extends AssemblyLineFrontend {

    public SuprachronalAssemblyFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
        NEIRecipePropertiesBuilder neiPropertiesBuilder) {
        super(uiPropertiesBuilder, neiPropertiesBuilder);
    }

    @Override
    public void addProgressBar(ModularWindow.Builder builder, GTNEIDefaultHandler.NEITemplateContext ctx) {
        List<Supplier<Float>> progress = splitProgress(ctx.progressSupplier, 17, 18);
        builder.widget(new ProgressBar().setTexture(GTUITextures.PROGRESSBAR_ASSEMBLY_LINE_1, 17)
            .setDirection(ProgressBar.Direction.RIGHT).setProgress(progress.get(0)).setSynced(false, false)
            .setPos(new Pos2d(88, 8).add(ctx.windowOffset)).setSize(17, 72));
        builder.widget(new ProgressBar().setTexture(GTUITextures.PROGRESSBAR_ASSEMBLY_LINE_2, 18)
            .setDirection(ProgressBar.Direction.RIGHT).setProgress(progress.get(1)).setSynced(false, false)
            .setPos(new Pos2d(124, 8).add(ctx.windowOffset)).setSize(18, 72));
    }
}
