package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTOreDictUnificator;

/** Reproduces a wildcard casting loop competing with a stocked native chemical-bath pattern. */
@Mod(
    modid = "patternplanqa",
    name = "Pattern planning QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PatternPlanChecks {

    private boolean started, finished;
    private int ticks, stage;
    private SuperMTEHatchCraftingInputME hatch;
    private Future<ICraftingJob> job;
    private IAEStack<?> fiber, plate, resin, reinforced;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "pattern-plan-" + System.currentTimeMillis(),
                "Pattern planning QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        World world = player.worldObj;
        try {
            if (++ticks == 1) setup(world, player);
            if (ticks < 100) return;
            if (ticks > 1600) throw new AssertionError("timeout stage " + stage);
            if (stage == 0) {
                if (!hatch.isActive()) return;
                var source = new MachineSource((IActionHost) hatch.getBaseMetaTileEntity());
                require(
                    hatch.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .injectItems(
                            (IAEItemStack) fiber.copy()
                                .setStackSize(280),
                            Actionable.MODULATE,
                            source)
                        == null,
                    "fiber stored");
                require(
                    hatch.getProxy()
                        .getStorage()
                        .getFluidInventory()
                        .injectItems(
                            (IAEFluidStack) resin.copy()
                                .setStackSize(111456),
                            Actionable.MODULATE,
                            source)
                        == null,
                    "resin stored");
                hatch.setInventorySlotContents(0, pattern(new IAEStack<?>[] { fiber, resin }, plate));
                stage = 1;
                return;
            }
            if (stage % 2 == 1) {
                if (ticks % 40 != 0) return;
                long amount = stage == 5 ? 280 : 1;
                job = hatch.getProxy()
                    .getCrafting()
                    .beginCraftingJob(
                        world,
                        hatch.getProxy()
                            .getGrid(),
                        new MachineSource((IActionHost) hatch.getBaseMetaTileEntity()),
                        plate.copy()
                            .setStackSize(amount),
                        null);
                stage++;
            } else if (job.isDone()) {
                var result = job.get();
                var plan = AEApi.instance()
                    .storage()
                    .createAEStackList();
                result.populatePlan(plan);
                System.out.println("PATTERN_PLAN_QA stage=" + stage + " simulation=" + result.isSimulation());
                for (var s : plan) System.out.println(
                    "PATTERN_PLAN_QA " + s.getDisplayName()
                        + " stored/missing="
                        + s.getStackSize()
                        + " craft="
                        + s.getCountRequestable());
                require(result.isSimulation() == (stage == 8), "stage " + stage + " simulation status");
                if (stage == 2) {
                    hatch.setInventorySlotContents(1, pattern(new IAEStack<?>[] { plate }, reinforced));
                    hatch.setInventorySlotContents(2, wildcard());
                    stage = 3;
                } else if (stage == 4) {
                    require(
                        ((CraftingGridCache) hatch.getProxy()
                            .getCrafting()).getCraftingMultiPatterns()
                                .get(plate)
                                .size()
                            == 2,
                        "equal-priority alternatives both retained");
                    hatch.gridChanged();
                    stage = 5;
                } else if (stage == 6) {
                    hatch.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .extractItems(
                            (IAEItemStack) fiber.copy()
                                .setStackSize(280),
                            Actionable.MODULATE,
                            new MachineSource((IActionHost) hatch.getBaseMetaTileEntity()));
                    stage = 7;
                } else {
                    finish("PASS");
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            finish("FAIL " + t);
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        var defs = AEApi.instance()
            .definitions();
        world.setBlock(0, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) world.getTileEntity(0, 10, 0);
        tile.setInitialValuesAsNBT(
            null,
            (short) GTNGItemList.SuperMTEHatchCraftingInputME.get(1)
                .getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        hatch = (SuperMTEHatchCraftingInputME) tile.getMetaTileEntity();
        hatch.setConnectsToAllSides(true);
        world.setBlock(
            1,
            10,
            0,
            defs.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            0,
            10,
            1,
            defs.blocks()
                .drive()
                .maybeBlock()
                .get());
        TileDrive drive = (TileDrive) world.getTileEntity(0, 10, 1);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                defs.items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        fiber = AEItemStack.create(ItemList.Circuit_Parts_GlassFiber.get(1));
        plate = AEItemStack.create(GTOreDictUnificator.get(OrePrefixes.plate, Materials.EpoxidFiberReinforced, 1));
        resin = AEFluidStack.create(Materials.Epoxid.getMolten(144));
        reinforced = AEFluidStack.create(Materials.EpoxidFiberReinforced.getMolten(144));
    }

    private static ItemStack wildcard() {
        ItemStack result = GTNGItemList.WildcardPattern.get(1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("WildcardPattern", true);
        tag.setInteger("WPModelVersion", 2);
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setInteger("Amount", 144);
        fluid.setString("State", "MOLTEN");
        tag.setTag("WPInputComponents", component("fluid", fluid));
        NBTTagCompound out = new NBTTagCompound();
        out.setInteger("Amount", 1);
        out.setString("Prefix", "plate");
        tag.setTag("WPOutputComponents", component("prefix", out));
        NBTTagCompound filter = new NBTTagCompound();
        filter.setString("Example", "Polytetrafluoroethylene");
        filter.setString("Property", "POLYMER");
        filter.setBoolean("Whitelist", true);
        tag.setTag("WPFilterComponents", component("property", filter));
        result.setTagCompound(tag);
        return result;
    }

    private static NBTTagList component(String type, NBTTagCompound data) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("type", type);
        entry.setTag("data", data);
        NBTTagList result = new NBTTagList();
        result.appendTag(entry);
        return result;
    }

    private static ItemStack pattern(IAEStack<?>[] inputs, IAEStack<?> output) {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (var stack : inputs) {
            NBTTagCompound entry = new NBTTagCompound();
            stack.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagCompound entry = new NBTTagCompound();
        output.writeToNBTGeneric(entry);
        out.appendTag(entry);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        result.setTagCompound(tag);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PATTERN_PLAN_QA " + message);
    }

    private void finish(String result) {
        finished = true;
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }
}
