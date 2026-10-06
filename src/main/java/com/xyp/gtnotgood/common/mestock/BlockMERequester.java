package com.xyp.gtnotgood.common.mestock;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Requester configuration survives relocation; completed products belong to the ME network. */
public final class BlockMERequester extends Block {

    public BlockMERequester() {
        super(Material.iron);
        // #tr tile.me_requester.name
        // # ME Requester
        // # zh_CN ME 自动请求器
        setBlockName("me_requester");
        setHardness(3);
        setResistance(10);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMERequester();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileMERequester tile)
            StockGuiFactory.instance.open(player, tile);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileMERequester tile) tile.neighborChanged();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileMERequester tile) {
            if (placer instanceof EntityPlayer player) tile.setOwnerName(player.getCommandSenderName());
            if (stack.hasTagCompound()) tile.readSettings(stack.getTagCompound());
            if (stack.hasDisplayName()) tile.setName(stack.getDisplayName());
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> result = new ArrayList<>();
        ItemStack stack = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileMERequester tile) {
            stack.setTagCompound(new NBTTagCompound());
            tile.writeSettings(stack.getTagCompound());
            if (!tile.name()
                .isEmpty()) stack.setStackDisplayName(tile.name());
        }
        result.add(stack);
        return result;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harvest) {
        return harvest || super.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        world.setBlockToAir(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("me_requester"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 3 ? blockIcon
            : AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeBlock()
                .get()
                .getIcon(side, 0);
    }
}
