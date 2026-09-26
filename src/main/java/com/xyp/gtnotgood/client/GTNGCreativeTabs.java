package com.xyp.gtnotgood.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import tectech.thing.CustomItemList;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/**
 * Defines the creative tabs used by GT Not Good items, blocks, and machines.
 * <p>
 * The machine tab is populated from {@link GTNGItemList} registration rather than by setting a vanilla creative tab on
 * GregTech machine items. This matches the GT-Not-Cool pattern and keeps meta-tile machines grouped under the mod's own
 * machine tab.
 *
 * @see GTNGItemList
 */
public final class GTNGCreativeTabs {

    // #tr itemGroup.GTNGItem
    // # GT Not Good Items
    // # zh_CN GT不好：物品
    public static final CreativeTabs GTNGItem = new CreativeTabs("GTNGItem") {

        @Override
        public Item getTabIconItem() {
            return getEyeOfHarmonyIcon().getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int func_151243_f() {
            return getEyeOfHarmonyIcon().getItemDamage();
        }
    };

    // #tr itemGroup.GTNGItemBlock
    // # GT Not Good Blocks
    // # zh_CN GT不好：方块
    public static final CreativeTabs GTNGItemBlock = new CreativeTabs("GTNGItemBlock") {

        @Override
        public Item getTabIconItem() {
            return getEyeOfHarmonyIcon().getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int func_151243_f() {
            return getEyeOfHarmonyIcon().getItemDamage();
        }
    };

    private static final List<ItemStack> GTNGItemMachineStack = new ArrayList<>();
    private static final List<ItemStack> WIRELESS_HATCH_STACKS = new ArrayList<>();

    /**
     * Routes a GregTech machine stack to its machine or wireless-hatch creative tab.
     * <p>
     * This is called automatically by {@link GTNGItemList#set(ItemStack)} for stacks backed by
     * {@code GregTechAPI.sBlockMachines}. Ordinary items and blocks should instead set their creative tab directly to
     * {@link #GTNGItem} or {@link #GTNGItemBlock}.
     *
     * @param stack machine stack to show in the appropriate GT Not Good tab
     */
    public static void addToMachineList(ItemStack stack) {
        if (stack != null) {
            int id = stack.getItemDamage();
            IMetaTileEntity machine = id >= 0 && id < GregTechAPI.METATILEENTITIES.length
                ? GregTechAPI.METATILEENTITIES[id]
                : null;
            if (machine instanceof MTEHatchWirelessMulti || machine instanceof MTEHatchWirelessDynamoMulti
                || machine instanceof MTEWirelessEnergy) {
                WIRELESS_HATCH_STACKS.add(stack);
            } else {
                GTNGItemMachineStack.add(stack);
            }
        }
    }

    // #tr itemGroup.GTNGItemMachine
    // # GT Not Good Machines
    // # zh_CN GT不好：机器
    public static final CreativeTabs GTNGItemMachine = new CreativeTabs("GTNGItemMachine") {

        @Override
        public Item getTabIconItem() {
            return getEyeOfHarmonyIcon().getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int func_151243_f() {
            return getEyeOfHarmonyIcon().getItemDamage();
        }

        @Override
        public void displayAllReleventItems(List<ItemStack> stackList) {
            stackList.addAll(GTNGItemMachineStack);
            super.displayAllReleventItems(stackList);
        }
    };

    // #tr itemGroup.GTNGWirelessHatches
    // # GT Not Good Wireless Hatches
    // # zh_CN GT不好：无线舱室
    public static final CreativeTabs GTNGWirelessHatches = new CreativeTabs("GTNGWirelessHatches") {

        @Override
        public Item getTabIconItem() {
            return getWirelessHatchIcon().getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int func_151243_f() {
            return getWirelessHatchIcon().getItemDamage();
        }

        @Override
        public void displayAllReleventItems(List<ItemStack> stackList) {
            stackList.addAll(WIRELESS_HATCH_STACKS);
            super.displayAllReleventItems(stackList);
        }
    };

    private GTNGCreativeTabs() {}

    /** Uses a wireless hatch icon after registration, with the existing safe fallback during early client loading. */
    private static ItemStack getWirelessHatchIcon() {
        return GTNGItemList.WirelessLaserEnergyLV_256.hasBeenSet() ? GTNGItemList.WirelessLaserEnergyLV_256.get(1)
            : getEyeOfHarmonyIcon();
    }

    /**
     * Resolves the creative-tab icon stack.
     * <p>
     * TecTech's Eye of Harmony is preferred because the user selected it as the tab icon. The fallback Ender Eye keeps
     * dev clients from crashing if TecTech is absent or its item list is not initialized yet.
     *
     * @return Eye of Harmony stack when available, otherwise a vanilla Ender Eye
     */
    private static ItemStack getEyeOfHarmonyIcon() {
        try {
            if (CustomItemList.Machine_Multi_EyeOfHarmony.hasBeenSet()) {
                return CustomItemList.Machine_Multi_EyeOfHarmony.get(1);
            }
        } catch (RuntimeException | LinkageError ignored) {}
        return new ItemStack(Items.ender_eye);
    }
}
