// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.advancedio;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Cable-mounted AdvancedAE IO bus, using the pinned upstream front, side and back textures. */
public final class ItemAdvancedIOBus extends Item implements IPartItem {

    @SideOnly(Side.CLIENT)
    public IIcon sides;
    @SideOnly(Side.CLIENT)
    public IIcon back;

    public ItemAdvancedIOBus() {
        // #tr item.advanced_io_bus.name
        // # ME Advanced IO Bus
        // # zh_CN ME 高级 IO 总线
        setUnlocalizedName("advanced_io_bus");
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setTextureName(ModList.GTNotGood.getResourcePath("advancedio/advanced_io_bus"));
    }

    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartAdvancedIOBus(stack);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return AEApi.instance()
            .partHelper()
            .placeBus(stack, x, y, z, side, player, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        sides = register.registerIcon(ModList.GTNotGood.getResourcePath("advancedio/advanced_io_bus_sides"));
        back = register.registerIcon(ModList.GTNotGood.getResourcePath("advancedio/advanced_io_bus_back"));
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.advancedio.stock
        // # Maintains selected item/fluid amounts; imports unlisted outputs.
        // # zh_CN 定量补充选定物品与流体，回收未标记的产物。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.stock"));
        // #tr tooltip.advancedio.regulate
        // # Optional excess recovery. Respects the connected machine face.
        // # zh_CN 可开关超量回收；遵循所连接机器面的输入输出限制。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.regulate"));
        // #tr tooltip.advancedio.cards
        // # 8 upgrade slots: acceleration, capacity and redstone cards.
        // # zh_CN 8 个升级槽：支持加速卡、容量卡与红石卡。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.cards"));
    }
}
