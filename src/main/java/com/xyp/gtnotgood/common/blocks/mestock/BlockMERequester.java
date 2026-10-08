package com.xyp.gtnotgood.common.blocks.mestock;

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
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.mestock.StockModelRenderer;
import com.xyp.gtnotgood.common.parts.mestock.StockGuiFactory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Requester configuration survives relocation; completed products belong to the ME network. */
public final class BlockMERequester extends Block {

    public static int renderId;

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
            int direction = MathHelper.floor_double(placer.rotationYaw * 4 / 360 + 0.5) & 3;
            world.setBlockMetadataWithNotify(x, y, z, switch (direction) {
                case 0 -> 2;
                case 1 -> 5;
                case 2 -> 3;
                default -> 4;
            }, 2);
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
            if (!tile.name().isEmpty()) stack.setStackDisplayName(tile.name());
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
        StockModelRenderer.registerIcons(register);
        blockIcon = StockModelRenderer.icon("merequester:block/requester");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (side == front(metadata)) return blockIcon;
        return StockModelRenderer.icon("ae2:block/generics/"
            + (side == 0 ? "bottom" : side == 1 ? "top" : side == (front(metadata) ^ 1) ? "back" : "side"));
    }

    /** Legacy metadata zero keeps its original south-facing front. */
    public static int front(int metadata) {
        return metadata >= 2 && metadata <= 5 ? metadata : 3;
    }

    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }
}
