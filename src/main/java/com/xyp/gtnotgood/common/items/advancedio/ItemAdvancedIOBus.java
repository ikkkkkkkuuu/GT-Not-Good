// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.items.advancedio;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.common.parts.advancedio.PartAdvancedIOBus;
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
        return AEApi.instance().partHelper().placeBus(stack, x, y, z, side, player, world);
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
        // # Maintains item and fluid stock in the attached machine; returns unmarked resources to ME.
        // # zh_CN 维持相邻机器的物品与流体库存，将未标记的资源送回 ME。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.stock"));
        // #tr tooltip.advancedio.configure
        // # Set item or fluid samples; middle-click a sample to set its target stock.
        // # zh_CN 放入物品或流体样本，中键设置目标库存。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.configure"));
        // #tr tooltip.advancedio.regulate
        // # Excess stock recovery is optional. Transfers obey the connected face's access rules.
        // # zh_CN 可切换超量回收；传输遵循机器连接面的访问限制。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.regulate"));
        // #tr tooltip.advancedio.speed
        // # Base throughput equals one superluminal card. Acceleration cards increase it further.
        // # zh_CN 基础吞吐量等同一张超级光速卡，可用加速卡继续提升。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.speed"));
        // #tr tooltip.advancedio.cards
        // # 8 upgrade slots for acceleration, capacity and redstone cards.
        // # zh_CN 8 个升级槽，支持加速卡、容量卡与红石卡。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.cards"));
        // #tr tooltip.advancedio.capacity
        // # 18 sample slots; each capacity card adds 9, up to 63.
        // # zh_CN 默认 18 个样本槽，每张容量卡增加 9 个，最多 63 个。
        lines.add(StatCollector.translateToLocal("tooltip.advancedio.capacity"));
    }
}
