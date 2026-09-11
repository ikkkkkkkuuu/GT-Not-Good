package com.xyp.gtnotgood.client.nei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.IntegratedProductionFactoryGui.FactoryActions;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.nei.GTNEIDefaultHandler;

/** Appends NEI recipes from the route-list panel bound to the originating MUI2 container. */
public final class FactoryRecipeImport {

    @SubscribeEvent
    public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        for (int i = 0; i < event.buttonList.size(); i++) {
            if (!(event.buttonList.get(i) instanceof GuiOverlayButton button)) continue;
            FactoryActions actions = actions(button.firstGui);
            if (actions != null && actions.importTarget() != -1) {
                event.buttonList.set(i, new ImportButton(button, actions));
            }
        }
    }

    private static FactoryActions actions(GuiContainer firstGui) {
        if (!(firstGui instanceof IMuiScreen screen) || screen.getScreen()
            .getSyncManager() == null) return null;
        return screen.getScreen()
            .getSyncManager()
            .getMainPSM()
            .findSyncHandlerNullable("factoryActions", FactoryActions.class);
    }

    /** Captures the import context when NEI builds the button; closed or locked panels cannot import. */
    private static final class ImportButton extends GuiOverlayButton {

        private final FactoryActions actions;
        private final int node;
        private final FactoryRecipeCatalog.Entry entry;
        private final FactoryText rejection;
        private final String recipeMap;

        private ImportButton(GuiOverlayButton original, FactoryActions actions) {
            super(original.firstGui, original.handlerRef, original.xPosition, original.yPosition);
            this.actions = actions;
            this.node = actions.importTarget();
            if (handlerRef.handler instanceof GTNEIDefaultHandler handler && handlerRef.recipeIndex >= 0
                && handlerRef.recipeIndex < handler.arecipes.size()
                && handler.arecipes
                    .get(handlerRef.recipeIndex) instanceof GTNEIDefaultHandler.CachedDefaultRecipe recipe) {
                entry = FactoryRecipeCatalog.find(handler.getRecipeMap(), recipe.mRecipe);
                FactoryText reason = FactoryRecipeCatalog.unsupportedReason(handler.getRecipeMap(), recipe.mRecipe);
                rejection = reason == null ? FactoryText.IMPORT_UNREGISTERED : reason;
                recipeMap = handler.getRecipeMap() == null ? "" : handler.getRecipeMap().unlocalizedName;
            } else {
                entry = null;
                rejection = FactoryText.IMPORT_HANDLER;
                recipeMap = "";
            }
            enabled = entry != null;
        }

        @Override
        public boolean canFillCraftingGrid() {
            return true;
        }

        @Override
        public boolean hasOverlay() {
            return true;
        }

        @Override
        public void setRequireShiftForOverlayRecipe(boolean require) {
            super.setRequireShiftForOverlayRecipe(false);
            enabled = entry != null;
        }

        @Override
        public List<String> handleTooltip(List<String> tooltip) {
            tooltip.add(entry == null ? rejection.text() : FactoryText.IMPORT_HELP.text());
            if (entry == null && !recipeMap.isEmpty()) {
                tooltip.add(net.minecraft.util.StatCollector.translateToLocal(recipeMap));
                tooltip.add(recipeMap);
            }
            return tooltip;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            if (entry == null || actions.importTarget() != node || actions(firstGui) != actions) return;
            actions.send(node == -2 ? 13 : 8, node, 0, 0, entry.id);
            Minecraft.getMinecraft()
                .displayGuiScreen(firstGui);
        }
    }
}
