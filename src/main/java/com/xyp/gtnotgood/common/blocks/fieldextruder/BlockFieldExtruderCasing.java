package com.xyp.gtnotgood.common.blocks.fieldextruder;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.fieldextruder.FieldExtruderConnectedIcons;
import com.xyp.gtnotgood.client.fieldextruder.FieldExtruderConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IHasIndexedTexture;

public final class BlockFieldExtruderCasing extends Block implements IHasIndexedTexture {

    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockFieldExtruderCasing(boolean glass) {
        super(glass ? Material.glass : Material.iron);
        this.glass = glass;
        setBlockName(glass ? "field_extruder_glass" : "field_extruder_casing");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (glass) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        GregTechAPI.registerMachineBlock(this, glass ? 1 << FieldExtruderCasing.CleanroomGlass.getMetadata() : 31);
    }

    public FieldExtruderCasing getVariant(int metadata) {
        FieldExtruderCasing variant = FieldExtruderCasing.fromMetadata(metadata);
        return variant != null && variant.isGlass() == glass ? variant : null;
    }

    public FieldExtruderCasing getDisplayVariant(int metadata) {
        FieldExtruderCasing variant = getVariant(metadata);
        return variant == null ? glass ? FieldExtruderCasing.CleanroomGlass : FieldExtruderCasing.Iridium : variant;
    }

    @Override
    public int getTextureIndex(int metadata) {
        FieldExtruderCasing variant = getVariant(metadata);
        return variant == null ? Textures.BlockIcons.ERROR_TEXTURE_INDEX : variant.getTextureId();
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        super.breakBlock(world, x, y, z, block, metadata);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public boolean isOpaqueCube() {
        return !glass;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return !glass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return glass ? 1 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (glass && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return !glass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (FieldExtruderCasing variant : FieldExtruderCasing.values()) {
            if (variant.isGlass() == glass) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[FieldExtruderCasing.values().length];
        for (FieldExtruderCasing variant : FieldExtruderCasing.values()) {
            if (variant.isGlass() != glass) continue;
            icons[variant.ordinal()] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("fieldextruder/" + variant.getTextureName()));
            if (variant.isConnected()) FieldExtruderConnectedIcons.register(register, variant);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icons[getDisplayVariant(metadata).ordinal()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        FieldExtruderCasing variant = getDisplayVariant(world.getBlockMetadata(x, y, z));
        IIcon icon = FieldExtruderConnectedIcons.getIcon(variant,
            FieldExtruderConnections.getMask(world, x, y, z, side, variant));
        return icon == null ? getIcon(side, variant.getMetadata()) : icon;
    }
}
