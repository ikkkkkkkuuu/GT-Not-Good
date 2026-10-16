package com.xyp.gtnotgood.common.machines.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.gtnewhorizon.cropsnh.api.ICropStickTile;
import com.gtnewhorizon.cropsnh.farming.registries.FertilizerRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.HydrationRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.WeedEXRegistry;
import com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures;
import com.gtnewhorizon.cropsnh.init.CropsNHFluids;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TieredVariant;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.modularui2.GTGuiTheme;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;

public final class SteamCropManager extends MTETieredMachineBlock {

    public static final int HORIZONTAL_RADIUS = 3;
    public static final int VERTICAL_RADIUS = 2;
    public static final int SLOT_WEEDEX_START = 0;
    public static final int SLOT_WEEDEX_END = 1;
    public static final int SLOT_FERT_START = 2;
    public static final int SLOT_FERT_END = 5;
    public static final int SLOT_OUTPUT_START = 6;
    public static final int SLOT_OUTPUT_END = 20;
    private static final int UPDATE_INTERVAL = 50;
    private static final int WATER_CAPACITY = 32_000;
    private static final int WEEDEX_CAPACITY = 1500;
    private static final int FERTILIZER_CAPACITY = 36_864;
    private final List<ICropStickTile> crops = new ArrayList<>();
    private final List<ItemStack> pendingDrops = new ArrayList<>();
    private int waterStored;
    private int weedEXStored;
    private int liquidFertilizerStored;
    private boolean refreshCrops = true;

    public boolean harvestEnabled = true;
    public boolean waterEnabled;
    public boolean fertilizerEnabled;
    public boolean weedEXEnabled;

    public SteamCropManager(int id, String name, String localizedName) {
        super(id, name, localizedName, 0, SLOT_OUTPUT_END + 1, description());
    }

    private SteamCropManager(String name, String[] description, ITexture[][][] textures) {
        super(name, 0, SLOT_OUTPUT_END + 1, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SteamCropManager(mName, mDescriptionArray, mTextures);
    }

    private static String[] description() {
        return new String[] {
            // #tr tooltip.steam_crop_manager.functions
            // # Harvests, waters, fertilizes and applies Weed-EX to CropsNH crops
            // # zh_CN 自动收割、浇水、施肥并为 CropsNH 作物施加除草剂
            StatCollector.translateToLocal("tooltip.steam_crop_manager.functions"),
            // #tr tooltip.steam_crop_manager.range
            // # Working area: 7 x 7 x 5 centered on the machine; cycle time: 2.5 s
            // # zh_CN 工作范围：以机器为中心的 7×7×5 区域；工作周期：2.5 秒
            StatCollector.translateToLocal("tooltip.steam_crop_manager.range"),
            // #tr tooltip.steam_crop_manager.steam
            // # Steam: 8 L per harvest, 2 L per care action; capacity: 32,000 L
            // # zh_CN 每次收割消耗 8 L 蒸汽，每次养护消耗 2 L；蒸汽容量：32,000 L
            StatCollector.translateToLocal("tooltip.steam_crop_manager.steam"),
            // #tr tooltip.steam_crop_manager.supplies
            // # Separate water, Weed-EX and fertilizer stores; 15 output slots; no harvest bonus
            // # zh_CN 水、除草剂和肥料独立存储；15 个输出槽；无额外收获加成
            StatCollector.translateToLocal("tooltip.steam_crop_manager.supplies"),
            // #tr tooltip.steam_crop_manager.safety
            // # Loaded chunks only; pauses harvesting when the output buffer is full
            // # zh_CN 仅管理已加载区块；输出缓存满时暂停收割
            StatCollector.translateToLocal("tooltip.steam_crop_manager.safety") };
    }

    @Override
    public boolean isSteampowered() {
        return true;
    }

    @Override
    public boolean isElectric() {
        return false;
    }

    @Override
    public long maxSteamStore() {
        return 16_000;
    }

    @Override
    public TieredVariant getTieredVariant() {
        return TieredVariant.BRONZE;
    }

    @Override
    public GTGuiTheme getGuiTheme() {
        return GTGuiThemes.TIERED_VARIANTS.get(TieredVariant.BRONZE);
    }

    @Override
    public boolean isLiquidInput(ForgeDirection side) {
        return side != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isLiquidOutput(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (super.onRightclick(tile, player)) return true;
        openGui(player);
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if (!tile.isAllowedToWork() || tile.getStoredSteam() < 1) {
            tile.setActive(false);
            return;
        }
        if (!tile.hasWorkJustBeenEnabled() && tick % UPDATE_INTERVAL != 0) return;
        if (refreshCrops || tick % (crops.isEmpty() ? 100 : 600) == 0) updateCrops(tile);
        flushPendingDrops();
        boolean worked = false;
        for (ICropStickTile crop : crops) {
            if (!isCropLoaded(tile, crop)) {
                refreshCrops = true;
                continue;
            }
            if (tile.getStoredSteam() < 1) break;
            if (waterEnabled) worked |= water(tile, crop);
            if (weedEXEnabled) worked |= applyWeedEX(tile, crop);
            if (fertilizerEnabled) worked |= fertilize(tile, crop);
            if (
                harvestEnabled && pendingDrops.isEmpty()
                    && hasOutputSpace()
                    && crop.canHarvest()
                    && tile.decreaseStoredEnergyUnits(4, false)
            ) {
                worked = true;
                List<ItemStack> drops = crop.harvest(1.0d);
                if (drops != null) {
                    for (ItemStack drop : drops) {
                        if (GTUtility.isStackInvalid(drop)) continue;
                        ItemStack remaining = drop.copy();
                        insertOutput(remaining);
                        if (remaining.stackSize > 0) pendingDrops.add(remaining);
                    }
                }
            }
        }
        tile.setActive(worked);
        if (worked) markDirty();
    }

    private void updateCrops(IGregTechTileEntity tile) {
        crops.clear();
        World world = tile.getWorld();
        for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
            for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
                for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                    int x = tile.getXCoord() + dx;
                    int y = tile.getYCoord() + dy;
                    int z = tile.getZCoord() + dz;
                    if (!world.blockExists(x, y, z)) continue;
                    TileEntity target = world.getTileEntity(x, y, z);
                    if (target instanceof ICropStickTile crop) crops.add(crop);
                }
            }
        }
        refreshCrops = false;
    }

    private boolean isCropLoaded(IGregTechTileEntity tile, ICropStickTile crop) {
        if (!(crop instanceof TileEntity target) || target.isInvalid()) return false;
        World world = tile.getWorld();
        return target.getWorldObj() == world && world.blockExists(target.xCoord, target.yCoord, target.zCoord)
            && world.getTileEntity(target.xCoord, target.yCoord, target.zCoord) == target;
    }

    private boolean hasOutputSpace() {
        for (int slot = SLOT_OUTPUT_START; slot <= SLOT_OUTPUT_END; slot++) {
            ItemStack stack = mInventory[slot];
            if (
                GTUtility.isStackInvalid(stack)
                    || stack.stackSize < Math.min(stack.getMaxStackSize(), getInventoryStackLimit())
            ) return true;
        }
        return false;
    }

    private void insertOutput(ItemStack drop) {
        for (int pass = 0; pass < 2 && drop.stackSize > 0; pass++) {
            for (int slot = SLOT_OUTPUT_START; slot <= SLOT_OUTPUT_END && drop.stackSize > 0; slot++) {
                ItemStack stored = mInventory[slot];
                boolean empty = GTUtility.isStackInvalid(stored);
                if (pass == 0 ? empty || !GTUtility.areStacksEqual(stored, drop) : !empty) continue;
                int limit = Math.min(drop.getMaxStackSize(), getInventoryStackLimit());
                int amount = Math.min(drop.stackSize, limit - (empty ? 0 : stored.stackSize));
                if (amount <= 0) continue;
                if (empty) {
                    stored = drop.copy();
                    stored.stackSize = amount;
                    mInventory[slot] = stored;
                } else stored.stackSize += amount;
                drop.stackSize -= amount;
                markDirty();
            }
        }
    }

    private void flushPendingDrops() {
        for (Iterator<ItemStack> it = pendingDrops.iterator(); it.hasNext();) {
            ItemStack drop = it.next();
            insertOutput(drop);
            if (drop.stackSize <= 0) {
                it.remove();
                markDirty();
            }
        }
    }

    private boolean water(IGregTechTileEntity tile, ICropStickTile crop) {
        int amount = Math.min(waterStored, 200 - crop.getWaterStorage());
        if (amount <= 0 || !crop.addWater(amount, 180, 200, true) || !tile.decreaseStoredEnergyUnits(1, false))
            return false;
        if (!crop.addWater(amount, 180, 200, false)) return false;
        waterStored -= amount;
        return true;
    }

    private boolean applyWeedEX(IGregTechTileEntity tile, ICropStickTile crop) {
        if (!crop.addWeedEx(75, 75, 150, true) || tile.getStoredSteam() < 1) return false;
        refillWeedEX();
        if (weedEXStored < 10 || !tile.decreaseStoredEnergyUnits(1, false)) return false;
        if (!crop.addWeedEx(75, 75, 150, false)) return false;
        weedEXStored -= 10;
        return true;
    }

    private void refillWeedEX() {
        for (int slot = SLOT_WEEDEX_START; slot <= SLOT_WEEDEX_END; slot++) {
            ItemStack can = mInventory[slot];
            if (!isWeedEXCan(can)) continue;
            int uses = Math.min((WEEDEX_CAPACITY - weedEXStored) / 10, can.getMaxDamage() + 1 - can.getItemDamage());
            if (uses <= 0) continue;
            weedEXStored += uses * 10;
            can.setItemDamage(can.getItemDamage() + uses);
            if (can.getItemDamage() > can.getMaxDamage()) mInventory[slot] = null;
            markDirty();
        }
    }

    private boolean fertilize(IGregTechTileEntity tile, ICropStickTile crop) {
        if (liquidFertilizerStored > 0) {
            int amount = Math.min(liquidFertilizerStored, 200 - crop.getFertilizerStorage());
            if (amount <= 0 || !crop.addFertilizer(amount, 180, 200, true) || !tile.decreaseStoredEnergyUnits(1, false))
                return false;
            if (!crop.addFertilizer(amount, 180, 200, false)) return false;
            liquidFertilizerStored -= amount;
            return true;
        }
        for (int slot = SLOT_FERT_START; slot <= SLOT_FERT_END; slot++) {
            ItemStack stack = mInventory[slot];
            if (GTUtility.isStackInvalid(stack)) continue;
            int potency = FertilizerRegistry.instance.getPotency(stack);
            int threshold = Math.max(100, 200 - potency / 2);
            if (potency <= 0 || !crop.addFertilizer(potency, threshold, 200, true)) continue;
            if (!tile.decreaseStoredEnergyUnits(1, false)) return false;
            if (!crop.addFertilizer(potency, threshold, 200, false)) return false;
            if (--stack.stackSize <= 0) mInventory[slot] = null;
            return true;
        }
        return false;
    }

    private static boolean isWeedEXCan(ItemStack stack) {
        return !GTUtility.isStackInvalid(stack) && stack.getItem() == CropsNHItemList.weedEX.getItem();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (GTUtility.isStackInvalid(stack)) return false;
        if (slot >= SLOT_WEEDEX_START && slot <= SLOT_WEEDEX_END) return isWeedEXCan(stack);
        return slot >= SLOT_FERT_START && slot <= SLOT_FERT_END && FertilizerRegistry.instance.isRegistered(stack);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    public int getWaterCapacity() {
        return WATER_CAPACITY;
    }

    public int getWaterAmount() {
        return waterStored;
    }

    public void setWaterAmount(int amount) {
        waterStored = Math.max(0, Math.min(amount, WATER_CAPACITY));
        markDirty();
    }

    public int getWeedEXCapacity() {
        return WEEDEX_CAPACITY;
    }

    public int getWeedEXAmount() {
        return weedEXStored;
    }

    public void setWeedEXAmount(int amount) {
        weedEXStored = Math.max(0, Math.min(amount, WEEDEX_CAPACITY));
        markDirty();
    }

    public int getLiquidFertilizerCapacity() {
        return FERTILIZER_CAPACITY;
    }

    public int getLiquidFertilizerAmount() {
        return liquidFertilizerStored;
    }

    public void setLiquidFertilizerAmount(int amount) {
        liquidFertilizerStored = Math.max(0, Math.min(amount, FERTILIZER_CAPACITY));
        markDirty();
    }

    @Override
    public boolean canFill(ForgeDirection side, Fluid fluid) {
        return fluid != null && fill(side, new FluidStack(fluid, 2), false) > 0;
    }

    @Override
    public int fill(ForgeDirection side, FluidStack fluid, boolean doFill) {
        if (fluid == null || fluid.amount <= 0) return 0;
        if (GTModHandler.isSteam(fluid)) return super.fill(side, fluid, doFill);
        return fill(fluid, doFill);
    }

    @Override
    public int fill(FluidStack fluid, boolean doFill) {
        if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0 || GTModHandler.isSteam(fluid)) return 0;
        int potency = HydrationRegistry.instance.getPotency(fluid.getFluid());
        if (potency > 0) {
            int accepted = Math.min(fluid.amount, (WATER_CAPACITY - waterStored) / potency);
            if (doFill && accepted > 0) setWaterAmount(waterStored + accepted * potency);
            return accepted;
        }
        potency = WeedEXRegistry.instance.getPotency(fluid.getFluid());
        if (potency > 0) {
            int accepted = Math.min(fluid.amount, (WEEDEX_CAPACITY - weedEXStored) / potency);
            if (doFill && accepted > 0) setWeedEXAmount(weedEXStored + accepted * potency);
            return accepted;
        }
        potency = FertilizerRegistry.instance.getPotency(fluid.getFluid());
        if (potency > 0) {
            int accepted = Math.min(fluid.amount, (FERTILIZER_CAPACITY - liquidFertilizerStored) / potency);
            if (doFill && accepted > 0) setLiquidFertilizerAmount(liquidFertilizerStored + accepted * potency);
            return accepted;
        }
        return 0;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection side) {
        return new FluidTankInfo[] {
            new FluidTankInfo(Materials.Steam.getGas((int) getSteamVar() * 2), (int) maxSteamStore() * 2),
            new FluidTankInfo(new FluidStack(FluidRegistry.WATER, waterStored), WATER_CAPACITY),
            new FluidTankInfo(CropsNHUtils.getWeedEXFluid(weedEXStored), WEEDEX_CAPACITY),
            new FluidTankInfo(new FluidStack(CropsNHFluids.fertilizer, liquidFertilizerStored), FERTILIZER_CAPACITY) };
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        nbt.setInteger("waterStored", waterStored);
        nbt.setInteger("weedEXStored", weedEXStored);
        nbt.setInteger("liquidFertilizerStored", liquidFertilizerStored);
        nbt.setBoolean("harvestEnabled", harvestEnabled);
        nbt.setBoolean("waterEnabled", waterEnabled);
        nbt.setBoolean("weedEXEnabled", weedEXEnabled);
        nbt.setBoolean("fertilizerEnabled", fertilizerEnabled);
        NBTTagList drops = new NBTTagList();
        for (ItemStack drop : pendingDrops) {
            NBTTagCompound savedDrop = drop.writeToNBT(new NBTTagCompound());
            savedDrop.setInteger("amount", drop.stackSize);
            drops.appendTag(savedDrop);
        }
        nbt.setTag("pendingDrops", drops);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        setWaterAmount(nbt.getInteger("waterStored"));
        setWeedEXAmount(nbt.getInteger("weedEXStored"));
        setLiquidFertilizerAmount(nbt.getInteger("liquidFertilizerStored"));
        harvestEnabled = !nbt.hasKey("harvestEnabled") || nbt.getBoolean("harvestEnabled");
        waterEnabled = nbt.getBoolean("waterEnabled");
        weedEXEnabled = nbt.getBoolean("weedEXEnabled");
        fertilizerEnabled = nbt.getBoolean("fertilizerEnabled");
        pendingDrops.clear();
        NBTTagList drops = nbt.getTagList("pendingDrops", 10);
        for (int i = 0; i < drops.tagCount(); i++) {
            NBTTagCompound savedDrop = drops.getCompoundTagAt(i);
            ItemStack drop = ItemStack.loadItemStackFromNBT(savedDrop);
            if (drop != null && savedDrop.hasKey("amount")) drop.stackSize = savedDrop.getInteger("amount");
            if (!GTUtility.isStackInvalid(drop)) pendingDrops.add(drop);
        }
        crops.clear();
        refreshCrops = true;
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        saveNBTData(nbt);
        nbt.setLong("mStoredSteam", getBaseMetaTileEntity().getStoredSteam());
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        ITexture[][][] result = new ITexture[2][17][];
        for (int color = -1; color < 16; color++) {
            result[0][color + 1] = new ITexture[] { TextureFactory.of(Textures.BlockIcons.MACHINE_BRONZE_SIDE),
                TextureFactory.of(CropsNHBlockTextures.Casing_CropHarvester_Cutter) };
            result[1][color + 1] = new ITexture[] { TextureFactory.of(Textures.BlockIcons.MACHINE_BRONZE_TOP),
                TextureFactory.of(CropsNHBlockTextures.Casing_CropHarvester_Boxes) };
        }
        return result;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return mTextures[side == ForgeDirection.UP || side == ForgeDirection.DOWN ? 1 : 0][color + 1];
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new SteamCropManagerGui(this).build(data, syncManager, settings);
    }
}
