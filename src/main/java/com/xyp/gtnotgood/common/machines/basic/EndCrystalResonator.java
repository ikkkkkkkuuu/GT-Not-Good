package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.enums.GTValues.V;
import static gregtech.api.enums.GTValues.VN;

import java.util.UUID;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.misc.WirelessNetworkManager;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class EndCrystalResonator extends MTETieredMachineBlock {

    private static final int UPDATE_INTERVAL = 20;
    private static final BlockIcons[] FRONTS = { BlockIcons.CrystalResonatorLV, BlockIcons.CrystalResonatorMV,
        BlockIcons.CrystalResonatorHV, BlockIcons.CrystalResonatorEV, BlockIcons.CrystalResonatorIV,
        BlockIcons.CrystalResonatorLuV, BlockIcons.CrystalResonatorZPM, BlockIcons.CrystalResonatorUV,
        BlockIcons.CrystalResonatorUHV, BlockIcons.CrystalResonatorUEV, BlockIcons.CrystalResonatorUIV,
        BlockIcons.CrystalResonatorUMV, BlockIcons.CrystalResonatorUXV, BlockIcons.CrystalResonatorMAX };

    private UUID ownerId;
    private long pendingEU;
    private boolean crystalPresent;

    public EndCrystalResonator(int id, String name, int tier) {
        super(id, name, "", tier, 0, new String[0]);
    }

    private EndCrystalResonator(String name, int tier, ITexture[][][] textures) {
        super(name, tier, 0, new String[0], textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new EndCrystalResonator(mName, mTier, mTextures);
    }

    public static long generationForTier(int tier) {
        return V[tier] * (tier == 1 ? 32L : 128L);
    }

    @Override
    public String getLocalName() {
        // #tr gtng.crystal_resonator.name
        // # End Crystal Resonator (%s)
        // # zh_CN 末晶谐振器 (%s)
        return StatCollector.translateToLocalFormatted("gtng.crystal_resonator.name", VN[mTier]);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr tooltip.crystal_resonator.generation
            // # Generates EU from an End Crystal without pollution
            // # zh_CN 利用末影水晶发电，无污染
            StatCollector.translateToLocal("tooltip.crystal_resonator.generation"),
            // #tr tooltip.crystal_resonator.crystal
            // # Requires an End Crystal directly above; the crystal is not consumed
            // # zh_CN 正上方一格需要末影水晶；运行时不消耗水晶
            StatCollector.translateToLocal("tooltip.crystal_resonator.crystal"),
            // #tr tooltip.crystal_resonator.rate
            // # Generation: %s EU/t
            // # zh_CN 发电量：%s EU/t
            StatCollector.translateToLocalFormatted("tooltip.crystal_resonator.rate",
                NumberFormatUtil.formatNumber(generationForTier(mTier))),
            // #tr tooltip.crystal_resonator.wireless
            // # Deposits EU into the owner's GT wireless network every second
            // # zh_CN 每秒将 EU 存入机主所属的 GT 无线电网
            StatCollector.translateToLocal("tooltip.crystal_resonator.wireless"),
            // #tr tooltip.crystal_resonator.placement
            // # Right-click the top with an Et Futurum End Crystal to place it
            // # zh_CN 手持 Et Futurum 的末影水晶右键顶面放置
            StatCollector.translateToLocal("tooltip.crystal_resonator.placement") };
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (!tile.isServerSide()) return;
        bindOwner(tile);
        crystalPresent = hasCrystalAbove(tile);
    }

    private void bindOwner(IGregTechTileEntity tile) {
        UUID placedBy = tile.getOwnerUuid();
        if (ownerId != null || placedBy == null || placedBy.equals(GTUtility.defaultUuid)) return;
        ownerId = placedBy;
        WirelessNetworkManager.strongCheckOrAddUser(ownerId);
        markDirty();
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if (tick % UPDATE_INTERVAL == 0) {
            bindOwner(tile);
            crystalPresent = hasCrystalAbove(tile);
        }
        boolean generating = crystalPresent && tile.isAllowedToWork();
        tile.setActive(generating);
        if (generating) {
            pendingEU += Math.min(generationForTier(mTier), Long.MAX_VALUE - pendingEU);
            markDirty();
        }
        if (tick % UPDATE_INTERVAL == 0) flushEnergy();
    }

    private static boolean hasCrystalAbove(IGregTechTileEntity tile) {
        int x = tile.getXCoord(), y = tile.getYCoord() + 1, z = tile.getZCoord();
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        for (Object entity : tile.getWorld().getEntitiesWithinAABB(EntityEnderCrystal.class, box)) {
            EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
            if (
                !crystal.isDead && MathHelper.floor_double(crystal.posX) == x
                    && MathHelper.floor_double(crystal.posY) == y
                    && MathHelper.floor_double(crystal.posZ) == z
            ) return true;
        }
        return false;
    }

    private void flushEnergy() {
        if (ownerId != null && pendingEU > 0 && WirelessNetworkManager.addEUToGlobalEnergyMap(ownerId, pendingEU)) {
            pendingEU = 0;
            markDirty();
        }
    }

    @Override
    public void onRemoval() {
        if (getBaseMetaTileEntity().isServerSide()) flushEnergy();
        super.onRemoval();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        tag.setLong("resonatorPendingEU", pendingEU);
        if (ownerId != null) tag.setString("resonatorOwner", ownerId.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        pendingEU = Math.max(0, tag.getLong("resonatorPendingEU"));
        ownerId = null;
        if (tag.hasKey("resonatorOwner", 8)) {
            try {
                ownerId = UUID.fromString(tag.getString("resonatorOwner"));
            } catch (IllegalArgumentException ignored) {}
        }
        crystalPresent = false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player, ForgeDirection side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (
            side != ForgeDirection.UP || GTUtility.isStackInvalid(held)
                || held.stackSize <= 0
                || !GTUtility.areStacksEqual(held, ModsItemlist.PlaceableEndCrystal.get(1))
        ) return false;
        if (tile.isClientSide()) return true;
        World world = tile.getWorld();
        int x = tile.getXCoord(), y = tile.getYCoord() + 1, z = tile.getZCoord();
        if (
            !world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, side.ordinal(), held)
                || !world.isAirBlock(x, y, z)
                || hasCrystalAbove(tile)
        ) return false;
        EntityEnderCrystal crystal = new EntityEnderCrystal(world, x + 0.5, y, z + 0.5);
        if (!world.spawnEntityInWorld(crystal)) return false;
        if (!player.capabilities.isCreativeMode) {
            player.inventory.decrStackSize(player.inventory.currentItem, 1);
            player.inventory.markDirty();
        }
        crystalPresent = true;
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isFacingValid(ForgeDirection side) {
        return side != ForgeDirection.UP && side != ForgeDirection.DOWN && side != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public boolean isEnetOutput() {
        return false;
    }

    @Override
    public boolean isValidSlot(int slot) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return new ITexture[0][][];
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1];
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.of(FRONTS[mTier - 1]) };
    }
}
