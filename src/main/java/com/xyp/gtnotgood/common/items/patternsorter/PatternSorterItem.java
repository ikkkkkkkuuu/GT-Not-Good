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
import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.utils.enums.ModList;

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
        return new com.xyp.ldlib.integration.modularui.LDLibModularScreen(
            ModList.GTNotGood.getID(),
            panel,
            new com.xyp.gtnotgood.client.gui.PatternSorterView(((PatternSorterGui.Panel) panel).model));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.pattern_sorter.use
        // # Right-click: group inventory patterns by circuit and mold.
        // # zh_CN 右键打开，按电路和模具整理背包中的样板。
        lines.add(StatCollector.translateToLocal("tooltip.pattern_sorter.use"));
    }
}
