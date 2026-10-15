package com.xyp.gtnotgood.common.machines.hatch.me;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.tile.networking.TileEnergyCell;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import kubatech.tileentity.gregtech.multiblock.MTEExtremeEntityCrusher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ConfigItems;

@Mod(
    modid = "meoutputslaughterqa",
    name = "ME Output Slaughter QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MEOutputAssemblySlaughterClientChecks {

    private boolean started;
    private volatile boolean finished;
    private int clientTicks;
    private int ticks;
    private int stage;
    private int stageStarted;
    private int batches;
    private Fixture powered;
    private Fixture finite;
    private final ItemStack[] removedCells = new ItemStack[10];

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.meOutputSlaughter.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (++clientTicks > 9000) throw new AssertionError("Client QA timed out");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("me-output-slaughter-qa-" + System.currentTimeMillis(),
                    "ME Output Slaughter QA",
                    new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++ticks > 1800) throw new AssertionError("Server QA timed out at stage " + stage);
            if (ticks == 1) {
                player.worldObj.setWorldTime(6000);
                powered = new Fixture(player, 0, true);
                finite = new Fixture(player, 20, false);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(7, 12, -7, 0, 10);
            }
            if (ticks < 80) return;
            if (stage == 0) {
                if (!powered.assembly.getProxy().isActive() || !finite.assembly.getProxy().isActive()) return;
                powered.verifyNetwork();
                finite.verifyNetwork();
                powered.completeBatch();
                finite.completeBatch();
                batches = 1;
                advance(1);
            } else if (stage == 1) {
                if (batches < 4 && ticks - stageStarted >= batches * 7) {
                    powered.completeBatch();
                    batches++;
                }
                if (batches < 4 || !powered.empty()) return;
                powered.verifyConservation(true);
                if (ticks - stageStarted < 90) return;
                finite.verifyConservation(false);
                long cached = finite.assembly.getProvider().getCachedAmount();
                require(cached > 0 && cached < finite.expectedItems(),
                    "finite powered ME network exports part of a slaughter batch and retains the remainder");
                require(!finite.empty(), "finite power never voids unexported slaughter outputs");
                finite.enableCreativePower();
                for (int i = 0; i < removedCells.length; i++) {
                    removedCells[i] = powered.drive.getInternalInventory().getStackInSlot(i);
                    powered.drive.getInternalInventory().setInventorySlotContents(i, null);
                }
                advance(2);
            } else if (stage == 2 && ticks - stageStarted >= 45) {
                powered.completeBatch();
                advance(3);
            } else if (stage == 3 && ticks - stageStarted >= 90) {
                require(powered.assembly.getProvider().getCachedAmount() == batchItemAmount(),
                    "ME storage rejection retains the entire next item batch in the assembly cache");
                require(powered.assembly.getFluidOutput().getProvider().getCachedAmount() == 5000,
                    "ME storage rejection retains the entire XP fluid batch");
                finite.verifyConservation(true);
                for (int i = 0; i < removedCells.length; i++) {
                    powered.drive.getInternalInventory().setInventorySlotContents(i, removedCells[i]);
                }
                advance(4);
            } else if (stage == 4) {
                if (!powered.empty()) return;
                powered.verifyConservation(true);
                require(powered.completed == 5 && finite.completed == 1,
                    "native slaughter completion preserves five powered batches and one finite-power batch");
                powered.controller.onUnload();
                finite.controller.onUnload();
                Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft().shutdown();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void advance(int next) {
        stage = next;
        stageStarted = ticks;
    }

    private static final class Fixture {

        private final EntityPlayerMP player;
        private final int x;
        private final MEOutputAssembly assembly;
        private final TileDrive drive;
        private final MTEExtremeEntityCrusher controller;
        private final BaseMetaTileEntity controllerBase;
        private final Method runMachine;
        private final Fluid fluid;
        private int completed;

        private Fixture(EntityPlayerMP player, int x, boolean creative) throws Exception {
            this.player = player;
            this.x = x;
            World world = player.worldObj;
            world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
            ItemStack stack = GTNGItemList.MEOutputAssembly.get(1);
            base.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
            base.setOwnerName(player.getCommandSenderName());
            base.setOwnerUuid(player.getUniqueID());
            base.setFrontFacing(ForgeDirection.NORTH);
            assembly = (MEOutputAssembly) base.getMetaTileEntity();
            var definitions = AEApi.instance().definitions();
            world.setBlock(x, 10, -1, definitions.blocks().multiPart().maybeBlock().get());
            require(((IPartHost) world.getTileEntity(x, 10, -1)).addPart(
                definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player) != null,
                "fixture connects the assembly front using a native physical ME glass cable");
            world.setBlock(x + 1, 10, -1,
                (creative ? definitions.blocks().energyCellCreative() : definitions.blocks().energyCell()).maybeBlock()
                    .get());
            if (!creative) {
                NBTTagCompound power = new NBTTagCompound();
                power.setDouble("internalCurrentPower", 10000);
                ((TileEnergyCell) world.getTileEntity(x + 1, 10, -1)).readFromNBT_TileEnergyCell(power);
            }
            world.setBlock(x - 1, 10, -1, definitions.blocks().drive().maybeBlock().get());
            drive = (TileDrive) world.getTileEntity(x - 1, 10, -1);
            for (int i = 0; i < 9; i++) {
                drive.getInternalInventory().setInventorySlotContents(i,
                    definitions.items().cell64k().maybeStack(1).get());
            }
            drive.getInternalInventory().setInventorySlotContents(9, new ItemStack(ItemAndBlockHolder.CELL16384KM));
            controllerBase = new BaseMetaTileEntity();
            controllerBase.setWorldObj(world);
            controllerBase.xCoord = x;
            controllerBase.yCoord = 10;
            controllerBase.zCoord = 4;
            controller = new MTEExtremeEntityCrusher("qa_me_output_slaughter");
            controller.setBaseMetaTileEntity(controllerBase);
            controllerBase.disableWorking();
            controller.mWrench = controller.mScrewdriver = controller.mSoftMallet = true;
            controller.mHardHammer = controller.mSolderingTool = controller.mCrowbar = true;
            controller.mEfficiency = 10000;
            require(controller.addToMachineList(base, 16), "native Industrial Slaughterhouse accepts the assembly");
            require(
                controller.mOutputBusses.size() == 1 && controller.mOutputBusses.get(0) == assembly
                    && controller.mOutputHatches.size() == 1
                    && controller.mOutputHatches.get(0) == assembly.getFluidOutput(),
                "native Industrial Slaughterhouse registers item and fluid output roles exactly once");
            runMachine = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
            runMachine.setAccessible(true);
            Fluid xp = FluidRegistry.getFluid("xpjuice");
            fluid = xp == null ? FluidRegistry.WATER : xp;
            require(true, "slaughter fluid fixture uses " + fluid.getName());
        }

        private void verifyNetwork() throws Exception {
            var proxy = assembly.getProxy();
            require(drive.getActionableNode().getGrid() == proxy.getGrid(),
                "native ME drive and slaughter assembly share the physical cable network");
            require(assembly.getFluidOutput().getProvider().getProxy() == proxy,
                "both slaughter output roles share one AE node");
            int channels = 0;
            for (var node : proxy.getGrid().getNodes()) if (node.hasFlag(GridFlags.REQUIRE_CHANNEL)) channels++;
            require(channels == 2, "slaughter assembly consumes one channel alongside its ME drive");
        }

        private void completeBatch() throws Exception {
            controller.mOutputItems = batchItems();
            controller.mOutputFluids = new FluidStack[] { new FluidStack(fluid, 5000) };
            controller.mProgresstime = 0;
            controller.mMaxProgresstime = 1;
            controller.lEUt = 0;
            runMachine.invoke(controller, controllerBase, completed + 1L);
            require(
                controller.mOutputItems == null && controller.mOutputFluids == null && controller.mMaxProgresstime == 0,
                "actual Industrial Slaughterhouse finishes its native runMachine output path, batch "
                    + (completed + 1));
            completed++;
        }

        private boolean empty() {
            return assembly.getProvider().getCachedAmount() == 0
                && assembly.getFluidOutput().getProvider().getCachedAmount() == 0;
        }

        private long expectedItems() {
            return batchItemAmount() * completed;
        }

        private void verifyConservation(boolean drained) throws Exception {
            for (ItemStack item : batchItems()) {
                var key = AEItemStack.create(item).setStackSize(Long.MAX_VALUE);
                long stored = storedInCells(key, StorageChannel.ITEMS);
                long cached = assembly.getProvider().getCachedAmount(key);
                long expected = (long) item.stackSize * completed;
                require(stored + cached == expected && (!drained || cached == 0),
                    "slaughter item conservation: " + item.getDisplayName()
                        + " stored="
                        + stored
                        + " cached="
                        + cached
                        + " expected="
                        + expected
                        + " nbt="
                        + item.getTagCompound());
            }
            var key = AEFluidStack.create(new FluidStack(fluid, 1)).setStackSize(Long.MAX_VALUE);
            long stored = storedInCells(key, StorageChannel.FLUIDS);
            long cached = assembly.getFluidOutput().getProvider().getCachedAmount(key);
            require(stored + cached == 5000L * completed && (!drained || cached == 0),
                "slaughter fluid conservation: " + fluid.getName() + " stored=" + stored + " cached=" + cached);
            if (drained)
                require(empty(), "native ticking empties both slaughter output caches after ME accepts outputs");
        }

        private <T extends IAEStack<T>> long storedInCells(T key, StorageChannel channel) {
            long stored = 0;
            for (int i = 0; i < drive.getInternalInventory().getSizeInventory(); i++) {
                ItemStack stack = drive.getInternalInventory().getStackInSlot(i);
                if (stack == null) continue;
                IMEInventory<T> cell = AEApi.instance().registries().cell().getCellInventory(stack, null, channel);
                if (cell == null) continue;
                T extracted = cell.extractItems(key.copy(), Actionable.SIMULATE, assembly.getActionSource());
                if (extracted != null) stored += extracted.getStackSize();
            }
            return stored;
        }

        private void enableCreativePower() {
            player.worldObj.setBlock(x + 1, 10, -1,
                AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        }
    }

    private static ItemStack[] batchItems() {
        Item[] types = { Items.rotten_flesh, Items.bone, Items.gunpowder, Items.string, Items.spider_eye,
            Items.ender_pearl, Items.blaze_rod, Items.ghast_tear, Items.slime_ball, Items.magma_cream, Items.leather,
            Items.feather, Items.egg, Items.beef, Items.porkchop, Items.chicken, Items.fish, Items.gold_nugget,
            Items.iron_ingot };
        int[] amounts = { 62976, 56320, 39168, 29952, 14336, 11264, 4096, 1536, 48, 32, 9216, 8960, 5376, 3072, 1792,
            512, 256, 16, 64 };
        ItemStack[] outputs = new ItemStack[types.length + 6];
        for (int i = 0; i < types.length; i++) outputs[i] = new ItemStack(types[i], amounts[i]);
        for (int i = 0; i < 2; i++) {
            ItemStack tagged = new ItemStack(Items.iron_ingot, 2048 + i * 512);
            tagged.setTagCompound(new NBTTagCompound());
            tagged.getTagCompound().setString("slaughterVariant", i == 0 ? "first" : "second");
            outputs[types.length + i] = tagged;
        }
        Aspect[] aspects = { Aspect.AIR, Aspect.FIRE, Aspect.ORDER };
        for (int i = 0; i < aspects.length; i++) {
            ItemStack crystal = new ItemStack(ConfigItems.itemCrystalEssence, 9216 - i * 1024);
            ((IEssentiaContainerItem) crystal.getItem()).setAspects(crystal, new AspectList().add(aspects[i], 1));
            outputs[types.length + 2 + i] = crystal;
        }
        ItemStack heart = ModsItemlist.DragonHeart.get(1);
        if (heart == null) throw new AssertionError("Real Draconic Evolution dragon heart is not registered");
        heart.stackSize = 1792;
        outputs[types.length + 5] = heart;
        return outputs;
    }

    private static long batchItemAmount() {
        long result = 0;
        for (ItemStack item : batchItems()) result += item.stackSize;
        return result;
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.meOutputSlaughter.output"));
        directory.mkdirs();
        return directory;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), trace.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("ME_OUTPUT_SLAUGHTER_QA: " + message);
    }
}
