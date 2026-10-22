// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.items.lighthunter;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.machines.multiblock.LightHunterSpaceStation;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class LightHunterItems {

    private LightHunterItems() {}

    public static void init() {
        // #tr item.gtnotgood.astral_array.name
        // # Astral Array
        // # zh_CN 星阵
        register("astral_array", GTNGItemList.AstralArray, 1);
        // #tr item.gtnotgood.compressed_astral_array.name
        // # Compressed Astral Array
        // # zh_CN 压缩星阵
        register("compressed_astral_array", GTNGItemList.CompressedAstralArray,
            LightHunterRecipeRules.COMPRESSED_ARRAY_EQUIVALENT);
    }

    private static void register(String name, GTNGItemList entry, int equivalent) {
        AstralArrayItem item = new AstralArrayItem(name, equivalent);
        GameRegistry.registerItem(item, name);
        entry.set(item);
    }

    private static final class AstralArrayItem extends GTNGItem {

        private final int equivalent;

        private AstralArrayItem(String name, int equivalent) {
            super(name);
            this.equivalent = equivalent;
            setTextureName(ModList.GTNotGood.getResourcePath("lighthunter/" + name));
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
            if (player == null || !player.isSneaking() || stack.stackSize <= 0) return false;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (
                !(tile instanceof IGregTechTileEntity base)
                    || !(base.getMetaTileEntity() instanceof LightHunterSpaceStation station)
            ) return false;
            if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
            if (world.isRemote) return true;

            int installed = station.installAstralArrays(stack.stackSize * equivalent);
            if (installed > 0) {
                if (!player.capabilities.isCreativeMode) stack.stackSize -= (installed + equivalent - 1) / equivalent;
                // #tr gtng.lighthunter.array.installed
                // # Installed %s Astral Arrays. Total: %s
                // # zh_CN 已安装 %s 个星阵，当前总数：%s
                player.addChatComponentMessage(new ChatComponentTranslation("gtng.lighthunter.array.installed",
                    installed, station.getAstralArrayCount()));
            } else {
                // #tr gtng.lighthunter.array.full
                // # The Astral Array limit has been reached
                // # zh_CN 星阵数量已达到上限
                player.addChatComponentMessage(new ChatComponentTranslation("gtng.lighthunter.array.full"));
            }
            return true;
        }

        @Override
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            // #tr gtng.lighthunter.array.install
            // # Sneak-right-click the Light Hunter Space Station to install held arrays
            // # zh_CN 潜行右键猎光号空间站，安装手持星阵
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtng.lighthunter.array.install"));
            if (equivalent > 1) {
                // #tr gtng.lighthunter.array.equivalent
                // # Equivalent to %s Astral Arrays
                // # zh_CN 等效于 %s 个普通星阵
                tooltip.add(EnumChatFormatting.GRAY
                    + StatCollector.translateToLocalFormatted("gtng.lighthunter.array.equivalent", equivalent));
                // #tr gtng.lighthunter.array.partial
                // # Partly installing a compressed array still consumes the entire item
                // # zh_CN 未用满压缩星阵的等效数量时，仍会消耗整件物品
                tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtng.lighthunter.array.partial"));
            }
            // #tr gtng.lighthunter.array.recovery
            // # Recovered as ordinary Astral Arrays when the station is dismantled
            // # zh_CN 拆除空间站时返还普通星阵
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtng.lighthunter.array.recovery"));
        }
    }
}
