package com.xyp.gtnotgood.common.compat;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Mouse;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.xyp.gtnotgood.common.gui.modularui.widget.GhostMoldItemStackHandler;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.UltimatePatternHelper;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ggfab.GGItemList;
import ggfab.items.SingleUseTool;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTRecipe;

/** Opt-in real Forge/GT reception and GUI checks, restricted to a newly generated disposable QA world. */
@Mod(
    modid = "virtualmoldqa",
    name = "Virtual Mold QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public class VirtualMoldClientChecks {

    private boolean started;
    private volatile boolean checked;
    private volatile boolean failed;
    private volatile boolean guiChecked;
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
        if (failed) {
            mc.shutdown();
            return;
        }
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
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP viewer = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (checked) {
            if (failed) return;
            try {
                if (manualSelection >= -1 && ++manualTicks >= 5) {
                    require(
                        VirtualMachineMolds.indexOf(VirtualMachineMolds.get((MTEBasicMachine) tile.getMetaTileEntity()))
                            == manualSelection,
                        "GUI tool mold selection synchronized to server");
                    manualSelection = -2;
                    guiChecked = true;
                    System.out.println("VIRTUAL_MOLD_QA: GUI selection PASS");
                }
                if (++ticks == 60 && tile != null) tile.getMetaTileEntity()
                    .onRightclick(tile, viewer, ForgeDirection.NORTH, 0.5f, 0.5f, 0.5f);
            } catch (Throwable error) {
                fail(error);
            }
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
                    .isRecipeInputEqual(true, new FluidStack[0], nativeInputs),
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
            ICraftingPatternDetails optimized = pattern(
                AEItemStack.create(new ItemStack(Items.apple, 128)),
                AEItemStack.create(new ItemStack(Items.feather, 128)));
            require(push(optimized, 128), "optimized batch accepted");
            CircuitInputBufferState overflow = ((CircuitInputBuffer) machine).gtng$getCircuitInputBuffer();
            require(!overflow.isEmpty(), "oversized input retained in persistent buffer");
            require(!push(optimized, 128), "another batch waits for buffer to drain");
            NBTTagCompound bufferedSave = new NBTTagCompound();
            machine.saveNBTData(bufferedSave);
            overflow.replace(Collections.emptyList(), null, ForgeDirection.UNKNOWN);
            machine.loadNBTData(bufferedSave);
            require(!overflow.isEmpty(), "buffer survives save and reload");
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            AutomaticMachineCircuit.refill(machine);
            require(overflow.isEmpty(), "buffer refills empty input slots");
            require(machine.getStackInSlot(machine.getInputSlot()).stackSize == 64, "refill respects stack limit");
            VirtualMachineMolds.set(machine, plateMold);
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            viewer.playerNetServerHandler.setPlayerLocation(2, 7, 2, 135, 30);
            viewer.capabilities.isFlying = true;
            viewer.sendPlayerAbilities();
            checkAlternativeMolds(machine, plateMold, rodMold);
            checkToolMoldCatalog(world, machine);
            checkFluidPush(world);
            System.out.println("VIRTUAL_MOLD_QA: server checks PASS");
        } catch (Throwable error) {
            fail(error);
        }
    }

    /** Reproduces engraver recipes with several interchangeable catalog catalysts and one consumed input. */
    private void checkAlternativeMolds(MTEBasicMachine machine, ItemStack plateMold, ItemStack rodMold) {
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.carrot), plateMold)
            .itemOutputs(new ItemStack(Items.bone))
            .duration(100)
            .eut(8)
            .addTo(machine.getRecipeMap());
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.carrot), rodMold)
            .itemOutputs(new ItemStack(Items.bone))
            .duration(100)
            .eut(8)
            .addTo(machine.getRecipeMap());
        ICraftingPatternDetails alternatives = pattern(AEItemStack.create(new ItemStack(Items.carrot)), Items.bone);
        VirtualMachineMolds.set(machine, null);
        require(push(alternatives, 1, Items.carrot), "equivalent catalysts accepted without a preselected mold");
        ItemStack chosen = VirtualMachineMolds.get(machine)
            .copy();
        machine.mMaxProgresstime = 100;
        require(push(alternatives, 1, Items.carrot), "equivalent catalysts refill a running machine");
        require(
            VirtualMachineMolds.get(machine)
                .isItemEqual(chosen),
            "refill keeps the selected catalyst");
        machine.mMaxProgresstime = 0;
        for (ItemStack mold : new ItemStack[] { plateMold, rodMold }) {
            for (int i = 0; i < machine.mInputSlotCount; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, null);
            VirtualMachineMolds.set(machine, mold);
            require(push(alternatives, 1, Items.carrot), "each equivalent catalyst is accepted");
            require(
                VirtualMachineMolds.get(machine)
                    .isItemEqual(mold),
                "existing equivalent catalyst is preferred");
        }
        for (int i = 0; i < machine.mInputSlotCount; i++)
            machine.setInventorySlotContents(machine.getInputSlot() + i, null);
        VirtualMachineMolds.set(machine, plateMold);
    }

    private boolean push(ICraftingPatternDetails pattern) {
        return push(pattern, 1);
    }

    private boolean push(ICraftingPatternDetails pattern, int amount) {
        return push(pattern, amount, Items.apple);
    }

    private boolean push(ICraftingPatternDetails pattern, int amount, Item input) {
        Container container = new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };
        MEInventoryCrafting table = new MEInventoryCrafting(container, 3, 3);
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(input, amount)));
        boolean accepted = AutomaticMachineCircuit.push(tile, pattern, table, ForgeDirection.NORTH);
        require(
            table.getAEStackInSlot(0)
                .getStackSize() == amount,
            "CPU table untouched");
        return accepted;
    }

    private static ICraftingPatternDetails pattern(Item output) {
        return pattern(AEItemStack.create(new ItemStack(Items.apple)), output);
    }

    private static ICraftingPatternDetails pattern(IAEStack<?> input, Item output) {
        return pattern(input, AEItemStack.create(new ItemStack(output)));
    }

    private static ICraftingPatternDetails pattern(IAEStack<?> input, IAEStack<?> output) {
        return (ICraftingPatternDetails) Proxy.newProxyInstance(
            ICraftingPatternDetails.class.getClassLoader(),
            new Class<?>[] { ICraftingPatternDetails.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getAEInputs":
                        return new IAEStack<?>[] { input };
                    case "getAEOutputs":
                        return new IAEStack<?>[] { output };
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

    /** Verifies the same native tool molds in the single-block selector and dual-input hatch ghost slot. */
    private static void checkToolMoldCatalog(WorldServer world, MTEBasicMachine machine) {
        world.setBlock(2, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity hatchTile = (BaseMetaTileEntity) world.getTileEntity(2, 6, 0);
        hatchTile.setInitialValuesAsNBT(
            null,
            (short) GTNGItemList.SuperMTEHatchCraftingInputME.get(1)
                .getItemDamage());
        GhostMoldItemStackHandler ghost = new GhostMoldItemStackHandler(hatchTile.getMetaTileEntity());
        for (SingleUseTool tool : SingleUseTool.values()) {
            ItemStack mold = tool.mold.get(1L);
            int index = VirtualMachineMolds.indexOf(mold);
            require(index >= 0, "tool mold catalog: " + tool);
            VirtualMachineMolds.set(machine, mold);
            require(
                VirtualMachineMolds.get(machine) != null && VirtualMachineMolds.get(machine)
                    .isItemEqual(mold) && VirtualMachineMolds.get(machine).stackSize == 0,
                "single-block accepts tool mold: " + tool);
            require(ghost.isItemValid(0, mold), "dual-input ghost accepts tool mold: " + tool);
            ghost.setStackInSlot(0, mold.copy());
            require(
                ghost.getMoldIndex() == index && ghost.getStackInSlot(0).stackSize == 0,
                "dual-input stores zero-size tool mold: " + tool);
            require(ghost.extractItem(0, 1, false) == null, "tool mold cannot be extracted: " + tool);
            require(VirtualMachineMolds.indexOf(tool.tool.get(1L)) < 0, "consumable tool excluded: " + tool);
        }
        ghost.setMoldIndex(GhostMoldItemStackHandler.NO_MOLD);
    }

    /** Uses an existing GG fabrication recipe with an Ultimate Encoded Pattern's native fluid NBT. */
    private void checkFluidPush(WorldServer world) throws Exception {
        world.setBlock(1, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity fluidTile = (BaseMetaTileEntity) world.getTileEntity(1, 6, 0);
        fluidTile.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_MV_FluidSolidifier.get(1)
                .getItemDamage());
        fluidTile.enableWorking();
        // GT intentionally refuses sided fluid access during a newly placed tile's first five ticks.
        for (int i = 0; i < 7; i++) fluidTile.updateEntity();
        MTEBasicMachine machine = (MTEBasicMachine) fluidTile.getMetaTileEntity();
        ItemStack hammerMold = GGItemList.SingleUseHardHammerMold.get(0L);
        GTRecipe recipe = machine.getRecipeMap()
            .getAllRecipes()
            .stream()
            .filter(
                candidate -> candidate.mInputs.length == 1 && candidate.mInputs[0].stackSize == 0
                    && candidate.mInputs[0].isItemEqual(hammerMold)
                    && candidate.mFluidInputs.length == 1
                    && candidate.mOutputs.length == 1
                    && candidate.mFluidOutputs.length == 0
                    && candidate.mEUt <= GTValues.V[machine.mTier])
            .findFirst()
            .orElseThrow(() -> new AssertionError("native hard hammer solidifier recipe exists"));
        FluidStack fluid = recipe.mFluidInputs[0].copy();
        ICraftingPatternDetails pattern = fluidPattern(world, AEFluidStack.create(fluid), recipe.mOutputs[0]);
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        table.setInventorySlotContents(0, AEFluidStack.create(fluid));
        require(table.getAEStackInSlot(0) instanceof IAEFluidStack, "CPU contains native AE fluid");
        require(
            AutomaticMachineCircuit.patternConfigurations(machine.getRecipeMap(), pattern)
                .stream()
                .anyMatch(pair -> pair[1] == VirtualMachineMolds.indexOf(hammerMold)),
            "native hard hammer pattern classified without a physical mold");
        require(
            AutomaticMachineCircuit.push(fluidTile, pattern, table, ForgeDirection.NORTH),
            "native hard hammer fluid table accepted");
        require(
            machine.getFillableStack().amount == fluid.amount && table.getAEStackInSlot(0)
                .getStackSize() == fluid.amount,
            "fluid quantity and CPU ownership preserved");
        require(
            VirtualMachineMolds.get(machine)
                .isItemEqual(hammerMold),
            "real fluid recipe selects hard hammer mold");
        Method allInputs = MTEBasicMachine.class.getDeclaredMethod("getAllInputs");
        allInputs.setAccessible(true);
        ItemStack[] nativeInputs = (ItemStack[]) allInputs.invoke(machine);
        GTRecipe runnable = machine.getRecipeMap()
            .findRecipeQuery()
            .items(nativeInputs)
            .fluids(machine.getFillableStack())
            .voltage(GTValues.V[machine.mTier])
            .find();
        require(
            runnable != null && runnable.mOutputs[0].isItemEqual(recipe.mOutputs[0]),
            "native GT lookup finds hard hammer recipe with virtual mold");
        require(
            runnable.isRecipeInputEqual(true, new FluidStack[] { machine.getFillableStack() }, nativeInputs),
            "native GT consumes hard hammer recipe inputs");
        require(machine.getFillableStack().amount == 0, "native GT consumes the exact fluid quantity");
        require(VirtualMachineMolds.get(machine).stackSize == 0, "native GT preserves hard hammer ghost mold");
        for (ItemStack stack : machine.mInventory)
            require(stack == null || !stack.isItemEqual(hammerMold), "tool mold is absent from real inventory");
        machine.setFillableStack(null);
        VirtualMachineMolds.set(machine, ItemList.Shape_Mold_Ingot.get(0));
        FluidStack invalidFluid = fluid.copy();
        invalidFluid.amount++;
        table.setInventorySlotContents(0, AEFluidStack.create(invalidFluid));
        require(
            !AutomaticMachineCircuit.push(fluidTile, pattern, table, ForgeDirection.NORTH),
            "incorrect native fluid batch rejected");
        require(machine.getFillableStack() == null, "failed fluid delivery restores tank");
        require(
            VirtualMachineMolds.get(machine)
                .isItemEqual(ItemList.Shape_Mold_Ingot.get(0)),
            "failed fluid delivery restores mold");
        VirtualMachineMolds.set(machine, ItemList.Shape_Mold_Plate.get(0));
        tile = fluidTile;
        System.out.println("VIRTUAL_MOLD_QA: 8 tool molds and native hard hammer recipe PASS");
    }

    private static ICraftingPatternDetails fluidPattern(WorldServer world, IAEFluidStack input, ItemStack output) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList();
        NBTTagCompound nativeInput = new NBTTagCompound();
        Platform.writeStackNBT(input, nativeInput);
        inputs.appendTag(nativeInput);
        for (int i = 1; i < 16; i++) inputs.appendTag(new NBTTagCompound());
        NBTTagList outputs = new NBTTagList();
        NBTTagCompound nativeOutput = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(output), nativeOutput);
        outputs.appendTag(nativeOutput);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        ICraftingPatternDetails details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(details instanceof UltimatePatternHelper, "real Ultimate Encoded Pattern decoded");
        require(details.getAEInputs()[0] instanceof IAEFluidStack, "pattern NBT decodes native fluid input");
        return details;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private void fail(Throwable error) {
        failed = true;
        error.printStackTrace();
        System.out.println("VIRTUAL_MOLD_QA: FAILED");
        try {
            writeResult("FAIL");
        } catch (Exception outputError) {
            outputError.printStackTrace();
        }
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.mold.qa.output", "."));
    }

    private static void writeResult(String result) throws Exception {
        Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !checked || failed || mc.theWorld == null || ticks < 65) return;
        try {
            Mouse.setCursorPosition(5, 5);
            if (++frames == 180) {
                ScreenShotHelper.saveScreenshot(
                    outputDirectory(),
                    "virtual-mold-qa.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                System.out.println("VIRTUAL_MOLD_QA: screenshot saved");
            }
            if (frames == 200) ModularScreen.getCurrent()
                .getSyncManager()
                .getMainPSM()
                .findPanelHandlerNullable("gtngMoldSelector")
                .openPanel();
            if (frames == 220) {
                int index = VirtualMachineMolds.indexOf(GGItemList.SingleUseHardHammerMold.get(0L));
                require(index >= 0, "hard hammer mold has a selectable GUI index");
                ModularScreen.getCurrent()
                    .getSyncManager()
                    .getMainPSM()
                    .findSyncHandler("gtngVirtualMold", IntSyncValue.class)
                    .setIntValue(index);
                manualSelection = index;
            }
            if (frames == 300) ScreenShotHelper.saveScreenshot(
                outputDirectory(),
                "virtual-tool-mold-selector-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (frames == 330) ModularScreen.getCurrent()
                .getSyncManager()
                .getMainPSM()
                .findPanelHandlerNullable("gtngMoldSelector")
                .closePanel();
            if (frames == 350) ScreenShotHelper.saveScreenshot(
                outputDirectory(),
                "virtual-tool-mold-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (frames == 380) {
                require(guiChecked, "hard hammer GUI synchronization completed");
                writeResult("PASS");
                System.out.println("VIRTUAL_MOLD_QA: PASS");
                mc.shutdown();
            }
        } catch (Throwable error) {
            fail(error);
            mc.shutdown();
        }
    }
}
