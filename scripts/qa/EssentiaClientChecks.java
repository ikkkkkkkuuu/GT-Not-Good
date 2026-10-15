package com.xyp.gtnotgood.common.machines.basic;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.tile.storage.TileDrive;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

@Mod(modid = "essentiaqa", name = "Essentia QA", version = "2", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class EssentiaClientChecks {

    private static final CacheFixtureItem CACHE_FIXTURE = new CacheFixtureItem();
    private boolean started;
    private volatile boolean finished;
    private volatile String failure;
    private volatile String screenshot;
    private int ticks, stage, stageStarted, frames;
    private CountingBase base;
    private EssentiaDisassembler machine;
    private TileDrive drive;
    private ItemStack cell;
    private AspectList expected;
    private IMEMonitor<AEEssentiaStack> monitor;
    private boolean observingRetries;
    private long previousAttempt;
    private int attempts;
    private long fullAir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(CountingBase.class, "essentiaqa:counting_base");
        GameRegistry.registerItem(CACHE_FIXTURE, "cache_fixture");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("essentia-qa-" + System.currentTimeMillis(), "Essentia QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        World world = player.getServerForPlayer();
        try {
            if (++ticks == 1) setup(world, player);
            if (ticks > 1800) throw new AssertionError("timeout at stage " + stage);
            if (observingRetries) observeRetries();
            if (ticks < 80 || screenshot != null) return;
            int elapsed = ticks - stageStarted;
            switch (stage) {
                case 0 -> {
                    require(machine.getProxy().isActive(), "active real network");
                    refreshMonitor();
                    setInput(new ItemStack(Items.stick, 32));
                    advance();
                }
                case 1 -> {
                    if (elapsed < 40) return;
                    require(input() == null, "32 consecutive crafts finish within 40 ticks");
                    requireYield(32);
                    cell = drive.getInternalInventory().getStackInSlot(0);
                    drive.getInternalInventory().setInventorySlotContents(0, null);
                    setInput(new ItemStack(Items.stick, 2));
                    advance();
                }
                case 2 -> {
                    if (elapsed < 40) return;
                    require(input() != null && input().stackSize == 2, "missing storage does not consume input");
                    setInput(null);
                    drive.getInternalInventory().setInventorySlotContents(0, cell);
                    advance();
                }
                case 3 -> {
                    if (elapsed < 10) return;
                    var remainder = monitor.injectItems(new AEEssentiaStack(Aspect.AIR, 10_000_000),
                        Actionable.MODULATE, new MachineSource(base));
                    require(remainder != null && remainder.getStackSize() > 0, "real essentia cell filled to capacity");
                    requireRejected(Aspect.AIR);
                    requireRejected(Aspect.FIRE);
                    fullAir = amount(Aspect.AIR);
                    loadBatch(new AspectList().add(Aspect.AIR, 3).add(Aspect.FIRE, 7));
                    previousAttempt = 0;
                    observingRetries = true;
                    show(player, "essentia-pending.png");
                    base.dirtyCalls = 0;
                    advance();
                }
                case 4 -> {
                    if (elapsed < 100) return;
                    observingRetries = false;
                    require(attempts >= 7 && attempts <= 11, "bounded retries while cell remains full: " + attempts);
                    require(base.dirtyCalls == 0, "rejected output never marks the machine dirty");
                    System.out.println("ESSENTIA_QA: full cell " + attempts
                        + " attempts in "
                        + elapsed
                        + " ticks, "
                        + base.dirtyCalls
                        + " dirty calls");
                    requireBatch(new AspectList().add(Aspect.AIR, 3).add(Aspect.FIRE, 7));
                    require("WaitingOutput".equals(machine.getDisplayState().name()), "pending output status");
                    var extracted = monitor.extractItems(new AEEssentiaStack(Aspect.AIR, 3), Actionable.MODULATE,
                        new MachineSource(base));
                    require(extracted != null && extracted.getStackSize() == 3, "open room for exactly one aspect");
                    base.dirtyCalls = 0;
                    advance();
                }
                case 5 -> {
                    if (elapsed < 25) return;
                    requireBatch(new AspectList().add(Aspect.FIRE, 7));
                    require(amount(Aspect.AIR) == fullAir, "partially accepted aspect inserted exactly once");
                    require(base.dirtyCalls == 1, "partial insertion marks dirty once; later refusals do not");
                    NBTTagCompound saved = savedBatch();
                    NBTTagCompound dropped = new NBTTagCompound();
                    machine.setItemNBT(dropped);
                    requireAspects(readBatch(dropped), new AspectList().add(Aspect.FIRE, 7), "wrench remainder");
                    require(!dropped.hasKey("essentia_proxy"), "wrench item does not clone AE node identity");
                    machine.loadNBTData(saved);
                    requireBatch(new AspectList().add(Aspect.FIRE, 7));
                    drive.getInternalInventory().setInventorySlotContents(0,
                        ThEApi.instance().items().EssentiaCell_64k.getStack());
                    setInput(new ItemStack(Items.stick, 2));
                    advance();
                }
                case 6 -> {
                    if (elapsed < 60) return;
                    require(input() == null, "processing resumes after capacity returns");
                    requireYield(2);
                    require(amount(Aspect.FIRE) == 7 && amount(Aspect.AIR) == 0,
                        "saved remainder delivered exactly once");
                    world.setBlockToAir(3, 8, 4);
                    advance();
                }
                case 7 -> {
                    if (elapsed < 40) return;
                    require(!machine.getProxy().isActive(), "machine loses its real AE connection");
                    loadBatch(new AspectList().add(Aspect.WATER, 5));
                    setInput(new ItemStack(Items.stick));
                    advance();
                }
                case 8 -> {
                    if (elapsed < 40) return;
                    require(input() != null && input().stackSize == 1, "disconnected network does not consume input");
                    requireBatch(new AspectList().add(Aspect.WATER, 5));
                    world.setBlock(3, 8, 4,
                        AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
                    advance();
                }
                case 9 -> {
                    if (elapsed < 80) return;
                    require(machine.getProxy().isActive(), "real AE connection recovers");
                    refreshMonitor();
                    require(input() == null, "processing resumes after network reconnects");
                    requireYield(3);
                    require(amount(Aspect.FIRE) == 7 && amount(Aspect.WATER) == 5, "no duplicate output on reconnect");
                    requireBatch(new AspectList());
                    base.disableWorking();
                    setInput(new ItemStack(Items.stick));
                    requireAspects(machine.getDisplayedEssentia(), expected, "input preview");
                    require("Disabled".equals(machine.getDisplayState().name()), "disabled preview status");
                    show(player, "essentia-input-preview.png");
                    advance();
                }
                case 10 -> {
                    setInput(null);
                    base.enableWorking();
                    require("Idle".equals(machine.getDisplayState().name()), "normal idle status");
                    require(machine.getDisplayedEssentia().size() == 0, "empty machine clears preview");
                    show(player, "essentia-normal.png");
                    advance();
                }
                case 11 -> {
                    System.out.println("ESSENTIA_QA: PASS cache identity/copy/expiry, 32 consecutive crafts, full-cell "
                        + "backoff, dirty counts, partial output persistence, disconnect/reconnect and three GUI states");
                    finished = true;
                }
                default -> throw new AssertionError("unknown stage " + stage);
            }
        } catch (Throwable problem) {
            problem.printStackTrace();
            failure = "FAIL stage " + stage + ": " + problem;
            finished = true;
        }
    }

    private void setup(World world, EntityPlayerMP player) throws Exception {
        world.setBlock(4, 8, 4, GregTechAPI.sBlockMachines, 0, 3);
        base = new CountingBase();
        world.setTileEntity(4, 8, 4, base);
        base.setInitialValuesAsNBT(null, (short) GTNGMachineID.EssentiaDisassembler.id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setStoredEU(100000);
        machine = (EssentiaDisassembler) base.getMetaTileEntity();
        var blocks = AEApi.instance().definitions().blocks();
        world.setBlock(3, 8, 4, blocks.energyCellCreative().maybeBlock().get());
        world.setBlock(3, 8, 5, blocks.drive().maybeBlock().get());
        drive = (TileDrive) world.getTileEntity(3, 8, 5);
        cell = ThEApi.instance().items().EssentiaCell_64k.getStack();
        drive.getInternalInventory().setInventorySlotContents(0, cell);
        player.playerNetServerHandler.setPlayerLocation(4.5, 9, 2, 0, 15);
        player.capabilities.isFlying = true;
        Config.recipeSpeedMode = 0;
        require(EssentiaDisassembler.processingDuration() == 20, "base duration");
        Config.recipeSpeedMode = 2;
        Config.recipeSpeedMultiplier = .1F;
        require(EssentiaDisassembler.processingDuration() == 2, "multiplier duration");
        Config.recipeSpeedMode = 1;
        Config.recipeSpeedFixedDuration = 1;
        require(EssentiaDisassembler.processingDuration() == 1, "fixed duration");
        expected = EssentiaDisassembler.getEssentia(new ItemStack(Items.stick));
        require(expected.size() > 0, "native stick aspects");
        checkCache();
    }

    private void checkCache() throws Exception {
        ThaumcraftApi.registerObjectTag(new ItemStack(CACHE_FIXTURE, 1, 32767), new AspectList());
        Method cached = EssentiaDisassembler.class.getDeclaredMethod("getCachedEssentia", ItemStack.class);
        cached.setAccessible(true);
        ItemStack stack = new ItemStack(CACHE_FIXTURE, 1, 0);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Amount", 4);
        CACHE_FIXTURE.reads = 0;
        AspectList first = (AspectList) cached.invoke(machine, stack);
        require(first.getAmount(Aspect.AIR) == 4, "cache fixture initial NBT");
        int reads = CACHE_FIXTURE.reads;
        first.add(Aspect.AIR, 100);
        stack.stackSize = 32;
        require(((AspectList) cached.invoke(machine, stack)).getAmount(Aspect.AIR) == 4, "cached result copy isolated");
        require(CACHE_FIXTURE.reads == reads, "stack size does not invalidate cache");
        stack.setItemDamage(1);
        require(((AspectList) cached.invoke(machine, stack)).getAmount(Aspect.AIR) == 5, "meta invalidates cache");
        require(CACHE_FIXTURE.reads > reads, "meta recomputes native yield");
        reads = CACHE_FIXTURE.reads;
        stack.getTagCompound().setInteger("Amount", 9);
        require(((AspectList) cached.invoke(machine, stack)).getAmount(Aspect.AIR) == 10, "NBT invalidates cache");
        require(CACHE_FIXTURE.reads > reads, "NBT recomputes native yield");
        reads = CACHE_FIXTURE.reads;
        field("essentiaCacheExpiresAt").setLong(machine, base.getTimer());
        cached.invoke(machine, stack);
        require(CACHE_FIXTURE.reads > reads, "expired yield cache refreshes");
        require(((AspectList) cached.invoke(machine, new Object[] { null })).size() == 0, "null input clears preview");
    }

    private void observeRetries() throws Exception {
        long next = field("nextOutputAttemptTick").getLong(machine);
        if (next == previousAttempt) return;
        long delay = next - base.getTimer();
        int expectedDelay = attempts < 5 ? 1 << attempts : 20;
        require(delay == expectedDelay, "retry delay " + attempts + ": expected " + expectedDelay + ", got " + delay);
        require(field("outputRetryDelay").getInt(machine) == Math.min(20, expectedDelay * 2), "next retry growth");
        require(base.dirtyCalls == 0, "failed retries do not mark dirty");
        previousAttempt = next;
        attempts++;
    }

    private static Field field(String name) throws Exception {
        Field field = EssentiaDisassembler.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private void loadBatch(AspectList aspects) {
        NBTTagCompound saved = savedBatch();
        NBTTagCompound pending = new NBTTagCompound();
        aspects.writeToNBT(pending);
        pending.setBoolean("Finished", true);
        saved.setTag("EssentiaBatch", pending);
        machine.loadNBTData(saved);
    }

    private NBTTagCompound savedBatch() {
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        saved.removeTag("essentia_proxy");
        return saved;
    }

    private static AspectList readBatch(NBTTagCompound tag) {
        AspectList aspects = new AspectList();
        aspects.readFromNBT(tag.getCompoundTag("EssentiaBatch"));
        return aspects;
    }

    private void requireBatch(AspectList expectedBatch) {
        requireAspects(readBatch(savedBatch()), expectedBatch, "persistent batch");
        if (expectedBatch.size() > 0) requireAspects(machine.getDisplayedEssentia(), expectedBatch, "batch preview");
    }

    private static void requireAspects(AspectList actual, AspectList expectedAspects, String label) {
        require(actual.size() == expectedAspects.size(), label + " aspect count");
        for (Aspect aspect : expectedAspects.getAspects()) {
            if (aspect == null) continue;
            require(actual.getAmount(aspect) == expectedAspects.getAmount(aspect), label + " " + aspect.getTag());
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshMonitor() throws Exception {
        monitor = (IMEMonitor<AEEssentiaStack>) machine.getProxy().getStorage()
            .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    private void requireRejected(Aspect aspect) {
        var remaining = monitor.injectItems(new AEEssentiaStack(aspect, 1), Actionable.SIMULATE,
            new MachineSource(base));
        require(remaining != null && remaining.getStackSize() == 1, "full cell rejects " + aspect.getTag());
    }

    private void requireYield(int count) {
        for (Aspect aspect : expected.getAspects()) {
            if (aspect == null) continue;
            require(amount(aspect) == expected.getAmount(aspect) * (long) count,
                "exact stick yield " + aspect.getTag());
        }
    }

    private long amount(Aspect aspect) {
        var stack = monitor.getStorageList().findPrecise(new AEEssentiaStack(aspect, 1));
        return stack == null ? 0 : stack.getStackSize();
    }

    private ItemStack input() {
        return base.getStackInSlot(machine.getInputSlot());
    }

    private void setInput(ItemStack stack) {
        base.setInventorySlotContents(machine.getInputSlot(), stack);
    }

    private void advance() {
        stage++;
        stageStarted = ticks;
    }

    private void show(EntityPlayerMP player, String name) {
        machine.onRightclick(base, player);
        frames = 0;
        screenshot = name;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("ESSENTIA_QA: " + message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.essentia.output"));
        if (screenshot != null && ++frames >= 30 && !finished) {
            try {
                require(mc.currentScreen instanceof GuiContainerWrapper, "native machine GUI open");
                ScreenShotHelper.saveScreenshot(output, screenshot, mc.displayWidth, mc.displayHeight,
                    mc.getFramebuffer());
                require(new File(output, "screenshots/" + screenshot).isFile(), "GUI screenshot saved");
                screenshot = null;
            } catch (Throwable problem) {
                problem.printStackTrace();
                failure = "FAIL screenshot: " + problem;
                finished = true;
            }
        }
        if (finished) {
            String result = failure == null ? "PASS" : failure;
            Files.write(new File(output, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            System.out.println("ESSENTIA_QA: " + result);
            FMLCommonHandler.instance().exitJava(failure == null ? 0 : 1, true);
        }
    }

    public static final class CountingBase extends BaseMetaTileEntity {

        int dirtyCalls;

        @Override
        public void markDirty() {
            dirtyCalls++;
            super.markDirty();
        }
    }

    private static final class CacheFixtureItem extends Item implements IEssentiaContainerItem {

        int reads;

        CacheFixtureItem() {
            setHasSubtypes(true);
        }

        @Override
        public AspectList getAspects(ItemStack stack) {
            reads++;
            int amount = stack.hasTagCompound() ? stack.getTagCompound().getInteger("Amount") : 0;
            return new AspectList().add(Aspect.AIR, amount + stack.getItemDamage());
        }

        @Override
        public void setAspects(ItemStack stack, AspectList aspects) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setInteger("Amount", aspects.getAmount(Aspect.AIR));
        }
    }
}
