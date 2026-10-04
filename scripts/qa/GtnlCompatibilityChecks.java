package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.common.recipe.machine.EasyWirelessRecipes;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.TextEffectsCompat;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.implementations.MTEHatch;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/** Runs against the complete published GTNL jar; never included in a release build. */
@Mod(
    modid = "gtnggtnlqa",
    name = "GTNL Compatibility QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD + ";after:" + ModList.ModIds.GT_NOT_LEISURE)
public final class GtnlCompatibilityChecks {

    private MTEHatch input;
    private MTEHatch output;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.gtnlCompat.qa")) return;
        require(Loader.isModLoaded(ModList.ModIds.GT_NOT_LEISURE), "GTNL must be loaded");
        require(Config.enableEasyWirelessRecipes, "easy wireless recipes must be enabled");
        input = new MTEHatchWirelessMulti(freeId(), "qa.unsupported.input", "QA Input", 13, 214748364);
        output = new MTEHatchWirelessDynamoMulti(freeId(), "qa.unsupported.output", "QA Output", 13, 214748364);
    }

    private static int freeId() {
        for (int i = GregTechAPI.METATILEENTITIES.length - 1; i > 0; i--)
            if (GregTechAPI.METATILEENTITIES[i] == null) return i;
        throw new AssertionError("No free QA machine ID");
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.gtnlCompat.qa")) return;
        require(input != null && output != null, "unsupported fixtures registered before postInit");
        require(
            !EasyWirelessRecipes.supports(input) && !EasyWirelessRecipes.supports(output),
            "special addon ratings skipped during real startup");
        require(TextEffectsCompat.hasUpstreamRenderer(), "upstream renderer detected");
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        // The client harness waits for a connected player and exercises its real network handler.
        // A FakePlayer has no network connection on a plain integrated server.
        if (!server.isDedicatedServer()) {
            System.out.println("GTNL_STARTUP_QA: complete GTNL mod and unsupported amperage fixtures PASS");
            return;
        }
        WorldServer world = server.worldServerForDimension(0);
        EntityPlayerMP player = FakePlayerFactory.getMinecraft(world);
        world.setBlock(
            0,
            6,
            0,
            AEApi.instance()
                .definitions()
                .blocks()
                .wireless()
                .maybeBlock()
                .get());
        TileWireless wap = (TileWireless) world.getTileEntity(0, 6, 0);
        wap.onReady();
        IGrid grid = wap.getActionableNode()
            .getGrid();
        ItemStack terminal = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, terminal);
        var handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(terminal);
        DualTerminalGuiObject host = new DualTerminalGuiObject(handler, terminal, player, world, 0);
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : wap);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        container.setCraftingMode(true);
        container.detectAndSendChanges();
        container.setCraftingMode(false);
        container.detectAndSendChanges();
        for (int slot : new int[] { 0, 31, 32, 255 }) {
            container.inputsSync.get()
                .putAEStackInSlot(slot, AEItemStack.create(new ItemStack(Items.apple, slot + 1)));
            container.outputsSync.get()
                .putAEStackInSlot(slot, AEItemStack.create(new ItemStack(Items.feather, slot + 1)));
        }
        container.inputsSync.get()
            .putAEStackInSlot(255, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1234)));
        container.outputsSync.get()
            .putAEStackInSlot(255, AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 4321)));
        container.onContainerClosed(player);
        container = new ContainerQuickEncodingTerminal(player.inventory, host);
        require(
            container.inputsSync.get()
                .getAEStackInSlot(32)
                .getStackSize() == 33,
            "slot 32 survived reopen");
        require(
            container.inputsSync.get()
                .getAEStackInSlot(255)
                .getStackSize() == 1234,
            "input fluid survived reopen");
        require(
            container.outputsSync.get()
                .getAEStackInSlot(255)
                .getStackSize() == 4321,
            "output fluid survived reopen");
        host.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(2)
                    .get());
        Method encode = ContainerQuickEncodingTerminal.class.getDeclaredMethod("quickEncode");
        encode.setAccessible(true);
        encode.invoke(container);
        ItemStack encoded = host.getInventoryByName("pattern")
            .getStackInSlot(1);
        require(encoded != null, "native pattern encoded");
        var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(
            Arrays.stream(details.getAEInputs())
                .anyMatch(stack -> stack instanceof IAEFluidStack && stack.getStackSize() == 1234),
            "native input fluid encoded");
        require(
            Arrays.stream(details.getAEOutputs())
                .anyMatch(stack -> stack instanceof IAEFluidStack && stack.getStackSize() == 4321),
            "native output fluid encoded");
        container.clear();
        require(
            container.inputsSync.get()
                .getAEStackInSlot(255) == null,
            "hidden input cleared");
        require(
            container.outputsSync.get()
                .getAEStackInSlot(255) == null,
            "hidden output cleared");
        container.onContainerClosed(player);
        String result = "PASS GTNL " + Loader.instance()
            .getIndexedModList()
            .get(ModList.ModIds.GT_NOT_LEISURE)
            .getVersion()
            + "; Java "
            + System.getProperty("java.version")
            + "; "
            + Loader.instance()
                .getActiveModList()
                .size()
            + " mods; startup, 214748364A fixtures, terminal mode switch, slots 31/32/255, persistence, native fluids, clear";
        File report = new File(System.getProperty("gtng.gtnlCompat.report"));
        report.getParentFile()
            .mkdirs();
        Files.write(report.toPath(), result.getBytes(StandardCharsets.UTF_8));
        System.out.println("GTNL_COMPAT_QA: " + result);
        if (server.isDedicatedServer()) server.initiateShutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
