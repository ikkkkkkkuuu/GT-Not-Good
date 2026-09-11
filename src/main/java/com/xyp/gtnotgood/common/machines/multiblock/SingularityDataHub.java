package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.xyp.gtnotgood.GTNotGood.RESOURCE_ROOT_ID;
import static gregtech.api.GregTechAPI.sBlockCasings2;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.api.IItemVault;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.SingularityDataHubGui;
import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.utils.StructureUtils;
import com.xyp.gtnotgood.utils.Utils;
import com.xyp.gtnotgood.utils.item.ItemId;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import lombok.Setter;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class SingularityDataHub extends GTNGMultiBlockBase<SingularityDataHub>
    implements ISurvivalConstructable, IItemVault {

    public static long MAX_DISTINCT_ITEMS = Long.MAX_VALUE - 1;
    public static long MAX_DISTINCT_FLUIDS = Long.MAX_VALUE - 1;
    public static final long MAX_STORAGE_BYTES = 5_764_607_523_034_234_870L;
    private static final int BYTES_PER_TYPE = 8;
    private static final int ITEMS_PER_BYTE = 8;
    private static final int FLUID_MB_PER_BYTE = 8 * 256;

    public static BigInteger MAX_CAPACITY_ITEM = BigInteger.valueOf(MAX_STORAGE_BYTES)
        .multiply(BigInteger.valueOf(ITEMS_PER_BYTE));
    public static BigInteger MAX_CAPACITY_FLUID = BigInteger.valueOf(MAX_STORAGE_BYTES)
        .multiply(BigInteger.valueOf(FLUID_MB_PER_BYTE));

    public long capacityPerItem = Long.MAX_VALUE;
    public long capacityPerFluid = Long.MAX_VALUE;

    public boolean wirelessMode = false;
    public boolean locked = true;
    @Setter
    public boolean doVoidExcess = false;
    public VaultPortHatch portHatch = null;
    public UUID ownerUUID;
    public int mCountCasing = 0;

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final String SDH_STRUCTURE_FILE_PATH = RESOURCE_ROOT_ID + ":" + "multiblock/singularity_data_hub";
    private static final String[][] shape = StructureUtils.readStructureFromFile(SDH_STRUCTURE_FILE_PATH);
    private static final int HORIZONTAL_OFF_SET = 1;
    private static final int VERTICAL_OFF_SET = 1;
    private static final int DEPTH_OFF_SET = 0;

    public static NumberFormat nf = NumberFormat.getNumberInstance();

    public IItemList<IAEItemStack> STORE_ITEM = AEApi.instance()
        .storage()
        .createItemList();

    public IItemList<IAEFluidStack> STORE_FLUID = AEApi.instance()
        .storage()
        .createFluidList();

    public long getUsedStorageBytes() {
        long used = 0;
        for (IAEItemStack item : STORE_ITEM) {
            used = saturatingAdd(used, BYTES_PER_TYPE);
            used = saturatingAdd(used, bytesForAmount(item.getStackSize(), ITEMS_PER_BYTE));
        }
        for (IAEFluidStack fluid : STORE_FLUID) {
            used = saturatingAdd(used, BYTES_PER_TYPE);
            used = saturatingAdd(used, bytesForAmount(fluid.getStackSize(), FLUID_MB_PER_BYTE));
        }
        return Math.min(used, MAX_STORAGE_BYTES);
    }

    private long getInsertableAmount(long currentAmount, long requestedAmount, int amountPerByte, boolean newType) {
        if (requestedAmount <= 0) return 0;
        long freeBytes = MAX_STORAGE_BYTES - getUsedStorageBytes();
        if (newType) freeBytes -= BYTES_PER_TYPE;
        if (freeBytes < 0) return 0;
        long unusedAmount = currentAmount <= 0 ? 0 : (amountPerByte - currentAmount % amountPerByte) % amountPerByte;
        long byteLimitedAmount = saturatingAdd(unusedAmount, saturatingMultiply(freeBytes, amountPerByte));
        long stackLimitedAmount = Long.MAX_VALUE - currentAmount;
        return Math.min(requestedAmount, Math.min(byteLimitedAmount, stackLimitedAmount));
    }

    private static long bytesForAmount(long amount, int amountPerByte) {
        if (amount <= 0) return 0;
        return amount / amountPerByte + (amount % amountPerByte == 0 ? 0 : 1);
    }

    private static long saturatingAdd(long first, long second) {
        if (first >= Long.MAX_VALUE - second) return Long.MAX_VALUE;
        return first + second;
    }

    private static long saturatingMultiply(long value, int factor) {
        if (value > Long.MAX_VALUE / factor) return Long.MAX_VALUE;
        return value * factor;
    }

    public SingularityDataHub(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public SingularityDataHub(String aName) {
        super(aName);
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new SingularityDataHubGui(this);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new SingularityDataHub(super.mName);
    }

    @Override
    public long maxItemCount() {
        return MAX_DISTINCT_ITEMS;
    }

    @Override
    public long maxFluidCount() {
        return MAX_DISTINCT_FLUIDS;
    }

    @Override
    public boolean hasItem() {
        return true;
    }

    @Override
    public boolean hasFluid() {
        return true;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        if (checkStructure(true, aBaseMetaTileEntity)) {
            this.mStartUpCheck = -1;
            this.mUpdate = 200;
        }
        this.ownerUUID = aBaseMetaTileEntity.getOwnerUuid();
        super.onFirstTick(aBaseMetaTileEntity);
    }

    @Override
    public void onBlockDestroyed() {
        if (portHatch != null) {
            portHatch.unbind();
        }
        super.onBlockDestroyed();
    }

    @Override
    public @NotNull CheckRecipeResult checkProcessing() {
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        mEUt = 0;
        mMaxProgresstime = 20;

        ArrayList<ItemStack> inputItems = getStoredInputs();
        ArrayList<FluidStack> inputFluids = getStoredFluids();

        if (!inputItems.isEmpty()) {
            for (ItemStack aItem : inputItems) {
                ItemStack toDeplete = aItem.copy();
                toDeplete.stackSize = this.injectItems(aItem, true);
                depleteInput(toDeplete);
            }
        }

        if (!inputFluids.isEmpty()) {
            for (FluidStack aFluid : inputFluids) {
                FluidStack toDeplete = aFluid.copy();
                toDeplete.amount = this.injectFluids(aFluid, true);
                depleteInput(toDeplete, false);
            }
        }

        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public ArrayList<ItemStack> getStoredInputsForColor(Optional<Byte> color) {
        ArrayList<ItemStack> rList = new ArrayList<>();
        Map<ItemId, ItemStack> inputsFromME = new HashMap<>();
        for (MTEHatchInputBus tHatch : GTUtility.validMTEList(mInputBusses)) {
            if (tHatch instanceof MTEHatchCraftingInputME) {
                continue;
            }
            byte busColor = tHatch.getColor();
            if (color.isPresent() && busColor != -1 && busColor != color.get()) continue;
            tHatch.mRecipeMap = getRecipeMap();
            IGregTechTileEntity tileEntity = tHatch.getBaseMetaTileEntity();
            boolean isMEBus = tHatch instanceof MTEHatchInputBusME;
            for (int i = tileEntity.getSizeInventory() - 1; i >= 0; i--) {
                ItemStack itemStack = tileEntity.getStackInSlot(i);
                if (itemStack != null) {
                    if (isMEBus) {
                        // Prevent the same item from different ME buses from being recognized
                        inputsFromME.put(ItemId.createNoCopy(itemStack), itemStack);
                    } else {
                        rList.add(itemStack);
                    }
                }
            }
        }

        if (!inputsFromME.isEmpty()) {
            rList.addAll(inputsFromME.values());
        }
        return rList;

    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            this.locked = !aBaseMetaTileEntity.isActive();
        }
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (getBaseMetaTileEntity().isServerSide()) {
            this.setDoVoidExcess(!doVoidExcess);
            // #tr Info_SingularityDataHub_AutoVoiding
            // # Auto-voiding: %b
            // # zh_CN 自动销毁溢出: %b
            GTUtility.sendChatToPlayer(
                aPlayer,
                StatCollector.translateToLocalFormatted("Info_SingularityDataHub_AutoVoiding", doVoidExcess));
        }
    }

    @Override
    public IStructureDefinition<SingularityDataHub> getStructureDefinition() {
        return StructureDefinition.<SingularityDataHub>builder()
            .addShape(STRUCTURE_PIECE_MAIN, transpose(shape))
            .addElement(
                'A',
                ofChain(
                    buildHatchAdder(SingularityDataHub.class).hatchClass(VaultPortHatch.class)
                        .shouldReject(t -> t.portHatch != null)
                        .adder(SingularityDataHub::addPortBusToMachineList)
                        .casingIndex(getCasingTextureID())
                        .hint(1)
                        .build(),
                    onElementPass(t -> t.mCountCasing++, ofBlock(sBlockCasings2, 0))))
            .build();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {

        mCountCasing = 0;

        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET, errors)) {
            return;
        }

        setupParameters();

        checkCasingMin(errors, mCountCasing, 1);

        if (portHatch == null) {
            // #tr structure_error.need_vault_port_hatch
            // # Vault Port Hatch is required
            // # zh_CN 需要仓库端口仓
            errors.add(StructureErrors.of("structure_error.need_vault_port_hatch"));
        }
    }

    public void setupParameters() {
        wirelessMode = mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty();
        if (portHatch != null && portHatch.controller == null) portHatch.bind(this);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        wirelessMode = false;
        if (portHatch != null) {
            portHatch = null;
        }
    }

    public int getCasingTextureID() {
        return 16; // Solid Steel Machine Casing texture ID (same as Large Steel Boiler)
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return new String[0];
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET,
            elementBudget,
            env,
            false,
            true);
    }

    public MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr SingularityDataHubRecipeType
        // # Vault
        // # zh_CN 保险库
        tt.addMachineType(StatCollector.translateToLocal("SingularityDataHubRecipeType"))
            // #tr Tooltip_SingularityDataHub_00
            // # §9§oA vault woven from unfolded dimensions
            // # zh_CN §9§o由展开维度编织的仓库
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_00"))
            // #tr Tooltip_SingularityDataHub_01
            // # Infinite storage for items and fluids!
            // # zh_CN §m无限的物品和流体存储！§r 其实并不无限:)
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_01"))
            // #tr Tooltip_SingularityDataHub_02
            // # No longer compatible with output bus or output hatche, input only
            // # zh_CN 不再兼容输出总线或输出仓，仅限输入
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_02"))
            // #tr Tooltip_SingularityDataHub_03
            // # Must be used with Vault Multiblock Input/Output Assembly
            // # zh_CN 必须与仓库端口仓配合使用
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_03"))
            // #tr Tooltip_SingularityDataHub_04
            // # Default energy consumption: NO!
            // # zh_CN 默认能耗：完全不消耗！
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_04"))
            // #tr Tooltip_SingularityDataHub_05
            // # If no energy hatch is installed, it will automatically enter wireless mode
            // # zh_CN 如果未安装能源仓，将自动进入无线模式
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_05"))
            // #tr Tooltip_SingularityDataHub_06
            // # The index of a stored item can be obtained through the Tricorder
            // # zh_CN 可通过扫描仪获取存储物品的索引
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_06"))
            // #tr Tooltip_SingularityDataHub_07
            // # Right clicking the controller with a screwdriver will turn on excess voiding
            // # zh_CN 用螺丝刀右键控制器可开启溢出销毁
            .addInfo(StatCollector.translateToLocal("Tooltip_SingularityDataHub_07"))
            .beginStructureBlock(15, 31, 15, false)
            // #tr Tooltip_SingularityDataHub_Casing
            // # Any Vibration-Safe Casing
            // # zh_CN 任意抗震机械方块
            .addInputBus("0+", StatCollector.translateToLocal("Tooltip_SingularityDataHub_Casing"), 1)
            .addInputHatch("0+", StatCollector.translateToLocal("Tooltip_SingularityDataHub_Casing"), 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean aActive, boolean aRedstone) {
        if (side == facing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.builder()
                    .addIcon(BlockIcons.OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(BlockIcons.OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.builder()
                    .addIcon(BlockIcons.OVERLAY_FRONT_SINGULARITY_DATA_HUB)
                    .extFacing()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()) };
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> ll = new ArrayList<>();
        // #tr Info_SingularityDataHub_StoredItems
        // # Stored Items:
        // # zh_CN 已存储物品：
        ll.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("Info_SingularityDataHub_StoredItems")
                + EnumChatFormatting.RESET);

        int i = 0;
        for (IAEItemStack tank : STORE_ITEM) {
            String localizedName = Objects.requireNonNull(
                tank.getItem()
                    .getItemStackDisplayName(tank.getItemStack()));
            String amount = nf.format(tank.getStackSize());
            String percentage = capacityPerItem > 0 ? String.valueOf(tank.getStackSize() * 100 / capacityPerItem) : "";
            ll.add(MessageFormat.format("{0} - {1}: {2} ({3}%)", i++, localizedName, amount, percentage));
            if (i >= 32) break;
        }

        // #tr Info_SingularityDataHub_StoredFluids
        // # Stored Fluids:
        // # zh_CN 已存储流体：
        ll.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("Info_SingularityDataHub_StoredFluids")
                + EnumChatFormatting.RESET);

        int j = 0;
        for (IAEFluidStack tank : STORE_FLUID) {
            String localizedName = Objects.requireNonNull(
                tank.getFluid()
                    .getLocalizedName(tank.getFluidStack()));
            String amount = nf.format(tank.getStackSize());
            String percentage = capacityPerFluid > 0 ? String.valueOf(tank.getStackSize() * 100 / capacityPerFluid)
                : "";
            ll.add(MessageFormat.format("{0} - {1}: {2} ({3}%)", j++, localizedName, amount, percentage));
            if (j >= 32) break;
        }

        // #tr Info_SingularityDataHub_OperationalData
        // # Operational Data
        // # zh_CN 运行数据
        ll.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("Info_SingularityDataHub_OperationalData")
                + EnumChatFormatting.RESET);

        // #tr Info_SingularityDataHub_ItemUsed
        // # Item Used Capacity: %s
        // # zh_CN 已用物品容量: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_ItemUsed", nf.format(getItemStoredAmount())));
        // #tr Info_SingularityDataHub_ItemTotal
        // # Item Total Capacity: %s
        // # zh_CN 物品总容量: %s
        ll.add(
            StatCollector.translateToLocalFormatted("Info_SingularityDataHub_ItemTotal", nf.format(MAX_CAPACITY_ITEM)));
        // #tr Info_SingularityDataHub_PerItemCapacity
        // # Per-Item Capacity: %s
        // # zh_CN 每种物品容量: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_PerItemCapacity", nf.format(capacityPerItem)));
        // #tr Info_SingularityDataHub_ItemUsedTypes
        // # Item Used Type: %s
        // # zh_CN 已用物品种类: %s
        ll.add(
            StatCollector.translateToLocalFormatted("Info_SingularityDataHub_ItemUsedTypes", nf.format(itemsCount())));
        // #tr Info_SingularityDataHub_ItemTotalTypes
        // # Item Total Type: %s
        // # zh_CN 物品总种类: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_ItemTotalTypes", nf.format(maxItemCount())));

        // #tr Info_SingularityDataHub_FluidUsed
        // # Fluid Used Capacity: %s
        // # zh_CN 已用流体容量: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_FluidUsed", nf.format(getFluidStoredAmount())));
        // #tr Info_SingularityDataHub_FluidTotal
        // # Fluid Total Capacity: %s
        // # zh_CN 流体总容量: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_FluidTotal", nf.format(MAX_CAPACITY_FLUID)));
        // #tr Info_SingularityDataHub_PerFluidCapacity
        // # Per-Fluid Capacity: %s
        // # zh_CN 每种流体容量: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_PerFluidCapacity", nf.format(capacityPerFluid)));
        // #tr Info_SingularityDataHub_FluidUsedTypes
        // # Fluid Used Type: %s
        // # zh_CN 已用流体种类: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_FluidUsedTypes", nf.format(fluidsCount())));
        // #tr Info_SingularityDataHub_FluidTotalTypes
        // # Fluid Total Type: %s
        // # zh_CN 流体总种类: %s
        ll.add(
            StatCollector
                .translateToLocalFormatted("Info_SingularityDataHub_FluidTotalTypes", nf.format(maxFluidCount())));

        // #tr Info_SingularityDataHub_RunningCost
        // # Running Cost: %dEU/t
        // # zh_CN 运行成本: %dEU/t
        ll.add(StatCollector.translateToLocalFormatted("Info_SingularityDataHub_RunningCost", getActualEnergyUsage()));
        // #tr Info_SingularityDataHub_AutoVoiding
        // # Auto-voiding: %b
        // # zh_CN 自动销毁溢出: %b
        ll.add(StatCollector.translateToLocalFormatted("Info_SingularityDataHub_AutoVoiding", doVoidExcess));
        // #tr Waila_WirelessMode
        // # Wireless Mode
        // # zh_CN 无线模式
        if (wirelessMode)
            ll.add(EnumChatFormatting.LIGHT_PURPLE + StatCollector.translateToLocal("Waila_WirelessMode"));
        ll.add(EnumChatFormatting.STRIKETHROUGH + "---------------------------------------------");

        return ll.toArray(new String[0]);
    }

    public long getActualEnergyUsage() {
        return 0;
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        aNBT.setBoolean("doVoidExcess", doVoidExcess);
        aNBT.setBoolean("locked", locked);

        String uuid = Utils.ensureUUID(aNBT);

        NBTTagCompound storeRoot = new NBTTagCompound();
        NBTTagList itemNbt = new NBTTagList();
        for (IAEItemStack aeItem : STORE_ITEM) {
            NBTTagCompound nbt = new NBTTagCompound();
            aeItem.writeToNBT(nbt);
            itemNbt.appendTag(nbt);
        }
        NBTTagList fluidNbt = new NBTTagList();
        for (IAEFluidStack aeFluid : STORE_FLUID) {
            NBTTagCompound nbt = new NBTTagCompound();
            aeFluid.writeToNBT(nbt);
            fluidNbt.appendTag(nbt);
        }
        storeRoot.setTag("STORE_ITEM", itemNbt);
        storeRoot.setTag("STORE_FLUID", fluidNbt);

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        File dataDir = new File(worldDir, "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        File storeFile = new File(dataDir, "ItemVault_" + uuid + ".dat");
        try {
            CompressedStreamTools.safeWrite(storeRoot, storeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setBoolean("wirelessMode", wirelessMode);
        aNBT.setBoolean("doVoidExcess", doVoidExcess);
        aNBT.setBoolean("locked", locked);
        Utils.ensureUUID(aNBT);
        NBTTagList itemNbt = new NBTTagList();
        aNBT.setTag("STORE_ITEM", itemNbt);
        NBTTagList fluidNbt = new NBTTagList();
        aNBT.setTag("STORE_FLUID", fluidNbt);
        for (IAEItemStack aeItem : STORE_ITEM) {
            var nbt = new NBTTagCompound();
            aeItem.writeToNBT(nbt);
            itemNbt.appendTag(nbt);
        }
        for (IAEFluidStack aeFluid : STORE_FLUID) {
            var nbt = new NBTTagCompound();
            aeFluid.writeToNBT(nbt);
            fluidNbt.appendTag(nbt);
        }
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        this.setDoVoidExcess(aNBT.getBoolean("doVoidExcess"));
        this.locked = aNBT.getBoolean("locked");
        wirelessMode = aNBT.getBoolean("wirelessMode");
        if (aNBT.hasKey("storeUUID")) {
            String uuid = aNBT.getString("storeUUID");
            try {
                File worldDir = DimensionManager.getCurrentSaveRootDirectory();
                File dataDir = new File(worldDir, "data");
                File vaultFile = new File(dataDir, "ItemVault_" + uuid + ".dat");

                if (vaultFile.exists()) {
                    NBTTagCompound fileNBT = CompressedStreamTools.read(vaultFile);
                    NBTTagList itemNbt = fileNBT.getTagList("STORE_ITEM", 10);
                    NBTTagList fluidNbt = fileNBT.getTagList("STORE_FLUID", 10);

                    for (int i = 0; i < itemNbt.tagCount(); i++) {
                        STORE_ITEM.add(AEItemStack.loadItemStackFromNBT(itemNbt.getCompoundTagAt(i)));
                    }

                    for (int i = 0; i < fluidNbt.tagCount(); i++) {
                        STORE_FLUID.add(AEFluidStack.loadFluidStackFromNBT(fluidNbt.getCompoundTagAt(i)));
                    }

                    if (!vaultFile.delete()) {
                        System.err.println("Warning: Failed to delete vault file " + vaultFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        NBTTagList itemNbt = aNBT.getTagList("STORE_ITEM", 10);
        if (itemNbt != null) {
            for (int i = 0; i < itemNbt.tagCount(); i++) {
                STORE_ITEM.add(AEItemStack.loadItemStackFromNBT(itemNbt.getCompoundTagAt(i)));
            }
        }
        NBTTagList fluidNbt = aNBT.getTagList("STORE_FLUID", 10);
        if (fluidNbt != null) {
            for (int i = 0; i < fluidNbt.tagCount(); i++) {
                STORE_FLUID.add(AEFluidStack.loadFluidStackFromNBT(fluidNbt.getCompoundTagAt(i)));
            }
        }
        super.loadNBTData(aNBT);
    }

    @Override
    public int injectItems(ItemStack aItem, boolean doInput) {
        if (locked) return 0;
        var aeItem = getStoredItem(aItem);
        long size = aeItem == null ? 0 : aeItem.getStackSize();
        long accepted = getInsertableAmount(size, aItem.stackSize, ITEMS_PER_BYTE, aeItem == null);
        if (accepted <= 0) return doVoidExcess ? aItem.stackSize : 0;
        if (doInput) {
            if (aeItem == null) {
                STORE_ITEM.addStorage(
                    AEItemStack.create(aItem)
                        .setStackSize(accepted));
            } else {
                aeItem.setStackSize(size + accepted);
            }
            portHatch.postUpdateItem(aItem, accepted);
        }
        return doVoidExcess ? aItem.stackSize : (int) accepted;
    }

    @Override
    public long injectItems(IAEItemStack aItem, boolean doInput) {
        if (locked) return 0;
        var aeItem = getStoredItem(aItem.getItemStack());
        long size = aeItem == null ? 0 : aeItem.getStackSize();
        long accepted = getInsertableAmount(size, aItem.getStackSize(), ITEMS_PER_BYTE, aeItem == null);
        if (accepted <= 0) return doVoidExcess ? aItem.getStackSize() : 0;
        if (doInput) {
            if (aeItem == null) {
                STORE_ITEM.addStorage(
                    aItem.copy()
                        .setStackSize(accepted));
            } else {
                aeItem.setStackSize(size + accepted);
            }
            portHatch.postUpdateItem(aItem.getItemStack(), accepted);
        }
        return doVoidExcess ? aItem.getStackSize() : accepted;
    }

    @Override
    public int injectFluids(FluidStack aFluid, boolean doInput) {
        if (locked) return 0;
        var aeFluid = getStoredFluid(aFluid);
        long size = aeFluid == null ? 0 : aeFluid.getStackSize();
        long accepted = getInsertableAmount(size, aFluid.amount, FLUID_MB_PER_BYTE, aeFluid == null);
        if (accepted <= 0) return doVoidExcess ? aFluid.amount : 0;
        if (doInput) {
            if (aeFluid == null) {
                STORE_FLUID.addStorage(
                    AEFluidStack.create(aFluid)
                        .setStackSize(accepted));
            } else {
                aeFluid.setStackSize(size + accepted);
            }
            portHatch.postUpdateFluid(aFluid, accepted);
        }
        return doVoidExcess ? aFluid.amount : (int) accepted;
    }

    @Override
    public long injectFluids(IAEFluidStack aFluid, boolean doInput) {
        if (locked) return 0;
        var aeFluid = getStoredFluid(aFluid.getFluidStack());
        long size = aeFluid == null ? 0 : aeFluid.getStackSize();
        long accepted = getInsertableAmount(size, aFluid.getStackSize(), FLUID_MB_PER_BYTE, aeFluid == null);
        if (accepted <= 0) return doVoidExcess ? aFluid.getStackSize() : 0;
        if (doInput) {
            if (aeFluid == null) {
                STORE_FLUID.addStorage(
                    aFluid.copy()
                        .setStackSize(accepted));
            } else {
                aeFluid.setStackSize(size + accepted);
            }
            portHatch.postUpdateFluid(aFluid.getFluidStack(), accepted);
        }
        return doVoidExcess ? aFluid.getStackSize() : accepted;
    }

    @Override
    public long extractItems(IAEItemStack aItem, boolean doOutput) {
        if (locked) return 0;
        var aeItem = getStoredItem(aItem.getItemStack());
        if (aeItem == null) return 0;
        long storedSize = aeItem.getStackSize();
        long requestSize = aItem.getStackSize();
        if (storedSize > requestSize) {
            if (doOutput) {
                aeItem.setStackSize(storedSize - requestSize);
                portHatch.postUpdateItem(aItem.getItemStack(), -requestSize);
            }
            return requestSize;
        } else {
            if (doOutput) {
                aeItem.setStackSize(0);
                portHatch.postUpdateItem(aItem.getItemStack(), -storedSize);
            }
            return storedSize;
        }
    }

    @Override
    public long extractFluids(IAEFluidStack aFluid, boolean doOutput) {
        if (locked) return 0;
        var aeFluid = getStoredFluid(aFluid.getFluidStack());
        if (aeFluid == null) return 0;
        long storedSize = aeFluid.getStackSize();
        long requestSize = aFluid.getStackSize();
        if (storedSize > requestSize) {
            if (doOutput) {
                aeFluid.setStackSize(storedSize - requestSize);
                portHatch.postUpdateFluid(aFluid.getFluidStack(), -requestSize);
            }
            return requestSize;
        } else {
            if (doOutput) {
                aeFluid.setStackSize(0);
                portHatch.postUpdateFluid(aFluid.getFluidStack(), -storedSize);
            }
            return storedSize;
        }
    }

    @Override
    public long itemsCount() {
        return STORE_ITEM.size();
    }

    @Override
    public long fluidsCount() {
        return STORE_FLUID.size();
    }

    @Override
    public IAEItemStack getStoredItem(@Nullable ItemStack aItem) {
        if (aItem == null) return null;
        return STORE_ITEM.findPrecise(AEItemStack.create(aItem));
    }

    @Override
    public IAEFluidStack getStoredFluid(@Nullable FluidStack aFluid) {
        if (aFluid == null) return null;
        return STORE_FLUID.findPrecise(AEFluidStack.create(aFluid));
    }

    @Override
    public boolean containsItems(ItemStack aItem) {
        return getStoredItem(aItem) != null;
    }

    @Override
    public boolean containsFluids(FluidStack aFluid) {
        return getStoredFluid(aFluid) != null;
    }

    public BigInteger getItemStoredAmount() {
        BigInteger amount = BigInteger.ZERO;
        for (IAEItemStack item : STORE_ITEM) {
            amount = amount.add(BigInteger.valueOf(item.getStackSize()));
        }
        return amount;
    }

    public BigInteger getFluidStoredAmount() {
        BigInteger amount = BigInteger.ZERO;
        for (IAEFluidStack fluid : STORE_FLUID) {
            amount = amount.add(BigInteger.valueOf(fluid.getStackSize()));
        }
        return amount;
    }

    @Override
    public IItemList<IAEItemStack> getStoreItems() {
        return STORE_ITEM;
    }

    @Override
    public IItemList<IAEFluidStack> getStoreFluids() {
        return STORE_FLUID;
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {}

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {}

    @Override
    public void checkMaintenance() {}

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    public boolean addPortBusToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity != null) {
            final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof VaultPortHatch vaultPortHatch) {
                if (portHatch != null) return false;
                portHatch = vaultPortHatch;
                portHatch.updateTexture(aBaseCasingIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean getDoVoidExcess() {
        return doVoidExcess;
    }
}
