package com.xyp.gtnotgood.common.items.stockio;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemStockIOInterface extends GTNGItem implements IPartItem {

    @SideOnly(Side.CLIENT)
    public IIcon sides;
    @SideOnly(Side.CLIENT)
    public IIcon back;

    // #tr item.gtnotgood.stock_io_interface.name
    // # ME Stock IO Interface
    // # zh_CN 库存 IO 接口 (ME)
    public ItemStockIOInterface() {
        super("stock_io_interface");
        setTextureName(ModList.GTNotGood.getResourcePath("advancedio/advanced_io_bus"));
    }

    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartStockIOInterface(stack);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.stock_io_interface.direct
        // # Lets GT machines read recipe inputs and fuel directly from ME. Debits only actual use.
        // # zh_CN 让 GT 机器直接读取 ME 中的配方原料与燃料，只扣除实际消耗。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.direct"));
        // #tr tooltip.stock_io_interface.configure
        // # Attach to the machine, then right-click to mark resources and set their quantities.
        // # zh_CN 连接目标机器，右键标记资源并设置数量。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.configure"));
        // #tr tooltip.stock_io_interface.slots
        // # 900 item marks and 900 fluid marks. Supports reserves, fixed amounts and output recovery.
        // # zh_CN 物品与流体各 900 个标记，支持保留量、固定可用量与产物回收。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.slots"));
        // #tr tooltip.stock_io_interface.network
        // # Requires an active ME connection and a supported GT machine.
        // # zh_CN 需要有效的 ME 连接与受支持的 GT 机器。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.network"));
    }
}
