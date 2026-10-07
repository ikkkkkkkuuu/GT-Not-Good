package com.xyp.gtnotgood.common.machines.basic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Mods;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Disposable integrated-world checks of real drains, throughput, persistence, protection and the native pump GUI. */
@Mod(
    modid = "universalpumpqa",
    name = "Universal Pump QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class UniversalPumpClientChecks {

    private boolean started;
    private boolean checked;
    private boolean finished;
    private volatile boolean screenshot;
    private int frames;
    private int ticks;
    private int openGuiAt;
    private UniversalFluidPump guiPump;
    private boolean protect;
    private Block sample;
    private static final int CENTER = 64 * 129 + 64;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.pump.qa")) return;
        Fluid fluid = new Fluid("universalpumpqa.sample");
        FluidRegistry.registerFluid(fluid);
        sample = new QuarterBucketBlock(fluid);
        GameRegistry.registerBlock(sample, "quarter_bucket");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void protect(BlockEvent.BreakEvent event) {
        if (protect && event.x == 0 && event.y == 99 && event.z == 0) event.setCanceled(true);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "universal-pump-qa-" + System.currentTimeMillis(),
                "Universal Pump QA",
                new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++ticks > 1200) throw new AssertionError("GUI timed out");
            if (!checked && ticks > 60) {
                checked = true;
                verifyCursor();
                verifySearch();
                BaseMetaTileEntity tile = place(player.worldObj, player);
                World world = player.worldObj;
                tile = verifyBatchAndReload(tile);
                tile = verifyOilSearchAndStalls(tile);
                UniversalFluidPump pump = (UniversalFluidPump) tile.getMetaTileEntity();
                verifyRadius(pump, tile);
                world.setBlock(0, 99, 0, Blocks.water, 0, 2);
                prepare(pump, tile);
                tile.setStoredEU(0);
                pump.onPostTick(tile, 3);
                require(world.getBlock(0, 99, 0) == Blocks.water, "no power preserves sources");
                prepare(pump, tile);
                pump.setDrainableStack(new FluidStack(FluidRegistry.WATER, pump.getCapacity() - 500));
                pump.onPostTick(tile, 4);
                require(world.getBlock(0, 99, 0) == Blocks.water, "insufficient capacity preserves source");
                prepare(pump, tile);
                pump.setDrainableStack(new FluidStack(FluidRegistry.LAVA, 1000));
                pump.onPostTick(tile, 5);
                require(
                    world.getBlock(0, 99, 0) == Blocks.water && pump.getDrainableStack().amount == 1000,
                    "different fluid waits without mixing or deletion");
                prepare(pump, tile);
                protect = true;
                pump.onPostTick(tile, 6);
                protect = false;
                require(
                    world.getBlock(0, 99, 0) == Blocks.water && pump.getDrainableStack() == null,
                    "Forge protection cancellation prevents draining");
                prepare(pump, tile);
                pump.onPostTick(tile, 7);
                require(
                    pump.getDrainableStack().amount == 1000 && world.getBlock(0, 99, 0) == Blocks.air,
                    "water source actually removed");
                world.setBlock(0, 99, 0, Blocks.flowing_lava, 3, 2);
                prepare(pump, tile);
                pump.onPostTick(tile, 8);
                require(
                    world.getBlock(0, 99, 0) == Blocks.air && pump.getDrainableStack() == null,
                    "flowing lava yields no duplicate bucket");
                world.setBlock(0, 99, 0, sample, 0, 2);
                prepare(pump, tile);
                pump.onPostTick(tile, 9);
                require(
                    pump.getDrainableStack().amount == 250 && world.getBlock(0, 99, 0) == Blocks.air,
                    "Forge fluid drain volume honored (250 mB)");
                BaseMetaTileEntity netherTile = place(server.worldServerForDimension(-1), player);
                UniversalFluidPump netherPump = (UniversalFluidPump) netherTile.getMetaTileEntity();
                netherTile.getWorld()
                    .setBlock(0, 99, 0, Blocks.lava, 0, 2);
                prepare(netherPump, netherTile);
                netherPump.onPostTick(netherTile, 1);
                require(
                    netherTile.getWorld()
                        .getBlock(0, 99, 0) == Blocks.air
                        && netherPump.getDrainableStack()
                            .getFluid() == FluidRegistry.LAVA,
                    "actual Nether dimension lava");
                netherTile.disableWorking();
                tile.disableWorking();
                pump.mFluidTransfer = false;
                // NBT restores a fresh base tile; finish its native first-tick setup before checking output.
                for (int warmup = 0; warmup < 10; warmup++) tile.updateEntity();
                world.setBlock(0, 100, -1, GregTechAPI.sBlockMachines, 0, 3);
                BaseMetaTileEntity sink = (BaseMetaTileEntity) world.getTileEntity(0, 100, -1);
                sink.setInitialValuesAsNBT(
                    null,
                    (short) ItemList.Hatch_Input_UV.get(1)
                        .getItemDamage());
                // Native GT fluid handlers reject transfers during the first five tile ticks.
                for (int warmup = 0; warmup < 10; warmup++) sink.updateEntity();
                sink.setFrontFacing(ForgeDirection.SOUTH);
                pump.mMainFacing = ForgeDirection.SOUTH;
                tile.setFrontFacing(ForgeDirection.NORTH);
                pump.mFluidTransfer = true;
                pump.setDrainableStack(new FluidStack(FluidRegistry.LAVA, 513000));
                System.out.println(
                    "UNIVERSAL_PUMP_QA: output before front=" + tile.getFrontFacing()
                        + ", main="
                        + pump.mMainFacing
                        + ", target="
                        + (tile.getITankContainerAtSide(tile.getFrontFacing()) == sink)
                        + ", sourceAge="
                        + tile.getTimer()
                        + ", sinkAge="
                        + sink.getTimer()
                        + ", sinkCapacity="
                        + sink.getTankInfo(ForgeDirection.SOUTH)[0].capacity
                        + ", sinkAmount="
                        + sink.getMetaTileEntity()
                            .getFluidAmount()
                        + ", sinkAccepts="
                        + sink.fill(ForgeDirection.SOUTH, new FluidStack(FluidRegistry.LAVA, 512000), false)
                        + ", pumpAmount="
                        + pump.getDrainableStack().amount);
                pump.onPostTick(tile, 20);
                int pumpRemaining = pump.getDrainableStack() == null ? 0 : pump.getDrainableStack().amount;
                int sinkAmount = sink.getMetaTileEntity()
                    .getFluidAmount();
                System.out.println(
                    "UNIVERSAL_PUMP_QA: output after pumpAmount=" + pumpRemaining + ", sinkAmount=" + sinkAmount);
                require(
                    sinkAmount == 512000 && pumpRemaining == 1000,
                    "auto-output transfers exactly 512 buckets in one tick (pump=" + pumpRemaining
                        + ", sink="
                        + sinkAmount
                        + ")");
                player.playerNetServerHandler.setPlayerLocation(2, 100, -3, 0, 15);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                guiPump = pump;
                openGuiAt = ticks + 80;
                screenshot = true;
            } else if (guiPump != null && ticks >= openGuiAt) {
                guiPump.onRightclick(guiPump.getBaseMetaTileEntity(), player);
                guiPump = null;
            } else if (checked && !screenshot) {
                Files.write(new File("universal-pump-qa-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft()
                    .shutdown();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finished = true;
            Minecraft.getMinecraft()
                .shutdown();
        }
    }

    private static BaseMetaTileEntity verifyBatchAndReload(BaseMetaTileEntity tile) {
        World world = tile.getWorld();
        UniversalFluidPump pump = (UniversalFluidPump) tile.getMetaTileEntity();
        for (int i = 0; i < 513; i++) world.setBlock(i % 64, 99, i / 64, Blocks.lava, 0, 2);
        prepare(pump, tile);
        long initialEnergy = tile.getStoredEU();
        long startedAt = System.nanoTime();
        pump.onPostTick(tile, 1);
        System.out
            .println("UNIVERSAL_PUMP_QA: 512-source tick took " + (System.nanoTime() - startedAt) / 1000000.0 + " ms");
        require(pump.getDrainableStack().amount == 512000, "512 sources across multiple rows drained in one tick");
        require(tile.getStoredEU() == initialEnergy - 4096, "full batch consumes exactly 8 EU per block");
        int remaining = 0;
        for (int i = 0; i < 513; i++) {
            if (world.getBlock(i % 64, 99, i / 64) == Blocks.lava) remaining++;
        }
        require(remaining == 1, "world mutations stop at the 512-block batch limit");
        tile = reload(tile);
        pump = (UniversalFluidPump) tile.getMetaTileEntity();
        pump.onPostTick(tile, 2);
        require(pump.getDrainableStack().amount == 513000, "tank and pending search survive native NBT reload");
        boolean allDrained = true;
        for (int i = 0; i < 513; i++) allDrained &= world.getBlock(i % 64, 99, i / 64) == Blocks.air;
        require(allDrained, "batch reload consumes the remaining source without skipping any rows");
        require(
            pump.maxAmperesIn() == 128 && pump.maxEUStore() == 32768,
            "LV input supports full speed with original buffer");
        return tile;
    }

    private static BaseMetaTileEntity verifyOilSearchAndStalls(BaseMetaTileEntity tile) {
        World world = tile.getWorld();
        Block oilBlock = GameRegistry.findBlock(Mods.BuildCraftEnergy.ID, "blockOil");
        require(oilBlock instanceof IFluidBlock, "actual BuildCraft oil block available");
        IFluidBlock oilHandler = (IFluidBlock) oilBlock;
        Fluid oil = oilHandler.getFluid();
        UniversalFluidPump pump = (UniversalFluidPump) tile.getMetaTileEntity();
        for (int y = 60; y <= 99; y++) world.setBlock(0, y, 0, oilBlock, 0, 2);
        for (int y = 20; y <= 60; y++) world.setBlock(2, y, 0, oilBlock, 0, 2);
        world.setBlock(1, 60, 0, oilBlock, 1, 2);
        world.setBlock(20, 99, 20, oilBlock, 0, 2);
        require(!oilHandler.canDrain(world, 1, 60, 0), "flowing oil connector is not a source");
        prepare(pump, tile);
        long initialEnergy = tile.getStoredEU();
        long startedAt = System.nanoTime();
        pump.onPostTick(tile, 1);
        System.out
            .println("UNIVERSAL_PUMP_QA: oil-column tick took " + (System.nanoTime() - startedAt) / 1000000.0 + " ms");
        require(
            pump.getDrainableStack()
                .getFluid() == oil && pump.getDrainableStack().amount == 82000,
            "narrow oil columns and disconnected pool drained in one tick");
        boolean columnsDrained = true;
        for (int y = 60; y <= 99; y++) columnsDrained &= world.getBlock(0, y, 0) == Blocks.air;
        for (int y = 20; y <= 60; y++) columnsDrained &= world.getBlock(2, y, 0) == Blocks.air;
        require(columnsDrained, "connected oil search crosses many Y levels instead of waiting for layer scans");
        require(world.getBlock(20, 99, 20) == Blocks.air, "fallback scan finds a disconnected oil pool");
        require(
            world.getBlock(1, 60, 0) == oilBlock && tile.getStoredEU() == initialEnergy - 82 * 8,
            "non-drainable oil connector guides search without fluid duplication or energy charge");
        world.setBlock(1, 60, 0, Blocks.air, 0, 2);

        world.setBlock(0, 99, 0, oilBlock, 0, 2);
        world.setBlock(1, 99, 0, oilBlock, 1, 2);
        for (int y = 80; y <= 99; y++) world.setBlock(2, y, 0, oilBlock, 0, 2);
        prepare(pump, tile);
        tile.setStoredEU(24);
        pump.onPostTick(tile, 2);
        require(
            pump.getDrainableStack().amount == 3000 && tile.getStoredEU() == 0 && world.getBlock(2, 97, 0) == oilBlock,
            "partial energy batch stalls at the next connected oil source");
        tile = reload(tile);
        pump = (UniversalFluidPump) tile.getMetaTileEntity();
        tile.setStoredEU(tile.getEUCapacity());
        pump.onPostTick(tile, 3);
        require(pump.getDrainableStack().amount == 21000, "power-stalled frontier resumes after native NBT reload");
        columnsDrained = true;
        for (int y = 80; y <= 99; y++) columnsDrained &= world.getBlock(2, y, 0) == Blocks.air;
        require(columnsDrained, "resumed connected search removes all remaining oil sources");
        world.setBlock(1, 99, 0, Blocks.air, 0, 2);

        for (int y = 97; y <= 99; y++) world.setBlock(0, y, 0, oilBlock, 0, 2);
        prepare(pump, tile);
        pump.setDrainableStack(new FluidStack(oil, pump.getCapacity() - 1000));
        pump.onPostTick(tile, 4);
        require(
            pump.getDrainableStack().amount == pump.getCapacity() && world.getBlock(0, 99, 0) == Blocks.air
                && world.getBlock(0, 98, 0) == oilBlock,
            "full tank retains the next connected oil source");
        tile = reload(tile);
        pump = (UniversalFluidPump) tile.getMetaTileEntity();
        pump.setDrainableStack(new FluidStack(oil, pump.getCapacity() - 2000));
        pump.onPostTick(tile, 5);
        require(
            world.getBlock(0, 98, 0) == Blocks.air && world.getBlock(0, 97, 0) == Blocks.air
                && pump.getDrainableStack().amount == pump.getCapacity(),
            "tank-stalled frontier resumes after NBT reload and output space returns");
        return tile;
    }

    private static void verifyRadius(UniversalFluidPump pump, BaseMetaTileEntity tile) {
        World world = tile.getWorld();
        world.setBlock(64, 99, 0, Blocks.lava, 0, 2);
        world.setBlock(65, 99, 0, Blocks.lava, 0, 2);
        prepare(pump, tile);
        pump.onPostTick(tile, 1);
        require(
            world.getBlock(64, 99, 0) == Blocks.air && world.getBlock(65, 99, 0) == Blocks.lava
                && pump.getDrainableStack().amount == 1000,
            "search and fallback include radius 64 but exclude radius 65");
    }

    private static BaseMetaTileEntity reload(BaseMetaTileEntity tile) {
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        World world = tile.getWorld();
        int x = tile.xCoord;
        int y = tile.yCoord;
        int z = tile.zCoord;
        world.removeTileEntity(x, y, z);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(saved);
        world.setTileEntity(x, y, z, restored);
        return restored;
    }

    private static void prepare(UniversalFluidPump pump, BaseMetaTileEntity tile) {
        NBTTagCompound data = new NBTTagCompound();
        pump.saveNBTData(data);
        data.setInteger("pumpScanOffset", CENTER);
        data.setIntArray("pumpFluidSearch", new int[0]);
        data.setBoolean("pumpSearchStarted", false);
        pump.loadNBTData(data);
        pump.setDrainableStack(null);
        pump.mFluidTransfer = false;
        tile.enableWorking();
        tile.setStoredEU(Math.min(100000, tile.getEUCapacity()));
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player) {
        world.setBlock(0, 100, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(0, 100, 0);
        tile.setInitialValuesAsNBT(
            null,
            (short) GTNGItemList.UniversalFluidPump.get(1)
                .getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private static void verifyCursor() {
        PumpScanCursor cursor = new PumpScanCursor(1);
        Set<String> positions = new HashSet<>();
        for (int i = 0; i < 27; i++) {
            require(
                positions.add(cursor.xOffset() + ":" + cursor.y(2) + ":" + cursor.zOffset()),
                "unique coordinate " + i);
            require(cursor.advance(2) == (i == 26), "full volume completed only at last coordinate");
        }
        require(cursor.offset() == 0, "cursor wraps for unloaded-chunk retry");
        cursor.restore(Integer.MAX_VALUE, 2);
        require(cursor.offset() == 0, "invalid persisted offset safely resets");
    }

    private static void verifySearch() {
        PumpFluidSearch search = new PumpFluidSearch(1, 32);
        search.start(2);
        require(search.xOffset() == 0 && search.zOffset() == 0 && search.y() == 2, "central column is checked first");
        search.restore(new int[0], true, 2);
        search.follow(0, 1, 0, 2);
        require(search.y() == 0, "connected search prioritizes the below neighbor");
        search.restore(new int[] { -1, 13, 13, Integer.MAX_VALUE, 26 }, true, 2);
        require(search.positions().length == 2, "restored frontier rejects invalid and duplicate coordinates");
        search.restore(new int[0], true, 2);
        search.follow(1, 0, 1, 2);
        boolean bounded = true;
        while (search.hasPending()) {
            bounded &= Math.abs(search.xOffset()) <= 1 && Math.abs(search.zOffset()) <= 1
                && search.y() >= 0
                && search.y() <= 2;
            search.removeFirst();
        }
        require(bounded, "connected search cannot escape horizontal or vertical bounds");
        PumpFluidSearch limited = new PumpFluidSearch(1, 2);
        limited.start(20);
        require(limited.positions().length == 2, "pending search stays within its memory limit");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !screenshot || ++frames < 40) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
        ScreenShotHelper.saveScreenshot(
            new File("."),
            "universal-pump.png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        screenshot = false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("UNIVERSAL_PUMP_QA: " + message);
    }

    /** Simulated and committed drains return a non-bucket amount to catch hard-coded 1000 mB assumptions. */
    private static final class QuarterBucketBlock extends BlockFluidClassic {

        QuarterBucketBlock(Fluid fluid) {
            super(fluid, Material.water);
        }

        @Override
        public FluidStack drain(World world, int x, int y, int z, boolean commit) {
            if (commit) world.setBlock(x, y, z, Blocks.air, 0, 2);
            return new FluidStack(getFluid(), 250);
        }
    }
}
