package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.xyp.gtnotgood.common.machines.basic.FragmentWorldCollector;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "fragmentcollectorqa",
    name = "Fragment Collector QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class FragmentCollectorClientChecks {

    private final StringBuilder report = new StringBuilder();
    private BaseMetaTileEntity tile;
    private FragmentWorldCollector machine;
    private GTRecipe ore;
    private GTRecipe fluid;
    private boolean launched;
    private volatile boolean checked;
    private volatile boolean finished;
    private volatile int requested = -1;
    private volatile int opened = -1;
    private volatile boolean clientTileReady;
    private boolean renderingMachine;
    private int clientTicks;
    private int ticks;
    private int frames;
    private int page;
    private int controlStage;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        clientTileReady = mc.theWorld != null
            && mc.theWorld.getTileEntity(0, 10, 0) instanceof BaseMetaTileEntity clientTile
            && clientTile.getMetaTileEntity() instanceof FragmentWorldCollector;
        if (launched && ++clientTicks > 6000 && !finished) fail(new AssertionError("client/world QA timeout"));
        if (finished) FMLCommonHandler.instance().exitJava(0, false);
        else if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.launchIntegratedServer("fragment-qa-" + System.currentTimeMillis(), "Fragment QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) checks(player);
            if (renderingMachine) tile.setStoredEU(machine.maxEUStore());
            if (requested >= 0 && ticks >= 40 && clientTileReady) {
                int target = requested;
                requested = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);
                if (target == 7) {
                    supply(ore);
                    require(machine.checkRecipe() == 2, "world render starts actual recipe");
                    renderingMachine = true;
                    player.playerNetServerHandler.setPlayerLocation(1.6, 10.6, -2.6, 24, 26);
                } else machine.onRightclick(tile, player);
                opened = target;
            }
            if (ticks > 1800) throw new AssertionError("client QA timeout");
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checks(EntityPlayerMP player) throws Exception {
        var world = player.worldObj;
        world.setWorldTime(1000);
        world.setBlock(0, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        tile = (BaseMetaTileEntity) world.getTileEntity(0, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) GTNGMachineID.FragmentWorldCollector.id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        machine = (FragmentWorldCollector) tile.getMetaTileEntity();
        machine.initDefaultModes(null);
        require(machine.mMainFacing == ForgeDirection.NORTH, "normal placement initializes the machine front");
        require(!machine.mItemTransfer && !machine.mFluidTransfer, "new machine retains native auto-output defaults");
        machine.onFirstTick(tile);
        machine.mItemTransfer = false;
        machine.mFluidTransfer = false;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);

        List<GTRecipe> recipes = new ArrayList<>(GTNGRecipeMaps.FragmentWorldCollectionRecipes.getAllRecipes());
        require(recipes.size() > 150, "collector recipe pool is populated: " + recipes.size());
        Set<String> dimensions = new HashSet<>();
        StringBuilder audit = new StringBuilder();
        for (GTRecipe recipe : recipes) {
            require(recipe.mEUt == 8, "ULV cost");
            require(recipe.mOutputs.length <= 12 && recipe.mInputs.length <= 3, "recipe fits physical slots");
            require(recipe.mInputs[0].stackSize == 0, "dimension is non-consumable");
            dimensions.add(itemStackKey(recipe.mInputs[0]));
            supply(recipe);
            require(machine.checkRecipe() == 2,
                "actual controller accepts recipe " + audit.length() + " inputs " + Arrays.toString(recipe.mInputs));
            require(machine.getStackInSlot(machine.getInputSlot()).stackSize == 1, "dimension remains");
            require(machine.mEUt == 8 && machine.mMaxProgresstime == recipe.mDuration, "exact power and duration");
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                if (recipe.getOutputChance(i) == 10000) {
                    require(
                        machine.mOutputItems[i] != null
                            && machine.mOutputItems[i].stackSize == recipe.mOutputs[i].stackSize
                            && GTUtility.areStacksEqual(machine.mOutputItems[i], recipe.mOutputs[i]),
                        "guaranteed output");
                }
            }
            if (recipe.mFluidOutputs.length > 0 && recipe.getFluidOutputChance(0) == 10000) {
                require(machine.mOutputFluid != null && machine.mOutputFluid.amount == recipe.mFluidOutputs[0].amount,
                    "exact fluid amount including integer maximum");
                if (
                    GTUtility.areStacksEqual(recipe.mInputs[0], ModsItemlist.DimensionOverworld.get(1))
                        && recipe.mFluidOutputs[0].amount == 1000
                ) fluid = recipe;
            }
            if (
                recipe.mOutputs.length >= 8 && recipe.mFluidOutputs.length == 0 && recipe.mDuration > 200 && ore == null
            ) ore = recipe;
            audit.append(recipe.mDuration).append(" ticks; ").append(Arrays.toString(recipe.mInputs)).append(" -> ")
                .append(Arrays.toString(recipe.mOutputs)).append("; fluids=")
                .append(Arrays.toString(recipe.mFluidOutputs)).append('\n');
        }
        Files.write(new File(output(), "recipes.txt").toPath(), audit.toString().getBytes(StandardCharsets.UTF_8));
        require(dimensions.size() == 16, "all sixteen dimensions, including approved Twilight/Triton substitutions");
        require(ore != null && fluid != null, "representative ore and fluid recipes");
        report.append("Accepted every registered recipe: ").append(recipes.size()).append(" across 16 dimensions.\n");
        require(fluid.mDuration == 200 && ore.mDuration > 200, "source duration retained with global speed disabled");

        supply(ore);
        machine.setInventorySlotContents(machine.getInputSlot(), null);
        require(machine.checkRecipe() != 2, "missing dimension rejected despite cached recipe");
        supply(fluid);
        machine.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Items.stick, 64));
        require(machine.checkRecipe() != 2, "full incompatible item output blocks start");
        require(machine.getStackInSlot(machine.getInputSlot() + 1).stackSize == 1, "failed start preserves drill");
        GTRecipe maximum = recipes.stream()
            .filter(r -> r.mFluidOutputs.length > 0 && r.mFluidOutputs[0].amount == Integer.MAX_VALUE).findFirst()
            .get();
        supply(maximum);
        FluidStack one = maximum.mFluidOutputs[0].copy();
        one.amount = 1;
        machine.setDrainableStack(one);
        require(machine.checkRecipe() != 2, "MAX fluid plus one is blocked without overflow");
        require(machine.getStackInSlot(machine.getInputSlot() + 1).stackSize == 1, "blocked MAX preserves drill");

        int consumed = 0;
        for (int attempt = 0; attempt < 3000; attempt++) {
            supply(fluid);
            require(machine.checkRecipe() == 2, "probabilistic drill recipe starts");
            consumed += 1 - machine.getStackInSlot(machine.getInputSlot() + 1).stackSize;
        }
        require(consumed > 0 && consumed < 100, "native drill chance is neither zero nor unconditional: " + consumed);
        report.append(
            "Missing selector, blocked outputs, MAX+1 overflow guard, cache switching, drill chance passed; consumed ")
            .append(consumed).append(" / 3000 steel drills.\n");

        GTRecipe probabilisticFluid = recipes.stream()
            .filter(r -> r.mFluidOutputs.length > 0 && r.getFluidOutputChance(0) == 500).findFirst().get();
        int fluidHits = 0;
        for (int attempt = 0; attempt < 2000; attempt++) {
            supply(probabilisticFluid);
            require(machine.checkRecipe() == 2, "chance fluid recipe starts");
            if (machine.mOutputFluid != null) fluidHits++;
        }
        require(fluidHits > 0 && fluidHits < 300, "native fluid output chance: " + fluidHits);
        report.append("5% lava output rolled ").append(fluidHits).append(" / 2000; source timings retained.\n");

        supply(ore);
        require(machine.checkRecipe() == 2, "NBT task starts");
        ItemStack[] expected = Arrays.stream(machine.mOutputItems).map(GTUtility::copyOrNull).toArray(ItemStack[]::new);
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        tile.readFromNBT(saved);
        machine = (FragmentWorldCollector) tile.getMetaTileEntity();
        require(machine.mMaxProgresstime == ore.mDuration, "full tile NBT restores active duration");
        require(machine.mMainFacing == ForgeDirection.NORTH, "full tile NBT restores main facing");
        machine.mItemTransfer = false;
        machine.mFluidTransfer = false;
        tile.disableWorking();
        machine.setInventorySlotContents(machine.getInputSlot(), null);
        for (int tick = 1; tick <= ore.mDuration + 5; tick++) {
            tile.setStoredEU(machine.maxEUStore());
            machine.onPostTick(tile, 1000 + tick);
        }
        for (int i = 0; i < expected.length; i++) {
            ItemStack actual = machine.getStackInSlot(machine.getOutputSlot() + i);
            require(expected[i] == null ? actual == null
                : actual != null && GTUtility.areStacksEqual(actual, expected[i])
                    && actual.stackSize == expected[i].stackSize,
                "resumed output exact once, slot " + i);
        }
        require(machine.mMaxProgresstime == 0, "restored task finishes");
        report.append("Full controller NBT restores task; real onPostTick completes once with exact rolled outputs.\n");
        autoOutputChecks(player);
        supply(ore);
        for (int i = 0; i < 12; i++)
            machine.setInventorySlotContents(machine.getOutputSlot() + i, new ItemStack(Items.diamond, i + 1));
        tile.markDirty();
        tile.issueTextureUpdate();
        checked = true;
        requested = 0;
    }

    private void autoOutputChecks(EntityPlayerMP player) {
        supply(ore);
        var world = player.worldObj;
        tile.setFrontFacing(ForgeDirection.SOUTH);
        world.setBlock(0, 10, 1, Blocks.chest, 0, 3);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(0, 10, 1);
        machine.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Items.diamond, 7));
        machine.mItemTransfer = false;
        machine.onPostTick(tile, 600);
        require(chest.getStackInSlot(0) == null, "disabled item output does not transfer");
        machine.mItemTransfer = true;
        machine.onPostTick(tile, 1200);
        require(
            chest.getStackInSlot(0) != null && chest.getStackInSlot(0).getItem() == Items.diamond
                && chest.getStackInSlot(0).stackSize == 7
                && machine.getStackInSlot(machine.getOutputSlot()) == null,
            "native auto output reaches adjacent chest exactly once");
        for (int i = 0; i < chest.getSizeInventory(); i++)
            chest.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
        machine.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Items.diamond, 7));
        machine.onPostTick(tile, 1800);
        require(machine.getStackInSlot(machine.getOutputSlot()).stackSize == 7, "full chest preserves output");
        machine.setInventorySlotContents(machine.getOutputSlot(), null);
        world.setBlock(0, 10, 1, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity inputTile = (BaseMetaTileEntity) world.getTileEntity(0, 10, 1);
        inputTile.setInitialValuesAsNBT(null, (short) ItemList.Hatch_Input_LV.get(1).getItemDamage());
        inputTile.setFrontFacing(ForgeDirection.NORTH);
        MTEHatchInput input = (MTEHatchInput) inputTile.getMetaTileEntity();
        input.onFirstTick(inputTile);
        for (int tick = 0; tick < 8; tick++) inputTile.updateEntity();
        machine.setDrainableStack(new FluidStack(FluidRegistry.WATER, 1000));
        machine.mFluidTransfer = false;
        machine.onPostTick(tile, 1820);
        require(input.getFillableStack() == null && machine.getDrainableStack().amount == 1000,
            "disabled fluid output does not transfer");
        machine.mFluidTransfer = true;
        machine.onPostTick(tile, 1840);
        require(
            input.getFillableStack() != null && input.getFillableStack().amount == 1000
                && machine.getDrainableStack() == null,
            "native fluid auto output reaches adjacent input hatch exactly once");
        NBTTagCompound save = new NBTTagCompound();
        tile.writeToNBT(save);
        machine.mItemTransfer = machine.mFluidTransfer = false;
        tile.readFromNBT(save);
        machine = (FragmentWorldCollector) tile.getMetaTileEntity();
        require(machine.mItemTransfer && machine.mFluidTransfer && tile.getFrontFacing() == ForgeDirection.SOUTH,
            "auto output flags and side survive full NBT restore");
        machine.mItemTransfer = machine.mFluidTransfer = false;
        tile.disableWorking();
        for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.SOUTH, ForgeDirection.EAST,
            ForgeDirection.WEST }) {
            world.setBlock(direction.offsetX, 10, direction.offsetZ, Blocks.chest, 0, 3);
            TileEntityChest fullChest = (TileEntityChest) world.getTileEntity(direction.offsetX, 10, direction.offsetZ);
            for (int slot = 0; slot < fullChest.getSizeInventory(); slot++)
                fullChest.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        }
        report.append(
            "Native auto output to chest / LV fluid input hatch, disabled switches, full chest and NBT persistence passed.\n");
    }

    private void supply(GTRecipe recipe) {
        for (int i = 0; i < machine.getSizeInventory(); i++) machine.setInventorySlotContents(i, null);
        Arrays.fill(machine.mOutputItems, null);
        machine.mOutputFluid = null;
        machine.mMaxProgresstime = machine.mProgresstime = machine.mEUt = 0;
        machine.setFillableStack(recipe.mFluidInputs.length == 0 ? null : recipe.mFluidInputs[0].copy());
        machine.setDrainableStack(null);
        int input = 0;
        for (ItemStack ingredient : recipe.mInputs) {
            ItemStack supplied = ingredient.copy();
            supplied.stackSize = Math.max(1, supplied.stackSize);
            if (supplied.getItem() == ItemList.Circuit_Integrated.getItem()) {
                require(supplied.getItemDamage() <= 24, "all circuits selectable in GTNH");
                machine.setInventorySlotContents(machine.getCircuitSlot(), supplied);
            } else machine.setInventorySlotContents(machine.getInputSlot() + input++, supplied);
        }
        tile.setStoredEU(machine.maxEUStore());
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !checked || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (page < 3) {
                if (opened != page || !(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
                if (++frames < 50) return;
                if (page == 0 && controlStage < 4) {
                    var screen = wrapper.getScreen();
                    if (controlStage == 0) {
                        var controls = named(screen.getMainPanel(), "collectorNativeAutoOutput").getChildren();
                        ((ToggleButton) controls.get(0)).onMousePressed(0);
                        ((ToggleButton) controls.get(1)).onMousePressed(0);
                    } else if (controlStage == 1) {
                        require(machine.mItemTransfer && machine.mFluidTransfer, "both GUI toggles reach server");
                        shiftClick((ToggleButton) named(screen.getMainPanel(), "collectorNativeAutoOutput")
                            .getChildren().get(0));
                    } else if (controlStage == 2) {
                        var popup = screen.getPanelManager().getOpenPanels().stream()
                            .filter(p -> p.getName().equals("sideSelector_fluidAutoOutput")).findFirst().get();
                        capture("output-side-selector");
                        List<ButtonWidget<?>> sides = new ArrayList<>();
                        collectButtons(popup, sides);
                        require(sides.size() == 5, "native side selector contains five output faces");
                        popup.doSafe(() -> sides.get(2).onMousePressed(0));
                    } else {
                        ForgeDirection expected = machine.mMainFacing.getRotation(ForgeDirection.DOWN);
                        BaseMetaTileEntity clientTile = (BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 10, 0);
                        require(tile.getFrontFacing() == expected && clientTile.getFrontFacing() == expected,
                            "direction selection reaches both sides: expected=" + expected
                                + ", server="
                                + tile.getFrontFacing()
                                + ", client="
                                + clientTile.getFrontFacing());
                        report.append(
                            "Native item/fluid toggles and Shift-click side selector synchronize to server; reopen retains settings.\n");
                    }
                    controlStage++;
                    frames = 0;
                    return;
                }
                long outputs = mc.thePlayer.openContainer.inventorySlots.stream()
                    .filter(slot -> slot.getHasStack() && slot.getStack().getItem() == Items.diamond).count();
                require(outputs == 12, "all twelve real output slots visible and synchronized");
                capture("gui-scale-" + (page + 1));
                mc.displayGuiScreen(null);
                page++;
                frames = 0;
                if (page < 3) {
                    mc.gameSettings.guiScale = page + 1;
                    requested = page;
                }
            } else if (page < 7) {
                if (++frames == 1) {
                    GTNEIDefaultHandler handler = new GTNEIDefaultHandler(
                        GTNGRecipeMaps.FragmentWorldCollectionRecipes.getDefaultRecipeCategory());
                    handler.loadCraftingRecipes(GTNGRecipeMaps.FragmentWorldCollectionRecipes.unlocalizedName);
                    GTRecipe target = page < 6 ? ore : fluid;
                    handler.arecipes
                        .removeIf(cached -> ((GTNEIDefaultHandler.CachedDefaultRecipe) cached).mRecipe != target);
                    require(handler.numRecipes() > 0, "NEI collector recipe page");
                    ArrayList<ICraftingHandler> handlers = new ArrayList<>();
                    handlers.add(handler);
                    var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
                    constructor.setAccessible(true);
                    mc.gameSettings.guiScale = (page - 3) % 3 + 1;
                    mc.displayGuiScreen(constructor.newInstance(handlers));
                }
                if (frames >= 50) {
                    capture("nei-" + page);
                    frames = 0;
                    if (++page == 7) {
                        mc.displayGuiScreen(null);
                        requested = 7;
                    }
                }
            } else if (opened == 7 && ++frames >= 50) {
                require(((BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 10, 0)).isActive(),
                    "running machine state reaches client");
                capture("machine-running");
                Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report
                    + "GUI scales 1/2/3, reopen, twelve synchronized output slots, NEI item/fluid pages and running block passed.\n")
                        .getBytes(StandardCharsets.UTF_8));
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static String itemStackKey(ItemStack stack) {
        return Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage();
    }

    private static IWidget named(IWidget root, String name) {
        if (name.equals(root.getName())) return root;
        for (IWidget child : root.getChildren()) {
            IWidget found = named(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static void collectButtons(IWidget root, List<ButtonWidget<?>> buttons) {
        if (root instanceof ButtonWidget<?>button) buttons.add(button);
        for (IWidget child : root.getChildren()) collectButtons(child, buttons);
    }

    private static void shiftClick(ToggleButton button) throws Exception {
        var field = Class.forName("org.lwjglx.input.Keyboard").getField("sdlKeyPressedArray");
        ByteBuffer previous = (ByteBuffer) field.get(null);
        ByteBuffer keys = ByteBuffer.allocate(previous.limit());
        keys.put(previous.duplicate()).rewind();
        int shift = (Integer) Class.forName("org.lwjglx.input.KeyCodes").getMethod("lwjglToSdlScancode", int.class)
            .invoke(null, Keyboard.KEY_LSHIFT);
        try {
            keys.put(shift, (byte) 1);
            field.set(null, keys);
            require(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT), "QA Shift key reaches the input API");
            button.onMousePressed(0);
        } finally {
            field.set(null, previous);
        }
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.fragmentCollector.output"));
        folder.mkdirs();
        return folder;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception secondary) {
            secondary.printStackTrace();
        }
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
