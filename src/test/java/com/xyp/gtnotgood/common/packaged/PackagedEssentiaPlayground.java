package com.xyp.gtnotgood.common.packaged;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;
import thaumicenergistics.common.tiles.TileInfusionProvider;

/** Adds a separate real AE-essentia-fed altar to an existing playground without modifying its jar-fed rig. */
@Mod(
    modid = "packagedessentiaplayground",
    name = "Packaged AE Essentia Playground",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PackagedEssentiaPlayground {

    private int ticks;
    private boolean done;
    private TilePackagedProvider provider;
    private InfusionRecipe recipe;
    private boolean repairing;
    private volatile boolean ready;
    private int frames;
    private java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingJob> craft;
    private boolean submitted;
    private boolean checkedRoute;
    private appeng.api.networking.crafting.ICraftingLink craftLink;
    private boolean verified;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        repairing = Boolean.getBoolean("gtng.packaged.playground.repair");
        if (Boolean.getBoolean("gtng.packaged.playground.ae")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || ++frames != 10) return;
        var mc = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.util.ScreenShotHelper.saveScreenshot(
            mc.mcDataDir,
            "packaged-ae-essentia.png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        try {
            if (++ticks == 1) {
                if (Boolean.getBoolean("gtng.packaged.playground.inspect")) {
                    provider = (TilePackagedProvider) world.getTileEntity(40, 8, 0);
                    return;
                }
                if (repairing) {
                    provider = (TilePackagedProvider) world.getTileEntity(40, 8, 0);
                    if (provider == null) throw new IllegalStateException("No second playground to repair");
                    recipe = (InfusionRecipe) ConfigResearch.recipes.get("WandRodQuartz");
                    pillars(world);
                    player.playerNetServerHandler.setPlayerLocation(40.5, 8, -5.5, -35, 12);
                } else {
                    // Refuse to overwrite a previously built rig or user construction.
                    for (int x = 32; x <= 54; x++) for (int z = -8; z <= 8; z++) for (int y = 5; y <= 11; y++) {
                        if (!world.isAirBlock(x, y, z)) throw new IllegalStateException("Second test area is occupied");
                    }
                    recipe = (InfusionRecipe) ConfigResearch.recipes.get("WandRodQuartz");
                    for (int x = 32; x <= 54; x++)
                        for (int z = -8; z <= 8; z++) world.setBlock(x, 7, z, Blocks.quartz_block);
                    world.setBlock(
                        40,
                        8,
                        0,
                        net.minecraft.block.Block
                            .getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
                    provider = (TilePackagedProvider) world.getTileEntity(40, 8, 0);
                    provider.setOwnerName(player.getCommandSenderName());
                    provider.setInventorySlotContents(
                        TilePackagedProvider.CORE,
                        GTNGItemList.ThaumcraftInfusionCore.get(1));
                    provider.autoReturn = true;
                    var blocks = AEApi.instance()
                        .definitions()
                        .blocks();
                    place(
                        world,
                        39,
                        0,
                        blocks.controller()
                            .maybeStack(1)
                            .get());
                    place(
                        world,
                        39,
                        1,
                        blocks.energyCellCreative()
                            .maybeStack(1)
                            .get());
                    place(
                        world,
                        38,
                        0,
                        blocks.drive()
                            .maybeStack(1)
                            .get());
                    var drive = ((TileDrive) world.getTileEntity(38, 8, 0)).getInternalInventory();
                    drive.setInventorySlotContents(
                        0,
                        AEApi.instance()
                            .definitions()
                            .items()
                            .cell64k()
                            .maybeStack(1)
                            .get());
                    drive.setInventorySlotContents(
                        1,
                        ThEApi.instance()
                            .items().EssentiaCell_64k.getStack());
                    place(
                        world,
                        38,
                        1,
                        blocks.craftingStorage64k()
                            .maybeStack(1)
                            .get());
                    place(
                        world,
                        37,
                        1,
                        blocks.craftingAccelerator()
                            .maybeStack(1)
                            .get());
                    cable(
                        world,
                        player,
                        37,
                        0,
                        AEApi.instance()
                            .definitions()
                            .parts()
                            .craftingTerminal()
                            .maybeStack(1)
                            .get());
                    cable(
                        world,
                        player,
                        36,
                        0,
                        ThEApi.instance()
                            .parts().Essentia_Terminal.getStack());
                    cable(
                        world,
                        player,
                        35,
                        0,
                        AEApi.instance()
                            .definitions()
                            .parts()
                            .patternTerminal()
                            .maybeStack(1)
                            .get());
                    world.setBlock(46, 10, 0, ConfigBlocks.blockStoneDevice, 2, 3);
                    if (!provider.bind(new PackagedTarget(0, 46, 10, 0, 1)))
                        throw new IllegalStateException("Altar binding failed");
                    world.setBlock(46, 8, 0, ConfigBlocks.blockStoneDevice, 1, 3);
                    pillars(world);
                    for (int[] p : new int[][] { { -3, 0 }, { 3, 0 }, { 0, -3 }, { 0, 3 }, { -3, -3 }, { 3, 3 },
                        { -3, 3 }, { 3, -3 } }) {
                        world.setBlock(46 + p[0], 8, p[1], ConfigBlocks.blockStoneDevice, 1, 3);
                    }
                    for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                        world.setBlock(46 + x, 5, z, Blocks.stonebrick);
                        world.setBlock(46 + x, 6, z, ConfigBlocks.blockCandle, 0, 3);
                    }
                    // Eight visible symmetric corner candles, clear of the AE cable route.
                    for (int x : new int[] { -4, 4 })
                        for (int z : new int[] { -4, 4 }) world.setBlock(46 + x, 8, z, ConfigBlocks.blockCandle, 0, 3);
                    Thaumcraft.proxy.getResearchManager()
                        .completeResearch(player, recipe.getResearch());
                    ItemStack pattern = AEApi.instance()
                        .definitions()
                        .items()
                        .encodedPattern()
                        .maybeStack(1)
                        .get();
                    NBTTagCompound tag = new NBTTagCompound();
                    NBTTagList inputs = new NBTTagList();
                    inputs.appendTag(
                        recipe.getRecipeInput()
                            .writeToNBT(new NBTTagCompound()));
                    for (ItemStack item : recipe.getComponents())
                        inputs.appendTag(item.writeToNBT(new NBTTagCompound()));
                    NBTTagList outputs = new NBTTagList();
                    outputs.appendTag(((ItemStack) recipe.getRecipeOutput()).writeToNBT(new NBTTagCompound()));
                    tag.setBoolean("crafting", false);
                    tag.setTag("in", inputs);
                    tag.setTag("out", outputs);
                    pattern.setTagCompound(tag);
                    provider.setInventorySlotContents(0, pattern);
                    sign(world, 36, -2, "AE ESSENTIA TEST", "Craft / Essentia", "Quartz wand core", "1024 per aspect");
                    player.playerNetServerHandler.setPlayerLocation(40.5, 8, -5.5, -35, 12);
                }
                if (!provider.networkEssentia && !provider.setNetworkEssentia(true))
                    throw new IllegalStateException("Direct mode rejected");
                if (world.getTileEntity(46, 8, 6) instanceof TileInfusionProvider) world.setBlockToAir(46, 8, 6);
                for (int x = 39; x <= 51; x++)
                    if (world.getTileEntity(x, 8, 2) instanceof IPartHost) world.setBlockToAir(x, 8, 2);
                for (int z = 3; z <= 6; z++)
                    if (world.getTileEntity(51, 8, z) instanceof IPartHost) world.setBlockToAir(51, 8, z);
                for (int x = 47; x <= 50; x++)
                    if (world.getTileEntity(x, 8, 6) instanceof IPartHost) world.setBlockToAir(x, 8, 6);
                sign(world, 46, -6, "DIRECT AE ESSENTIA", "TC4 core supplies", "No extra provider", "No essentia jars");
            }
            if (Boolean.getBoolean("gtng.packaged.playground.inspect")) {
                if (ticks == 100) {
                    com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(player, 40, 8, 0);
                    ready = true;
                    done = true;
                    System.out.println("PACKAGED_MANUAL_HANDOFF: existing world retained, no new verification craft");
                }
                return;
            }
            if (ticks == 120) {
                ((TileInfusionMatrix) world.getTileEntity(46, 10, 0))
                    .onWandRightClick(world, null, player, 46, 10, 0, 1, 0);
                if (!repairing) {
                    stock(recipe.getRecipeInput());
                    for (ItemStack item : recipe.getComponents()) stock(item);
                    IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                        .getStorage()
                        .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
                    for (Aspect aspect : recipe.getAspects()
                        .getAspects()) {
                        if (monitor.injectItems(
                            new AEEssentiaStack(aspect, 1024),
                            Actionable.MODULATE,
                            new MachineSource(provider)) != null) {
                            throw new IllegalStateException("Essentia not stored in AE");
                        }
                    }
                }
            }
            if (ticks >= 200 && craft == null
                && provider.queuedJobs() == 0
                && provider.getProxy()
                    .getCrafting()
                    .getCpus()
                    .stream()
                    .noneMatch(cpu -> cpu.isBusy())) {
                if (!provider.getProxy()
                    .isActive() || provider.getProxy()
                        .getCrafting()
                        .getCpus()
                        .isEmpty())
                    throw new IllegalStateException("AE rig not active");
                IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                    .getStorage()
                    .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
                var matrix = (TileInfusionMatrix) world.getTileEntity(46, 10, 0);
                if (!matrix.active) throw new IllegalStateException("Altar not active");
                craft = provider.getProxy()
                    .getCrafting()
                    .beginCraftingJob(
                        world,
                        provider.getProxy()
                            .getGrid(),
                        new MachineSource(provider),
                        AEItemStack.create((ItemStack) recipe.getRecipeOutput()),
                        null);
            }
            if (craft != null && !submitted && craft.isDone()) {
                craftLink = provider.getProxy()
                    .getCrafting()
                    .submitJob(craft.get(), null, null, false, new MachineSource(provider));
                if (craftLink == null) {
                    throw new IllegalStateException("AE CPU refused real crafting request");
                }
                submitted = true;
                System.out.println("PACKAGED_DIRECT_QA: real AE CPU job submitted");
            }
            if (submitted) {
                var matrix = (TileInfusionMatrix) world.getTileEntity(46, 10, 0);
                if (provider.queuedJobs() > 0 && !checkedRoute) {
                    if (!matrix.crafting || !(matrix instanceof InfusionSourceAccess access)
                        || access.gtnotgood$getEssentiaSource() == null)
                        throw new IllegalStateException("Missing direct route");
                    if (provider.setNetworkEssentia(false))
                        throw new IllegalStateException("Source changed during active job");
                    if (!Boolean.FALSE.equals(DirectEssentiaSupply.drain(matrix, Aspect.AIR)))
                        throw new IllegalStateException("Missing aspect did not wait");
                    var route = access.gtnotgood$getEssentiaSource();
                    var badRoute = (NBTTagCompound) route.copy();
                    badRoute.setString("Identity", "different-provider");
                    access.gtnotgood$setEssentiaSource(badRoute);
                    if (!Boolean.FALSE.equals(
                        DirectEssentiaSupply.drain(
                            matrix,
                            recipe.getAspects()
                                .getAspects()[0]))) {
                        throw new IllegalStateException("Replacement Provider accepted stale job");
                    }
                    access.gtnotgood$setEssentiaSource(route);
                    verifyBatch(matrix);
                    NBTTagCompound saved = new NBTTagCompound();
                    matrix.writeToNBT(saved);
                    TileInfusionMatrix copy = new TileInfusionMatrix();
                    copy.readFromNBT(saved);
                    if (!route.equals(((InfusionSourceAccess) copy).gtnotgood$getEssentiaSource()))
                        throw new IllegalStateException("Route NBT lost");
                    NBTTagCompound providerSave = new NBTTagCompound();
                    provider.writeToNBT(providerSave);
                    TilePackagedProvider providerCopy = new TilePackagedProvider();
                    providerCopy.readFromNBT(providerSave);
                    if (!providerCopy.networkEssentia || !providerCopy.essentiaIdentity()
                        .equals(provider.essentiaIdentity()) || providerCopy.queuedJobs() != 1)
                        throw new IllegalStateException("Provider NBT lost");
                    checkedRoute = true;
                    System.out.println("PACKAGED_DIRECT_QA: shortage, source lock, identity and NBT checks passed");
                }
                var result = provider.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .extractItems(
                        AEItemStack.create((ItemStack) recipe.getRecipeOutput()),
                        Actionable.SIMULATE,
                        new MachineSource(provider));
                if (ticks % 200 == 0) System.out.println(
                    "PACKAGED_DIRECT_PROGRESS: jobs=" + provider.queuedJobs()
                        + " crafting="
                        + matrix.crafting
                        + " auto="
                        + provider.autoReturn
                        + " result="
                        + result
                        + " status="
                        + provider.altarStatus
                        + " center="
                        + ((thaumcraft.common.tiles.TilePedestal) world.getTileEntity(46, 8, 0)).getStackInSlot(0));
                if (checkedRoute && !matrix.crafting && provider.altarStatus == AltarStatus.INTERRUPTED) {
                    System.out.println(
                        "PACKAGED_DIRECT_QA_INTERRUPTED: manual test world changed during the verification craft; preserving player state");
                    com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(player, 40, 8, 0);
                    ready = true;
                    done = true;
                }
                // Standalone AE requests need not retain a live link after completion; inspect the actual
                // receipt/output.
                if (checkedRoute && !verified && result != null && provider.queuedJobs() == 0) {
                    IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                        .getStorage()
                        .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
                    for (Aspect aspect : recipe.getAspects()
                        .getAspects()) {
                        var stored = monitor.extractItems(
                            new AEEssentiaStack(aspect, 2048),
                            Actionable.SIMULATE,
                            new MachineSource(provider));
                        long available = stored == null ? 0 : stored.getStackSize();
                        if (available < 1024) monitor.injectItems(
                            new AEEssentiaStack(aspect, 1024 - available),
                            Actionable.MODULATE,
                            new MachineSource(provider));
                    }
                    // Remove only the verification output, leaving the craftable terminal entry ready for the player.
                    provider.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .extractItems(
                            AEItemStack.create((ItemStack) recipe.getRecipeOutput()),
                            Actionable.MODULATE,
                            new MachineSource(provider));
                    world.saveAllChunks(true, null);
                    server.getConfigurationManager()
                        .saveAllPlayerData();
                    System.out.println(
                        "PACKAGED_AE_ESSENTIA_READY: DIRECT; actual AE CPU craft completed; bounded batches verified; stocked 1024 each; symmetry="
                            + matrix.symmetry);
                    com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(player, 40, 8, 0);
                    verified = true;
                    ready = true;
                    done = true;
                }
                if (ticks > 6000) throw new IllegalStateException("Direct infusion timed out");
            }
        } catch (Exception error) {
            done = true;
            System.err.println("PACKAGED_AE_ESSENTIA_FAILED");
            error.printStackTrace();
        }
    }

    /** Validate each debit atomically, independent of manual crafting requests elsewhere on the network. */
    @SuppressWarnings("unchecked")
    private void verifyBatch(TileInfusionMatrix matrix) throws Exception {
        var aspect = recipe.getAspects()
            .getAspects()[0];
        IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
            .getStorage()
            .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
        int oldSpeed = provider.essentiaSpeed;
        for (int speed : new int[] { 1, 8, 32 }) for (int stock : new int[] { 0, 3, 64 }) {
            AEEssentiaStack saved = monitor.extractItems(
                new AEEssentiaStack(aspect, Long.MAX_VALUE),
                Actionable.MODULATE,
                new MachineSource(provider));
            try {
                if (stock > 0) monitor
                    .injectItems(new AEEssentiaStack(aspect, stock), Actionable.MODULATE, new MachineSource(provider));
                provider.essentiaSpeed = speed;
                var remaining = new thaumcraft.api.aspects.AspectList().add(aspect, 50);
                if (Boolean.TRUE.equals(DirectEssentiaSupply.drain(matrix, aspect)))
                    DirectEssentiaSupply.reduceBatch(matrix, remaining, aspect, 1);
                var after = monitor
                    .extractItems(new AEEssentiaStack(aspect, 1000), Actionable.SIMULATE, new MachineSource(provider));
                int consumed = Math.min(stock, speed);
                if ((after == null ? 0 : after.getStackSize()) != stock - consumed
                    || remaining.getAmount(aspect) != 50 - consumed) {
                    throw new IllegalStateException("Batch debit mismatch: speed=" + speed + " stock=" + stock);
                }
            } finally {
                monitor.extractItems(
                    new AEEssentiaStack(aspect, Long.MAX_VALUE),
                    Actionable.MODULATE,
                    new MachineSource(provider));
                if (saved != null) monitor.injectItems(saved, Actionable.MODULATE, new MachineSource(provider));
                provider.essentiaSpeed = oldSpeed;
            }
        }
        System.out.println(
            "PACKAGED_DIRECT_QA: exact per-cycle debit passed at 1x/8x/32x with empty, partial and sufficient storage");
    }

    /** Place both halves before neighbor notifications, matching a normally placed TC4 pillar. */
    private static void pillars(World world) {
        for (int x : new int[] { -1, 1 }) for (int z : new int[] { -1, 1 }) {
            world.setBlock(46 + x, 8, z, ConfigBlocks.blockStoneDevice, 3, 2);
            world.setBlock(46 + x, 9, z, ConfigBlocks.blockStoneDevice, 4, 2);
            world.notifyBlocksOfNeighborChange(46 + x, 8, z, ConfigBlocks.blockStoneDevice);
        }
    }

    private void stock(ItemStack item) throws Exception {
        ItemStack copy = item.copy();
        copy.stackSize = 64;
        if (provider.getProxy()
            .getStorage()
            .getItemInventory()
            .injectItems(AEItemStack.create(copy), Actionable.MODULATE, new MachineSource(provider)) != null)
            throw new IllegalStateException("Ingredient storage failed");
    }

    private static void place(World world, int x, int z, ItemStack stack) {
        world.setBlock(x, 8, z, net.minecraft.block.Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3);
    }

    private static void cable(World world, EntityPlayerMP player, int x, int z, ItemStack terminal) {
        if (!(world.getTileEntity(x, 8, z) instanceof IPartHost)) place(
            world,
            x,
            z,
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart()
                .maybeStack(1)
                .get());
        IPartHost host = (IPartHost) world.getTileEntity(x, 8, z);
        if (host.getPart(ForgeDirection.UNKNOWN) == null) host.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        if (terminal != null) host.addPart(terminal, ForgeDirection.NORTH, player);
    }

    private static void sign(World world, int x, int z, String... lines) {
        world.setBlock(x, 8, z, Blocks.standing_sign, 8, 3);
        TileEntitySign sign = (TileEntitySign) world.getTileEntity(x, 8, z);
        System.arraycopy(lines, 0, sign.signText, 0, 4);
        sign.markDirty();
        world.markBlockForUpdate(x, 8, z);
    }
}
