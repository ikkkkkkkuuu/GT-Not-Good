package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary.BeeSimulator;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.common.loaders.ItemRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.apiculture.genetics.BeeDefinition;
import forestry.plugins.PluginApiculture;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;

@Mod(
    modid = "industrialapiaryqa",
    name = "Industrial Apiary QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LargeIndustrialApiaryClientChecks {

    private BaseMetaTileEntity base;
    private LargeIndustrialApiary machine;
    private MTEHatchInputBus input;
    private int[] flower, glass;
    private boolean started;
    private volatile boolean checked, finished, openRequested, opened;
    private int clientTicks, serverTicks, frames, stage;
    private final StringBuilder report = new StringBuilder();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.industrialApiary.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks < 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("industrial-apiary-qa-" + System.currentTimeMillis(), "Industrial Apiary QA",
                    new WorldSettings(1703L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
        try {
            require(++serverTicks < 3600, "server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) setup(player);
            if (serverTicks == 40) {
                verifyStructure(player.worldObj);
                verifyRecipe();
                verifyProduction(player.worldObj);
                verifyPersistence();
                verifyQueenTransfer(player.worldObj);
                verifySwarmer(player.worldObj);
                machine.mMaxProgresstime = 0;
                machine.storage.clear();
                machine.secondaryMode = 0;
                machine.primaryMode = 0;
                addQueen(player.worldObj);
                machine.onStorageContentChanged(false);
                checked = true;
            }
            if (openRequested && !opened) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 12, -3, 0, 0);
                machine.onRightclick(base, player);
                opened = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        world.setWorldTime(6000);
        base = place(world, 0, 12, 0, GTNGMachineID.LargeIndustrialApiary.id);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        require(base.getMetaTileEntity() instanceof LargeIndustrialApiary, "registered controller preserves new class");
        machine = (LargeIndustrialApiary) base.getMetaTileEntity();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.construct(new ItemStack(Items.diamond), false);
        String[][] actual = structure(LargeIndustrialApiary.class);
        require(Arrays.deepEquals(actual, structure(MTEMegaIndustrialApiary.class)),
            "all structure cells match upstream");
        for (int z = 0; z < actual.length; z++) for (int y = 0; y < actual[z].length; y++) {
            for (int x = 0; x < actual[z][y].length(); x++) {
                if (actual[z][y].charAt(x) == 'A' && glass == null) glass = new int[] { x - 7, 20 - y, z };
                if (actual[z][y].charAt(x) == 'F') {
                    world.setBlockToAir(x - 7, 20 - y, z);
                    if (flower == null && y + 1 < actual[z].length && actual[z][y + 1].charAt(x) == 'B')
                        flower = new int[] { x - 7, 20 - y, z };
                }
            }
        }
        require(flower != null, "flower position exists");
        place(world, -1, 12, 0, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
        place(world, 1, 12, 0, ItemList.Hatch_Output_Bus_LV.get(1).getItemDamage());
        input = (MTEHatchInputBus) place(world, -4, 12, 1, ItemList.Hatch_Input_Bus_MV.get(1).getItemDamage())
            .getMetaTileEntity();
        place(world, -3, 12, 1, ItemList.Hatch_Maintenance.get(1).getItemDamage());
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(-16, 23, -16, -38, 15);
    }

    private void verifyStructure(World world) {
        valid("LV energy hatch forms without LuV minimum");
        long power = machine.getMaxInputEu();
        require(machine.maxSlots == power / 32 * 8, "LV-based capacity multiplied by eight");
        require(LargeIndustrialApiary.calculateCapacity(31) == 0, "below-LV capacity boundary");
        require(LargeIndustrialApiary.calculateCapacity(32) == 8, "one LV amp stores eight queens");
        require(LargeIndustrialApiary.calculateCapacity(128) == 32, "one MV amp stores thirty-two queens");
        require(LargeIndustrialApiary.calculateCapacity(32768) == 8192, "one LuV amp stores 8192 queens");
        require(LargeIndustrialApiary.calculateCapacity(Long.MAX_VALUE) == Integer.MAX_VALUE,
            "capacity cannot overflow");
        place(world, -1, 12, 0, ItemList.Hatch_Energy_UV.get(1).getItemDamage());
        valid("standard borosilicate glass accepts a UV energy hatch");
        require(
            world.getBlock(glass[0], glass[1], glass[2]) == ItemRegistry.bw_realglas
                && world.getBlockMetadata(glass[0], glass[1], glass[2]) == 0,
            "creative construction places standard borosilicate glass");
        world.setBlock(glass[0], glass[1], glass[2], Blocks.glass, 0, 3);
        require(!errors().isEmpty(), "vanilla glass is not accepted");
        world.setBlock(glass[0], glass[1], glass[2], ItemRegistry.bw_realglas, 1, 3);
        require(!errors().isEmpty(), "upgraded borosilicate glass is not accepted");
        world.setBlock(glass[0], glass[1], glass[2], ItemRegistry.bw_realglas, 0, 3);
        place(world, -1, 12, 0, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
        valid("LV hatch remains accepted after downgrade");
        world.setBlockToAir(1, 12, 0);
        require(!errors().isEmpty(), "missing required shell cell is rejected");
        place(world, 1, 12, 0, ItemList.Hatch_Output_Bus_LV.get(1).getItemDamage());
        valid("restored structure forms");
    }

    private void verifyRecipe() {
        List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(r -> Arrays.stream(r.mOutputs)
                .anyMatch(s -> GTUtility.areStacksEqual(s, GTNGItemList.LargeIndustrialApiary.get(1))))
            .collect(Collectors.toList());
        require(recipes.size() == 1, "one controller assembler recipe");
        require(recipes.get(0).mEUt > 32 && recipes.get(0).mEUt <= 128, "controller recipe is MV");
        report.append("MV controller recipe registered\n");
    }

    private void verifyProduction(World world) {
        machine.primaryMode = 2;
        machine.secondaryMode = 0;
        addQueen(world);
        require(!"flowers".equals(BeeManager.beeRoot.getMember(machine.storage.get(0).queenStack).getGenome()
            .getFlowerProvider().getFlowerType()), "fixture uses a nonstandard flower requirement");
        machine.onStorageContentChanged(false);
        require(!machine.checkProcessing().wasSuccessful(), "no flowers prevents ordinary production");
        world.setBlock(flower[0], flower[1], flower[2], Blocks.red_flower, 0, 3);
        machine.onStorageContentChanged(false);
        require(machine.checkProcessing().wasSuccessful(), "ordinary flower supports cactus-requiring queen");
        require(machine.lEUt == -32 && machine.mMaxProgresstime == 100, "one queen uses exactly LV for five seconds");
        double unboosted = outputAmount();
        require(unboosted > 0, "ordinary and specialty production generated");
        machine.dropProgress.clear();
        input.setInventorySlotContents(0, PluginApiculture.items.royalJelly.getItemStack(40));
        machine.mMaxProgresstime = 0;
        require(machine.checkProcessing().wasSuccessful(), "royal jelly boosted run starts");
        require(Math.abs(outputAmount() - unboosted * 3) < 0.0001, "forty royal jelly gives three times total yield");
        require(input.getStackInSlot(0) == null || input.getStackInSlot(0).stackSize == 0, "royal jelly consumed");
        addQueen(world);
        machine.mMaxProgresstime = 0;
        require(machine.checkProcessing().wasSuccessful() && machine.lEUt == -64, "two queens use two LV amps");
        addQueen(world);
        machine.mMaxProgresstime = 0;
        require(!machine.checkProcessing().wasSuccessful(), "storage expansion does not bypass available power");
        while (machine.storage.size() > 1) machine.storage.remove(machine.storage.size() - 1);
        machine.mMaxProgresstime = 100;
        int primary = machine.primaryMode;
        machine.setPrimaryMode(0);
        require(machine.primaryMode == primary, "running machine rejects mode changes");
        machine.mMaxProgresstime = 0;
        machine.setSecondaryMode(1);
        require(machine.secondaryMode == 0, "nonempty machine rejects secondary mode changes");
        report.append("Any-flower production, LV power, eightfold storage, jelly multiplier and mode guards passed\n");
    }

    private void verifyPersistence() {
        NBTTagCompound tag = new NBTTagCompound();
        machine.saveNBTData(tag);
        BaseMetaTileEntity restoredBase = place(base.getWorld(), 32, 12, 0, GTNGMachineID.LargeIndustrialApiary.id);
        restoredBase.disableWorking();
        LargeIndustrialApiary restored = (LargeIndustrialApiary) restoredBase.getMetaTileEntity();
        restored.loadNBTData(tag);
        require(restored.storage.size() == machine.storage.size(), "queen count persists");
        require(ItemStack.areItemStacksEqual(restored.storage.get(0).queenStack, machine.storage.get(0).queenStack),
            "complete queen genetics persist");
        require(restored.dropProgress.equals(machine.dropProgress), "fractional outputs persist");
        report.append("Queen NBT and fractional production survive reload\n");
    }

    private void verifyQueenTransfer(World world) {
        MTEHatchOutputBus output = (MTEHatchOutputBus) ((BaseMetaTileEntity) world.getTileEntity(1, 12, 0))
            .getMetaTileEntity();
        ItemStack queen = machine.storage.get(0).queenStack.copy();
        machine.primaryMode = 1;
        machine.mMaxProgresstime = 0;
        for (int slot = 0; slot < output.getSizeInventory(); slot++)
            output.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        require(!machine.checkProcessing().wasSuccessful() && machine.storage.size() == 1,
            "full output retains queen without duplication or deletion");
        output.setInventorySlotContents(0, null);
        require(machine.checkProcessing().wasSuccessful() && machine.storage.isEmpty(),
            "queen removed only after successful output");
        require(ItemStack.areItemStacksEqual(queen, output.getStackInSlot(0)), "queen output retains full genetics");
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        machine.primaryMode = 0;
        machine.mMaxProgresstime = 0;
        input.setInventorySlotContents(0, queen.copy());
        require(machine.checkProcessing().wasSuccessful() && machine.storage.size() == 1,
            "input mode accepts returned queen");
        require(machine.lEUt == 0 && machine.mMaxProgresstime == 10, "queen loading is free and takes ten ticks");
        require(input.getStackInSlot(0) == null || input.getStackInSlot(0).stackSize == 0,
            "loaded queen removed from input once");
        report.append("Input/output transfer preserves queen genetics and refuses full outputs\n");
    }

    private void verifySwarmer(World world) {
        machine.storage.clear();
        machine.mMaxProgresstime = 0;
        machine.setSecondaryMode(1);
        place(world, -1, 12, 0, ItemList.Hatch_Energy_LuV.get(1).getItemDamage());
        valid("swarmer structure remains valid");
        addQueen(world);
        machine.primaryMode = 2;
        input.setInventorySlotContents(0, PluginApiculture.items.royalJelly.getItemStack(100));
        require(machine.checkProcessing().wasSuccessful(), "native swarmer operation starts");
        require(machine.maxSlots == 1 && machine.storage.size() == 1, "swarmer keeps one queen");
        require(
            machine.mOutputItems.length == 1
                && BeeManager.beeRoot.getType(machine.mOutputItems[0]) == EnumBeeType.PRINCESS,
            "swarmer creates a princess");
        require(!BeeManager.beeRoot.getMember(machine.mOutputItems[0]).isNatural(), "swarmer princess is ignoble");
        require(machine.mMaxProgresstime < 1200 && machine.lEUt < -8190, "swarmer retains ordinary overclocking");
        report.append("Swarmer preserves jelly cost, ignoble princess and overclocking\n");
    }

    private void addQueen(World world) {
        machine.storage.add(new BeeSimulator(BeeDefinition.MODEST.getMemberStack(EnumBeeType.QUEEN), world,
            (float) machine.getVoltageTierExact()));
    }

    private double outputAmount() {
        return Arrays.stream(machine.mOutputItems).mapToInt(s -> s.stackSize).sum()
            + machine.dropProgress.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private void valid(String message) {
        List<StructureError> errors = errors();
        require(errors.isEmpty(), message + ": " + errors);
        report.append(message).append('\n');
    }

    private List<StructureError> errors() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        return errors;
    }

    private static String[][] structure(Class<?> type) throws Exception {
        Field field = type.getDeclaredField("struct");
        field.setAccessible(true);
        return (String[][]) field.get(null);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 0) {
                if (++frames < 60) return;
                capture("large-industrial-apiary-structure");
                openRequested = true;
                stage = 1;
                frames = 0;
                return;
            }
            require(++frames < 1800, "GUI timeout");
            if (!(mc.currentScreen instanceof GuiContainerWrapper) || frames < 90) return;
            require("大型工业蜂箱".equals(GTNGItemList.LargeIndustrialApiary.get(1).getDisplayName()),
                "localized controller name");
            GuiContainerWrapper gui = (GuiContainerWrapper) mc.currentScreen;
            if (stage == 1) {
                capture("large-industrial-apiary-gui");
                require(openInventory(gui.getScreen().getMainPanel()), "bee inventory toggle exists");
                stage = 2;
                frames = 0;
                return;
            }
            if (stage == 2) {
                capture("large-industrial-apiary-inventory");
                var panel = gui.getScreen().getSyncManager().getMainPSM().findPanelHandlerNullable("apiaryConfigPanel");
                require(panel != null, "mode configuration is registered");
                panel.openPanel();
                stage = 3;
                frames = 0;
                return;
            }
            capture("large-industrial-apiary-config");
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report + "MUI2 client GUI rendered\n").getBytes(StandardCharsets.UTF_8));
            System.out.println("INDUSTRIAL_APIARY_QA: PASS\n" + report);
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static boolean openInventory(IWidget parent) {
        List<IWidget> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            IWidget child = children.get(i);
            if (child instanceof ButtonWidget<?>button && button.getOverlay() == GuiTextures.GEAR) {
                require(i > 0 && children.get(i - 1) instanceof ButtonWidget<?>,
                    "inventory toggle precedes configuration");
                ((ButtonWidget<?>) children.get(i - 1)).onMousePressed(0);
                return true;
            }
            if (openInventory(child)) return true;
        }
        return false;
    }

    private void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.industrialApiary.output", "build/industrial-apiary-qa"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("INDUSTRIAL_APIARY_QA: " + message);
    }
}
