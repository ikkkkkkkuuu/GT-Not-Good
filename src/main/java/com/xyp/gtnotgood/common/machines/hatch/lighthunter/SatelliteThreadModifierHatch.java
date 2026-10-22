// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.hatch.lighthunter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.machines.multiblock.NexusSatelliteFactory;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class SatelliteThreadModifierHatch extends MTEHatch {

    private static final IIconContainer OVERLAY = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "lighthunter/thread_modifier_hatch");
    private transient NexusSatelliteFactory controller;

    public SatelliteThreadModifierHatch(int id, String name) {
        super(id, name, "", 0, 1, new String[0]);
    }

    private SatelliteThreadModifierHatch(String name, ITexture[][][] textures) {
        super(name, 0, 1, new String[0], textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SatelliteThreadModifierHatch(mName, mTextures);
    }

    @Override
    public String getLocalName() {
        // #tr gtng.light_hunter.thread_modifier.name
        // # {\\LIGHT_PURPLE}Ω-Spheres Divergence Engine
        // # zh_CN {\\LIGHT_PURPLE}Ω-天球分歧引擎
        return StatCollector.translateToLocal("gtng.light_hunter.thread_modifier.name");
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr gtng.light_hunter.thread_modifier.purpose
            // # Adds threads to a Nexus Satellite Factory
            // # zh_CN 为枢纽卫星工厂增加额外线程
            StatCollector.translateToLocal("gtng.light_hunter.thread_modifier.purpose"),
            // #tr gtng.light_hunter.thread_modifier.amount
            // # Each ordinary Astral Array adds 128 threads; maximum 8192
            // # zh_CN 每个普通星阵增加 128 线程，最多增加 8192
            StatCollector.translateToLocal("gtng.light_hunter.thread_modifier.amount"),
            // #tr gtng.light_hunter.thread_modifier.inventory
            // # Holds up to 64 ordinary Astral Arrays without consuming them
            // # zh_CN 最多放入 64 个普通星阵，不消耗星阵
            StatCollector.translateToLocal("gtng.light_hunter.thread_modifier.inventory"),
            // #tr gtng.light_hunter.thread_modifier.limit
            // # One per satellite factory; cannot be shared; compressed arrays are not accepted
            // # zh_CN 每座卫星工厂最多 1 个，不可共享，不接受压缩星阵
            StatCollector.translateToLocal("gtng.light_hunter.thread_modifier.limit") };
    }

    public boolean bind(NexusSatelliteFactory factory) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (factory == null || !live(tile) || tile.isClientSide()) return false;
        IGregTechTileEntity owner = factory.getBaseMetaTileEntity();
        if (!live(owner) || tile.getWorld() != owner.getWorld()) return false;
        if (controller != null && controller != factory && isBoundTo(controller)) return false;
        controller = factory;
        return true;
    }

    public void unbind(NexusSatelliteFactory factory) {
        if (controller == factory) controller = null;
    }

    public int getAdditionalThreads(NexusSatelliteFactory factory) {
        return controller == factory && isBoundTo(factory) ? LightHunterRecipeRules.additionalThreads(getArrayCount())
            : 0;
    }

    public int getThreadMultiplier() {
        return isBoundTo(controller) ? LightHunterRecipeRules.THREAD_MULTIPLIER : 0;
    }

    public int getArrayCount() {
        ItemStack stack = mInventory[0];
        return acceptsArray(stack) ? Math.max(0, Math.min(LightHunterRecipeRules.MAX_THREAD_ARRAYS, stack.stackSize))
            : 0;
    }

    private boolean isBoundTo(NexusSatelliteFactory factory) {
        if (factory == null || !factory.mMachine || !factory.hasThreadModifier(this)) return false;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        IGregTechTileEntity owner = factory.getBaseMetaTileEntity();
        return live(tile) && live(owner) && tile.getWorld() == owner.getWorld();
    }

    private static boolean live(IGregTechTileEntity tile) {
        return tile != null && !tile.isDead()
            && tile.getWorld() != null
            && tile.getWorld().blockExists(tile.getXCoord(), tile.getYCoord(), tile.getZCoord())
            && tile.getWorld().getTileEntity(tile.getXCoord(), tile.getYCoord(), tile.getZCoord()) == tile;
    }

    public static boolean acceptsArray(ItemStack stack) {
        return stack != null && stack.getItem() == GTNGItemList.AstralArray.getItem();
    }

    @Override
    public int getInventoryStackLimit() {
        return LightHunterRecipeRules.MAX_THREAD_ARRAYS;
    }

    @Override
    public boolean isValidSlot(int index) {
        return index == 0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && acceptsArray(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return false;
    }

    @Override
    public boolean isLiquidInput(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && controller != null && !isBoundTo(controller)) controller = null;
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        controller = null;
    }

    @Override
    public void onRemoval() {
        controller = null;
        super.onRemoval();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return side == facing ? getTexturesActive(null) : new ITexture[] { HeartCasing.CreateCasing.getTexture() };
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { HeartCasing.CreateCasing.getTexture(),
            TextureFactory.builder().addIcon(OVERLAY).extFacing().glow().build() };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return getTexturesActive(base);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new ThreadGui(this).build(data, sync, settings);
    }

    private static final class ThreadGui extends MTEHatchBaseGui<SatelliteThreadModifierHatch> {

        private ThreadGui(SatelliteThreadModifierHatch machine) {
            super(machine);
        }

        @Override
        protected int getBasePanelHeight() {
            return 190;
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            ParentWidget<?> content = getEmptyContent();
            IntSyncValue multiplier = new IntSyncValue(machine::getThreadMultiplier);
            IntSyncValue threads = new IntSyncValue(() -> machine.getAdditionalThreads(machine.controller));
            sync.syncValue("threadMultiplier", multiplier);
            sync.syncValue("additionalThreads", threads);
            // #tr gtng.light_hunter.thread_modifier.multiplier
            // # Thread multiplier: %s
            // # zh_CN 当前线程倍率：%s
            content.child(IKey.dynamic(() -> StatCollector
                .translateToLocalFormatted("gtng.light_hunter.thread_modifier.multiplier", multiplier.getIntValue()))
                .asWidget().pos(5, 5));
            // #tr gtng.light_hunter.thread_modifier.threads
            // # Additional threads: %s
            // # zh_CN 额外线程总数：%s
            content.child(
                IKey.dynamic(() -> StatCollector.translateToLocalFormatted("gtng.light_hunter.thread_modifier.threads",
                    NumberFormatUtil.formatNumber(threads.getIntValue()))).asWidget().pos(5, 22));
            content.child(new ItemSlot().slot(new ModularSlot(machine.inventoryHandler, 0).singletonSlotGroup()
                .filter(SatelliteThreadModifierHatch::acceptsArray)).pos(120, 40));
            return content;
        }
    }
}
