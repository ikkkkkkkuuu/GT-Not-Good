package com.xyp.gtnotgood.common.items.patternsorter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import appeng.api.AEApi;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;

/** Real AE decoding, GT matching, inventory conservation and server-synchronized LDLib action checks. */
@Mod(
    modid = "patternsorterqa",
    name = "Pattern Sorter QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PatternSorterClientChecks {

    private boolean started, setup;
    private int ticks, frames, stage, serverTicks;
    private volatile int request, verified;
    private ItemStack tool, unrelated;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.sorter.qa")) {
            com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "sorter-qa-" + System.currentTimeMillis(),
                "Pattern Sorter QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (++ticks > 6000) throw new AssertionError("Pattern sorter QA timed out");
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (!setup) {
            setup = true;
            var map = RecipeMaps.benderRecipes;
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.apple), GTUtility.getIntegratedCircuit(24))
                .itemOutputs(new ItemStack(Items.stick))
                .duration(20)
                .eut(8)
                .addTo(map);
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.carrot), GTUtility.getIntegratedCircuit(2))
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 1000))
                .itemOutputs(new ItemStack(Items.feather))
                .duration(20)
                .eut(8)
                .addTo(map);
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.potato), ItemList.Shape_Extruder_Plate.get(0))
                .itemOutputs(new ItemStack(Items.brick))
                .duration(20)
                .eut(8)
                .addTo(map);
            for (int circuit : new int[] { 1, 2 }) {
                GTValues.RA.stdBuilder()
                    .itemInputs(new ItemStack(Items.apple), GTUtility.getIntegratedCircuit(circuit))
                    .itemOutputs(new ItemStack(Items.bone))
                    .duration(20)
                    .eut(8)
                    .addTo(map);
            }
            for (int i = 0; i < 36; i++) player.inventory.mainInventory[i] = null;
            tool = GTNGItemList.PatternSorter.get(1);
            tool.setTagCompound(new NBTTagCompound());
            tool.getTagCompound()
                .setString("PatternSorterRecipeMap", map.unlocalizedName);
            player.inventory.mainInventory[0] = tool;
            player.inventory.currentItem = 0;
            player.inventory.mainInventory[9] = encode(Items.apple, Items.stick, false);
            player.inventory.mainInventory[10] = encode(Items.carrot, Items.feather, true);
            player.inventory.mainInventory[11] = encode(Items.potato, Items.brick, false);
            player.inventory.mainInventory[12] = encode(Items.apple, Items.bone, false);
            player.inventory.mainInventory[13] = encode(Items.wheat, Items.stick, false);
            unrelated = new ItemStack(Items.diamond, 13);
            player.inventory.mainInventory[14] = unrelated;
            List<PatternSorter.Entry> entries = PatternSorter
                .classify(player.inventory.mainInventory, player.worldObj, map);
            require(entries.size() == 5, "all patterns found");
            require(entries.get(0).circuit == 24, "dry circuit");
            require(
                entries.get(1).circuit == 2 && entries.get(1).status == PatternSorter.MATCHED,
                "native fluid circuit");
            require(entries.get(2).mold >= 0 && entries.get(2).circuit == -1, "virtual mold");
            require(entries.get(3).status == PatternSorter.AMBIGUOUS, "ambiguity preserved");
            require(entries.get(4).status == PatternSorter.UNMATCHED, "wrong material not accepted by quantity alone");
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
            System.out.println("PATTERN_SORTER_QA classification PASS");
        }
        // The fixture grants the tool; wait for that inventory update before asking the client factory to use it.
        if (++serverTicks == 60) tool.getItem()
            .onItemRightClick(tool, player.worldObj, player);
        if (request > verified) {
            List<PatternSorter.Entry> entries = PatternSorter
                .classify(player.inventory.mainInventory, player.worldObj, RecipeMaps.benderRecipes);
            if (request == 1 && entries.get(0).mold >= 0 && entries.get(1).circuit == 2 && entries.get(2).circuit == 24)
                verified = 1;
            if (request == 2 && entries.get(0).circuit == 24) verified = 2;
            require(
                player.inventory.mainInventory[0] == tool && player.inventory.mainInventory[14] == unrelated,
                "tool and unrelated item unchanged");
            require(entries.size() == 5, "inventory conservation");
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)
            || !(wrapper.getScreen() instanceof LDLibModularScreen screen)) return;
        if (++frames % 80 != 0 || request > verified) return;
        PatternSorterGui model = ((PatternSorterGui.Panel) screen.getMainPanel()).model;
        if (model.entries.getValue() == null || model.entries.getValue()
            .size() != 5) return;
        if (stage == 0) {
            capture("before");
            click(screen, 138, 287);
            require(mc.thePlayer.inventory.getItemStack() == null, "tool slot cannot be extracted");
            click(screen, 80, 182);
            request = 1;
        } else if (stage == 1) {
            capture("sorted");
            // Selection remains on the original circuit-24 group even after list order changes.
            click(screen, 280, 182);
            request = 2;
        } else if (stage == 2) {
            capture("selected-first");
            click(screen, 240, 35);
        } else if (stage == 3) {
            capture("machine-search");
            File output = new File(System.getProperty("gtng.sorter.qa.output"));
            Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("PATTERN_SORTER_QA_PASS native fluids, ambiguity, materials, sorting, server sync, GUI");
            mc.shutdown();
        }
        stage++;
    }

    /** Uses Ultimate Encoded Pattern native fluid NBT, matching the installed AE2 version. */
    private static ItemStack encode(Item input, Item output, boolean fluid) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound in = new NBTTagCompound(), out = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(new ItemStack(input)), in);
        Platform.writeStackNBT(AEItemStack.create(new ItemStack(output)), out);
        inputs.appendTag(in);
        if (fluid) {
            NBTTagCompound water = new NBTTagCompound();
            Platform.writeStackNBT(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)), water);
            inputs.appendTag(water);
        }
        for (int i = inputs.tagCount(); i < 16; i++) inputs.appendTag(new NBTTagCompound());
        outputs.appendTag(out);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static void click(LDLibModularScreen screen, int x, int y) {
        var area = screen.getMainPanel()
            .getArea();
        screen.getContext()
            .updateState(area.x + x, area.y + y, 0);
        screen.onMousePressed(0);
        screen.onMouseRelease(0);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.sorter.qa.output"));
        output.mkdirs();
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("PATTERN_SORTER_QA: " + message);
    }
}
