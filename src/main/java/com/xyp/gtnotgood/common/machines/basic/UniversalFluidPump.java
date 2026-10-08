package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import gregtech.api.enums.Mods;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;

/**
 * Pipe-free LV world-fluid pump. Follows connected fluid downward, then scans the remaining area in bounded batches.
 * The native GT output tank owns fluid persistence and sided extraction; scan and search progress are saved here.
 * No dimension filter is applied, so Nether lava is handled exactly like Overworld lava.
 */
public final class UniversalFluidPump extends MTEBasicMachine {

    public static final int RADIUS = 64;
    public static final int BLOCKS_PER_TICK = 512;
    public static final int SCANS_PER_TICK = 8192;
    public static final int EU_PER_BLOCK = 8;
    private static final int INPUT_AMPERAGE = BLOCKS_PER_TICK * EU_PER_BLOCK / 32;
    private final PumpScanCursor scan = new PumpScanCursor(RADIUS);
    private final PumpFluidSearch search = new PumpFluidSearch(RADIUS, 32_768);
    private int scanTop = -1;
    private int rescanDelay;
    private boolean nativeTick;
    private FakePlayer fakePlayer;

    public UniversalFluidPump(int id, String name, String localizedName) {
        super(id, name, localizedName, 1, INPUT_AMPERAGE, description(), 0, 0, overlays());
        mFluidTransfer = true;
    }

    private UniversalFluidPump(String name, String[] description, ITexture[][][] textures) {
        super(name, 1, INPUT_AMPERAGE, description, textures, 0, 0);
        mFluidTransfer = true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new UniversalFluidPump(mName, mDescriptionArray, mTextures);
    }

    private static String[] description() {
        return new String[] {
            // #tr gtng.pump.area
            // # No mining pipes; scans 129 x 129 blocks below the pump
            // # zh_CN 无需采矿管；向下扫描 129×129 格范围
            StatCollector.translateToLocal("gtng.pump.area"),
            // #tr gtng.pump.search
            // # Checks below first, then follows connected fluid downward before scanning the remaining area
            // # zh_CN 先检查正下方，优先沿连通流体向下抽取，再扫描其余区域
            StatCollector.translateToLocal("gtng.pump.search"),
            // #tr gtng.pump.speed
            // # Up to 512 blocks/tick, 8 EU/block (4096 EU/t at full speed)
            // # zh_CN 最多 512 方块/tick，每方块 8 EU（满速 4096 EU/t）
            StatCollector.translateToLocal("gtng.pump.speed"),
            // #tr gtng.pump.fluids
            // # Drains water, lava (including Nether lava), and drainable Forge fluid blocks
            // # zh_CN 实际抽走水、岩浆（含下界）及可抽取的 Forge 流体方块
            StatCollector.translateToLocal("gtng.pump.fluids"),
            // #tr gtng.pump.output
            // # Buffer: 16,000 buckets; auto-output: up to 512 buckets/tick
            // # zh_CN 缓存 16000 桶；自动输出最高 512 桶/tick
            StatCollector.translateToLocal("gtng.pump.output"),
            // #tr gtng.pump.safety
            // # Loaded chunks only; pauses when full or when changing buffered fluid
            // # zh_CN 仅扫描已加载区块；缓存满或切换流体时等待输出
            StatCollector.translateToLocal("gtng.pump.safety") };
    }

    /** References GT's installed pump textures without copying assets into this mod. */
    private static ITexture[] overlays() {
        String[] faces = { "SIDE", "FRONT", "TOP", "BOTTOM" };
        ITexture[] result = new ITexture[8];
        for (int i = 0; i < faces.length; i++) {
            String path = "basicmachines/pump/OVERLAY_" + faces[i];
            result[i * 2] = TextureFactory
                .of(Textures.BlockIcons.customOptional(Mods.GregTech.resourceDomain, path + "_ACTIVE"));
            result[i * 2 + 1] = TextureFactory
                .of(Textures.BlockIcons.customOptional(Mods.GregTech.resourceDomain, path));
        }
        return result;
    }

    @Override
    public int getCapacity() {
        return 16_000_000;
    }

    /** Supplies the 4096 EU/t pumping budget at LV voltage (32 EU per packet). */
    @Override
    public long maxAmperesIn() {
        return INPUT_AMPERAGE;
    }

    /** Preserves the original energy buffer independently of the lowered voltage tier. */
    @Override
    public long maxEUStore() {
        return 32_768;
    }

    @Override
    public boolean canTankBeFilled() {
        return false;
    }

    @Override
    public int checkRecipe() {
        return DID_NOT_FIND_RECIPE;
    }

    /** Prevents native batch-output mixins from bypassing the pump's own per-tick transfer budget. */
    @Override
    public boolean doesAutoOutputFluids() {
        return !nativeTick && super.doesAutoOutputFluids();
    }

    @Override
    protected BasicUIProperties getUIProperties() {
        return BasicUIProperties.builder().maxItemInputs(0).maxItemOutputs(0).maxFluidInputs(0).maxFluidOutputs(1)
            .build();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEBasicMachineBaseGui<>(this, getUIProperties()).useGregTechLogo(true).build(data, syncManager,
            settings);
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("pumpScanOffset", scan.offset());
        nbt.setIntArray("pumpFluidSearch", search.positions());
        nbt.setBoolean("pumpSearchStarted", search.started());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        World world = tile.getWorld();
        scanTop = (world == null ? tile.getYCoord() : Math.min(tile.getYCoord(), world.getActualHeight())) - 1;
        scan.restore(nbt.getInteger("pumpScanOffset"), scanTop);
        search.restore(nbt.getIntArray("pumpFluidSearch"), nbt.getBoolean("pumpSearchStarted"), scanTop);
        rescanDelay = 0;
    }

    /**
     * Keeps world mutations on the server and bounds both empty-space probes and successful drains per tick.
     * Unloaded/protected blocks are skipped and revisited on the next pass; tank/energy stalls retain the cursor.
     *
     * @param tile owning GT tile
     * @param tick native tile tick counter
     */
    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        nativeTick = true;
        try {
            super.onPostTick(tile, tick);
        } finally {
            nativeTick = false;
        }
        if (!tile.isServerSide()) return;
        outputFluid(tile, BLOCKS_PER_TICK * 1000);
        tile.setActive(false);
        if (!tile.isAllowedToWork() || !tile.isUniversalEnergyStored(EU_PER_BLOCK)) return;
        if (rescanDelay > 0 && !search.hasPending()) {
            search.reset();
            rescanDelay--;
            return;
        }
        int top = Math.min(tile.getYCoord(), tile.getWorld().getActualHeight()) - 1;
        if (top < 0) return;
        if (top != scanTop) {
            scan.restore(scan.offset(), top);
            search.restore(search.positions(), search.started(), top);
            scanTop = top;
        }
        search.start(top);
        int pumped = 0;
        for (int checked = 0; checked < SCANS_PER_TICK && pumped < BLOCKS_PER_TICK; checked++) {
            boolean following = search.hasPending();
            if (!following && rescanDelay > 0) {
                search.reset();
                break;
            }
            int dx = following ? search.xOffset() : scan.xOffset();
            int y = following ? search.y() : scan.y(top);
            int dz = following ? search.zOffset() : scan.zOffset();
            int x = tile.getXCoord() + dx;
            int z = tile.getZCoord() + dz;
            int result = pumpBlock(tile, x, y, z);
            if (result < 0) break;
            if (following) search.removeFirst();
            if (result == 1) pumped++;
            if (result > 0) search.follow(dx, y, dz, top);
            if (!following && scan.advance(top)) rescanDelay = 100;
        }
        tile.setActive(pumped > 0);
        markDirty();
    }

    /**
     * Simulates mod-fluid drainage and tank acceptance before mutation. Flowing vanilla blocks yield no fluid.
     * Forge implementations are required to honor their drain simulation; their actual returned volume is stored.
     *
     * @return -1 to retry after output/power becomes available, 0 to skip, 1 for a consumed block,
     *         or 2 to follow a non-drainable Forge fluid without producing fluid or consuming energy
     */
    private int pumpBlock(IGregTechTileEntity tile, int x, int y, int z) {
        World world = tile.getWorld();
        if (!world.blockExists(x, y, z)) return 0;
        Block block = world.getBlock(x, y, z);
        boolean water = block == Blocks.water || block == Blocks.flowing_water;
        boolean lava = block == Blocks.lava || block == Blocks.flowing_lava;
        FluidStack preview;
        if (water || lava) {
            preview = world.getBlockMetadata(x, y, z) == 0
                ? new FluidStack(water ? FluidRegistry.WATER : FluidRegistry.LAVA, 1000)
                : null;
        } else if (block instanceof IFluidBlock fluidBlock) {
            if (fluidBlock.getFluid() == null) return 0;
            if (!fluidBlock.canDrain(world, x, y, z)) return 2;
            preview = fluidBlock.drain(world, x, y, z, false);
            if (preview == null || preview.amount <= 0 || preview.getFluid() == null) return 0;
        } else {
            return 0;
        }
        if (!tile.isUniversalEnergyStored(EU_PER_BLOCK)) return -1;
        if (preview != null && fluidOutputTank.fill(preview, false) != preview.amount) return -1;
        if (fakePlayer == null) fakePlayer = GTUtility.getFakePlayer(tile);
        fakePlayer.setWorld(world);
        fakePlayer.setPosition(tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        if (!GTUtility.eraseBlockByFakePlayer(fakePlayer, x, y, z, true)) return 0;
        FluidStack drained = preview;
        if (water || lava) {
            if (!world.setBlock(x, y, z, Blocks.air, 0, 2)) return 0;
        } else {
            drained = ((IFluidBlock) block).drain(world, x, y, z, true);
            if (drained == null || drained.amount <= 0) return 0;
        }
        if (drained != null) fluidOutputTank.fill(drained, true);
        tile.decreaseStoredEnergyUnits(EU_PER_BLOCK, false);
        return 1;
    }

    /** Transfers the pump's per-tick budget independently of native batch-output mixins. */
    private void outputFluid(IGregTechTileEntity tile, int limit) {
        if (limit <= 0) return;
        if (!doesAutoOutputFluids() || getDrainableStack() == null || tile.getFrontFacing() == mMainFacing) return;
        ForgeDirection side = tile.getFrontFacing();
        IFluidHandler target = tile.getITankContainerAtSide(side);
        if (target == null) return;
        FluidStack offered = drain(limit, false);
        if (offered == null) return;
        int accepted = target.fill(side.getOpposite(), offered, true);
        if (accepted > 0) drain(Math.min(accepted, offered.amount), true);
    }
}
