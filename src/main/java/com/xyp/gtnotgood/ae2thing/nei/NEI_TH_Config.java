package com.xyp.gtnotgood.ae2thing.nei;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.recipes.FluidRecipe;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

@SuppressWarnings("unused")
public class NEI_TH_Config implements IConfigureNEI {

    private static final ConfigTagParent tag = NEIClientConfig.global.config;

    @Override
    public void loadConfig() {
        API.registerNEIGuiHandler(new AE2TH_NEIGuiHandler());
        NativePatternIngredientCycler.register();
        for (String identifier : FluidRecipe.getSupportRecipes()) {
            if (!API.hasGuiOverlayHandler(GuiQuickEncodingTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiQuickEncodingTerminal.class,
                    QuickTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        // #tr nei.options.gtnotgood
        // # GTNotGood
        // # zh_CN GTNotGood
        // #tr nei.options.gtnotgood.history
        // # Draw terminal history
        // # zh_CN 终端历史记录
        // #tr nei.options.gtnotgood.history.true
        // # Yes
        // # zh_CN 显示
        // #tr nei.options.gtnotgood.history.false
        // # No
        // # zh_CN 隐藏
        // #tr nei.options.gtnotgood.state
        // # Display terminal inventory state
        // # zh_CN 终端库存状态
        // #tr nei.options.gtnotgood.state.true
        // # Yes
        // # zh_CN 显示
        // #tr nei.options.gtnotgood.state.false
        // # No
        // # zh_CN 隐藏
        // #tr nei.options.gtnotgood.ultra_terminal_mode
        // # Universal terminal support selection mode in the terminal menu
        // # zh_CN 终端菜单中通用无限终端支持选择模式
        // #tr nei.options.gtnotgood.ultra_terminal_mode.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.ultra_terminal_mode.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_search_names
        // # Dual interface terminal autofill recipe name to search field
        // # zh_CN 二合一接口终端自动填充合成菜单名字
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_search_names.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_search_names.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.dual_interface_terminal_append_circuit_damage
        // # Dual interface terminal append NC items (molds, circuit boards, etc.) to recipe name
        // # zh_CN 二合一接口终端将nc物品(模头，电路板等)追加到配方名称
        // #tr nei.options.gtnotgood.dual_interface_terminal_append_circuit_damage.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.dual_interface_terminal_append_circuit_damage.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.nei_craft_item
        // # Middle click NEI panel to quick order item from ae terminal
        // # zh_CN NEI中鼠标中键快速下单终端中的物品
        // #tr nei.options.gtnotgood.nei_craft_item.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.nei_craft_item.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_circuit
        // # Dual interface terminal autofill recipe name to search field,ignore circuit
        // # zh_CN 二合一接口终端自动填充合成菜单名字时,忽略电路编号
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_circuit.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.dual_interface_terminal_fill_circuit.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.block_render
        // # Pattern ignore hatches when encoding
        // # zh_CN 样板终端编码时忽略功能仓室
        // #tr nei.options.gtnotgood.block_render.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.block_render.false
        // # No
        // # zh_CN 否
        // #tr nei.options.gtnotgood.pinned_bar
        // # Enable terminal crafting pinned bar
        // # zh_CN 终端合成固定栏
        // #tr nei.options.gtnotgood.pinned_bar.true
        // # Yes
        // # zh_CN 显示
        // #tr nei.options.gtnotgood.pinned_bar.false
        // # No
        // # zh_CN 隐藏
        // #tr nei.options.gtnotgood.pinned_bar_remove
        // # Pinned bar remove crafting finished item
        // # zh_CN 固定栏移除合成结束物品
        // #tr nei.options.gtnotgood.pinned_bar_remove.true
        // # Default
        // # zh_CN 默认
        // #tr nei.options.gtnotgood.pinned_bar_remove.false
        // # Never
        // # zh_CN 溢出移除
        // #tr nei.options.gtnotgood.pinned_bar_crafting_state
        // # Pinned bar preview current item crafting state
        // # zh_CN 固定栏中预览当前物品合成状态
        // #tr nei.options.gtnotgood.pinned_bar_crafting_state.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtnotgood.pinned_bar_crafting_state.false
        // # No
        // # zh_CN 否
        API.addOption(new BaseToggleButton(ButtonConstants.HISTORY, false));
        API.addOption(new BaseToggleButton(ButtonConstants.INVENTORY_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.ULTRA_TERMINAL_MODE));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL, false));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR)); //remove
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_REMOVE));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_CRAFTING_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.NEI_CRAFT_ITEM));
        if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT, false));
        }
        if (Mods.BLOCK_RENDERER.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.BLOCK_RENDER));
        }
    }

    public static boolean getConfigValue(String identifier) {
        return tag.getTag(identifier)
            .getBooleanValue(true);
    }

    @Override
    public String getName() {
        return AE2Thing.NAME;
    }

    @Override
    public String getVersion() {
        return AE2Thing.VERSION;
    }
}
