package com.xyp.gtnotgood.common.items.patternsorter;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.client.gui.PatternSorterView;
import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Handheld classifier with no internal storage; the real patterns always remain in the player's inventory. */
public final class PatternSorterItem extends GTNGItem implements IGuiHolder<PlayerInventoryGuiData> {

    public PatternSorterItem() {
        super("pattern_sorter");
        // #tr item.pattern_sorter.name
        // # Pattern Sorter
        // # zh_CN 样板分类工具
        setUnlocalizedName("pattern_sorter");
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
        return stack;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        return new PatternSorterGui(data, sync).build();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return new LDLibModularScreen(ModList.GTNotGood.getID(), panel,
            new PatternSorterView(((PatternSorterGui.Panel) panel).model));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.pattern_sorter.use
        // # Right-click to group inventory patterns by their circuits and molds.
        // # zh_CN 右键打开界面，按电路与模具对背包中的样板分组。
        lines.add(StatCollector.translateToLocal("tooltip.pattern_sorter.use"));
        // #tr tooltip.pattern_sorter.map
        // # Select a recipe map before sorting. Keep the cursor empty.
        // # zh_CN 先选择配方类型，再整理样板；操作时鼠标须为空。
        lines.add(StatCollector.translateToLocal("tooltip.pattern_sorter.map"));
    }
}
