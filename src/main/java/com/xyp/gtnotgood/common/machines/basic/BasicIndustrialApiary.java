/*
 * Adapted from GT5-Unofficial MTEIndustrialApiary (kuba6000 and GTNH contributors).
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Source and modifications: META-INF/industrial-apiary/BASIC_APIARY_NOTICE.md
 */
package com.xyp.gtnotgood.common.machines.basic;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_BOTTOM_INDUSTRIAL_APIARY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_BOTTOM_INDUSTRIAL_APIARY_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_BOTTOM_INDUSTRIAL_APIARY_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_BOTTOM_INDUSTRIAL_APIARY_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_APIARY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_APIARY_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_APIARY_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_APIARY_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SIDE_INDUSTRIAL_APIARY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SIDE_INDUSTRIAL_APIARY_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SIDE_INDUSTRIAL_APIARY_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SIDE_INDUSTRIAL_APIARY_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_TOP_INDUSTRIAL_APIARY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_TOP_INDUSTRIAL_APIARY_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_TOP_INDUSTRIAL_APIARY_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_TOP_INDUSTRIAL_APIARY_GLOW;
import static gregtech.api.metatileentity.BaseTileEntity.STALLED_STUTTERING_TOOLTIP;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableSet;
import com.gtnewhorizons.modularui.api.drawable.FallbackableUITexture;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.forge.IItemHandlerModifiable;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.builder.UIInfo;
import com.gtnewhorizons.modularui.common.internal.wrapper.BaseSlot;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.CycleButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotGroup;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.common.machines.bee.ApiaryOutputBuffer;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IAlleleBeeAcceleratableEffect;
import forestry.api.apiculture.IAlleleBeeEffect;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IApiaristTracker;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.IBeekeepingMode;
import forestry.api.arboriculture.EnumGermlingType;
import forestry.api.core.BiomeHelper;
import forestry.api.core.EnumHumidity;
import forestry.api.core.EnumTemperature;
import forestry.api.core.ForestryAPI;
import forestry.api.core.IErrorLogic;
import forestry.api.core.IErrorState;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IIndividual;
import forestry.apiculture.genetics.Bee;
import forestry.apiculture.genetics.alleles.AlleleEffectThrottled;
import forestry.core.errors.EnumErrorCode;
import forestry.plugins.PluginApiculture;
import gregtech.GTMod;
import gregtech.api.gui.modularui.GTUIInfos;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.modularui.IBindPlayerInventoryUI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTApiaryModifier;
import gregtech.api.util.GTApiaryUpgrade;
import gregtech.api.util.GTItemTransfer;
import gregtech.api.util.GTUtility;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class BasicIndustrialApiary extends MTEBasicMachine
    implements IBeeHousing, IBeeHousingInventory, IErrorLogic, IBeeModifier, IBeeListener, IBindPlayerInventoryUI {

    public static final int BEE_CYCLE_LENGTH = 550;
    public static final int BASE_EUT = 32;
    public static final int OUTPUT_SLOTS = 54;
    public static final int BASE_SPEED = 6;
    public static final int PRODUCT_MULTIPLIER = 16;
    public static final long BUFFER_LIMIT = 1_048_576;
    private static final int QUEEN_SLOT = 5;
    private static final int DRONE_SLOT = 6;
    private static final int UPGRADE_SLOT = DRONE_SLOT + 1;
    private static final int UPGRADE_SLOT_COUNT = 4;

    final IBeeRoot beeRoot = (IBeeRoot) AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");

    public int mSpeed = BASE_SPEED;
    public boolean mLockedSpeed = true;
    public boolean mAutoQueen = true;

    private ItemStack usedQueen = null;
    private ItemStack usedDrone = null;
    private IBee usedQueenBee = null;
    private IEffectData[] effectData = new IEffectData[2];
    private final ApiaryOutputBuffer pendingProducts = new ApiaryOutputBuffer();
    private final ApiaryOutputBuffer bufferedProducts = new ApiaryOutputBuffer();

    private static String[] description() {
        return new String[] {
            // #tr tooltip.basic_apiary.purpose
            // # Breeds bees and collects their products
            // # zh_CN 培育蜜蜂并收集产物
            StatCollector.translateToLocal("tooltip.basic_apiary.purpose"),
            // #tr tooltip.basic_apiary.power
            // # Uses 32 EU/t (LV, 1 A), including all upgrades
            // # zh_CN 耗电 32 EU/t（LV，1 A），安装升级后不增加耗电
            StatCollector.translateToLocal("tooltip.basic_apiary.power"),
            // #tr tooltip.basic_apiary.production
            // # Base speed: 64x; product quantity: 16x; accepts Industrial Apiary upgrades
            // # zh_CN 基础速度 64 倍，产物数量 16 倍；支持工业蜂箱升级
            StatCollector.translateToLocal("tooltip.basic_apiary.production"),
            // #tr tooltip.basic_apiary.environment
            // # Works without flowers, sky access, suitable climate, or time and weather restrictions
            // # zh_CN 无需花源或露天环境，不受温湿度、昼夜和天气限制
            StatCollector.translateToLocal("tooltip.basic_apiary.environment"),
            // #tr tooltip.basic_apiary.offspring
            // # Automatically recycles offspring; bee genetics and breeding rules are preserved
            // # zh_CN 自动回收子代，保留蜜蜂基因与杂交规则
            StatCollector.translateToLocal("tooltip.basic_apiary.offspring"),
            // #tr tooltip.basic_apiary.output
            // # 54 output slots; auto-output moves up to 54 stacks per tick
            // # zh_CN 54 格输出；自动输出每刻最多传输 54 组
            StatCollector.translateToLocal("tooltip.basic_apiary.output"),
            // #tr tooltip.basic_apiary.buffer
            // # Saves overflow; pauses new cycles at 1,048,576 buffered items until space is available
            // # zh_CN 溢出产物随存档保存；缓存达到 1,048,576 件时暂停新周期，腾出空间后继续
            StatCollector.translateToLocal("tooltip.basic_apiary.buffer") };
    }

    public long getBufferedItemCount() {
        return bufferedProducts.getItemCount();
    }

    public long getPendingItemCount() {
        return pendingProducts.getItemCount();
    }

    @Override
    public void setItemNBT(NBTTagCompound data) {
        super.setItemNBT(data);
        data.setBoolean("mItemTransfer", mItemTransfer);
        data.setInteger("mSpeed", mSpeed);
        data.setBoolean("mLockedSpeed", mLockedSpeed);
        data.setBoolean("mAutoQueen", mAutoQueen);
        ApiaryOutputBuffer saved = new ApiaryOutputBuffer();
        saved.readFromNBT(bufferedProducts.writeToNBT());
        if (mMaxProgresstime > 0 && usedQueen != null) saved.add(usedQueen, usedQueen.stackSize);
        if (mMaxProgresstime > 0 && usedDrone != null) saved.add(usedDrone, usedDrone.stackSize);
        if (!saved.isEmpty()) data.setTag("BufferedProducts", saved.writeToNBT());
    }

    public BasicIndustrialApiary(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 1, 1, description(), 6, OUTPUT_SLOTS,
            TextureFactory.of(TextureFactory.of(OVERLAY_SIDE_INDUSTRIAL_APIARY_ACTIVE),
                TextureFactory.builder().addIcon(OVERLAY_SIDE_INDUSTRIAL_APIARY_ACTIVE_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_SIDE_INDUSTRIAL_APIARY),
                TextureFactory.builder().addIcon(OVERLAY_SIDE_INDUSTRIAL_APIARY_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_FRONT_INDUSTRIAL_APIARY_ACTIVE),
                TextureFactory.builder().addIcon(OVERLAY_FRONT_INDUSTRIAL_APIARY_ACTIVE_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_FRONT_INDUSTRIAL_APIARY),
                TextureFactory.builder().addIcon(OVERLAY_FRONT_INDUSTRIAL_APIARY_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_TOP_INDUSTRIAL_APIARY_ACTIVE),
                TextureFactory.builder().addIcon(OVERLAY_TOP_INDUSTRIAL_APIARY_ACTIVE_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_TOP_INDUSTRIAL_APIARY),
                TextureFactory.builder().addIcon(OVERLAY_TOP_INDUSTRIAL_APIARY_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_BOTTOM_INDUSTRIAL_APIARY_ACTIVE),
                TextureFactory.builder().addIcon(OVERLAY_BOTTOM_INDUSTRIAL_APIARY_ACTIVE_GLOW).glow().build()),
            TextureFactory.of(TextureFactory.of(OVERLAY_BOTTOM_INDUSTRIAL_APIARY),
                TextureFactory.builder().addIcon(OVERLAY_BOTTOM_INDUSTRIAL_APIARY_GLOW).glow().build()));
        mItemTransfer = true;
    }

    private BasicIndustrialApiary(String aName, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, 1, 1, aDescription, aTextures, 6, OUTPUT_SLOTS);
        mItemTransfer = true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new BasicIndustrialApiary(mName, mDescriptionArray, mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) return true;
        if (!GTMod.proxy.mForceFreeFace) {
            openGUI(aBaseMetaTileEntity, aPlayer);
            return true;
        }
        for (final ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (aBaseMetaTileEntity.getAirAtSide(side)) {
                openGUI(aBaseMetaTileEntity, aPlayer);
                return true;
            }
        }
        GTUtility.sendChatTrans(aPlayer, "GT5U.chat.machine.no_free_side");
        return true;
    }

    private void openGUI(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        INDUSTRIAL_APIARY_UI.open(aPlayer, aBaseMetaTileEntity.getWorld(), aBaseMetaTileEntity.getXCoord(),
            aBaseMetaTileEntity.getYCoord(), aBaseMetaTileEntity.getZCoord());
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("mSpeed", mSpeed);
        aNBT.setBoolean("mLockedSpeed", mLockedSpeed);
        aNBT.setBoolean("mAutoQueen", mAutoQueen);
        if (usedQueen != null) aNBT.setTag("usedQueen", usedQueen.writeToNBT(new NBTTagCompound()));
        if (usedDrone != null) aNBT.setTag("UsedDrone", usedDrone.writeToNBT(new NBTTagCompound()));
        aNBT.setBoolean("retrievingPollenInThisOperation", retrievingPollenInThisOperation);
        aNBT.setInteger("pollinationDelay", pollinationDelay);
        aNBT.setFloat("usedBeeLife", usedBeeLife);
        aNBT.setTag("PendingProducts", pendingProducts.writeToNBT());
        aNBT.setTag("BufferedProducts", bufferedProducts.writeToNBT());
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mSpeed = aNBT.hasKey("mSpeed") ? aNBT.getInteger("mSpeed") : BASE_SPEED;
        mLockedSpeed = !aNBT.hasKey("mLockedSpeed") || aNBT.getBoolean("mLockedSpeed");
        if (!aNBT.hasKey("mItemTransfer")) mItemTransfer = true;
        if (aNBT.hasKey("mAutoQueen")) mAutoQueen = aNBT.getBoolean("mAutoQueen");
        if (aNBT.hasKey("usedQueen")) usedQueen = ItemStack.loadItemStackFromNBT(aNBT.getCompoundTag("usedQueen"));
        usedDrone = aNBT.hasKey("UsedDrone") ? ItemStack.loadItemStackFromNBT(aNBT.getCompoundTag("UsedDrone")) : null;
        retrievingPollenInThisOperation = aNBT.getBoolean("retrievingPollenInThisOperation");
        pollinationDelay = Math.max(20, aNBT.getInteger("pollinationDelay"));
        usedBeeLife = aNBT.getFloat("usedBeeLife");
        pendingProducts.readFromNBT(aNBT.getTagList("PendingProducts", 10));
        bufferedProducts.readFromNBT(aNBT.getTagList("BufferedProducts", 10));
        mSpeed = Math.max(BASE_SPEED, Math.min(mSpeed, BASE_SPEED + 8));
    }

    boolean retrievingPollenInThisOperation = false;
    IIndividual retrievedPollen = null;
    int pollinationDelay = 100;
    float usedBeeLife = 0f;

    @Override
    public int checkRecipe() {
        updateModifiers();
        if (mMaxProgresstime > 0 || !pendingProducts.isEmpty() || bufferedProducts.getItemCount() >= BUFFER_LIMIT) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        if (canWork()) {

            final ItemStack queen = getQueen();
            usedQueen = queen.copy();
            usedDrone = null;
            if (beeRoot.getType(queen) == EnumBeeType.QUEEN) {
                final IBee bee = beeRoot.getMember(queen);
                usedQueenBee = bee;

                float mod = this.getLifespanModifier(null, null, 1.f);
                final IBeekeepingMode mode = beeRoot.getBeekeepingMode(this.getWorld());
                final IBeeModifier beeModifier = mode.getBeeModifier();
                mod *= beeModifier.getLifespanModifier(null, null, 1.f);
                final int h = bee.getHealth();
                mod = 1.f / mod;
                final float cycles = h / mod;

                final HashMap<GTUtility.ItemId, ItemStack> pollen = new HashMap<>();

                if (isRetrievingPollen && floweringMod > 0f) {
                    final int integerCycles = (int) cycles
                        + (getWorld().rand.nextFloat() < (cycles - (float) ((int) cycles)) ? 1 : 0);
                    for (int z = 0; z < integerCycles; z++) {
                        final IIndividual p = bee.retrievePollen(this);
                        if (p != null) {
                            final ItemStack s = p.getGenome().getSpeciesRoot().getMemberStack(p,
                                EnumGermlingType.POLLEN.ordinal());
                            if (s != null) {
                                final GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(s);
                                pollen.computeIfAbsent(id, k -> {
                                    final ItemStack ns = s.copy();
                                    ns.stackSize = 0;
                                    return ns;
                                });
                                pollen.get(id).stackSize += s.stackSize;
                            }
                        }
                    }
                }

                retrievedPollen = null;
                retrievingPollenInThisOperation = isRetrievingPollen;

                final IBeeGenome genome = bee.getGenome();
                final IAlleleBeeSpecies primary = genome.getPrimary();
                final IAlleleBeeSpecies secondary = genome.getSecondary();

                final float speed = genome.getSpeed();
                final float prodMod = getProductionModifier(null, 0f) + beeModifier.getProductionModifier(null, 0f);

                final HashMap<GTUtility.ItemId, Float> drops = new HashMap<>();
                final HashMap<GTUtility.ItemId, ItemStack> dropStacks = new HashMap<>();

                for (Map.Entry<ItemStack, Float> entry : primary.getProductChances().entrySet()) {
                    final GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(entry.getKey());
                    drops.merge(id,
                        Bee.getFinalChance(entry.getValue(), speed, prodMod, 8f) * (float) entry.getKey().stackSize
                            * cycles,
                        Float::sum);
                    dropStacks.computeIfAbsent(id, k -> entry.getKey());
                }
                for (Map.Entry<ItemStack, Float> entry : secondary.getProductChances().entrySet()) {
                    final GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(entry.getKey());
                    drops.merge(id,
                        Bee.getFinalChance(entry.getValue() / 2f, speed, prodMod, 8f) * (float) entry.getKey().stackSize
                            * cycles,
                        Float::sum);
                    dropStacks.computeIfAbsent(id, k -> entry.getKey());
                }
                for (Map.Entry<ItemStack, Float> entry : primary.getSpecialtyChances().entrySet()) {
                    final GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(entry.getKey());
                    drops.merge(id,
                        Bee.getFinalChance(entry.getValue(), speed, prodMod, 8f) * (float) entry.getKey().stackSize
                            * cycles,
                        Float::sum);
                    dropStacks.computeIfAbsent(id, k -> entry.getKey());
                }

                int i = 0;
                final IApiaristTracker breedingTracker = beeRoot.getBreedingTracker(getWorld(), getOwner());

                if (!bee.canSpawn()) {
                    final ItemStack convert = new ItemStack(PluginApiculture.items.beePrincessGE);
                    final NBTTagCompound nbttagcompound = new NBTTagCompound();
                    bee.writeToNBT(nbttagcompound);
                    convert.setTagCompound(nbttagcompound);
                    this.mOutputItems[i++] = convert;
                } else {
                    final IBee b = bee.spawnPrincess(this);
                    if (b != null) {
                        final ItemStack princess = beeRoot.getMemberStack(b, EnumBeeType.PRINCESS.ordinal());
                        breedingTracker.registerPrincess(b);
                        this.mOutputItems[i++] = princess;
                    }
                    final IBee[] d = bee.spawnDrones(this);
                    if (d != null && d.length > 0) {
                        final HashMap<GTUtility.ItemId, ItemStack> drones = new HashMap<>(d.length);
                        for (IBee dr : d) {
                            final ItemStack drone = beeRoot.getMemberStack(dr, EnumBeeType.DRONE.ordinal());
                            breedingTracker.registerDrone(dr);
                            final GTUtility.ItemId drid = GTUtility.ItemId.createNoCopy(drone);
                            if (drones.containsKey(drid)) drones.get(drid).stackSize += drone.stackSize;
                            else {
                                this.mOutputItems[i++] = drone;
                                drones.put(drid, drone);
                            }
                        }
                    }
                }

                setQueen(null);

                for (Map.Entry<GTUtility.ItemId, Float> entry : drops.entrySet()) {
                    final ItemStack s = dropStacks.get(entry.getKey()).copy();
                    double expected = entry.getValue().doubleValue() * PRODUCT_MULTIPLIER;
                    long count = (long) expected;
                    if (getWorld().rand.nextDouble() < expected - count) count++;
                    if (count > 0) pendingProducts.add(s, count);
                }

                for (ItemStack s : pollen.values()) pendingProducts.add(s, (long) s.stackSize * PRODUCT_MULTIPLIER);

                usedBeeLife = cycles * (float) BEE_CYCLE_LENGTH;
                final int usedDivider = 1 << this.mSpeed;
                this.mMaxProgresstime = Math.max(1, (int) Math.ceil(usedBeeLife / usedDivider));

                pollinationDelay = Math.max((int) (this.mMaxProgresstime / cycles), 20); // don't run too often

                this.mProgresstime = 0;
                this.mEUt = BASE_EUT;
            } else {

                retrievingPollenInThisOperation = true;
                usedBeeLife = 0;

                this.mMaxProgresstime = 100;
                this.mProgresstime = 0;
                final int usedDivider = Math.min(100, 1 << this.mSpeed);
                this.mMaxProgresstime /= usedDivider;
                this.mEUt = BASE_EUT;

                final IBee princess = beeRoot.getMember(getQueen());
                usedQueenBee = princess;
                final IBee drone = beeRoot.getMember(getDrone());
                usedDrone = GTUtility.copyAmount(1, getDrone());
                princess.mate(drone);
                final NBTTagCompound nbttagcompound = new NBTTagCompound();
                princess.writeToNBT(nbttagcompound);
                this.mOutputItems[0] = new ItemStack(PluginApiculture.items.beeQueenGE);
                this.mOutputItems[0].setTagCompound(nbttagcompound);
                beeRoot.getBreedingTracker(getWorld(), getOwner()).registerQueen(princess);

                setQueen(null);
                getDrone().stackSize -= 1;
                if (getDrone().stackSize == 0) setDrone(null);
            }

            markDirty();
            return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
        }

        return DID_NOT_FIND_RECIPE;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        updateModifiers();
    }

    @Override
    protected boolean hasEnoughEnergyToCheckRecipe() {
        return getBaseMetaTileEntity().isUniversalEnergyStored(BASE_EUT * 8L);
    }

    @Override
    public long maxAmperesIn() {
        return 1L;
    }

    private void doEffect() {
        final IBeeGenome genome = usedQueenBee.getGenome();
        final IAlleleBeeEffect effect = genome.getEffect();
        if (!(effect instanceof IAlleleBeeAcceleratableEffect)) {
            effectData[0] = effect.validateStorage(effectData[0]);
            effect.doEffect(genome, effectData[0], this);
        }

        if (!effect.isCombinable()) return;

        final IAlleleBeeEffect secondary = (IAlleleBeeEffect) genome.getInactiveAllele(EnumBeeChromosome.EFFECT);
        if (!secondary.isCombinable()) return;

        if (!(secondary instanceof IAlleleBeeAcceleratableEffect)) {
            effectData[1] = secondary.validateStorage(effectData[1]);
            secondary.doEffect(genome, effectData[1], this);
        }
    }

    private void doAcceleratedEffects() {
        final IBeeGenome genome = usedQueenBee.getGenome();
        final IAlleleBeeEffect effect = genome.getEffect();
        if (effect instanceof IAlleleBeeAcceleratableEffect) {
            effectData[0] = effect.validateStorage(effectData[0]);
            effectData[0] = ((IAlleleBeeAcceleratableEffect) effect).doEffectAccelerated(genome, effectData[0], this,
                usedBeeLife
                    / (effect instanceof AlleleEffectThrottled ? (float) ((AlleleEffectThrottled) effect).getThrottle()
                        : 1f));
        }

        if (!effect.isCombinable()) return;

        final IAlleleBeeEffect secondary = (IAlleleBeeEffect) genome.getInactiveAllele(EnumBeeChromosome.EFFECT);
        if (!secondary.isCombinable()) return;

        if (secondary instanceof IAlleleBeeAcceleratableEffect) {
            effectData[1] = secondary.validateStorage(effectData[1]);
            effectData[1] = ((IAlleleBeeAcceleratableEffect) secondary).doEffectAccelerated(genome, effectData[1], this,
                usedBeeLife / (secondary instanceof AlleleEffectThrottled
                    ? (float) ((AlleleEffectThrottled) secondary).getThrottle()
                    : 1f));
        }
    }

    @Override
    public boolean needsClientTick() {
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isClientSide()) {
            if (aBaseMetaTileEntity.isActive()) {
                if (usedQueen != null) {
                    if (aTick % 2 == 0) {
                        final IBee bee = beeRoot.getMember(usedQueen);
                        effectData = bee.doFX(effectData, this);
                    }
                }
            }
        }
        if (aBaseMetaTileEntity.isServerSide()) {
            flushOutputs();

            mCharge = aBaseMetaTileEntity.getStoredEU() / 2 > aBaseMetaTileEntity.getEUCapacity() / 3;
            mDecharge = aBaseMetaTileEntity.getStoredEU() < aBaseMetaTileEntity.getEUCapacity() / 3;

            if (!aBaseMetaTileEntity.isActive()) {
                if (
                    aBaseMetaTileEntity.isAllowedToWork()
                        && (aBaseMetaTileEntity.hasInventoryBeenModified() || aTick % 20 == 0
                            || aBaseMetaTileEntity.hasWorkJustBeenEnabled())
                        && hasEnoughEnergyToCheckRecipe()
                ) {
                    final int check = checkRecipe();
                    if (check == FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
                        aBaseMetaTileEntity.setActive(true);
                    }
                }
            } else {

                if (this.mProgresstime < 0) {
                    this.mProgresstime++;
                    return;
                }
                if (this.mStuttering) {
                    if (!aBaseMetaTileEntity.isAllowedToWork()) return;
                    if (aTick % 20 == 0) this.mStuttering = false;
                    return;
                }
                if (!drainEnergyForProcess(this.mEUt)) {
                    this.mStuttering = true;
                    this.stutterProcess();
                    return;
                }
                this.mProgresstime++;
                if (usedQueen != null) {
                    if (usedQueenBee == null) usedQueenBee = beeRoot.getMember(usedQueen);
                    doEffect();
                    if (
                        !retrievingPollenInThisOperation && floweringMod > 0f
                            && this.mProgresstime % pollinationDelay == 0
                    ) {
                        if (retrievedPollen == null) retrievedPollen = usedQueenBee.retrievePollen(this);
                        if (
                            retrievedPollen != null && (usedQueenBee.pollinateRandom(this, retrievedPollen)
                                || this.mProgresstime % (pollinationDelay * 5) == 0)
                        ) retrievedPollen = null;
                    }
                }

                if (this.mProgresstime >= this.mMaxProgresstime) {
                    if (usedQueenBee != null && usedBeeLife > 0) doAcceleratedEffects();
                    updateModifiers();
                    for (ItemStack output : mOutputItems) {
                        if (output == null) continue;
                        boolean recycled = false;
                        if (mAutoQueen) {
                            if (
                                beeRoot.isMember(output, EnumBeeType.QUEEN.ordinal())
                                    || beeRoot.isMember(output, EnumBeeType.PRINCESS.ordinal())
                            ) {
                                recycled = aBaseMetaTileEntity.addStackToSlot(QUEEN_SLOT, output);
                            } else if (beeRoot.isMember(output, EnumBeeType.DRONE.ordinal())) {
                                recycled = aBaseMetaTileEntity.addStackToSlot(DRONE_SLOT, output);
                            }
                        }
                        if (!recycled) bufferedProducts.add(output, output.stackSize);
                    }
                    bufferedProducts.mergeFrom(pendingProducts);
                    bufferedProducts.flushToInventory(aBaseMetaTileEntity, getOutputSlot(), OUTPUT_SLOTS);
                    Arrays.fill(mOutputItems, null);
                    mEUt = 0;
                    mProgresstime = 0;
                    mMaxProgresstime = 0;
                    mStuttering = false;
                    aBaseMetaTileEntity.setActive(false);
                    usedQueen = null;
                    usedDrone = null;
                    usedQueenBee = null;
                    markDirty();

                    if (aBaseMetaTileEntity.isAllowedToWork() && checkRecipe() == FOUND_AND_SUCCESSFULLY_USED_RECIPE)
                        aBaseMetaTileEntity.setActive(true);
                }
            }
        }
    }

    private void flushOutputs() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        boolean changed = bufferedProducts.flushToInventory(base, getOutputSlot(), OUTPUT_SLOTS);
        if (doesAutoOutput() && !isOutputEmpty() && base.getFrontFacing() != mMainFacing) {
            GTItemTransfer transfer = new GTItemTransfer();
            transfer.outOfMachine(this, base.getFrontFacing());
            transfer.setStacksToTransfer(OUTPUT_SLOTS);
            transfer.transfer();
            changed = true;
            bufferedProducts.flushToInventory(base, getOutputSlot(), OUTPUT_SLOTS);
        }
        if (changed) markDirty();
    }

    public void cancelProcess() {
        if (
            this.getBaseMetaTileEntity().isActive() && this.getBaseMetaTileEntity().isServerSide()
                && usedQueen != null
                && getQueen() == null
                && beeRoot.isMember(usedQueen, EnumBeeType.QUEEN.ordinal())
        ) {
            Arrays.fill(mOutputItems, null);
            pendingProducts.clear();
            mEUt = 0;
            mProgresstime = 0;
            mMaxProgresstime = 0;
            mStuttering = false;
            this.getBaseMetaTileEntity().setActive(false);
            setQueen(usedQueen);
            usedQueen = null;
            usedDrone = null;
            usedQueenBee = null;
            this.getBaseMetaTileEntity().disableWorking();
            markDirty();
        }
    }

    @Override
    public boolean isItemValidForSlot(int aIndex, ItemStack aStack) {
        if (aStack == null) return false;
        if (aIndex < getInputSlot()) return true;
        if (aIndex == QUEEN_SLOT) return beeRoot.isMember(aStack, EnumBeeType.QUEEN.ordinal())
            || beeRoot.isMember(aStack, EnumBeeType.PRINCESS.ordinal());
        else if (aIndex == DRONE_SLOT) return beeRoot.isMember(aStack, EnumBeeType.DRONE.ordinal());
        else if (aIndex < getOutputSlot()) {
            if (!GTApiaryUpgrade.isUpgrade(aStack)) return false;
            for (int i = UPGRADE_SLOT; i < UPGRADE_SLOT + UPGRADE_SLOT_COUNT; i++) {
                if (aIndex == i) continue;
                final ItemStack s = getStackInSlot(i);
                if (s == null) continue;
                if (GTUtility.areStacksEqual(getStackInSlot(i), aStack)) return false;
                if (GTApiaryUpgrade.isUpgrade(aStack)) {
                    if (!GTApiaryUpgrade.getUpgrade(aStack).isAllowedToWorkWith(getStackInSlot(i))) return false;
                } else if (GTApiaryUpgrade.isUpgrade(s)) {
                    if (!GTApiaryUpgrade.getUpgrade(s).isAllowedToWorkWith(aStack)) return false;
                }
            }
            return true;
        } else return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        if (!super.allowPutStack(aBaseMetaTileEntity, aIndex, side, aStack)) return false;
        return isItemValidForSlot(aIndex, aStack);
    }

    @Override
    public void setInventorySlotContents(int aIndex, ItemStack aStack) {
        if (aIndex == QUEEN_SLOT && aStack != null && getBaseMetaTileEntity().isClientSide()) usedQueen = aStack.copy();
        super.setInventorySlotContents(aIndex, aStack);
    }

    public void onInventoryUpdate(int aIndex) {
        if (aIndex > DRONE_SLOT && aIndex < getOutputSlot()) updateModifiers();
    }

    public ItemStack getUsedQueen() {
        return usedQueen;
    }

    @Override
    public Iterable<IBeeModifier> getBeeModifiers() {
        return Collections.singletonList(this);
    }

    @Override
    public Iterable<IBeeListener> getBeeListeners() {
        return Collections.singletonList(this);
    }

    @Override
    public IBeeHousingInventory getBeeInventory() {
        return this;
    }

    @Override
    public IBeekeepingLogic getBeekeepingLogic() {
        return DUMMY_LOGIC;
    }

    @Override
    public int getBlockLightValue() {
        return this.getBaseMetaTileEntity().getLightLevelAtSide(ForgeDirection.UP);
    }

    @Override
    public boolean canBlockSeeTheSky() {
        return this.getBaseMetaTileEntity().getSkyAtSide(ForgeDirection.UP);
    }

    @Override
    public World getWorld() {
        return this.getBaseMetaTileEntity().getWorld();
    }

    GameProfile owner = null;

    @Override
    public GameProfile getOwner() {
        if (owner == null) owner = new GameProfile(this.getBaseMetaTileEntity().getOwnerUuid(),
            this.getBaseMetaTileEntity().getOwnerName());
        return owner;
    }

    @Override
    public Vec3 getBeeFXCoordinates() {
        return Vec3.createVectorHelper(getBaseMetaTileEntity().getXCoord() + 0.5,
            getBaseMetaTileEntity().getYCoord() + 0.5, getBaseMetaTileEntity().getZCoord() + 0.5);
    }

    @Override
    public BiomeGenBase getBiome() {
        if (biomeOverride == null) return this.getBaseMetaTileEntity().getBiome();
        return biomeOverride;
    }

    @Override
    public EnumTemperature getTemperature() {
        if (BiomeHelper.isBiomeHellish(getBiome())) return EnumTemperature.HELLISH;
        float biomeTemperature = getBiome().getFloatTemperature(getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(), getBaseMetaTileEntity().getZCoord());
        return EnumTemperature.getFromValue(biomeTemperature + temperatureMod);
    }

    @Override
    public EnumHumidity getHumidity() {
        return EnumHumidity.getFromValue(getBiome().rainfall + humidityMod);
    }

    @Override
    public IErrorLogic getErrorLogic() {
        return this;
    }

    @Override
    public ChunkCoordinates getCoordinates() {
        return this.getBaseMetaTileEntity().getCoords();
    }

    @Override
    public ItemStack getQueen() {
        return getStackInSlot(QUEEN_SLOT);
    }

    @Override
    public ItemStack getDrone() {
        return getStackInSlot(DRONE_SLOT);
    }

    @Override
    public void setQueen(ItemStack itemStack) {
        setInventorySlotContents(QUEEN_SLOT, itemStack);
    }

    @Override
    public void setDrone(ItemStack itemStack) {
        setInventorySlotContents(DRONE_SLOT, itemStack);
    }

    @Override
    public boolean addProduct(ItemStack itemStack, boolean b) {
        throw new RuntimeException("Should not happen :F");
    }

    public HashSet<IErrorState> mErrorStates = new HashSet<>();

    @Override
    public boolean setCondition(boolean b, IErrorState iErrorState) {
        if (b) mErrorStates.add(iErrorState);
        else mErrorStates.remove(iErrorState);
        return b;
    }

    @Override
    public boolean contains(IErrorState iErrorState) {
        return mErrorStates.contains(iErrorState);
    }

    @Override
    public boolean hasErrors() {
        return !mErrorStates.isEmpty();
    }

    @Override
    public void clearErrors() {
        mErrorStates.clear();
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(mErrorStates.size());
        for (IErrorState s : mErrorStates) dataOutputStream.writeUTF(s.getUniqueName());
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        for (int i = dataInputStream.readInt(); i > 0; i--)
            mErrorStates.add(ForestryAPI.errorStateRegistry.getErrorState(dataInputStream.readUTF()));
    }

    @Override
    public ImmutableSet<IErrorState> getErrorStates() {
        return ImmutableSet.copyOf(mErrorStates);
    }

    private boolean canWork() {
        clearErrors();
        final EnumBeeType beeType = beeRoot.getType(getQueen());
        if (beeType == EnumBeeType.PRINCESS) {
            setCondition(!beeRoot.isDrone(getDrone()), EnumErrorCode.NO_DRONE);
            return !hasErrors();
        }
        if (beeType == EnumBeeType.QUEEN) {
            return true;
        } else {
            setCondition(true, EnumErrorCode.NO_QUEEN);
            return false;
        }
    }

    private float territoryMod = 1f;
    private float mutationMod = 1f;
    private float lifespanMod = 1f;
    private float productionMod = 2f;
    private float floweringMod = 1f;
    private float geneticDecayMod = 1f;
    private BiomeGenBase biomeOverride = null;
    private float humidityMod = 0f;
    private float temperatureMod = 0f;
    private boolean isRetrievingPollen = false;
    private int maxSpeed = BASE_SPEED;

    public void updateModifiers() {
        final GTApiaryModifier mods = new GTApiaryModifier();
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            final ItemStack s = getStackInSlot(UPGRADE_SLOT + i);
            if (s == null) continue;
            if (GTApiaryUpgrade.isUpgrade(s)) {
                final GTApiaryUpgrade upgrade = GTApiaryUpgrade.getUpgrade(s);
                upgrade.applyModifiers(mods, s);
            }
        }

        territoryMod = mods.territory;
        mutationMod = mods.mutation;
        lifespanMod = mods.lifespan;
        productionMod = mods.production;
        floweringMod = mods.flowering;
        geneticDecayMod = mods.geneticDecay;
        biomeOverride = mods.biomeOverride;
        humidityMod = mods.humidity;
        temperatureMod = mods.temperature;
        isRetrievingPollen = mods.isCollectingPollen;
        maxSpeed = BASE_SPEED + mods.maxSpeed;

        if (mLockedSpeed) mSpeed = maxSpeed;
        else mSpeed = Math.max(BASE_SPEED, Math.min(mSpeed, maxSpeed));
    }

    public void addUpgrade(ItemStack stack) {
        if (stack == null || !GTApiaryUpgrade.isUpgrade(stack)) return;

        int amount = stack.stackSize;
        for (int i = UPGRADE_SLOT; i < UPGRADE_SLOT + UPGRADE_SLOT_COUNT; i++) {
            if (!isItemValidForSlot(i, stack)) continue;

            int maxStackSize = GTApiaryUpgrade.getUpgrade(stack).getMaxNumber();
            ItemStack stackInSlot = getStackInSlot(i);
            if (stackInSlot == null) {
                amount = Math.min(amount, maxStackSize);
                setInventorySlotContents(i, stack.splitStack(amount));
                return;
            }

            if (!GTUtility.areStacksEqual(stack, stackInSlot)) continue;
            amount = Math.max(Math.min(amount, maxStackSize - stackInSlot.stackSize), 0);
            if (amount == 0) return;
            stackInSlot.stackSize += amount;
            stack.stackSize -= amount;
            return;
        }
    }

    public ItemStack getUpgrade(int index) {
        if (index < 0 || index >= UPGRADE_SLOT_COUNT) return null;
        return getStackInSlot(UPGRADE_SLOT + index);
    }

    public ItemStack removeUpgrade(int index, int amount) {
        if (index < 0 || index >= UPGRADE_SLOT_COUNT || amount <= 0) return null;

        ItemStack stackInSlot = getUpgrade(index);
        if (stackInSlot == null) return null;

        amount = Math.min(amount, stackInSlot.stackSize);
        ItemStack result = stackInSlot.splitStack(amount);
        if (stackInSlot.stackSize <= 0) setInventorySlotContents(UPGRADE_SLOT + index, null);
        return result;
    }

    public static int getMaxUpgradeCount() {
        return UPGRADE_SLOT_COUNT;
    }

    @Override
    public float getTerritoryModifier(IBeeGenome iBeeGenome, float v) {
        return Math.min(5, territoryMod);
    }

    @Override
    public float getMutationModifier(IBeeGenome iBeeGenome, IBeeGenome iBeeGenome1, float v) {
        return mutationMod;
    }

    @Override
    public float getLifespanModifier(IBeeGenome iBeeGenome, IBeeGenome iBeeGenome1, float v) {
        return lifespanMod;
    }

    @Override
    public float getProductionModifier(IBeeGenome iBeeGenome, float v) {
        return productionMod;
    }

    @Override
    public float getFloweringModifier(IBeeGenome iBeeGenome, float v) {
        return floweringMod;
    }

    @Override
    public float getGeneticDecay(IBeeGenome iBeeGenome, float v) {
        return geneticDecayMod;
    }

    public float getEnergyModifier() {
        return 1f;
    }

    @Override
    public boolean isSealed() {
        return true;
    }

    @Override
    public boolean isSelfLighted() {
        return true;
    }

    @Override
    public boolean isSelfUnlighted() {
        return true;
    }

    @Override
    public boolean isSunlightSimulated() {
        return true;
    }

    @Override
    public boolean isHellish() {
        return getBiome() == BiomeGenBase.hell;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    @Override
    public void wearOutEquipment(int i) {}

    @Override
    public void onQueenDeath() {}

    @Override
    public boolean onPollenRetrieved(IIndividual iIndividual) {
        return false;
    }

    static final IBeekeepingLogic DUMMY_LOGIC = new IBeekeepingLogic() {

        @Override
        public boolean canWork() {
            return true;
        }

        @Override
        public void doWork() {}

        @Override
        public void syncToClient() {}

        @Override
        public void syncToClient(EntityPlayerMP entityPlayerMP) {}

        @Override
        public int getBeeProgressPercent() {
            return 0;
        }

        @Override
        public boolean canDoBeeFX() {
            return false;
        }

        @Override
        public void doBeeFX() {}

        @Override
        public void readFromNBT(NBTTagCompound nbtTagCompound) {}

        @Override
        public void writeToNBT(NBTTagCompound nbtTagCompound) {}
    };

    private static final String POWER_SOURCE_POWER = "GT5U.machines.powersource.power",
        CANCEL_PROCESS_TOOLTIP = "GT5U.machines.industrialapiary.cancel.tooltip",
        SPEED_TOOLTIP = "GT5U.machines.industrialapiary.speed.tooltip",
        SPEED_LOCKED_TOOLTIP = "GT5U.machines.industrialapiary.speedlocked.tooltip",
        INFO_TOOLTIP = "GT5U.machines.industrialapiary.info.tooltip",
        INFO_WITH_BEE_TOOLTIP = "GT5U.machines.industrialapiary.infoextended.tooltip",
        UPGRADE_TOOLTIP = "GT5U.machines.industrialapiary.upgradeslot.tooltip";

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        builder
            .widget(new SlotWidget(new ApiarySlot(inventoryHandler, QUEEN_SLOT))
                .setBackground(getGUITextureSet().getItemSlot(), GTUITextures.OVERLAY_SLOT_BEE_QUEEN).setPos(36, 21))
            .widget(new SlotWidget(new ApiarySlot(inventoryHandler, DRONE_SLOT))
                .setBackground(getGUITextureSet().getItemSlot(), GTUITextures.OVERLAY_SLOT_BEE_DRONE).setPos(36, 41))
            .widget(SlotGroup.ofItemHandler(inventoryHandler, 2).startFromSlot(7).endAtSlot(10)
                .slotCreator(i -> new ApiarySlot(inventoryHandler, i)).applyForWidget(widget -> widget
                    .setGTTooltip(() -> mTooltipCache.getData(UPGRADE_TOOLTIP)).setTooltipShowUpDelay(TOOLTIP_DELAY))
                .build().setPos(61, 23));

        super.addUIWidgets(builder, buildContext);

        builder.widget(new CycleButtonWidget().setToggle(() -> this.getBaseMetaTileEntity().isAllowedToWork(), x -> {
            final IGregTechTileEntity te = this.getBaseMetaTileEntity();
            if (te.isClientSide()) return;
            if (x) te.enableWorking();
            else te.disableWorking();
            markDirty();
        }).setTextureGetter(
            i -> i == 0 ? GTUITextures.OVERLAY_BUTTON_POWER_SWITCH_OFF : GTUITextures.OVERLAY_BUTTON_POWER_SWITCH_ON)
            .setVariableBackgroundGetter(i -> i == 0 ? new IDrawable[] { GTUITextures.BUTTON_STANDARD }
                : new IDrawable[] { GTUITextures.BUTTON_STANDARD_PRESSED })
            .setGTTooltip(() -> mTooltipCache.getData("GT5U.gui.button.power_switch"))
            .setTooltipShowUpDelay(TOOLTIP_DELAY).setPos(7, 8).setSize(18, 18))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> cancelProcess())
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_CROSS)
                .setGTTooltip(() -> mTooltipCache.getData(CANCEL_PROCESS_TOOLTIP)).setTooltipShowUpDelay(TOOLTIP_DELAY)
                .setPos(7, 26).setSize(18, 18))

            .widget(new CycleButtonWidget().setToggle(() -> mAutoQueen, x -> {
                if (getBaseMetaTileEntity().isClientSide()) return;
                mAutoQueen = x;
                markDirty();
            }).setTextureGetter(i -> i == 0 ? GTUITextures.OVERLAY_BUTTON_CROSS : GTUITextures.OVERLAY_BUTTON_CHECKMARK)
                .setGTTooltip(() -> {
                    // #tr gui.gtnotgood.basic_apiary.recycle
                    // # Return bee offspring to the input slots
                    // # zh_CN 将蜜蜂子代自动送回输入槽
                    return mTooltipCache.getData("gui.gtnotgood.basic_apiary.recycle");
                }).setTooltipShowUpDelay(TOOLTIP_DELAY).setPos(7, 44).setSize(18, 18)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_SLOT_BEE_QUEEN))
            .widget(new DrawableWidget().setDrawable(GTUITextures.PICTURE_INFORMATION).setGTTooltip(() -> {
                final String energyreq = formatNumber(BASE_EUT);
                final String temperature = AlleleManager.climateHelper.toDisplay(getTemperature());
                final String humidity = AlleleManager.climateHelper.toDisplay(getHumidity());
                if (
                    getUsedQueen() != null && BeeManager.beeRoot.isMember(getUsedQueen(), EnumBeeType.QUEEN.ordinal())
                ) {
                    final IBee bee = BeeManager.beeRoot.getMember(getUsedQueen());
                    if (bee.isAnalyzed()) {
                        final IBeeGenome genome = bee.getGenome();
                        final IBeeModifier mod = BeeManager.beeRoot.getBeekeepingMode(getWorld()).getBeeModifier();
                        final float tmod = getTerritoryModifier(null, 1f) * mod.getTerritoryModifier(null, 1f);
                        final int[] t = Arrays.stream(genome.getTerritory()).map(i -> (int) ((float) i * tmod))
                            .toArray();
                        return mTooltipCache.getUncachedTooltipData(INFO_WITH_BEE_TOOLTIP, energyreq, temperature,
                            humidity, genome.getSpeed(),
                            getProductionModifier(null, 0f) + mod.getProductionModifier(null, 0f),
                            Math.round(getFloweringModifier(null, 1f) * genome.getFlowering()
                                * mod.getFloweringModifier(null, 1f)),
                            Math.round(getLifespanModifier(null, null, 1f) * genome.getLifespan()
                                * mod.getLifespanModifier(null, null, 1f)),
                            t[0], t[1], t[2]);
                    }
                }
                return mTooltipCache.getUncachedTooltipData(INFO_TOOLTIP, energyreq, temperature, humidity);
            }).attachSyncer(new FakeSyncWidget.ItemStackSyncer(() -> usedQueen, val -> usedQueen = val), builder,
                (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.IntegerSyncer(() -> mSpeed, val -> {}), builder,
                    (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.ItemStackSyncer(() -> getStackInSlot(UPGRADE_SLOT), val -> {}),
                    builder, (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.ItemStackSyncer(() -> getStackInSlot(UPGRADE_SLOT + 1), val -> {}),
                    builder, (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.ItemStackSyncer(() -> getStackInSlot(UPGRADE_SLOT + 2), val -> {}),
                    builder, (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.ItemStackSyncer(() -> getStackInSlot(UPGRADE_SLOT + 3), val -> {}),
                    builder, (widget, val) -> widget.notifyTooltipChange())
                .setPos(104, 21).setSize(7, 18))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> {
                if (widget.isClient()) return;
                if (clickData.mouseButton == 0) {
                    if (mLockedSpeed) return;
                    if (!clickData.shift) {
                        mSpeed++;
                        if (mSpeed > getMaxSpeed()) mSpeed = BASE_SPEED;
                    } else {
                        mSpeed--;
                        if (mSpeed < BASE_SPEED) mSpeed = getMaxSpeed();
                    }
                } else if (clickData.mouseButton == 1) {
                    mLockedSpeed = !mLockedSpeed;
                    if (mLockedSpeed) mSpeed = getMaxSpeed();
                }
                markDirty();
            }).setGTTooltip(
                () -> mTooltipCache.getUncachedTooltipData(mLockedSpeed ? SPEED_LOCKED_TOOLTIP : SPEED_TOOLTIP,
                    getAcceleration(), formatNumber(getAdditionalEnergyUsage())))
                .attachSyncer(new FakeSyncWidget.IntegerSyncer(() -> mSpeed, val -> mSpeed = val), builder,
                    (widget, val) -> widget.notifyTooltipChange())
                .attachSyncer(new FakeSyncWidget.BooleanSyncer(() -> mLockedSpeed, val -> mLockedSpeed = val), builder,
                    (widget, val) -> widget.notifyTooltipChange())
                .setTooltipShowUpDelay(TOOLTIP_DELAY).setBackground(GTUITextures.PICTURE_SQUARE_LIGHT_GRAY)
                .setPos(25, 62).setSize(48, 18))
            .widget(TextWidget.dynamicString(() -> "x" + getAcceleration()).setSynced(false)
                .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(28, 67));

        // #tr gui.gtnotgood.basic_apiary.output
        // # Product Output
        // # zh_CN 产物输出
        builder.widget(new TextWidget(StatCollector.translateToLocal("gui.gtnotgood.basic_apiary.output"))
            .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(134, 6));
        // #tr gui.gtnotgood.basic_apiary.energy
        // # Power: %s EU/t (LV)
        // # zh_CN 耗电：%s EU/t（LV）
        builder.widget(
            new TextWidget(StatCollector.translateToLocalFormatted("gui.gtnotgood.basic_apiary.energy", BASE_EUT))
                .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(7, 90));
        // #tr gui.gtnotgood.basic_apiary.products
        // # Products: %sx
        // # zh_CN 产物数量：%s倍
        builder.widget(new TextWidget(
            StatCollector.translateToLocalFormatted("gui.gtnotgood.basic_apiary.products", PRODUCT_MULTIPLIER))
                .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(7, 102));
        // #tr gui.gtnotgood.basic_apiary.slots
        // # Output slots: %s
        // # zh_CN 输出槽：%s格
        builder.widget(
            new TextWidget(StatCollector.translateToLocalFormatted("gui.gtnotgood.basic_apiary.slots", OUTPUT_SLOTS))
                .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(7, 114));
        builder.widget(new TextWidget(StatCollector.translateToLocal("container.inventory"))
            .setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(71, 138));
        builder.widget(TextWidget.dynamicString(() -> {
            // #tr gui.gtnotgood.basic_apiary.buffered
            // # Buffered: %s
            // # zh_CN 缓存：%s件
            return StatCollector.translateToLocalFormatted("gui.gtnotgood.basic_apiary.buffered",
                formatNumber(getBufferedItemCount()));
        }).setDefaultColor(COLOR_TEXT_GRAY.get()).setPos(7, 126));
    }

    @Override
    public int getGUIWidth() {
        return 304;
    }

    @Override
    public int getGUIHeight() {
        return 232;
    }

    @Override
    public void bindPlayerInventoryUI(ModularWindow.Builder builder, UIBuildContext buildContext) {
        builder.bindPlayerInventory(buildContext.getPlayer(), new Pos2d(71, 150), getGUITextureSet().getItemSlot());
    }

    @Override
    public void addGregTechLogo(ModularWindow.Builder builder) {
        builder.widget(
            new DrawableWidget().setDrawable(getGUITextureSet().getGregTechLogo()).setPos(101, 107).setSize(17, 17));
    }

    @Override
    protected void addIOSlots(ModularWindow.Builder builder, BasicUIProperties uiProperties) {
        IDrawable[] backgrounds = { getGUITextureSet().getItemSlot() };
        for (int index = 0; index < OUTPUT_SLOTS; index++) {
            builder
                .widget(createItemOutputSlot(index, backgrounds, new Pos2d(134 + index % 9 * 18, 18 + index / 9 * 18)));
        }
    }

    private static final FallbackableUITexture PROGRESS_BAR_TEXTURE = GTUITextures.fallbackableProgressbar("iapiary",
        GTUITextures.PROGRESSBAR_ARROW);

    @Override
    protected BasicUIProperties getUIProperties() {
        return super.getUIProperties().toBuilder().progressBarTexture(PROGRESS_BAR_TEXTURE)
            .progressBarPos(new Pos2d(101, 43)).progressBarSize(new Size(20, 18)).build();
    }

    @Override
    protected SlotWidget createItemInputSlot(int index, IDrawable[] backgrounds, Pos2d pos) {
        return null;
    }

    @Override
    protected CycleButtonWidget createItemAutoOutputButton() {
        return (CycleButtonWidget) super.createItemAutoOutputButton().setToggle(() -> mItemTransfer, value -> {
            if (getBaseMetaTileEntity().isClientSide()) return;
            mItemTransfer = value;
            markDirty();
        }).setPos(7, 62);
    }

    @Override
    protected CycleButtonWidget createFluidAutoOutputButton() {
        return null;
    }

    @Override
    protected SlotWidget createChargerSlot(int x, int y, String tooltipKey, Object[] tooltipArgs) {
        return (SlotWidget) super.createChargerSlot(x, y, tooltipKey, tooltipArgs).setPos(79, 62);
    }

    @Override
    protected DrawableWidget createErrorStatusArea(ModularWindow.Builder builder, IDrawable picture) {
        return (DrawableWidget) super.createErrorStatusArea(builder, picture).setPos(100, 62).attachSyncer(
            new FakeSyncWidget.ListSyncer<>(() -> Arrays.asList(mErrorStates.toArray(new IErrorState[0])), val -> {
                mErrorStates.clear();
                mErrorStates.addAll(new HashSet<>(val));
            }, (buffer, val) -> buffer.writeShort(val.getID()),
                buffer -> ForestryAPI.errorStateRegistry.getErrorState(buffer.readShort())),
            builder, (widget, val) -> widget.notifyTooltipChange());
    }

    @Override
    protected List<String> getErrorDescriptions() {
        if (!mErrorStates.isEmpty()) {
            return mErrorStates.stream()
                .map(state -> EnumChatFormatting.RED + StatCollector.translateToLocal("for." + state.getDescription()))
                .collect(Collectors.toList());
        } else if (mStuttering) {
            return mTooltipCache.getData(STALLED_STUTTERING_TOOLTIP,
                StatCollector.translateToLocal(POWER_SOURCE_POWER)).text;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getErrorDescriptionsShift() {
        return getErrorDescriptions();
    }

    private int getAcceleration() {
        return 1 << mSpeed;
    }

    private int getAdditionalEnergyUsage() {
        return 0;
    }

    private class ApiarySlot extends BaseSlot {

        public ApiarySlot(IItemHandlerModifiable inventory, int index) {
            super(inventory, index);
        }

        @Override
        public boolean isItemValidPhantom(ItemStack stack) {
            return super.isItemValidPhantom(stack) && getBaseMetaTileEntity().isItemValidForSlot(getSlotIndex(), stack);
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            onInventoryUpdate(getSlotIndex());
        }
    }

    private static final UIInfo<?, ?> INDUSTRIAL_APIARY_UI = GTUIInfos.GTTileEntityUIFactory
        .apply(IndustrialApiaryContainer::new);

    private static class IndustrialApiaryContainer extends ModularUIContainer {

        public IndustrialApiaryContainer(ModularUIContext context, ModularWindow mainWindow) {
            super(context, mainWindow);
        }

        private final int playerInventorySlot = 36;

        @Override
        public ItemStack slotClick(int aSlotNumber, int aMouseclick, int aShifthold, EntityPlayer aPlayer) {
            if (!(aSlotNumber >= playerInventorySlot + 2 && aSlotNumber < playerInventorySlot + 2 + 4))
                return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            if (aShifthold == 5) return null;
            if (aShifthold != 0) return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            if (aMouseclick > 1) return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            final ItemStack s = aPlayer.inventory.getItemStack();
            if (s == null) return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            final Slot slot = getSlot(aSlotNumber);
            final ItemStack slotStack = slot.getStack();
            if (slotStack != null && !GTUtility.areStacksEqual(slotStack, s)) return null; // super would replace item
            if (slotStack == null && !slot.isItemValid(s))
                return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            if (!GTApiaryUpgrade.isUpgrade(s)) return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            int max = GTApiaryUpgrade.getUpgrade(s).getMaxNumber();
            if (slotStack != null) max = Math.max(0, max - slotStack.stackSize);
            max = Math.min(max, s.stackSize);
            if (max == 0) return null;
            if (aMouseclick == 1) max = 1;
            if (max == s.stackSize) return super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            final ItemStack newStack = s.splitStack(s.stackSize - max);
            final ItemStack result = super.slotClick(aSlotNumber, aMouseclick, aShifthold, aPlayer);
            aPlayer.inventory.setItemStack(newStack);
            return result;
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer aPlayer, int aSlotIndex) {
            final Slot s = getSlot(aSlotIndex);
            if (s instanceof ApiarySlot) return super.transferStackInSlot(aPlayer, aSlotIndex);
            final ItemStack aStack = s.getStack();
            if (!GTApiaryUpgrade.isUpgrade(aStack)) return super.transferStackInSlot(aPlayer, aSlotIndex);
            for (int i = playerInventorySlot + 2; i < playerInventorySlot + 2 + 4; i++) {
                final Slot iSlot = getSlot(i);
                final ItemStack iStack = iSlot.getStack();
                if (iStack == null) {
                    if (!iSlot.isItemValid(aStack)) continue;
                } else {
                    if (!GTUtility.areStacksEqual(aStack, iStack)) continue;
                }
                int max = GTApiaryUpgrade.getUpgrade(aStack).getMaxNumber();
                if (iStack == null) {
                    max = Math.min(max, aStack.stackSize);
                    final ItemStack newstack = aStack.splitStack(max);
                    iSlot.putStack(newstack);
                } else {
                    max = Math.max(0, max - iStack.stackSize);
                    max = Math.min(max, aStack.stackSize);
                    iStack.stackSize += max;
                    aStack.stackSize -= max;
                    iSlot.onSlotChanged();
                }
                if (aStack.stackSize == 0) s.putStack(null);
                else s.onSlotChanged();
                break;
            }
            return null;
        }
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        if (tag.hasKey("queen")) {
            currenttip.add(StatCollector.translateToLocalFormatted("GT5U.waila.industrial_apiary.current_queen",
                StatCollector.translateToLocal(tag.getString("queen"))));
        }
        if (tag.hasKey("dummyProduction")) {
            currenttip.add(StatCollector.translateToLocalFormatted("GT5U.waila.industrial_apiary.effective_production",
                String.format("b^0.52 * %.2f", tag.getFloat("dummyProduction"))));
        }
        if (tag.hasKey("errors")) {
            NBTTagCompound errorNbt = tag.getCompoundTag("errors");
            for (int i = 0; i < errorNbt.getInteger("size"); i++) {
                currenttip.add(StatCollector.translateToLocalFormatted("GT5U.waila.industrial_apiary.error",
                    StatCollector.translateToLocal("for." + errorNbt.getString("e" + i))));
            }
        }
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        if (usedQueen != null) {
            IBeeGenome genome = beeRoot.getMember(usedQueen).getGenome();
            tag.setString("queen", genome.getPrimary().getUnlocalizedName());
            float prodModifier = getProductionModifier(genome, 0f);
            prodModifier += beeRoot.getBeekeepingMode(world).getBeeModifier().getProductionModifier(genome,
                prodModifier);
            float dummyProduction = 100f * Bee.getFinalChance(0.01f, genome.getSpeed(), prodModifier, 8f)
                * PRODUCT_MULTIPLIER;
            tag.setFloat("dummyProduction", dummyProduction);
        }
        if (hasErrors()) {
            NBTTagCompound errorNbt = new NBTTagCompound();
            int errorCounter = 0;
            for (IErrorState error : mErrorStates) {
                errorNbt.setString("e" + errorCounter++, error.getDescription());
            }
            errorNbt.setInteger("size", errorCounter);
            tag.setTag("errors", errorNbt);
        }
    }
}
