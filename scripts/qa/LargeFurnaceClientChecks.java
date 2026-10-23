package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.machines.hatch.lighthunter.SatelliteThreadModifierHatch;
import com.xyp.gtnotgood.common.machines.hatch.parallel.ParallelControlHatch;
import com.xyp.gtnotgood.common.machines.multiblock.parallelmachine.LargeFurnace;
import com.xyp.gtnotgood.common.machines.multiblock.structure.LargeFurnaceStructure;
import com.xyp.gtnotgood.loader.ParallelMachineLoader;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.render.IIconTexture;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@Mod(
    modid = "largefurnaceqa",
    name = "Large Furnace QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LargeFurnaceClientChecks {

    private LargeFurnace machine;
    private BaseMetaTileEntity base, parallelTile;
    private ParallelControlHatch parallel;
    private final List<int[]> positions = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private boolean started;
    private volatile boolean finished;
    private int ticks, serverTicks, frames, openAt, syncWaitTicks;
    private volatile int stage;
    private volatile boolean opened;
    private long simulatedTick = 1000;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.largeFurnace.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (finished) FMLCommonHandler.instance().exitJava(0, false);
        try {
            require(++ticks < 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI().clearChatMessages();
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.gameSettings.guiScale = 2;
                mc.launchIntegratedServer("furnace-qa-" + System.currentTimeMillis(), "Large Furnace QA",
                    new WorldSettings(774L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            serverTicks++;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (machine == null) {
                build(player);
                checks(player);
                stage = 1;
            }
            if (stage >= 1 && stage <= 4 && !opened) {
                if (openAt == 0) {
                    player.closeScreen();
                    if (stage == 4) player.playerNetServerHandler.setPlayerLocation(parallelTile.getXCoord() + 0.5,
                        parallelTile.getYCoord() + 1, parallelTile.getZCoord() - 3, 0, 0);
                    openAt = serverTicks + 10;
                } else if (serverTicks >= openAt) {
                    if (stage <= 3) machine.onRightclick(base, player);
                    else parallel.onRightclick(parallelTile, player);
                    opened = true;
                    openAt = 0;
                }
            }
            if (stage == 5 && parallel.getParallel() == 3) {
                player.closeScreen();
                stage = 6;
                openAt = serverTicks + 10;
                opened = false;
            }
            if (stage == 6 && !opened && serverTicks >= openAt) {
                parallel.onRightclick(parallelTile, player);
                opened = true;
                openAt = 0;
            }
            if (
                stage == 5
            ) require(++syncWaitTicks < 100,
                "server multiplier sync timeout: " + parallel.getParallel()
                    + ", container="
                    + player.openContainer.getClass().getSimpleName());
            if (stage == 7) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0.5, 99, -3, 0, 2);
                stage = 8;
            }
            if (stage == 9) {
                player.playerNetServerHandler.setPlayerLocation(17, 109, -18, 38, 20);
                stage = 10;
            }
            if (stage == 11) {
                for (int i = 0; i < ParallelMachineLoader.HATCHES.length; i++)
                    place(player.worldObj, i - 5, 100, -12, ParallelMachineLoader.HATCHES[i].get(1));
                player.playerNetServerHandler.setPlayerLocation(0, 100, -24, 0, 7);
                stage = 12;
            }
            if (stage == 14) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 99, -3, 0, 2);
                base.setActive(false);
                stage = 15;
            }
            if (stage == 16) {
                reset();
                form();
                seed();
                require(machine.checkProcessing().wasSuccessful(), "start batch for active front rendering");
                report.append("Active render batch duration: ").append(machine.mMaxProgresstime).append('\n');
                base.enableWorking();
                stage = 17;
            }
            if (stage == 17) {
                for (var energy : machine.mEnergyHatches)
                    ((BaseMetaTileEntity) energy.getBaseMetaTileEntity()).setStoredEU(energy.maxEUStore());
                if (inputCount() == 0) seed();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void build(EntityPlayerMP player) {
        World world = player.worldObj;
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.setWorldTime(6000);
        base = place(world, 0, 100, 0, GTNGItemList.LargeFurnace.get(1));
        base.setOwnerUuid(player.getUniqueID());
        base.setOwnerName(player.getCommandSenderName());
        machine = (LargeFurnace) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        String[][] shape = LargeFurnaceStructure.shape();
        int cells = 0, firebricks = 0, cokeBricks = 0;
        for (int y = 0; y < shape.length; y++)
            for (int z = 0; z < shape[y].length; z++) for (int x = 0; x < shape[y][z].length(); x++) {
                char c = shape[y][z].charAt(x);
                if (c != ' ') cells++;
                if (c == 'L') positions.add(position(x, y, z));
                if (c == 'D' || c == 'E') {
                    int[] p = position(x, y, z);
                    require(
                        world.getBlock(p[0], p[1], p[2])
                            == (c == 'D' ? GregTechAPI.sBlockCasings12 : GregTechAPI.sBlockCasings4)
                            && world.getBlockMetadata(p[0], p[1], p[2]) == (c == 'D' ? 0 : 15),
                        "native GTNH structure brick " + c);
                    if (c == 'D') cokeBricks++;
                    else firebricks++;
                }
            }
        require(cells == 716, "original 716-cell geometry");
        require(cokeBricks == 64 && firebricks == 102, "64 native coke bricks and 102 native firebricks");
        require(
            GameRegistry.findBlock(ModList.ModIds.GT_NOT_GOOD, "furnace_firebricks") == null
                && GameRegistry.findBlock(ModList.ModIds.GT_NOT_GOOD, "furnace_coke_bricks") == null,
            "no duplicate brick registrations");
        report.append("PASS native GTNH bricks: 64 Coke Oven Bricks, 102 Firebricks, no duplicate registrations.\n");
        hatch(0, ItemList.Hatch_Input_Bus_UV.get(1));
        hatch(1, ItemList.Hatch_Output_Bus_UV.get(1));
        hatch(2, ItemList.Hatch_Maintenance.get(1));
        hatch(3, ItemList.Hatch_Energy_LV.get(1));
        parallelTile = hatch(4, GTNGItemList.ParallelControlHatchIV.get(1));
        parallel = (ParallelControlHatch) parallelTile.getMetaTileEntity();
        machine.setAlwaysMaxParallel(true);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        repair();
        form();
        for (int i = 0; i < ParallelMachineLoader.HATCHES.length; i++)
            player.inventory.setInventorySlotContents(9 + i, ParallelMachineLoader.HATCHES[i].get(1));
        player.inventoryContainer.detectAndSendChanges();
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 101, -4, 0, 0);
    }

    private void checks(EntityPlayerMP player) throws Exception {
        require(machine.getMaxParallelRecipes() == 64 && machine.getThreads() == 2, "LV baseline 64 x 2");
        for (int i = 0; i < ParallelMachineLoader.HATCHES.length; i++) {
            ParallelControlHatch hatch = (ParallelControlHatch) GregTechAPI.METATILEENTITIES[ParallelMachineLoader.HATCHES[i]
                .get(1).getItemDamage()];
            require(hatch.getMaximumParallel() == (1 << (2 * (i + 1))), "hatch capacity tier " + (i + 5));
        }
        parallel.setParallel(99);
        require(parallel.getParallel() == 4 && machine.getMaxParallelRecipes() == 256,
            "IV clamp and multiplicative capacity");
        NBTTagCompound hatchTag = new NBTTagCompound();
        parallel.saveNBTData(hatchTag);
        parallel.setParallel(1);
        parallel.loadNBTData(hatchTag);
        require(parallel.getParallel() == 4, "hatch NBT");
        parallel.setParallel(1);
        hatch(5, GTNGItemList.ParallelControlHatchIV.get(1));
        require(!machine.checkStructure(true, base), "reject duplicate parallel hatches");
        restore(5);
        form();
        hatch(3, ItemList.Hatch_Energy_MV.get(1));
        form();
        require(machine.getMaxParallelRecipes() == 128 && machine.getThreads() == 4, "MV scaling");
        hatch(3, ItemList.Hatch_Energy_LV.get(1));
        form();
        SatelliteThreadModifierHatch engine = (SatelliteThreadModifierHatch) hatch(5,
            GTNGItemList.SatelliteThreadModifierHatch.get(1)).getMetaTileEntity();
        engine.setInventorySlotContents(0, GTNGItemList.AstralArray.get(2));
        form();
        require(machine.getThreads() == 258, "Omega adds 256 threads");
        restore(5);
        form();
        require(machine.getThreads() == 2, "removed Omega no stale capacity");
        reset();
        seed();
        require(machine.checkProcessing().wasSuccessful(), "native furnace batch starts");
        require(machine.getBatchRecipeCount() == 2 && machine.getBatchParallels() == 128,
            "two distinct recipes consume 128 shared parallels");
        require(inputCount() == 0, "exactly 64 of each input consumed");
        base.setActive(true);
        NBTTagCompound active = wailaData(player);
        require(active.getInteger("parallelMachineThreads") == 2 && active.getInteger("parallelMachineUsedThreads") == 2
            && active.getInteger("parallelMachineParallel") == 64, "Waila active server snapshot");
        base.setActive(false);
        require(wailaData(player).getInteger("parallelMachineUsedThreads") == 0, "inactive Waila usage is zero");
        require(
            wailaBody(player, active)
                .contains(StatCollector.translateToLocalFormatted("gtng.parallel_machine.status", 2, 2, 64)),
            "Waila body uses server snapshot independently of local state");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        form();
        repair();
        require(machine.getBatchRecipeCount() == 2, "batch NBT roundtrip");
        finishBatch();
        require(outputCount(Items.iron_ingot) == 64 && outputCount(Items.gold_ingot) == 64,
            "exact native outputs after powered processing");
        reset();
        seed();
        var bus = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Blocks.stone, 64));
        require(!machine.checkProcessing().wasSuccessful() && inputCount() == 128,
            "full output rejects without input consumption");
        reset();
        seed();
        require(machine.checkProcessing().wasSuccessful(), "second batch starts");
        for (int slot = 0; slot < bus.getSizeInventory(); slot++)
            bus.setInventorySlotContents(slot, new ItemStack(Blocks.stone, 64));
        finishBatch();
        saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        require(saved.getBoolean("parallelMachineOutputReady"), "completed output retained");
        machine.loadNBTData(saved);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        machine.onPostTick(base, ++simulatedTick);
        require(outputCount(Items.iron_ingot) == 64 && outputCount(Items.gold_ingot) == 64,
            "blocked output restores and flushes once");
        machine.onPostTick(base, ++simulatedTick);
        require(outputCount(Items.iron_ingot) == 64, "no duplicate output");
        reset();
        seed();
        require(machine.checkProcessing().wasSuccessful(), "power-loss batch starts");
        for (var energy : machine.mEnergyHatches) ((BaseMetaTileEntity) energy.getBaseMetaTileEntity()).setStoredEU(0);
        int progress = machine.mProgresstime;
        tick();
        require(machine.mProgresstime == progress && inputCount() == 0, "power loss does not advance or consume again");
        finishBatch();
        require(outputCount(Items.iron_ingot) == 64, "resumes after power restored");
        reset();
        base.disableWorking();
        report.append(
            "PASS native geometry, tier scaling, 10 hatch tiers, clamp/NBT, duplicate rejection, Omega, two-recipe batch, input/output safety, powered processing, NBT and power-loss resume.\n");
        report.append("PASS Waila active/inactive thread counts, capacity and server snapshot.\n");
    }

    private NBTTagCompound wailaData(EntityPlayerMP player) {
        NBTTagCompound tag = new NBTTagCompound();
        machine.getWailaNBTData(player, base, tag, player.worldObj, base.xCoord, base.yCoord, base.zCoord);
        return tag;
    }

    private List<String> wailaBody(EntityPlayerMP player, NBTTagCompound tag) {
        IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
            IWailaDataAccessor.class.getClassLoader(), new Class<?>[] { IWailaDataAccessor.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getNBTData" -> tag;
            case "getTileEntity" -> base;
            case "getPlayer" -> player;
            case "getWorld" -> player.worldObj;
            default -> null;
            });
        IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
            IWailaConfigHandler.class.getClassLoader(), new Class<?>[] { IWailaConfigHandler.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getConfig" -> args.length > 1 ? args[1] : true;
            case "getModuleNames" -> Collections.emptySet();
            case "getConfigKeys" -> new HashMap<>();
            default -> null;
            });
        List<String> lines = new ArrayList<>();
        machine.getWailaBody(null, lines, accessor, config);
        return lines;
    }

    private void reset() {
        machine.stopMachine(ShutDownReasonRegistry.NONE);
        base.disableWorking();
        for (var bus : machine.mInputBusses)
            for (int i = 0; i < bus.getSizeInventory(); i++) bus.setInventorySlotContents(i, null);
        for (var bus : machine.mOutputBusses)
            for (int i = 0; i < bus.getSizeInventory(); i++) bus.setInventorySlotContents(i, null);
        machine.mProgresstime = machine.mMaxProgresstime = 0;
        repair();
    }

    private void seed() {
        machine.mInputBusses.get(0).setInventorySlotContents(0, new ItemStack(Blocks.iron_ore, 64));
        machine.mInputBusses.get(0).setInventorySlotContents(1, new ItemStack(Blocks.gold_ore, 64));
    }

    private int inputCount() {
        int n = 0;
        for (var bus : machine.mInputBusses) for (int i = 0; i < bus.getSizeInventory(); i++) {
            ItemStack stack = bus.getStackInSlot(i);
            if (stack != null) n += stack.stackSize;
        }
        return n;
    }

    private int outputCount(net.minecraft.item.Item item) {
        int n = 0;
        for (var bus : machine.mOutputBusses) for (int i = 0; i < bus.getSizeInventory(); i++) {
            ItemStack stack = bus.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) n += stack.stackSize;
        }
        return n;
    }

    private void finishBatch() throws Exception {
        int duration = machine.mMaxProgresstime;
        require(duration > 0 && duration < 10000, "bounded duration " + duration);
        for (int i = 0; i <= duration; i++) {
            for (var e : machine.mEnergyHatches)
                ((BaseMetaTileEntity) e.getBaseMetaTileEntity()).setStoredEU(e.maxEUStore());
            tick();
        }
    }

    private void tick() throws Exception {
        Method method = MTEMultiBlockBase.class.getDeclaredMethod("runMachine",
            gregtech.api.interfaces.tileentity.IGregTechTileEntity.class, long.class);
        method.setAccessible(true);
        method.invoke(machine, base, ++simulatedTick);
    }

    private void repair() {
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
    }

    private void form() {
        machine.clearHatches();
        require(machine.checkStructure(true, base), "original structure forms");
    }

    private BaseMetaTileEntity hatch(int index, ItemStack stack) {
        int[] p = positions.get(index);
        return place(base.getWorld(), p[0], p[1], p[2], stack);
    }

    private void restore(int index) {
        int[] p = positions.get(index);
        base.getWorld().setBlock(p[0], p[1], p[2], GregTechAPI.sBlockCasings2, 0, 3);
    }

    private int[] position(int x, int y, int z) {
        int[] offset = new int[3];
        machine.getExtendedFacing().getWorldOffset(new int[] { x - 5, y - 11, z }, offset);
        return new int[] { base.getXCoord() + offset[0], base.getYCoord() + offset[1], base.getZCoord() + offset[2] };
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, ItemStack stack) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !opened) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 8 && ++frames > 120) {
                require(mc.currentScreen == null && mc.objectMouseOver != null
                    && mc.objectMouseOver.blockX == 0
                    && mc.objectMouseOver.blockY == 100
                    && mc.objectMouseOver.blockZ == 0, "real Waila controller hover");
                capture("waila");
                frames = 0;
                stage = 9;
                return;
            }
            if (stage == 10 && ++frames > 90) {
                capture("structure");
                frames = 0;
                stage = 11;
                return;
            }
            if (stage == 12 && ++frames > 90) {
                checkHatchTextures();
                capture("parallel-hatches-world");
                mc.refreshResources();
                frames = 0;
                stage = 13;
                return;
            }
            if (stage == 13 && ++frames > 90) {
                checkHatchTextures();
                capture("parallel-hatches-reloaded");
                report.append(
                    "PASS all 10 hatch tiers: real animated atlas sprites, 4 frames, inventory/world rendering and resource reload.\n");
                frames = 0;
                stage = 14;
                return;
            }
            if (stage == 15 && ++frames > 90) {
                capture("furnace-front-inactive");
                frames = 0;
                stage = 16;
                return;
            }
            if (stage == 17) {
                require(++frames < 360,
                    "active front sync timeout: server=" + base.isActive()
                        + ", progress="
                        + machine.mProgresstime
                        + "/"
                        + machine.mMaxProgresstime
                        + ", formed="
                        + machine.mMachine
                        + ", allowed="
                        + base.isAllowedToWork()
                        + ", reason="
                        + base.getLastShutDownReason().getID());
                if (frames < 10 || !((BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 100, 0)).isActive()) return;
                capture("furnace-front-active");
                checkFurnaceTextures();
                require(RecipeCatalysts.containsCatalyst(new FurnaceRecipeHandler(), GTNGItemList.LargeFurnace.get(1)),
                    "Large Furnace registered for native NEI smelting");
                require(
                    RecipeCatalysts.getRecipeCatalysts("smelting").stream()
                        .filter(stack -> stack.contains(GTNGItemList.LargeFurnace.get(1))).count() == 1,
                    "exactly one Large Furnace catalyst");
                require(!RecipeCatalysts.containsCatalyst("fuel", GTNGItemList.LargeFurnace.get(1)),
                    "electric furnace not registered as fuel burner");
                require(GuiCraftingRecipe.openRecipeGui("smelting"), "open native smelting page");
                frames = 0;
                stage = 18;
                return;
            }
            if (stage == 18 && ++frames > 90) {
                require(mc.currentScreen instanceof GuiRecipe<?>, "native smelting GUI open");
                GuiRecipe<?> gui = (GuiRecipe<?>) mc.currentScreen;
                require(gui.getHandler() instanceof FurnaceRecipeHandler, "native furnace recipe handler");
                var widget = gui.getRecipeCatalystWidget();
                require(widget.isShowWidget(), "NEI left machine list visible");
                boolean found = false;
                for (int y = widget.y; y < widget.y + widget.h; y++) {
                    for (int x = widget.x; x < widget.x + widget.w; x++) {
                        var stack = widget.getPositionedStackMouseOver(x, y);
                        if (stack != null && stack.contains(GTNGItemList.LargeFurnace.get(1))) found = true;
                    }
                }
                require(found, "Large Furnace actually visible in NEI left machine list");
                capture("nei-smelting-catalyst");
                require(GuiUsageRecipe.openRecipeGui("item", GTNGItemList.LargeFurnace.get(1)),
                    "open Large Furnace usages");
                frames = 0;
                stage = 19;
                return;
            }
            if (stage == 19 && ++frames > 90) {
                require(mc.currentScreen instanceof GuiUsageRecipe, "machine usage GUI open");
                GuiUsageRecipe gui = (GuiUsageRecipe) mc.currentScreen;
                require(
                    gui.currenthandlers.stream()
                        .anyMatch(handler -> handler instanceof FurnaceRecipeHandler && handler.numRecipes() > 0),
                    "machine U lookup finds smelting recipes");
                capture("nei-furnace-usages");
                report.append(
                    "PASS transparent frame revealing native casing in both atlas sprites; native NEI smelting catalyst visible and machine usage lookup works.\n");
                finished = true;
                Files.write(new File(output(), "result.txt").toPath(),
                    ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                mc.shutdown();
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 45) return;
            if (stage <= 3) {
                checkHatchTextures();
                require(wrapper.getScreen().getSyncManager().getMainPSM().findSyncHandlerNullable("furnaceThreads", 0)
                    == null, "no thread status on main GUI");
                capture("controller-scale-" + mc.gameSettings.guiScale);
                frames = 0;
                opened = false;
                stage++;
                mc.gameSettings.guiScale = stage == 2 ? 1 : stage == 3 ? 3 : 2;
            } else if (stage == 4) {
                IntSyncValue value = wrapper.getScreen().getSyncManager().getMainPSM()
                    .findSyncHandler("parallel_multiplier", IntSyncValue.class);
                TextFieldWidget field = (TextFieldWidget) named(wrapper.getScreen().getMainPanel(),
                    "parallelMultiplier");
                require(field != null, "parallel field exists");
                field.setText("3");
                field.onRemoveFocus(field.getContext());
                require(value.getIntValue() == 3,
                    "client field edit sets multiplier to 3, actual " + value.getIntValue());
                capture("parallel-hatch");
                stage = 5;
                frames = 0;
            } else if (stage == 6) {
                IntSyncValue value = wrapper.getScreen().getSyncManager().getMainPSM()
                    .findSyncHandler("parallel_multiplier", IntSyncValue.class);
                require(value.getIntValue() == 3, "GUI server sync and reopen");
                capture("parallel-hatch-reopen");
                report.append("PASS GUI scales 1/2/3, parallel setting server sync and reopen.\n");
                stage = 7;
                frames = 0;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checkHatchTextures() {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < ParallelMachineLoader.HATCHES.length; i++) {
            ParallelControlHatch hatch = (ParallelControlHatch) GregTechAPI.METATILEENTITIES[ParallelMachineLoader.HATCHES[i]
                .get(1).getItemDamage()];
            TextureAtlasSprite sprite = (TextureAtlasSprite) ((IIconTexture) hatch.getTexturesInactive(null)[1])
                .getIcon(2, null);
            String name = ModList.GTNotGood.getResourceLocation() + ":parallel/hatch_" + Math.min(4, i + 1);
            require(sprite != null && sprite.getIconName().equals(name)
                && mc.getTextureMapBlocks().getAtlasSprite(name) == sprite, "real hatch atlas sprite tier " + i);
            require(sprite.hasAnimationMetadata() && sprite.getFrameCount() == 4
                && sprite.getIconWidth() == 16
                && sprite.getIconHeight() == 16, "four animated hatch frames tier " + i);
        }
    }

    private void checkFurnaceTextures() throws Exception {
        for (String state : new String[] { "front", "active" }) {
            String name = ModList.GTNotGood.getResourceLocation() + ":parallel/furnace_" + state;
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
            require(sprite.getIconName().equals(name) && sprite.getIconWidth() == 16 && sprite.getIconHeight() == 16,
                "original furnace sprite and dimensions: " + state);
            BufferedImage image;
            try (InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation(ModList.GTNotGood.getResourceLocation(),
                    "textures/blocks/parallel/furnace_" + state + ".png"))
                .getInputStream()) {
                image = ImageIO.read(stream);
            }
            int transparent = 0;
            for (int pixel : image.getRGB(0, 0, 16, 16, null, 0, 16)) if ((pixel >>> 24) == 0) transparent++;
            require(transparent == 122, "original 60 transparent pixels plus 62 exposed casing pixels: " + state);
        }
    }

    private void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static IWidget named(IWidget root, String name) {
        if (name.equals(root.getName())) return root;
        for (IWidget child : root.getChildren()) {
            IWidget found = named(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static File output() {
        File dir = new File(System.getProperty("gtng.largeFurnace.output"));
        dir.mkdirs();
        return dir;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
