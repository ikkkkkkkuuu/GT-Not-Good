package com.xyp.gtnotgood.common.machines.basicMachine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
                BaseMetaTileEntity tile = place(player.worldObj, player);
                UniversalFluidPump pump = (UniversalFluidPump) tile.getMetaTileEntity();
                World world = player.worldObj;
                for (int x = 0; x <= 65; x++) world.setBlock(x, 99, 0, Blocks.lava, 0, 2);
                prepare(pump, tile);
                long initialEnergy = tile.getStoredEU();
                pump.onPostTick(tile, 1);
                require(pump.getDrainableStack().amount == 64000, "64 source blocks drained per tick without pipes");
                require(tile.getStoredEU() == initialEnergy - 512, "8 EU per block");
                require(
                    world.getBlock(63, 99, 0) == Blocks.air && world.getBlock(64, 99, 0) == Blocks.lava,
                    "batch limit retains next source");
                NBTTagCompound saved = new NBTTagCompound();
                tile.writeToNBT(saved);
                world.removeTileEntity(0, 100, 0);
                tile = new BaseMetaTileEntity();
                tile.setWorldObj(world);
                tile.readFromNBT(saved);
                world.setTileEntity(0, 100, 0, tile);
                pump = (UniversalFluidPump) tile.getMetaTileEntity();
                pump.onPostTick(tile, 2);
                require(pump.getDrainableStack().amount == 65000, "tank and cursor survive native NBT reload");
                require(
                    world.getBlock(64, 99, 0) == Blocks.air && world.getBlock(65, 99, 0) == Blocks.lava,
                    "radius includes 64 but excludes 65");
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
                world.setBlock(0, 100, -1, GregTechAPI.sBlockMachines, 0, 3);
                BaseMetaTileEntity sink = (BaseMetaTileEntity) world.getTileEntity(0, 100, -1);
                sink.setInitialValuesAsNBT(
                    null,
                    (short) gregtech.api.enums.ItemList.Hatch_Input_HV.get(1)
                        .getItemDamage());
                // Native GT fluid handlers reject transfers during the first five tile ticks.
                for (int warmup = 0; warmup < 10; warmup++) sink.updateEntity();
                sink.setFrontFacing(ForgeDirection.SOUTH);
                pump.mMainFacing = ForgeDirection.SOUTH;
                tile.setFrontFacing(ForgeDirection.NORTH);
                pump.mFluidTransfer = true;
                pump.setDrainableStack(new FluidStack(FluidRegistry.LAVA, 65000));
                pump.onPostTick(tile, 20);
                require(pump.getDrainableStack().amount == 1000, "auto-output transfers 64 buckets in one tick");
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

    private static void prepare(UniversalFluidPump pump, BaseMetaTileEntity tile) {
        NBTTagCompound data = new NBTTagCompound();
        pump.saveNBTData(data);
        data.setInteger("pumpScanOffset", CENTER);
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
        java.util.Set<String> positions = new java.util.HashSet<>();
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
