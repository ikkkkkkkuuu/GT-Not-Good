package com.xyp.gtnotgood.common.blocks.packaged;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.container.ContainerNull;
import appeng.container.implementations.ContainerRenamer;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.UltimatePatternHelper;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileCrucible;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Disposable client checks for paid crucible crafting, rejected dispatches, persistence and the registered icon. */
@Mod(modid = "crucibleqa", name = "Crucible QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CrucibleClientChecks {

    private boolean started;
    private volatile boolean finished;
    private int ticks, frames;
    private TilePackagedProvider provider;
    private TileCrucible crucible;
    private ICraftingPatternDetails details;
    private InventoryCrafting input;
    private IMEMonitor<AEEssentiaStack> essentia;
    private final ThaumcraftCrucibleAdapter adapter = new ThaumcraftCrucibleAdapter();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.crucible.qa")) return;
        ThaumcraftApi.addCrucibleRecipe("@GTNG_CRUCIBLE_QA", new ItemStack(Items.emerald), new ItemStack(Items.brick),
            new AspectList().add(Aspect.AIR, 2).add(Aspect.FIRE, 3));
        ThaumcraftApi.addCrucibleRecipe("GTNG_QA_LOCKED", new ItemStack(Items.diamond), new ItemStack(Items.stick),
            new AspectList().add(Aspect.AIR, 2));
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (finished) {
            if (!(mc.currentScreen instanceof CoreScreen)) mc.displayGuiScreen(new CoreScreen());
        } else if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("crucible-qa-" + System.currentTimeMillis(), "Crucible QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        if (++ticks == 1) {
            ResearchManager.completeResearchUnsaved(player.getCommandSenderName(), "@GTNG_CRUCIBLE_QA");
            var blocks = AEApi.instance().definitions().blocks();
            world.setBlock(4, 8, 4, Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
            world.setBlock(3, 8, 4, blocks.energyCellCreative().maybeBlock().get());
            world.setBlock(3, 8, 5, blocks.drive().maybeBlock().get());
            var drive = ((TileDrive) world.getTileEntity(3, 8, 5)).getInternalInventory();
            drive.setInventorySlotContents(0, AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
            drive.setInventorySlotContents(1, ThEApi.instance().items().EssentiaCell_64k.getStack());
            world.setBlock(8, 8, 4, ConfigBlocks.blockMetalDevice, 0, 3);
            provider = (TilePackagedProvider) world.getTileEntity(4, 8, 4);
            crucible = (TileCrucible) world.getTileEntity(8, 8, 4);
            provider.setOwnerName(player.getCommandSenderName());
            provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.ThaumcraftCrucibleCore.get(1));
            require(provider.bind(new PackagedTarget(world.provider.dimensionId, 8, 8, 4, 1)), "crucible binding");
            provider.setInventorySlotContents(0, encode(new ItemStack(Items.brick), new ItemStack(Items.emerald)));
            input = new InventoryCrafting(new ContainerNull(), 3, 3);
            input.setInventorySlotContents(7, new ItemStack(Items.brick));
            player.playerNetServerHandler.setPlayerLocation(4.5, 10, 1.5, 0, 15);
            player.capabilities.isFlying = true;
        }
        if (ticks == 80) {
            require(provider.getProxy().isActive(), "active grid");
            essentia = (IMEMonitor<AEEssentiaStack>) provider.getProxy().getStorage()
                .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            provider.provideCrafting((ICraftingProviderHelper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ICraftingProviderHelper.class }, (proxy, method, args) -> {
                    if (method.getName().equals("addCraftingOption")) details = (ICraftingPatternDetails) args[1];
                    return null;
                }));
            require(details != null, "advertised pattern");
            checks(player);
        }
        if (ticks >= 81 && ticks <= 180) {
            require(crucible.tank.getFluidAmount() == 0, "dry crucible remains empty");
            require(provider.pushPattern(details, input), "scheduled dispatch " + ticks);
            require(!provider.pushPattern(details, input), "one reaction per target per tick");
            require(provider.queuedJobs() == 0, "immediate return has no receipt");
        }
        if (ticks == 183) {
            var result = provider.getProxy().getStorage().getItemInventory().getStorageList()
                .findPrecise(AEItemStack.create(new ItemStack(Items.emerald)));
            require(result != null && result.getStackSize() == 100, "100 paid outputs in AE");
            require(amount(Aspect.AIR) == 800 && amount(Aspect.FIRE) == 700, "exact network debit");
            System.out.println(
                "CRUCIBLE_QA: PASS local-first, AE deficit, shortage, dry heat, no water debit, research, output, full returns, NBT, rename, 100 dispatches");
            finished = true;
        }
    }

    private void checks(EntityPlayerMP player) {
        // Initial TC player-data sync may replace the research map after the first server tick.
        ResearchManager.completeResearchUnsaved(player.getCommandSenderName(), "@GTNG_CRUCIBLE_QA");
        require(ResearchManager.isResearchComplete(player.getCommandSenderName(), "@GTNG_CRUCIBLE_QA"),
            "fixture research unlocked");
        crucible.heat = 150;
        crucible.tank.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        require(dispatch() == null, "cold rejection");
        crucible.heat = 200;
        crucible.tank.setFluid(new FluidStack(FluidRegistry.WATER, 49));
        put(Aspect.AIR, 1000);
        require(dispatch() == null && amount(Aspect.AIR) == 1000, "all-aspect simulation prevents partial debit");
        put(Aspect.FIRE, 1000);
        input.setInventorySlotContents(0, new ItemStack(Items.stick));
        require(dispatch() == null, "extra ingredient rejected");
        input.setInventorySlotContents(0, null);
        input.getStackInSlot(7).stackSize = 2;
        require(dispatch() == null, "extra catalyst rejected");
        input.getStackInSlot(7).stackSize = 1;
        var savedDetails = details;
        ItemStack wrong = encode(new ItemStack(Items.brick), new ItemStack(Items.diamond));
        details = ((ICraftingPatternItem) wrong.getItem()).getPatternForItem(wrong, player.worldObj);
        require(dispatch() == null, "wrong output rejected");
        ItemStack locked = encode(new ItemStack(Items.stick), new ItemStack(Items.diamond));
        details = ((ICraftingPatternItem) locked.getItem()).getPatternForItem(locked, player.worldObj);
        input.setInventorySlotContents(7, new ItemStack(Items.stick));
        require(dispatch() == null, "locked research rejected");
        input.setInventorySlotContents(7, new ItemStack(Items.brick));
        details = savedDetails;
        for (int i = 36; i < 45; i++) provider.setInventorySlotContents(i, new ItemStack(Items.brick, 64));
        require(dispatch() == null && amount(Aspect.AIR) == 1000 && crucible.tank.getFluidAmount() == 49,
            "full returns leaves resources untouched");
        clearReturns();
        crucible.aspects = new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 3).add(Aspect.WATER, 7);
        ItemStack localResult = dispatch();
        require(localResult != null, "local-first reaction: " + provider.altarStatus);
        require(amount(Aspect.AIR) == 999 && amount(Aspect.FIRE) == 1000, "only missing aspect withdrawn");
        require(crucible.aspects.getAmount(Aspect.WATER) == 7 && crucible.aspects.getAmount(Aspect.FIRE) == 0
            && crucible.tank.getFluidAmount() == 49, "exact local resources consumed");
        clearReturns();
        crucible.aspects = new AspectList().add(Aspect.AIR, 2).add(Aspect.FIRE, 3);
        require(dispatch() != null && amount(Aspect.AIR) == 999, "local-only reaction");
        clearReturns();
        require(input.getStackInSlot(7).stackSize == 1, "CPU input retained until success contract");
        new ContainerRenamer(player.inventory, (ICustomNameObject) provider).setNewName("炼金接口 QA");
        require(provider.getName().equals("炼金接口 QA") && provider.hasCustomInventoryName(), "terminal rename contract");
        provider.crucibleEssentiaCredit.put(Aspect.ORDER.getTag(), 2L);
        NBTTagCompound tag = new NBTTagCompound();
        provider.writeToNBT(tag);
        var restored = new TilePackagedProvider();
        restored.setWorldObj(player.worldObj);
        restored.readFromNBT(tag);
        require(
            restored.getCustomName().equals("炼金接口 QA")
                && restored.crucibleEssentiaCredit.equals(provider.crucibleEssentiaCredit),
            "name and reservation persistence");
        provider.crucibleEssentiaCredit.clear();
        provider.setCustomName("");
        require(!provider.hasCustomName() && provider.getName().equals("tile.wireless_packaged_pattern_provider.name"),
            "clear custom name");
        put(Aspect.AIR, 1);
        crucible.tank.setFluid(null);
        crucible.heat = 0;
        require(dispatch() == null && provider.altarStatus == AltarStatus.CrucibleHeat,
            "dry crucible still needs a heat source");
        player.worldObj.setBlock(crucible.xCoord, crucible.yCoord - 1, crucible.zCoord, Blocks.lava);
        // Match the instance's native ultimate pattern and CPU inventory for iron -> thaumium.
        ResearchManager.completeResearchUnsaved(player.getCommandSenderName(), "THAUMIUM");
        ItemStack thaumium = new ItemStack(ConfigItems.itemResource, 1, 2);
        ItemStack thaumiumPattern = encode(new ItemStack(Items.iron_ingot), thaumium);
        var thaumiumDetails = ((ICraftingPatternItem) thaumiumPattern.getItem()).getPatternForItem(thaumiumPattern,
            player.worldObj);
        var nativeInput = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        nativeInput.setInventorySlotContents(0, AEItemStack.create(new ItemStack(Items.iron_ingot)));
        put(Aspect.MAGIC, 4);
        require(thaumiumDetails instanceof UltimatePatternHelper, "native ultimate pattern decoded");
        require(adapter.dispatch(provider, provider.targets.get(0), thaumiumDetails, nativeInput) != null,
            "ultimate iron to thaumium: " + provider.altarStatus);
        require(amount(Aspect.MAGIC) == 0 && TilePackagedProvider.sameItem(provider.getStackInSlot(36), thaumium),
            "thaumium output and four magic essentia debited");
        clearReturns();
        nativeInput.setInventorySlotContents(0, AEItemStack.create(new ItemStack(Items.brick)));
        input = nativeInput;
    }

    private ItemStack dispatch() {
        return adapter.dispatch(provider, provider.targets.get(0), details, input);
    }

    private void clearReturns() {
        for (int i = 36; i < 45; i++) provider.setInventorySlotContents(i, null);
    }

    private void put(Aspect aspect, long count) {
        require(
            essentia.injectItems(new AEEssentiaStack(aspect, count), Actionable.MODULATE, new MachineSource(provider))
                == null,
            "inject essentia");
    }

    private long amount(Aspect aspect) {
        var stack = essentia.getStorageList().findPrecise(new AEEssentiaStack(aspect, 1));
        return stack == null ? 0 : stack.getStackSize();
    }

    private static ItemStack encode(ItemStack input, ItemStack output) {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ins = new NBTTagList(), outs = new NBTTagList();
        NBTTagCompound nativeIn = new NBTTagCompound(), nativeOut = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(input), nativeIn);
        Platform.writeStackNBT(AEItemStack.create(output), nativeOut);
        ins.appendTag(nativeIn);
        for (int i = 1; i < 16; i++) ins.appendTag(new NBTTagCompound());
        outs.appendTag(nativeOut);
        tag.setTag("in", ins);
        tag.setTag("out", outs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("CRUCIBLE_QA: " + message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof CoreScreen)) return;
        if (++frames == 40) {
            ScreenShotHelper.saveScreenshot(new File("."), "crucible-core-qa.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File("crucible-qa-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    /** Exercises the registered item renderer in a real client. */
    private static final class CoreScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Crucible Packaged Core - 100 craft QA", width / 2, height / 2 - 40,
                0xffffff);
            RenderHelper.enableGUIStandardItemLighting();
            new RenderItem().renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(),
                GTNGItemList.ThaumcraftCrucibleCore.get(1), width / 2 - 8, height / 2);
            RenderHelper.disableStandardItemLighting();
        }
    }
}
