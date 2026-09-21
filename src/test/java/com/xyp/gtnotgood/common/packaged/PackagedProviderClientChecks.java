package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.lang.reflect.Proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

/** Opt-in real-client checks for selection, transactions, persistence and original-texture GUI rendering. */
@Mod(
    modid = "packagedqa",
    name = "Packaged Provider QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class PackagedProviderClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile boolean failed;
    private int tick;
    private int frames;
    private TilePackagedProvider provider;
    private ICraftingPatternDetails details;
    private InventoryCrafting input;
    private String saveName;
    private volatile boolean reloadRequested;
    private volatile boolean reloadFinished;
    private boolean reloadChecked;
    private NBTTagCompound inFlight;
    private boolean awaitingStorage;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.packaged.qa")) {
            saveName = System.getProperty("gtng.packaged.qa.resume", "");
            if (!saveName.isEmpty()) {
                if (!saveName.matches("packaged-qa-[0-9]+")) throw new IllegalArgumentException("Not a QA save");
                tick = 100;
                reloadFinished = true;
            }
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (reloadRequested) {
            reloadRequested = false;
            try {
                java.nio.file.Files.write(
                    new File("packaged-qa-resume.txt").toPath(),
                    saveName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                java.nio.file.Files.write(
                    new File("packaged-qa-result.txt").toPath(),
                    "SAVED".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException error) {
                throw new RuntimeException(error);
            }
            mc.shutdown();
            return;
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (mc.theWorld != null && !finished && mc.currentScreen instanceof net.minecraft.client.gui.GuiIngameMenu) {
            mc.displayGuiScreen(null);
        }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            if (!reloadFinished) saveName = "packaged-qa-" + System.currentTimeMillis();
            mc.launchIntegratedServer(
                saveName,
                "Packaged Provider QA",
                reloadFinished ? null
                    : new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        try {
            tick++;
            if (tick == 1) {
                world.setBlock(
                    0,
                    8,
                    0,
                    net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
                provider = (TilePackagedProvider) world.getTileEntity(0, 8, 0);
                provider.setOwnerName(player.getCommandSenderName());
                world.setBlock(
                    0,
                    8,
                    1,
                    AEApi.instance()
                        .definitions()
                        .blocks()
                        .energyCellCreative()
                        .maybeBlock()
                        .get());
                world.setBlock(4, 10, 0, ConfigBlocks.blockStoneDevice, 2, 3);
                world.setBlock(4, 8, 0, ConfigBlocks.blockStoneDevice, 1, 3);
                for (int x : new int[] { -1, 1 }) for (int z : new int[] { -1, 1 }) {
                    world.setBlock(4 + x, 8, z, ConfigBlocks.blockStoneDevice, 3, 3);
                }
                world.setBlock(7, 8, 0, ConfigBlocks.blockStoneDevice, 1, 3);
                world.setBlock(1, 8, 0, ConfigBlocks.blockStoneDevice, 1, 3);
                player.playerNetServerHandler.setPlayerLocation(0.5, 9, -3.5, 0, 15);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.inventory
                    .setInventorySlotContents(player.inventory.currentItem, GTNGItemList.ItemWirelessConnector.get(1));
                player.setSneaking(false);
                ItemWirelessConnector.use(player, 0, 8, 0, 1);
                require(ItemWirelessConnector.selection(player.getHeldItem()) != null, "normal right click selects");
                ItemWirelessConnector.use(player, 4, 10, 0, 1);
                require(provider.targets.isEmpty(), "normal target click does not bind");
                player.setSneaking(true);
                ItemWirelessConnector.use(player, 4, 10, 0, 1);
                require(provider.targets.size() == 1, "bind target");
                ItemWirelessConnector.use(player, 4, 10, 0, 2);
                require(provider.targets.size() == 1, "duplicate block rejected across faces");
                player.setSneaking(false);
                provider
                    .setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.ThaumcraftInfusionCore.get(1));
                provider.autoReturn = true;
                ItemStack pattern = AEApi.instance()
                    .definitions()
                    .items()
                    .encodedPattern()
                    .maybeStack(1)
                    .get();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setBoolean("crafting", false);
                NBTTagList ins = new NBTTagList();
                ins.appendTag(new ItemStack(Items.ender_pearl).writeToNBT(new NBTTagCompound()));
                ins.appendTag(new ItemStack(Items.nether_star).writeToNBT(new NBTTagCompound()));
                NBTTagList outs = new NBTTagList();
                outs.appendTag(new ItemStack(Items.diamond).writeToNBT(new NBTTagCompound()));
                tag.setTag("in", ins);
                tag.setTag("out", outs);
                pattern.setTagCompound(tag);
                provider.setInventorySlotContents(0, pattern);
                for (var expected : new ItemStack[] { GTNGItemList.BasicPackagedCore.get(1),
                    GTNGItemList.WirelessPackagedPatternProvider.get(1), GTNGItemList.ItemWirelessConnector.get(1),
                    GTNGItemList.ThaumcraftInfusionCore.get(1) }) {
                    require(
                        net.minecraft.item.crafting.CraftingManager.getInstance()
                            .getRecipeList()
                            .stream()
                            .anyMatch(recipe -> TilePackagedProvider.sameItem(recipe.getRecipeOutput(), expected)),
                        "survival recipe " + expected.getUnlocalizedName());
                }
                ThaumcraftApi.addInfusionCraftingRecipe(
                    "",
                    new ItemStack(Items.diamond),
                    0,
                    new AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 8),
                    new ItemStack(Items.ender_pearl),
                    new ItemStack[] { new ItemStack(Items.nether_star) });
                input = new InventoryCrafting(new Container() {

                    @Override
                    public boolean canInteractWith(EntityPlayer p) {
                        return true;
                    }
                }, 3, 3);
                input.setInventorySlotContents(0, new ItemStack(Items.ender_pearl));
                input.setInventorySlotContents(1, new ItemStack(Items.nether_star));
                ThaumcraftApi.addInfusionCraftingRecipe(
                    "GTNG_QA_UNRESEARCHED",
                    new ItemStack(Items.diamond),
                    0,
                    new AspectList(),
                    new ItemStack(Items.emerald),
                    new ItemStack[] { new ItemStack(Items.nether_star) });
            }
            if (tick == 80) {
                require(
                    provider.getProxy()
                        .isActive(),
                    "powered ME provider/channel");
                provider.provideCrafting(
                    (ICraftingProviderHelper) Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] { ICraftingProviderHelper.class },
                        (proxy, method, args) -> {
                            if (method.getName()
                                .equals("addCraftingOption")) details = (ICraftingPatternDetails) args[1];
                            return null;
                        }));
                require(details != null, "AE processing pattern advertised");
                var matrix = (TileInfusionMatrix) world.getTileEntity(4, 10, 0);
                var center = (TilePedestal) world.getTileEntity(4, 8, 0);
                var adapter = PackagedCoreRegistry.get(provider.getStackInSlot(TilePackagedProvider.CORE));
                require(
                    adapter.dispatch(provider, provider.targets.get(0), details, input) == null
                        && center.getStackInSlot(0) == null,
                    "inactive altar rejects without consuming inputs");
                matrix.onWandRightClick(world, null, player, 4, 10, 0, 1, 0);
                input.setInventorySlotContents(0, new ItemStack(Items.emerald));
                require(
                    adapter.dispatch(provider, provider.targets.get(0), details, input) == null
                        && center.getStackInSlot(0) == null,
                    "missing research rejects without consuming inputs");
                input.setInventorySlotContents(0, new ItemStack(Items.ender_pearl));
                center.setInventorySlotContents(0, new ItemStack(Items.stick));
                require(
                    adapter.dispatch(provider, provider.targets.get(0), details, input) == null
                        && center.getStackInSlot(0)
                            .getItem() == Items.stick,
                    "occupied pedestal preserved");
                center.setInventorySlotContents(0, null);
                require(
                    provider.getPatterns()
                        .getSizeInventory() == 36
                        && provider.getPatterns()
                            .getStackInSlot(45) == null,
                    "terminal cannot access core or returns");
                provider.setCraftingLock(PackagedCraftingLock.LOW);
                require(provider.craftingLocked() && !provider.pushPattern(details, input), "low redstone lock");
                provider.setCraftingLock(PackagedCraftingLock.HIGH);
                require(!provider.craftingLocked(), "high redstone lock without power allows crafting");
                provider.setCraftingLock(PackagedCraftingLock.RESULT);
                require(provider.pushPattern(details, input), "real infusion accepted");
                require(matrix.crafting && provider.queuedJobs() == 1, "one active lane");
                require(
                    provider.decrStackSize(TilePackagedProvider.CORE, 1) == null,
                    "automation cannot extract an active core");
                require(!provider.pushPattern(details, input), "no double dispatch");
                NBTTagCompound saved = new NBTTagCompound();
                provider.writeToNBT(saved);
                inFlight = (NBTTagCompound) saved.copy();
                TilePackagedProvider restored = new TilePackagedProvider();
                restored.readFromNBT(saved);
                require(restored.queuedJobs() == 1 && restored.targets.size() == 1, "job and target NBT roundtrip");
                require(
                    restored.getStackInSlot(0) != null && restored.getStackInSlot(45) != null,
                    "inventory roundtrip");
                require(provider.removeTarget(0) == false, "active target cannot be removed");
                require(!provider.releaseInterrupted(0), "active altar recovery refused");
                require(restored.craftingLocked(), "result lock survives NBT");
                NBTTagCompound pulse = (NBTTagCompound) saved.copy();
                pulse.setInteger("CraftingLock", PackagedCraftingLock.PULSE.ordinal());
                pulse.setBoolean("PulseLocked", true);
                pulse.setBoolean("PreviousRedstone", true);
                TilePackagedProvider pulseFixture = new TilePackagedProvider();
                pulseFixture.readFromNBT(pulse);
                pulseFixture.setWorldObj(world);
                world.setBlock(-1, 8, 0, net.minecraft.init.Blocks.redstone_block);
                pulseFixture.updateCraftingLockPower();
                require(pulseFixture.craftingLocked(), "sustained redstone does not count as a new pulse");
                world.setBlockToAir(-1, 8, 0);
                pulseFixture.updateCraftingLockPower();
                world.setBlock(-1, 8, 0, net.minecraft.init.Blocks.redstone_block);
                pulseFixture.updateCraftingLockPower();
                require(!pulseFixture.craftingLocked(), "rising redstone edge unlocks pulse mode");
                world.setBlockToAir(-1, 8, 0);
            }
            if (tick == 100) reloadRequested = true;
            if (reloadFinished && !reloadChecked) {
                reloadChecked = true;
                provider = (TilePackagedProvider) world.getTileEntity(0, 8, 0);
                require(
                    provider != null && provider.queuedJobs() == 1 && provider.craftingLocked(),
                    "actual save-close-reopen preserves receipt and lock");
                inFlight = new NBTTagCompound();
                provider.writeToNBT(inFlight);
                require(
                    ((TileInfusionMatrix) world.getTileEntity(4, 10, 0)).crafting,
                    "real TC altar survives world restart while waiting for essentia");
                world.setBlock(6, 8, 2, ConfigBlocks.blockJar, 0, 3);
                var jar = (thaumcraft.common.tiles.TileJarFillable) world.getTileEntity(6, 8, 2);
                jar.addToContainer(thaumcraft.api.aspects.Aspect.AIR, 64);
                jar.markDirty();
                world.markBlockForUpdate(6, 8, 2);
            }
            if (tick > 100 && !awaitingStorage && provider.queuedJobs() == 0 && provider.getStackInSlot(36) != null) {
                require(
                    provider.getStackInSlot(36)
                        .getItem() == Items.diamond && provider.getStackInSlot(36).stackSize == 1,
                    "real TC output returned exactly once");
                require(
                    ((TilePedestal) world.getTileEntity(4, 8, 0)).getStackInSlot(0) == null,
                    "altar output removed");
                require(
                    reloadChecked
                        && ((thaumcraft.common.tiles.TileJarFillable) world.getTileEntity(6, 8, 2)).amount <= 56,
                    "real jar essentia consumed by TC");
                require(provider.craftingLocked(), "result stays locked while ME has no output storage");
                TilePackagedProvider interrupted = new TilePackagedProvider();
                interrupted.readFromNBT(inFlight);
                interrupted.setWorldObj(world);
                var center = (TilePedestal) world.getTileEntity(4, 8, 0);
                center.setInventorySlotContents(0, new ItemStack(Items.diamond));
                require(!interrupted.releaseInterrupted(0), "recovery refuses uncollected expected output");
                center.setInventorySlotContents(0, new ItemStack(Items.stick));
                require(
                    interrupted.releaseInterrupted(0) && interrupted.queuedJobs() == 0
                        && center.getStackInSlot(0)
                            .getItem() == Items.stick,
                    "interrupted receipt released without changing altar items");
                center.setInventorySlotContents(0, null);
                require(
                    !provider.bind(new PackagedTarget(world.provider.dimensionId, 16000000, 8, 16000000, 1))
                        && !world.getChunkProvider()
                            .chunkExists(1000000, 1000000),
                    "binding never loads remote chunks");
                world.setBlock(
                    0,
                    8,
                    2,
                    AEApi.instance()
                        .definitions()
                        .blocks()
                        .drive()
                        .maybeBlock()
                        .get());
                ((appeng.tile.storage.TileDrive) world.getTileEntity(0, 8, 2)).getInternalInventory()
                    .setInventorySlotContents(
                        0,
                        AEApi.instance()
                            .definitions()
                            .items()
                            .cell1k()
                            .maybeStack(1)
                            .get());
                awaitingStorage = true;
            }
            if (awaitingStorage && provider.getStackInSlot(36) == null) {
                require(!provider.craftingLocked(), "result lock clears only after actual ME insertion");
                var stored = provider.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .getStorageList()
                    .findPrecise(appeng.util.item.AEItemStack.create(new ItemStack(Items.diamond)));
                require(stored != null && stored.getStackSize() == 1, "ME storage receives exactly one result");
                for (int i = 1; i <= 6; i++) {
                    world.setBlock(16 + i, 8, 2, ConfigBlocks.blockStoneDevice, 1, 3);
                    require(
                        provider.bind(new PackagedTarget(world.provider.dimensionId, 16 + i, 8, 2, 1)),
                        "GUI scroll fixture " + i);
                }
                System.out.println("PACKAGED_QA: transactions and persistence PASS");
                com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(player, 0, 8, 0);
                finished = true;
            }
            if (tick > 900) throw new AssertionError("Timed out: real altar completion/return");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("PACKAGED_QA: FAILED");
            failed = true;
            finished = true;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++frames == 120) {
            mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "packaged-provider-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            System.out.println("PACKAGED_QA: screenshot saved");
        }
        var screen = com.cleanroommc.modularui.screen.ModularScreen.getCurrent();
        if (frames == 140 && screen != null) click(screen, "targets");
        if (frames == 160) {
            var targetPanel = screen.getPanelManager()
                .getOpenPanel("packaged_targets");
            for (var widget : targetPanel.getChildren()) {
                if (widget instanceof com.cleanroommc.modularui.widgets.SliderWidget slider) slider.setValue(1, true);
            }
        }
        if (frames == 180) {
            var targetPanel = screen.getPanelManager()
                .getOpenPanel("packaged_targets");
            for (var widget : targetPanel.getChildren()) {
                if ("target_row_0".equals(widget.getName()))
                    ((com.cleanroommc.modularui.widgets.ButtonWidget<?>) widget).onMousePressed(1);
            }
        }
        if (frames == 200) {
            require(
                provider.targets.size() == 6 && provider.targets.stream()
                    .noneMatch(target -> target.x == 17)
                    && provider.targets.stream()
                        .anyMatch(target -> target.x == 4),
                "scrolled target action uses the correct server row");
            require(
                screen != null && screen.getPanelManager()
                    .getOpenPanel("packaged_targets") != null,
                "target panel opened");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "packaged-targets-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
        if (frames == 220 && screen != null) screen.getPanelManager()
            .closeTopPanel();
        if (frames == 240 && screen != null) click(screen, "priority");
        if (frames == 260) {
            for (var widget : screen.getPanelManager()
                .getOpenPanel("packaged_priority")
                .getChildren()) {
                if ("priority_0_1".equals(widget.getName()))
                    ((com.cleanroommc.modularui.widgets.ButtonWidget<?>) widget).onMousePressed(0);
            }
        }
        if (frames == 280) {
            require(provider.priority == 10, "priority button updates server value");
            for (var widget : screen.getPanelManager()
                .getOpenPanel("packaged_priority")
                .getChildren()) {
                if (widget instanceof com.cleanroommc.modularui.widgets.textfield.TextFieldWidget field) {
                    ((com.cleanroommc.modularui.value.sync.IntSyncValue) field.getSyncHandler()).setIntValue(42);
                }
            }
        }
        if (frames == 300) {
            require(provider.priority == 42, "priority input value synchronizes to server");
            require(
                screen != null && screen.getPanelManager()
                    .getOpenPanel("packaged_priority") != null,
                "priority panel opened");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "packaged-priority-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
        if (frames == 310 && screen != null) {
            screen.getPanelManager()
                .closeTopPanel();
            click(screen, "crafting_lock");
        }
        if (frames == 340) {
            require(provider.craftingLock == PackagedCraftingLock.NONE, "GUI lock button synchronizes to server");
            click(screen, "terminal_visible");
        }
        if (frames == 370) require(!provider.shouldDisplay(), "GUI terminal visibility synchronizes to server");
        if (frames == 390) {
            try {
                java.nio.file.Files.write(
                    new File("packaged-qa-result.txt").toPath(),
                    (failed ? "FAILED" : "PASS").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException error) {
                throw new RuntimeException(error);
            }
            mc.shutdown();
        }
    }

    private static void click(com.cleanroommc.modularui.screen.ModularScreen screen, String name) {
        for (var child : screen.getMainPanel()
            .getChildren()) {
            if (name.equals(child.getName())
                && child instanceof com.cleanroommc.modularui.widgets.ButtonWidget<?>button) {
                button.onMousePressed(0);
                return;
            }
        }
        throw new AssertionError("Missing button: " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PACKAGED_QA: " + message + " PASS");
    }
}
