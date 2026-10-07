package com.xyp.gtnotgood.common.items.largeinterface;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.blocks.largeinterface.BlockLargeInterface;
import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.parts.largeinterface.PartLargeInterface;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemLargeInterface extends GTNGItem implements IPartItem {

    // #tr item.gtnotgood.large_interface_part.name
    // # ME Large Dual Interface
    // # zh_CN ME 大容量二合一接口
    public ItemLargeInterface() {
        super("large_interface_part");
        setTextureName(ModList.AE2FluidCraft.getResourcePath("interface/fluid_interface_Orange"));
    }

    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartLargeInterface(stack);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        BlockLargeInterface.addInterfaceInformation(lines);
    }
}
