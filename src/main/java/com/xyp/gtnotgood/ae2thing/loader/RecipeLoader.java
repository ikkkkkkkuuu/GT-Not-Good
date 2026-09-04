package com.xyp.gtnotgood.ae2thing.loader;

import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_INTERFACE_TERM;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_PATTERN_TERM;
import static com.xyp.gtnotgood.ae2thing.loader.ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL;

import com.xyp.gtnotgood.ae2thing.loader.recipe.WirelessTerminalEnergyRecipe;
import com.xyp.gtnotgood.ae2thing.loader.recipe.WirelessTerminalQuantumBridgeRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

public class RecipeLoader implements Runnable {

    public static final RecipeLoader INSTANCE = new RecipeLoader();

    @Override
    public void run() {
        GameRegistry.addShapelessRecipe(
            ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack(),
            WIRELESS_INTERFACE_TERM,
            WIRELESS_PATTERN_TERM.stack());
        WirelessTerminalQuantumBridgeRecipe.register(ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack());
        WirelessTerminalEnergyRecipe.register(ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack());
    }
}
