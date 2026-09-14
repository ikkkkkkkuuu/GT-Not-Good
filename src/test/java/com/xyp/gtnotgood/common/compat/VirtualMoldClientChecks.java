package com.xyp.gtnotgood.common.compat;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/** Opt-in real Forge/GT reception and GUI checks, restricted to a newly generated disposable QA world. */
@Mod(
    modid = "virtualmoldqa",
    name = "Virtual Mold QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public class VirtualMoldClientChecks {

    private boolean started;
    private volatile boolean checked;
    private int ticks;
    private int frames;
    private BaseMetaTileEntity tile;
    private volatile int manualSelection = -2;
    private int manualTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.mold.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) return;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "virtual-mold-qa-" + System.currentTimeMillis(),
                "Virtual Mold QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP viewer = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (checked) {
            if (manualSelection >= -1 && ++manualTicks >= 5) {
                require(
                    VirtualMachineMolds.indexOf(VirtualMachineMolds.get((MTEBasicMachine) tile.getMetaTileEntity()))
                        == manualSelection,
                    "GUI selection synchronized to server");
                manualSelection = -2;
                System.out.println("VIRTUAL_MOLD_QA: GUI selection PASS");
            }
            if (++ticks == 60 && tile != null) tile.getMetaTileEntity()
                .onRightclick(tile, viewer, ForgeDirection.NORTH, 0.5f, 0.5f, 0.5f);
            return;
        }
        checked = true;
        try {
            WorldServer world = server.worldServerForDimension(0);
            world.setBlock(0, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
            tile = (BaseMetaTileEntity) world.getTileEntity(0, 6, 0);
            tile.setInitialValuesAsNBT(
                null,
                (short) ItemList.Machine_LV_Extruder.get(1)
                    .getItemDamage());
            tile.enableWorking();
            MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
            require(VirtualMachineMolds.supports(machine), "mixin attached");
            ItemStack plateMold = ItemList.Shape_Extruder_Plate.get(0);
            ItemStack rodMold = ItemList.Shape_Extruder_Rod.get(0);
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.apple), plateMold)
                .itemOutputs(new ItemStack(Items.feather))
                .duration(100)
                .eut(8)
                .addTo(machine.getRecipeMap());
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.apple), rodMold)
                .itemOutputs(new ItemStack(Items.stick))
                .duration(100)
                .eut(8)
                .addTo(machine.getRecipeMap());
            ICraftingPatternDetails plate = pattern(Items.feather);
            ICraftingPatternDetails rod = pattern(Items.stick);
            require(push(plate), "auto mold selection");
            require(
                VirtualMachineMolds.get(machine)
                    .isItemEqual(plateMold),
                "selected plate mold");
            require(!push(rod), "buffered mold switch blocked");
            machine.mMaxProgresstime = 100;
            require(push(plate), "same recipe refills during processing");
            require(!push(rod), "running mold switch blocked");
            machine.mMaxProgresstime = 0;
            // Native GT consumption sees the zero-size mold without consuming or exposing a real mold stack.
            Method allInputs = MTEBasicMachine.class.getDeclaredMethod("getAllInputs");
            allInputs.setAccessible(true);
            ItemStack[] nativeInputs = (ItemStack[]) allInputs.invoke(machine);
            require(
                machine.getRecipeMap()
                    .findRecipeQuery()
                    .items(nativeInputs)
                    .voltage(32)
                    .find() != null,
                "native GT lookup sees virtual mold");
            require(
                machine.getRecipeMap()
                    .findRecipeQuery()
                    .items(nativeInputs)
                    .voltage(32)
                    .find()
                    .isRecipeInputEqual(true, new net.minecraftforge.fluids.FluidStack[0], nativeInputs),
                "native GT consumes recipe");
            require(
                machine.getStackInSlot(machine.getInputSlot()).stackSize == 1,
                "native consumption uses exactly one real input");
            require(VirtualMachineMolds.get(machine).stackSize == 0, "native consumption preserves ghost mold");
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            require(push(rod), "empty machine changes mold");
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            VirtualMachineMolds.set(machine, null);
            machine.loadNBTData(saved);
            require(
                VirtualMachineMolds.get(machine)
                    .isItemEqual(rodMold),
                "NBT restores mold");
            for (ItemStack stack : machine.mInventory)
                require(stack == null || !stack.isItemEqual(rodMold), "mold is not extractable inventory");
            VirtualMachineMolds.set(machine, new ItemStack(Items.diamond));
            require(VirtualMachineMolds.get(machine) == null, "noncatalog item rejected");
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            VirtualMachineMolds.set(machine, rodMold);
            require(!push(plate, 129), "oversized delivery rejected");
            require(
                VirtualMachineMolds.get(machine)
                    .isItemEqual(rodMold),
                "failed delivery restores old mold");
            for (int i = 0; i < machine.mInputSlotCount; i++)
                require(machine.getStackInSlot(machine.getInputSlot() + i) == null, "failed delivery restores inputs");
            VirtualMachineMolds.set(machine, plateMold);
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            viewer.playerNetServerHandler.setPlayerLocation(2, 7, 2, 135, 30);
            viewer.capabilities.isFlying = true;
            viewer.sendPlayerAbilities();
            checkFluidPush(world);
            System.out.println("VIRTUAL_MOLD_QA: PASS");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("VIRTUAL_MOLD_QA: FAILED");
        }
    }

    private boolean push(ICraftingPatternDetails pattern) {
        return push(pattern, 1);
    }

    private boolean push(ICraftingPatternDetails pattern, int amount) {
        Container container = new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };
        MEInventoryCrafting table = new MEInventoryCrafting(container, 3, 3);
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(Items.apple, amount)));
        boolean accepted = AutomaticMachineCircuit.push(tile, pattern, table, ForgeDirection.NORTH);
        require(
            table.getAEStackInSlot(0)
                .getStackSize() == amount,
            "CPU table untouched");
        return accepted;
    }

    private static ICraftingPatternDetails pattern(net.minecraft.item.Item output) {
        return pattern(AEItemStack.create(new ItemStack(Items.apple)), output);
    }

    private static ICraftingPatternDetails pattern(IAEStack<?> input, net.minecraft.item.Item output) {
        return (ICraftingPatternDetails) Proxy.newProxyInstance(
            ICraftingPatternDetails.class.getClassLoader(),
            new Class<?>[] { ICraftingPatternDetails.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getAEInputs":
                        return new IAEStack<?>[] { input };
                    case "getAEOutputs":
                        return new IAEStack<?>[] { AEItemStack.create(new ItemStack(output)) };
                    case "getPattern":
                        return new ItemStack(Items.paper);
                    case "isCraftable":
                        return false;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    /** Exercises the native AE fluid-table path and mold restoration when the tank rejects an oversized batch. */
    private static void checkFluidPush(WorldServer world) {
        world.setBlock(1, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity fluidTile = (BaseMetaTileEntity) world.getTileEntity(1, 6, 0);
        fluidTile.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_LV_FluidSolidifier.get(1)
                .getItemDamage());
        fluidTile.enableWorking();
        // GT intentionally refuses sided fluid access during a newly placed tile's first five ticks.
        for (int i = 0; i < 7; i++) fluidTile.updateEntity();
        MTEBasicMachine machine = (MTEBasicMachine) fluidTile.getMetaTileEntity();
        net.minecraftforge.fluids.FluidStack water = new net.minecraftforge.fluids.FluidStack(
            net.minecraftforge.fluids.FluidRegistry.WATER,
            100);
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.Shape_Mold_Plate.get(0))
            .fluidInputs(water)
            .itemOutputs(new ItemStack(Items.carrot))
            .duration(100)
            .eut(8)
            .addTo(machine.getRecipeMap());
        ICraftingPatternDetails pattern = pattern(appeng.util.item.AEFluidStack.create(water), Items.carrot);
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        table.setInventorySlotContents(0, appeng.util.item.AEFluidStack.create(water));
        require(AutomaticMachineCircuit.push(fluidTile, pattern, table, ForgeDirection.NORTH), "fluid table accepted");
        require(
            machine.getFillableStack().amount == 100 && table.getAEStackInSlot(0)
                .getStackSize() == 100,
            "fluid quantity and CPU ownership preserved");
        require(
            VirtualMachineMolds.get(machine)
                .isItemEqual(ItemList.Shape_Mold_Plate.get(0)),
            "fluid recipe selects mold");
        machine.setFillableStack(null);
        VirtualMachineMolds.set(machine, ItemList.Shape_Mold_Ingot.get(0));
        water.amount = 100000;
        table.setInventorySlotContents(0, appeng.util.item.AEFluidStack.create(water));
        require(
            !AutomaticMachineCircuit.push(fluidTile, pattern, table, ForgeDirection.NORTH),
            "full tank rejects batch");
        require(machine.getFillableStack() == null, "failed fluid delivery restores tank");
        require(
            VirtualMachineMolds.get(machine)
                .isItemEqual(ItemList.Shape_Mold_Ingot.get(0)),
            "failed fluid delivery restores mold");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !checked || mc.theWorld == null || ticks < 65) return;
        org.lwjgl.input.Mouse.setCursorPosition(5, 5);
        if (++frames == 180) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "virtual-mold-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            System.out.println("VIRTUAL_MOLD_QA: screenshot saved");
        }
        if (frames == 200) com.cleanroommc.modularui.screen.ModularScreen.getCurrent()
            .getSyncManager()
            .getMainPSM()
            .findPanelHandlerNullable("gtngMoldSelector")
            .openPanel();
        if (frames == 220) {
            int index = VirtualMachineMolds.indexOf(ItemList.Shape_Extruder_Rod.get(0));
            com.cleanroommc.modularui.screen.ModularScreen.getCurrent()
                .getSyncManager()
                .getMainPSM()
                .findSyncHandler("gtngVirtualMold", com.cleanroommc.modularui.value.sync.IntSyncValue.class)
                .setIntValue(index);
            manualSelection = index;
        }
        if (frames == 300) ScreenShotHelper.saveScreenshot(
            new File("."),
            "virtual-mold-selector-qa.png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        if (frames == 350) mc.shutdown();
    }
}
