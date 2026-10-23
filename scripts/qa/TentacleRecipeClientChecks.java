package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.NyarlathotepsTentacle;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NyarlathotepsTentacleStructure;
import com.xyp.gtnotgood.common.recipe.machine.TentacleAssemblyRecipes;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import bartworks.API.recipe.BartWorksRecipeMaps;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.maps.AssemblyLineFrontend;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "tentaclerecipeqa",
    name = "Tentacle Recipe QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class TentacleRecipeClientChecks {

    private BaseMetaTileEntity tile;
    private NyarlathotepsTentacle machine;
    private MTEHatchCraftingInputME buffer;
    private final List<int[]> hatchPositions = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private boolean launched;
    private volatile boolean checked;
    private volatile boolean finished;
    private int ticks;
    private int frames;
    private int page;
    private GTRecipe assembly;
    private GTRecipe highAssembly;
    private GTRecipe circuit;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.clouds = false;
            mc.launchIntegratedServer("tentacle-recipes-" + System.currentTimeMillis(), "Tentacle Recipe QA",
                new WorldSettings(83L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || checked) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++ticks == 1) build(player);
            if (ticks == 80) ordinaryInputs();
            if (ticks == 81) replaceInputs(player);
            if (ticks == 140) {
                patternInputs(assembly, 0);
                patternInputs(circuit, 1);
                modeMigration();
                checked = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void build(EntityPlayerMP player) {
        World world = player.worldObj;
        for (int x = -7; x <= 7; x++) for (int z = -2; z <= 5; z++) world.getChunkFromChunkCoords(x, z);
        tile = place(world, 0, 100, 0, GTNGMachineID.NyarlathotepsTentacle.id, player);
        machine = (NyarlathotepsTentacle) tile.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        String[][] shape = NyarlathotepsTentacleStructure.shape();
        for (int y = 0; y < shape.length; y++) for (int z = 0; z < shape[y].length; z++)
            for (int x = 0; x < shape[y][z].length(); x++) if (shape[y][z].charAt(x) == 'A') {
                int[] offset = new int[3];
                machine.getExtendedFacing().getWorldOffset(new int[] { x - 82, y - 23, z }, offset);
                hatchPositions.add(new int[] { offset[0], 100 + offset[1], offset[2] });
            }
        ItemStack[] hatches = { ItemList.Hatch_Input_Bus_UV.get(1), ItemList.Hatch_Output_Bus_UV.get(1),
            ItemList.Hatch_Input_UV.get(1), ItemList.Hatch_Input_UV.get(1), ItemList.Hatch_Input_UV.get(1),
            ItemList.Hatch_Input_UV.get(1), ItemList.Hatch_Energy_UIV.get(1) };
        for (int i = 0; i < hatches.length; i++) {
            int[] p = hatchPositions.get(i);
            place(world, p[0], p[1], p[2], hatches[i].getItemDamage(), player);
        }
        player.playerNetServerHandler.setPlayerLocation(0.5, 101, -3.5, 0, 0);
    }

    private void form() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        require(errors.isEmpty() && machine.checkStructure(true, tile), "full structure forms: " + errors);
    }

    private void ordinaryInputs() throws Exception {
        form();
        require(machine.getDataHatches().isEmpty(), "structure needs no data hatch");
        require(!RecipeMap.ALL_RECIPE_MAPS.containsKey("recipe.gtnotgood.suprachronal_assembly"),
            "duplicate UIV recipe map is not registered");
        require(GTNGRecipeMaps.TentacleAssemblyRecipes.getAssociatedCategories().size() == 1,
            "one assembly NEI category");
        require(machine.getAvailableRecipeMaps().size() == 2, "one assembly mode and one circuit mode");
        long nativeCount = RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> TentacleAssemblyRecipes.convert(recipe) != null).count();
        require(GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().size() == nativeCount,
            "all native tiers imported once");
        report.append("Single assembly NEI category contains all ").append(nativeCount)
            .append(" native assembly recipes, with no separate UIV map.\n");
        verifyImportedAlternatives();
        assembly = GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().stream()
            .filter(recipe -> usable(recipe) && recipe.mEUt <= TierEU.RECIPE_UV

                && recipe.mInputs.length > 1)
            .findFirst().orElseThrow(() -> new AssertionError("native recipe fixture missing"));
        machine.setMachineMode(0);
        supply(assembly);
        var input = machine.mInputBusses.get(0);
        ItemStack missing = input.mInventory[0];
        input.mInventory[0] = null;
        int before = itemCount(input.mInventory);
        require(!machine.checkProcessing().wasSuccessful() && itemCount(input.mInventory) == before,
            "missing item rejects without consuming remaining inputs");
        input.mInventory[0] = missing;
        FluidStack fluid = machine.mInputHatches.get(0).mFluid;
        machine.mInputHatches.get(0).mFluid = null;
        require(!machine.checkProcessing().wasSuccessful() && itemCount(input.mInventory) == before + missing.stackSize,
            "missing fluid rejects without consuming items");
        machine.mInputHatches.get(0).mFluid = fluid;
        start(assembly);
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        World world = tile.getWorld();
        world.removeTileEntity(0, 100, 0);
        tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.readFromNBT(saved);
        world.setTileEntity(0, 100, 0, tile);
        machine = (NyarlathotepsTentacle) tile.getMetaTileEntity();
        form();
        require(machine.getModuleWorks().size() == 1, "task survives full controller NBT reload");
        finish(assembly);
        supply(assembly);
        start(assembly);
        finish(assembly);
        highAssembly = GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().stream()
            .filter(recipe -> usable(recipe) && recipe.mEUt == TierEU.RECIPE_UIV).findFirst()
            .orElseThrow(() -> new AssertionError("native recipe fixture missing"));
        require(highAssembly.getRecipeCategory() == assembly.getRecipeCategory(), "UIV shares assembly category");
        require(machine.getMachineMode() == 0, "low and UIV recipes use the same machine mode");
        supply(highAssembly);
        start(highAssembly);
        finish(highAssembly);
        GTRecipe nativeCircuit = BartWorksRecipeMaps.circuitAssemblyLineRecipes.getAllRecipes().stream().filter(
            recipe -> usable(recipe) && recipe.mEUt <= TierEU.RECIPE_UIV && recipe.mSpecialItems instanceof ItemStack)
            .findFirst().orElseThrow(() -> new AssertionError("native recipe fixture missing"));
        require(BartWorksRecipeMaps.circuitAssemblyLineRecipes.findRecipeQuery().items(nativeCircuit.mInputs)
            .fluids(nativeCircuit.mFluidInputs).find() == null, "original circuit map still requires imprint");
        circuit = GTNGRecipeMaps.TentacleCircuitAssemblyRecipes.findRecipeQuery().items(nativeCircuit.mInputs)
            .fluids(nativeCircuit.mFluidInputs).find();
        require(circuit != null && circuit.mSpecialItems == null && nativeCircuit.mSpecialItems != null,
            "local copy removes imprint without modifying original: local="
                + GTNGRecipeMaps.TentacleCircuitAssemblyRecipes.getAllRecipes().size()
                + ", native="
                + BartWorksRecipeMaps.circuitAssemblyLineRecipes.getAllRecipes().size()
                + ", candidate="
                + nativeCircuit.mOutputs[0].getDisplayName()
                + ", enabled="
                + nativeCircuit.mEnabled
                + ", fake="
                + nativeCircuit.mFakeRecipe
                + ", matched="
                + (circuit != null));
        machine.setMachineMode(1);
        supply(circuit);
        start(circuit);
        finish(circuit);
        report.append(
            "No data hatch/stick or imprint; reversed inputs, missing-material refusal, cached repeat, low/UIV recipes in the same mode, full NBT reload and exact outputs passed.\n");
    }

    private void modeMigration() {
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        for (int legacyMode = 0; legacyMode < 3; legacyMode++) {
            NBTTagCompound legacy = (NBTTagCompound) saved.copy();
            legacy.removeTag("tentacleUnifiedAssembly");
            legacy.setInteger("machineMode", legacyMode);
            machine.loadNBTData(legacy);
            int expected = legacyMode == 2 ? 1 : 0;
            require(machine.getMachineMode() == expected, "legacy mode migrates " + legacyMode);
            require(legacy.getInteger("machineMode") == legacyMode, "input NBT stays unchanged");
            NBTTagCompound migrated = new NBTTagCompound();
            machine.saveNBTData(migrated);
            machine.setMachineMode(machine.nextMachineMode());
            machine.loadNBTData(migrated);
            require(machine.getMachineMode() == expected, "migrated mode survives another reload");
        }
        machine.setMachineMode(0);
        require(machine.nextMachineMode() == 1, "assembly switches to circuit");
        machine.setMachineMode(1);
        require(machine.nextMachineMode() == 0, "circuit switches back to assembly");
        report.append(
            "Legacy modes 0/1 merge to assembly; legacy circuit mode 2 migrates to 1; resave/reload and two-mode cycling passed.\n");
    }

    private static boolean usable(GTRecipe recipe) {
        return recipe.mEUt > 0 && recipe.mDuration > 0
            && recipe.mDuration <= 10000
            && recipe.mInputs.length <= 16
            && recipe.mFluidInputs.length > 0
            && recipe.mFluidInputs.length <= 4
            && recipe.mOutputs.length == 1
            && Arrays.stream(recipe.mInputs)
                .allMatch(stack -> stack != null && stack.stackSize > 0 && stack.stackSize <= 64);
    }

    private void supply(GTRecipe recipe) {
        var input = machine.mInputBusses.get(0);
        Arrays.fill(input.mInventory, null);
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (int i = 0; i < recipe.mInputs.length; i++)
            input.setInventorySlotContents(i, recipe.mInputs[recipe.mInputs.length - 1 - i].copy());
        for (int i = 0; i < recipe.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = recipe.mFluidInputs[recipe.mFluidInputs.length - 1 - i].copy();
    }

    private void start(GTRecipe recipe) {
        for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
            .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
        require(machine.getControllerSlot() == null && machine.getDataHatches().isEmpty(), "no research media");
        var result = machine.checkProcessing();
        require(result.wasSuccessful(),
            "starts " + recipe.mOutputs[0].getDisplayName() + ": " + result.getDisplayString());
        require(machine.getModuleWorks().size() == 1, "one funded task");
    }

    private void finish(GTRecipe recipe) throws Exception {
        tile.enableWorking();
        Method run = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        for (int tick = 0; tick < 12000 && !machine.getModuleWorks().isEmpty(); tick++) {
            for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
                .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
            run.invoke(machine, tile, 100000L + tick);
        }
        tile.disableWorking();
        require(machine.getModuleWorks().isEmpty(), "task completes");
        int produced = 0;
        for (var bus : machine.mOutputBusses) {
            for (ItemStack output : bus.mInventory)
                if (GTUtility.areStacksEqual(output, recipe.mOutputs[0])) produced += output.stackSize;
            Arrays.fill(bus.mInventory, null);
        }
        require(produced == recipe.mOutputs[0].stackSize, "output exactly once");
    }

    private void replaceInputs(EntityPlayerMP player) {
        World world = tile.getWorld();
        for (int index : new int[] { 0, 2, 3, 4, 5 }) {
            int[] p = hatchPositions.get(index);
            world.setBlock(p[0], p[1], p[2], TentacleCasing.MolecularCasing.getBlock(),
                TentacleCasing.MolecularCasing.getMetadata(), 3);
        }
        int[] p = hatchPositions.get(0);
        BaseMetaTileEntity bufferTile = place(world, p[0], p[1], p[2],
            ItemList.Hatch_CraftingInput_Bus_ME.get(1).getItemDamage(), player);
        buffer = (MTEHatchCraftingInputME) bufferTile.getMetaTileEntity();
        buffer.setConnectsToAllSides(true);
        bufferTile.enableWorking();
        world.setBlock(p[0], p[1], p[2] - 1,
            AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
    }

    private void patternInputs(GTRecipe recipe, int mode) throws Exception {
        form();
        require(
            machine.mInputBusses.isEmpty() && machine.mInputHatches.isEmpty() && machine.mDualInputHatches.size() == 1,
            "pattern buffer alone supplies items and fluids");
        require(buffer.isActive(), "real ME buffer grid is powered and active");
        List<IAEStack<?>> ingredients = new ArrayList<>();
        for (int i = recipe.mFluidInputs.length - 1; i >= 0; i--)
            ingredients.add(AEFluidStack.create(recipe.mFluidInputs[i].copy()));
        for (int i = recipe.mInputs.length - 1; i >= 0; i--)
            ingredients.add(AEItemStack.create(recipe.mInputs[i].copy()));
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (IAEStack<?> ingredient : ingredients) {
            NBTTagCompound entry = new NBTTagCompound();
            ingredient.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagCompound result = new NBTTagCompound();
        AEItemStack.create(recipe.mOutputs[0].copy()).writeToNBTGeneric(result);
        out.appendTag(result);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        buffer.setInventorySlotContents(0, pattern);
        var slot = buffer.inventories().next();
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, ingredients.size(), 1);
        for (int i = 0; i < ingredients.size(); i++) table.setInventorySlotContents(i, ingredients.get(i));
        require(buffer.pushPattern(slot.getPatternDetails(), table), "native Ultimate pattern delivery accepted");
        machine.setMachineMode(mode);
        start(recipe);
        require(slot.isEmpty(), "buffer ingredients consumed exactly once");
        finish(recipe);
        buffer.setInventorySlotContents(0, null);
        report.append("Active ME pattern buffer, Ultimate fluid NBT, real pushPattern and exact output passed in mode ")
            .append(mode).append(".\n");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !checked || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (++frames == 1) {
                mc.gameSettings.guiScale = page % 3 + 1;
                RecipeMap<?> map = page < 4 ? GTNGRecipeMaps.TentacleAssemblyRecipes
                    : GTNGRecipeMaps.TentacleCircuitAssemblyRecipes;
                GTNEIDefaultHandler handler = new GTNEIDefaultHandler(map.getDefaultRecipeCategory());
                if (page < 3) handler.loadCraftingRecipes(GTNGItemList.NyarlathotepsTentacle.get(1));
                else if (page == 3) handler.loadCraftingRecipes(highAssembly.mOutputs[0]);
                else handler.loadCraftingRecipes(circuit.mOutputs[0]);
                require(handler.numRecipes() > 0, "NEI has recipes");
                require(!map.getFrontend().getUIProperties().useSpecialSlot, "no data/imprint slot in NEI");
                if (
                    page == 3
                ) require(
                    handler.arecipes.stream().anyMatch(
                        recipe -> ((GTNEIDefaultHandler.CachedDefaultRecipe) recipe).mRecipe.mEUt >= TierEU.RECIPE_UIV),
                    "UIV recipe is visible in the merged assembly tab");
                if (page < 3) {
                    require(StatCollector.translateToLocal(map.unlocalizedName).equals("超时空装配线"), "pool title");
                    var recipe = (GTNEIDefaultHandler.CachedDefaultRecipe) handler.arecipes.get(0);
                    require(recipe.mRecipe.mInputs.length == 12 && recipe.mRecipe.mFluidInputs.length == 4,
                        "controller recipe retains all twelve inputs and four fluids");
                    require(recipe.getIngredients().size() == 16, "NEI renders every item and fluid");
                    require(map.getFrontend() instanceof AssemblyLineFrontend, "native assembly layout");
                }
                ArrayList<ICraftingHandler> handlers = new ArrayList<>();
                handlers.add(handler);
                var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
                constructor.setAccessible(true);
                mc.displayGuiScreen(constructor.newInstance(handlers));
            }
            if (frames == 50) {
                ScreenShotHelper.saveScreenshot(output(), "recipe-" + page + ".png", mc.displayWidth, mc.displayHeight,
                    mc.getFramebuffer());
                if (++page == 5) {
                    Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report
                        + "NEI: scales 1/2/3, reopen, 12 item + 4 fluid recipe, no special slot, UIV recipe in the single assembly category and circuit page.\n")
                            .getBytes(StandardCharsets.UTF_8));
                    finished = true;
                }
                frames = 0;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id, EntityPlayerMP player) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        return tile;
    }

    private static int itemCount(ItemStack[] stacks) {
        return Arrays.stream(stacks).filter(stack -> stack != null).mapToInt(stack -> stack.stackSize).sum();
    }

    private void verifyImportedAlternatives() {
        int checked = 0;
        for (RecipeAssemblyLine source : RecipeAssemblyLine.sAssemblylineRecipes) {
            GTRecipe.GTRecipe_WithAlt converted = (GTRecipe.GTRecipe_WithAlt) TentacleAssemblyRecipes.convert(source);
            if (converted == null) continue;
            for (int slot = 0; slot < source.mInputs.length; slot++) {
                require(ItemStack.areItemStacksEqual(source.mInputs[slot], converted.mInputs[slot]),
                    "native representative item and quantity are preserved");
            }
            require(converted.mDuration == source.mDuration && converted.mEUt == source.mEUt,
                "native recipe time and voltage are preserved");
            require(ItemStack.areItemStacksEqual(source.mOutput, converted.mOutputs[0]), "native output is preserved");
            checked++;
        }
        int count = GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().size();
        GTNGRecipeMaps.TentacleAssemblyRecipes.getBackend().reInit();
        require(GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().size() == count,
            "recipe reinitialization preserves all imported recipes");
        report.append("Native representative inputs, quantities, outputs, time, voltage and reInit preserved for ")
            .append(checked).append(" imported recipes.\n");
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.tentacle.output"));
        output.mkdirs();
        return output;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reporting) {
            reporting.printStackTrace();
        }
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
