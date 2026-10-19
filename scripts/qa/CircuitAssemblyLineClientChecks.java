package com.xyp.gtnotgood.common.blocks.packaged;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerNull;
import appeng.helpers.UltimatePatternHelper;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import bartworks.API.enums.CircuitImprint;
import bartworks.API.recipe.BartWorksRecipeMaps;
import bartworks.common.loaders.ItemRegistry;
import bartworks.common.tileentities.multis.MTECircuitAssemblyLine;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.GTRecipe;

@Mod(
    modid = "circuitassemblyqa",
    name = "Circuit Assembly Line QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CircuitAssemblyLineClientChecks {

    private boolean started;
    private volatile boolean finished, checked;
    private int clientTicks, serverTicks, frames;
    private Item fixture;
    private GTRecipe recipe;
    private BaseMetaTileEntity base;
    private MTECircuitAssemblyLine line;
    private TilePackagedProvider provider;
    private PackagedTarget target;
    private CircuitAssemblyLineAdapter adapter;
    private ICraftingPatternDetails pattern;
    private MEInventoryCrafting input;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.circuitAssembly.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("circuitAssemblyFixture");
        GameRegistry.registerItem(fixture, "fixture");
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (checked) {
                if (!(mc.currentScreen instanceof CoreScreen)) mc.displayGuiScreen(new CoreScreen());
            } else if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("circuit-assembly-qa-" + System.currentTimeMillis(), "Circuit Assembly QA",
                    new WorldSettings(741L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || checked) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks <= 3600, "server timeout");
            var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) setup(player);
            if (serverTicks != 80) return;
            // This fixture tests the adapter and native processing, independently of casing construction.
            attachHatches(player.worldObj);
            line.mMachine = true;
            base.enableWorking();
            checkRegistration();
            checkPattern(player.worldObj);
            checkPlanning();
            checkImprint(player);
            checkRejections(player);
            checkDispatchAndCollection();
            BartWorksRecipeMaps.circuitAssemblyLineRecipes.getBackend().removeRecipes(Arrays.asList(recipe));
            checked = true;
            System.out.println("CIRCUIT_ASSEMBLY_QA: server checks passed");
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        base = place(world, player, 8, ItemRegistry.cal);
        base.disableWorking();
        line = (MTECircuitAssemblyLine) base.getMetaTileEntity();
        for (int i = 0; i < 3; i++) {
            place(world, player, 10 + i, ItemList.Hatch_Input_Bus_IV.get(1));
        }
        place(world, player, 14, ItemList.Hatch_Input_IV.get(1));
        place(world, player, 15, ItemList.Hatch_Output_Bus_IV.get(1));
        place(world, player, 16, ItemList.Hatch_Energy_IV.get(1));
        line.mWrench = line.mScrewdriver = line.mSoftMallet = line.mHardHammer = line.mSolderingTool = line.mCrowbar = true;
        world.setBlock(3, 12, 0, Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
        provider = (TilePackagedProvider) world.getTileEntity(3, 12, 0);
        provider.setOwnerName(player.getCommandSenderName());
        provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.CircuitAssemblyLineCore.get(1));
        adapter = (CircuitAssemblyLineAdapter) PackagedCoreRegistry.get(GTNGItemList.CircuitAssemblyLineCore.get(1));
        target = new PackagedTarget(world.provider.dimensionId, 8, 12, 0, 2);
        require(provider.bind(target), "CAL controller binding");
        world.setBlock(2, 12, 0, AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        recipe = new GTRecipe(false, new ItemStack[] { stack(0, 2), stack(1, 1), stack(0, 3) },
            new ItemStack[] { stack(2, 1) }, CircuitImprint.Microprocessor.imprint.get(1), null, null, null, null,
            new FluidStack[] { water(300) }, null, 200, 30, 0);
        require(BartWorksRecipeMaps.circuitAssemblyLineRecipes.addRecipe(recipe) == recipe,
            "fixture recipe registered");
        require(recipe.mInputs.length == 3, "native fixture preserves repeated ordered ingredients");
        player.playerNetServerHandler.setPlayerLocation(3, 15, -3, 0, 0);
    }

    private void attachHatches(World world) {
        line.mInputBusses.clear();
        line.mInputHatches.clear();
        line.mOutputBusses.clear();
        line.mEnergyHatches.clear();
        for (int x = 10; x <= 12; x++) line.mInputBusses
            .add((MTEHatchInputBus) ((BaseMetaTileEntity) world.getTileEntity(x, 12, 0)).getMetaTileEntity());
        line.mInputHatches
            .add((MTEHatchInput) ((BaseMetaTileEntity) world.getTileEntity(14, 12, 0)).getMetaTileEntity());
        line.mOutputBusses
            .add((MTEHatchOutputBus) ((BaseMetaTileEntity) world.getTileEntity(15, 12, 0)).getMetaTileEntity());
        line.mEnergyHatches
            .add((MTEHatchEnergy) ((BaseMetaTileEntity) world.getTileEntity(16, 12, 0)).getMetaTileEntity());
    }

    private void checkRegistration() {
        require(adapter != null && adapter.accepts(base), "registered core accepts CAL");
        require(CraftingManager.getInstance().getRecipeList().stream().anyMatch(candidate -> candidate != null
            && candidate.getClass().getSimpleName().contains("Shapeless")
            && TilePackagedProvider.sameItem(candidate.getRecipeOutput(), GTNGItemList.CircuitAssemblyLineCore.get(1))),
            "registered shapeless core recipe");
        require(!new AssemblyLineAdapter(false).accepts(base) && !new AssemblyLineAdapter(true).accepts(base),
            "existing assembly cores do not claim CAL");
    }

    private void checkPattern(World world) {
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        var tag = new NBTTagCompound();
        var ins = new NBTTagList();
        var outs = new NBTTagList();
        for (IAEStack<?> entry : new IAEStack<?>[] { AEItemStack.create(stack(0, 5)), AEItemStack.create(stack(1, 1)),
            AEFluidStack.create(water(300)) }) {
            var nativeEntry = new NBTTagCompound();
            Platform.writeStackNBT(entry, nativeEntry);
            ins.appendTag(nativeEntry);
        }
        for (int i = 3; i < 16; i++) ins.appendTag(new NBTTagCompound());
        var result = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(stack(2, 1)), result);
        outs.appendTag(result);
        tag.setTag("in", ins);
        tag.setTag("out", outs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        pattern = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(pattern instanceof UltimatePatternHelper, "persisted Ultimate Encoded Pattern decodes natively");
        require(
            Arrays.stream(pattern.getAEInputs())
                .anyMatch(entry -> entry instanceof IAEFluidStack && entry.getStackSize() == 300),
            "Ultimate pattern preserves native 300 mB fluid");
        input = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        int slot = 0;
        for (var entry : pattern.getAEInputs()) if (entry != null) input.setInventorySlotContents(slot++, entry.copy());
        require(input.getAEStackInSlot(2) instanceof IAEFluidStack, "CPU input retains native fluid stack");
        provider.setInventorySlotContents(0, encoded);
    }

    private void checkPlanning() {
        ItemStack[] planned = CircuitAssemblyLineAdapter.planItems(recipe, input);
        require(planned != null && planned.length == 3, "native fluid packets ignored by item planner");
        require(planned[0].stackSize == 2 && planned[1].getItemDamage() == 1 && planned[2].stackSize == 3,
            "condensed repeated ingredient split in recipe lane order");
        require(CircuitAssemblyLineAdapter.matchesFluids(recipe, input), "native fluid matches exact recipe");
        String before = inputState();
        planned[0].stackSize = 40;
        require(before.equals(inputState()), "plans do not alias CPU stacks");
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 4)));
        require(CircuitAssemblyLineAdapter.planItems(recipe, input) == null, "item shortage rejected");
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 6)));
        require(CircuitAssemblyLineAdapter.planItems(recipe, input) == null, "item surplus rejected");
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 5)));
        input.setInventorySlotContents(2, AEFluidStack.create(water(299)));
        require(!CircuitAssemblyLineAdapter.matchesFluids(recipe, input), "native fluid shortage rejected");
        input.setInventorySlotContents(2, AEFluidStack.create(water(301)));
        require(!CircuitAssemblyLineAdapter.matchesFluids(recipe, input), "native fluid surplus rejected");
        input.setInventorySlotContents(2, AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 300)));
        require(!CircuitAssemblyLineAdapter.matchesFluids(recipe, input), "wrong fluid rejected");
        input.setInventorySlotContents(2, AEFluidStack.create(water(300)));
    }

    private void checkImprint(EntityPlayerMP player) {
        reject("unimprinted machine rejects dispatch");
        ItemStack previous = player.inventory.getCurrentItem();
        try {
            player.inventory.setInventorySlotContents(player.inventory.currentItem,
                CircuitImprint.Microprocessor.imprint.get(1));
            line.onLeftclick(base, player);
            require(line.getCircuitImprint() == CircuitImprint.Microprocessor, "native left click applies imprint");
            require(player.inventory.getCurrentItem() == null, "native imprint is consumed");
            player.inventory.setInventorySlotContents(player.inventory.currentItem,
                CircuitImprint.NanoProcessor.imprint.get(1));
            line.onLeftclick(base, player);
            require(line.getCircuitImprint() == CircuitImprint.Microprocessor
                && player.inventory.getCurrentItem().stackSize == 1, "native permanent imprint cannot be replaced");
            var saved = new NBTTagCompound();
            line.saveNBTData(saved);
            line.loadNBTData(saved);
            require(line.getCircuitImprint() == CircuitImprint.Microprocessor, "native imprint survives NBT reload");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, previous);
        }
    }

    private void checkRejections(EntityPlayerMP player) throws Exception {
        Method bufferSupport = MTECircuitAssemblyLine.class.getDeclaredMethod("supportsCraftingMEBuffer");
        bufferSupport.setAccessible(true);
        require(!(Boolean) bufferSupport.invoke(line), "native CAL mode rejects crafting input buffer");
        line.setMachineMode(1);
        require((Boolean) bufferSupport.invoke(line), "native circuit assembler mode supports crafting input buffer");
        reject("circuit assembler mode is outside CAL core scope");
        line.setMachineMode(0);
        line.mMaxProgresstime = 100;
        reject("running controller rejected");
        line.mMaxProgresstime = 0;
        base.disableWorking();
        reject("disabled controller rejected");
        base.enableWorking();
        line.mMachine = false;
        reject("unformed controller rejected");
        line.mMachine = true;
        var saved = new NBTTagCompound();
        line.saveNBTData(saved);
        var wrong = (NBTTagCompound) saved.copy();
        wrong.setInteger(MTECircuitAssemblyLine.IMPRINT_ID_KEY, CircuitImprint.NanoProcessor.id);
        line.loadNBTData(wrong);
        reject("wrong permanent imprint rejected");
        line.loadNBTData(saved);
        line.mInputBusses.get(1).setInventorySlotContents(0, stack(1, 1));
        reject("occupied input rejected");
        clear(line.mInputBusses.get(1));
        line.mInputHatches.get(0).mFluid = water(1);
        reject("occupied fluid hatch rejected");
        line.mInputHatches.get(0).mFluid = null;
        line.mOutputBusses.get(0).setInventorySlotContents(0, stack(2, 1));
        reject("occupied output rejected");
        clear(line.mOutputBusses.get(0));
        var ordinary = line.mInputBusses.set(0,
            (MTEHatchInputBus) place(player.worldObj, player, 18, ItemList.Hatch_Input_Bus_ME.get(1))
                .getMetaTileEntity());
        reject("ME input bus rejected");
        line.mInputBusses.set(0, ordinary);
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 4)));
        reject("dispatch item shortage rejected atomically");
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 6)));
        reject("dispatch item surplus rejected atomically");
        input.setInventorySlotContents(0, AEItemStack.create(stack(0, 5)));
        input.setInventorySlotContents(2, AEFluidStack.create(water(299)));
        reject("dispatch fluid shortage rejected atomically");
        input.setInventorySlotContents(2, AEFluidStack.create(water(300)));
    }

    private void checkDispatchAndCollection() throws Exception {
        require(provider.getProxy().isActive(), "provider grid active");
        final ICraftingPatternDetails[] advertised = new ICraftingPatternDetails[1];
        provider.provideCrafting((ICraftingProviderHelper) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { ICraftingProviderHelper.class }, (proxy, method, args) -> {
                if (method.getName().equals("addCraftingOption")) advertised[0] = (ICraftingPatternDetails) args[1];
                return null;
            }));
        require(advertised[0] != null, "Ultimate pattern advertised by real provider");
        String cpuBefore = inputState();
        require(provider.pushPattern(advertised[0], input), "real provider dispatch succeeds");
        require(provider.queuedJobs() == 1, "successful dispatch creates one receipt");
        require(cpuBefore.equals(inputState()), "adapter leaves CPU-owned input intact for AE success contract");
        for (int i = 0; i < 3; i++) {
            ItemStack actual = line.mInputBusses.get(i).getFirstStack();
            require(TilePackagedProvider.sameItem(actual, recipe.mInputs[i])
                && actual.stackSize == recipe.mInputs[i].stackSize, "ordered input lane " + i);
        }
        require(line.mInputHatches.get(0).getFluid().amount == 300, "exact native fluid delivered");
        require(line.getCircuitImprint() == CircuitImprint.Microprocessor && line.getControllerSlot() == null,
            "dispatch preserves native imprint and controller slot");
        reject("second receipt refused while target occupied");
        var result = line.checkProcessing();
        require(result.wasSuccessful(), "native CAL processing accepts delivered order: " + result.getID());
        require(line.mMaxProgresstime > 0, "native processing starts duration");
        for (var bus : line.mInputBusses) require(bus.getFirstStack() == null, "native recipe consumes item lane");
        require(line.mInputHatches.get(0).getFluid() == null || line.mInputHatches.get(0).getFluid().amount == 0,
            "native recipe consumes fluid");
        require(adapter.output(base) == line.mOutputBusses.get(0), "adapter exposes ordinary native output bus");
        // Complete only the output handoff here; power and casing validation remain the native controller's job.
        require(line.addOutputAtomic(recipe.mOutputs[0].copy()), "native output handoff reaches real output bus");
        line.setMachineMode(1);
        require(adapter.output(base) == line.mOutputBusses.get(0),
            "accepted receipt remains collectible after mode switch");
        Method collect = TilePackagedProvider.class.getDeclaredMethod("collectCompleted");
        collect.setAccessible(true);
        collect.invoke(provider);
        require(provider.queuedJobs() == 0 && line.mOutputBusses.get(0).getStackInSlot(0) == null,
            "receipt collects expected real bus output");
        require(TilePackagedProvider.sameItem(provider.getStackInSlot(TilePackagedProvider.PATTERNS), stack(2, 1)),
            "collected output retained safely without network storage");
        line.setMachineMode(0);
    }

    private void reject(String message) {
        String before = state();
        require(adapter.dispatch(provider, target, pattern, input) == null, message);
        require(before.equals(state()), message + " preserves CPU, hatches, controller and imprint");
    }

    private String state() {
        StringBuilder state = new StringBuilder(inputState());
        state.append('|').append(line.getCircuitImprint()).append('|').append(line.mMaxProgresstime);
        appendInventory(state, line);
        for (var bus : line.mInputBusses) appendInventory(state, bus);
        for (var hatch : line.mInputHatches) {
            appendInventory(state, hatch);
            if (hatch.getFluid() != null) state.append(hatch.getFluid().writeToNBT(new NBTTagCompound()));
        }
        for (var bus : line.mOutputBusses) appendInventory(state, bus);
        return state.toString();
    }

    private String inputState() {
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < input.getSizeInventory(); i++) {
            var entry = input.getAEStackInSlot(i);
            var tag = new NBTTagCompound();
            if (entry != null) Platform.writeStackNBT(entry, tag);
            state.append(i).append(':').append(tag).append(';');
        }
        return state.toString();
    }

    private static void appendInventory(StringBuilder state, IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            state.append('|').append(stack == null ? "empty" : stack.writeToNBT(new NBTTagCompound()));
        }
    }

    private static void clear(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) inventory.setInventorySlotContents(i, null);
    }

    private ItemStack stack(int meta, int amount) {
        return new ItemStack(fixture, amount, meta);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 12, 0, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) world.getTileEntity(x, 12, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof CoreScreen) || ++frames != 40) return;
        try {
            ScreenShotHelper.saveScreenshot(output(), "circuit-assembly-core.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File(output(), "result.txt").toPath(), ("PASS\nUltimate native fluid pattern round trip; "
                + "ordered duplicate lanes; exact input amounts; permanent native imprint; native mode/buffer contract; "
                + "atomic refusal; native recipe consumption; provider receipt and real output collection; core render.\n")
                    .getBytes(StandardCharsets.UTF_8));
            finished = true;
            System.out.println("CIRCUIT_ASSEMBLY_QA: PASS");
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.circuitAssembly.output", "build/circuit-assembly-qa"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            var trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("CIRCUIT_ASSEMBLY_QA: " + message);
    }

    private static final class CoreScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Circuit Assembly Line Packaged Core", width / 2, height / 2 - 45,
                0xffffff);
            ItemStack stack = GTNGItemList.CircuitAssemblyLineCore.get(1);
            RenderHelper.enableGUIStandardItemLighting();
            new RenderItem().renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, width / 2 - 8,
                height / 2 - 12);
            RenderHelper.disableStandardItemLighting();
            drawCenteredString(fontRendererObj, stack.getDisplayName(), width / 2, height / 2 + 20, 0xffffff);
            drawCenteredString(fontRendererObj, "Native imprint retained - exact ordered items and fluids", width / 2,
                height / 2 + 40, 0xaaaaaa);
        }
    }
}
