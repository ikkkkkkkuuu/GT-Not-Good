package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import WayofTime.alchemicalWizardry.ModBlocks;
import WayofTime.alchemicalWizardry.api.altarRecipeRegistry.AltarRecipe;
import WayofTime.alchemicalWizardry.api.altarRecipeRegistry.AltarRecipeRegistry;
import WayofTime.alchemicalWizardry.common.tileEntity.TEAltar;
import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in real altar, provider, restart and renderer checks using recipes confined to disposable QA worlds. */
@Mod(
    modid = "bloodaltarqa",
    name = "Blood Altar QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class BloodAltarClientChecks {

    private boolean started;
    private volatile boolean saveRequested;
    private volatile boolean finished;
    private String save;
    private boolean resume;
    private int ticks;
    private int frames;
    private TilePackagedProvider provider;
    private TEAltar altar;
    private ICraftingPatternDetails details;
    private InventoryCrafting input;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.blood.qa")) return;
        save = System.getProperty("gtng.blood.qa.resume", "");
        resume = !save.isEmpty();
        if (resume && !save.matches("blood-qa-[0-9]+")) throw new IllegalArgumentException("Not a QA save");
        AltarRecipeRegistry.altarRecipes
            .add(0, new AltarRecipe(new ItemStack(Items.emerald), new ItemStack(Items.brick), 1, 30, 10, 1, false));
        AltarRecipeRegistry.altarRecipes
            .add(0, new AltarRecipe(new ItemStack(Items.diamond), new ItemStack(Items.emerald), 1, 30, 10, 1, false));
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (saveRequested) {
            saveRequested = false;
            Files.write(new File("blood-qa-resume.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
            Files.write(new File("blood-qa-result.txt").toPath(), "SAVED".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        } else if (finished) {
            if (!(mc.currentScreen instanceof CoreScreen)) mc.displayGuiScreen(new CoreScreen());
        } else if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            if (!resume) save = "blood-qa-" + System.currentTimeMillis();
            mc.launchIntegratedServer(
                save,
                "Blood Altar QA",
                resume ? null : new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || saveRequested) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        ticks++;
        if (ticks == 1) {
            if (!resume) {
                world.setBlock(
                    4,
                    8,
                    4,
                    net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
                world.setBlock(
                    4,
                    8,
                    5,
                    AEApi.instance()
                        .definitions()
                        .blocks()
                        .energyCellCreative()
                        .maybeBlock()
                        .get());
                world.setBlock(8, 8, 4, ModBlocks.blockAltar);
                player.playerNetServerHandler.setPlayerLocation(4.5, 10, 1.5, 0, 15);
                player.capabilities.isFlying = true;
            }
            provider = (TilePackagedProvider) world.getTileEntity(4, 8, 4);
            altar = (TEAltar) world.getTileEntity(8, 8, 4);
            require(altar instanceof BloodAltarAccess, "optional late mixin applied");
            provider.setOwnerName(player.getCommandSenderName());
            if (!resume) {
                provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.BloodAltarCore.get(1));
                require(provider.bind(new PackagedTarget(world.provider.dimensionId, 8, 8, 4, 1)), "bind altar");
                require(
                    !provider.bind(new PackagedTarget(world.provider.dimensionId, 8, 8, 4, 2)),
                    "duplicate face rejected");
                provider.setInventorySlotContents(0, pattern());
                provider.autoReturn = false;
            }
        }
        if (ticks == 80) {
            require(
                provider.getProxy()
                    .isActive(),
                "powered provider");
            if (resume) {
                require(provider.queuedJobs() == 1, "receipt survived process restart");
                require(is(altar.getStackInSlot(0), Items.emerald), "real result survived process restart");
                int blood = altar.getCurrentBlood();
                altar.startCycle();
                require(!altar.isActive() && altar.getCurrentBlood() == blood, "persisted completion stop");
                for (int slot = TilePackagedProvider.PATTERNS; slot < TilePackagedProvider.CORE; slot++) {
                    provider.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
                }
                provider.autoReturn = true;
            } else {
                provider.provideCrafting(
                    (ICraftingProviderHelper) Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] { ICraftingProviderHelper.class },
                        (proxy, method, args) -> {
                            if (method.getName()
                                .equals("addCraftingOption")) details = (ICraftingPatternDetails) args[1];
                            return null;
                        }));
                require(details != null, "processing pattern advertised");
                input = new InventoryCrafting(new appeng.container.ContainerNull(), 3, 3);
                input.setInventorySlotContents(0, new ItemStack(Items.brick));
                checkPlanning();
                altar.setInventorySlotContents(0, new ItemStack(Items.stick));
                var adapter = PackagedCoreRegistry.get(GTNGItemList.BloodAltarCore.get(1));
                require(
                    adapter.dispatch(provider, provider.targets.get(0), details, input) == null,
                    "occupied altar rejected");
                require(is(altar.getStackInSlot(0), Items.stick), "rejection preserves inventory");
                altar.setInventorySlotContents(0, null);
                require(provider.pushPattern(details, input), "dispatch without blood accepted");
                require(!provider.pushPattern(details, input), "second dispatch rejected");
                require(!provider.releaseInterrupted(0), "starved job cannot be released");
                require(
                    net.minecraft.item.crafting.CraftingManager.getInstance()
                        .getRecipeList()
                        .stream()
                        .anyMatch(
                            recipe -> TilePackagedProvider
                                .sameItem(recipe.getRecipeOutput(), GTNGItemList.BloodAltarCore.get(1))),
                    "core recipe registered");
            }
        }
        if (!resume && ticks == 200) {
            require(is(altar.getStackInSlot(0), Items.brick), "no LP means no product");
            altar.fillMainTank(1000);
        }
        if (!resume && ticks == 750) {
            require(is(altar.getStackInSlot(0), Items.emerald), "real native recipe completed and result held");
            require(altar.getCurrentBlood() < 1000, "real LP consumed");
            altar.startCycle();
            require(!altar.isActive(), "next recipe cannot start");
            NBTTagCompound tag = new NBTTagCompound();
            altar.writeToNBT(tag);
            require(tag.hasKey("GTNGBloodAltarResult"), "persistent stop written");
            saveRequested = true;
        }
        if (resume && ticks == 120) {
            require(
                is(altar.getStackInSlot(0), Items.emerald) && provider.queuedJobs() == 1,
                "full return inventory retains product and receipt");
            provider.setInventorySlotContents(TilePackagedProvider.PATTERNS, null);
        }
        if (resume && ticks == 160) {
            require(altar.getStackInSlot(0) == null && provider.queuedJobs() == 0, "exact-once collection");
            require(
                is(provider.getStackInSlot(TilePackagedProvider.PATTERNS), Items.emerald),
                "network without storage retains returned output");
            NBTTagCompound tag = new NBTTagCompound();
            altar.writeToNBT(tag);
            require(!tag.hasKey("GTNGBloodAltarResult"), "collection clears completion stop");
            altar.setInventorySlotContents(0, new ItemStack(Items.emerald));
            altar.startCycle();
            require(altar.isActive(), "manual crafting works after collection");
            altar.setInventorySlotContents(0, null);
            altar.setActive();
            System.out.println("BLOOD_QA: all runtime checks passed");
            finished = true;
        }
        require(ticks < 1000, "runtime timeout");
    }

    private void checkPlanning() {
        require(BloodAltarAdapter.matchResult(new ItemStack(Items.brick), 0, details) == null, "tier rejected");
        require(
            BloodAltarAdapter.matchResult(new ItemStack(Items.emerald), 1, details) == null,
            "wrong output rejected");
        require(
            BloodAltarAdapter.matchResult(new ItemStack(Items.brick, 2), 1, details) == null,
            "wrong count rejected");
        input.setInventorySlotContents(1, new ItemStack(Items.stick));
        require(BloodAltarAdapter.planInput(input) == null, "mixed input rejected");
        input.setInventorySlotContents(1, null);
        var nativeInput = new appeng.util.inv.MEInventoryCrafting(new appeng.container.ContainerNull(), 2, 1);
        nativeInput.setInventorySlotContents(0, appeng.util.item.AEItemStack.create(new ItemStack(Items.brick)));
        nativeInput.setInventorySlotContents(
            1,
            appeng.util.item.AEFluidStack
                .create(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 100)));
        require(BloodAltarAdapter.planInput(nativeInput) == null, "native fluid rejected atomically");
        require(input.getStackInSlot(0).stackSize == 1, "planning preserves dispatch input");
    }

    private static ItemStack pattern() {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ins = new NBTTagList();
        ins.appendTag(new ItemStack(Items.brick).writeToNBT(new NBTTagCompound()));
        NBTTagList outs = new NBTTagList();
        outs.appendTag(new ItemStack(Items.emerald).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", ins);
        tag.setTag("out", outs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static boolean is(ItemStack stack, net.minecraft.item.Item item) {
        return stack != null && stack.getItem() == item && stack.stackSize == 1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("BLOOD_QA: " + message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        var mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof CoreScreen)) return;
        if (++frames == 40) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "blood-core-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File("blood-qa-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    /** Renders the registered core with the existing upstream overlay layout. */
    private static final class CoreScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Blood Altar Packaged Core", width / 2, height / 2 - 40, 0xffffff);
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            new RenderItem().renderItemAndEffectIntoGUI(
                fontRendererObj,
                mc.getTextureManager(),
                GTNGItemList.BloodAltarCore.get(1),
                width / 2 - 8,
                height / 2);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        }
    }
}
