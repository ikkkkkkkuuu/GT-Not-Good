package com.xyp.gtnotgood.common.packaged;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;

/** Opt-in Forge runtime checks for ingredient planning, the late accessor, recipes and core rendering. */
@Mod(
    modid = "assemblycoreqa",
    name = "Assembly Core QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class AssemblyLineClientChecks {

    private boolean started;
    private int frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.assembly.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || started || mc.currentScreen == null) return;
        started = true;
        checkPlanning();
        require(new ggfab.mte.MTEAdvAssLine("qa") instanceof AssemblyLineDataAccess, "late data accessor");
        for (var entry : new GTNGItemList[] { GTNGItemList.AssemblyLineCore, GTNGItemList.AdvancedAssemblyLineCore }) {
            require(PackagedCoreRegistry.get(entry.get(1)) != null, "adapter registration");
            require(
                net.minecraft.item.crafting.CraftingManager.getInstance()
                    .getRecipeList()
                    .stream()
                    .anyMatch(recipe -> TilePackagedProvider.sameItem(recipe.getRecipeOutput(), entry.get(1))),
                "recipe");
        }
        mc.displayGuiScreen(new CoreScreen());
    }

    static void checkPlanning() {
        var buffer = new net.minecraft.inventory.InventoryBasic("prefetch", false, 3);
        ItemStack batch = new ItemStack(Items.iron_ingot, 64);
        buffer.setInventorySlotContents(0, batch.copy());
        require(AssemblyLineAdapter.insertionSlot(buffer, batch) == 1, "full lane stack uses next input slot");
        require(
            buffer.getStackInSlot(0).stackSize == 64 && buffer.getStackInSlot(1) == null,
            "buffer planning does not mutate existing material");
        buffer.setInventorySlotContents(2, new ItemStack(Items.gold_ingot));
        require(AssemblyLineAdapter.insertionSlot(buffer, batch) < 0, "foreign lane material rejected atomically");
        buffer.setInventorySlotContents(2, null);
        buffer.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 63));
        require(
            AssemblyLineAdapter.insertionSlot(buffer, new ItemStack(Items.iron_ingot, 3)) < 0,
            "nondivisible ingredients cannot compact into an unusable short first stack");
        checkReceiptPersistence();
        InventoryCrafting input = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        ItemStack iron = new ItemStack(Items.iron_ingot, 2);
        ItemStack gold = new ItemStack(Items.gold_ingot, 2);
        RecipeAssemblyLine recipe = new RecipeAssemblyLine(
            null,
            0,
            0,
            new ItemStack[] { iron, iron },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 100), new FluidStack(FluidRegistry.WATER, 200) },
            new ItemStack(Items.diamond),
            100,
            30,
            new ItemStack[][] { { iron, gold }, null });
        input.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 2));
        input.setInventorySlotContents(1, new ItemStack(Items.gold_ingot, 2));
        ItemStack[] plan = AssemblyLineAdapter.planItems(recipe, input);
        require(
            plan != null && plan[0].getItem() == Items.gold_ingot && plan[1].getItem() == Items.iron_ingot,
            "alternatives backtrack to reserve iron for the restricted lane");
        require(input.getStackInSlot(0).stackSize == 2, "planning does not consume input");
        input.setInventorySlotContents(1, new ItemStack(Items.iron_ingot, 2));
        plan = AssemblyLineAdapter.planItems(recipe, input);
        require(
            plan != null && plan[0].stackSize == 2 && plan[1].stackSize == 2,
            "duplicate input slots merge and split");
        input.setInventorySlotContents(1, new ItemStack(Items.iron_ingot));
        require(AssemblyLineAdapter.planItems(recipe, input) == null, "shortage rejected");
        input.setInventorySlotContents(1, new ItemStack(Items.iron_ingot, 3));
        require(AssemblyLineAdapter.planItems(recipe, input) == null, "surplus rejected");
        input.setInventorySlotContents(2, FluidDropCompat.newStack(new FluidStack(FluidRegistry.WATER, 300)));
        require(AssemblyLineAdapter.matchesFluids(recipe, input), "fluid split across hatches");
        require(input.getStackInSlot(2).stackSize == 300, "fluid planning preserves input");
        input.setInventorySlotContents(2, FluidDropCompat.newStack(new FluidStack(FluidRegistry.WATER, 301)));
        require(!AssemblyLineAdapter.matchesFluids(recipe, input), "extra fluid rejected");
        input.setInventorySlotContents(2, FluidDropCompat.newStack(new FluidStack(FluidRegistry.LAVA, 300)));
        require(!AssemblyLineAdapter.matchesFluids(recipe, input), "wrong fluid rejected");
        var nativeInput = new appeng.util.inv.MEInventoryCrafting(new appeng.container.ContainerNull(), 3, 1);
        nativeInput
            .setInventorySlotContents(0, appeng.util.item.AEItemStack.create(new ItemStack(Items.iron_ingot, 4)));
        nativeInput.setInventorySlotContents(
            1,
            appeng.util.item.AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 300)));
        require(
            AssemblyLineAdapter.planItems(recipe, nativeInput) != null,
            "native AE fluid packets are not item ingredients");
        require(AssemblyLineAdapter.matchesFluids(recipe, nativeInput), "native AE fluid payload matches recipe");
        require(
            nativeInput.getAEStackInSlot(1)
                .getStackSize() == 300,
            "native fluid planning preserves CPU ownership");
        nativeInput.setInventorySlotContents(
            1,
            appeng.util.item.AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 299)));
        require(!AssemblyLineAdapter.matchesFluids(recipe, nativeInput), "native fluid shortage rejected");
        System.out.println("ASSEMBLY_QA: planning checks passed");
    }

    /** Checks that multiple in-flight batches survive saves and cannot mix patterns, including legacy receipts. */
    private static void checkReceiptPersistence() {
        var adapter = new AssemblyLineAdapter(true);
        if (!PackagedCoreRegistry.entries()
            .containsKey("qa_receipt")) PackagedCoreRegistry.register("qa_receipt", adapter);
        else adapter = (AssemblyLineAdapter) PackagedCoreRegistry.entries()
            .get("qa_receipt");
        var tag = new net.minecraft.nbt.NBTTagCompound();
        var saved = new net.minecraft.nbt.NBTTagList();
        var target = new PackagedTarget(0, 0, 10, 0, 2);
        ItemStack pattern = new ItemStack(Items.paper);
        for (int i = 0; i < 3; i++) {
            var entry = target.write();
            entry.setString("Core", "qa_receipt");
            entry.setTag("Expected", new ItemStack(Items.diamond).writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            entry.setTag("Pattern", pattern.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            saved.appendTag(entry);
        }
        tag.setTag("GTNGJobs", saved);
        var provider = new TilePackagedProvider();
        provider.readFromNBT(tag);
        require(provider.queuedJobs() == 3, "all receipts for one target restored");
        require(provider.canQueue(target, adapter, pattern), "identical recipe can prefetch after reload");
        require(!provider.canQueue(target, adapter, new ItemStack(Items.book)), "different recipe rejected");
        var roundTrip = new net.minecraft.nbt.NBTTagCompound();
        provider.writeToNBT(roundTrip);
        provider.readFromNBT(roundTrip);
        require(provider.queuedJobs() == 3 && provider.canQueue(target, adapter, pattern), "receipt round trip");
        saved.getCompoundTagAt(0)
            .removeTag("Pattern");
        provider.readFromNBT(tag);
        require(!provider.canQueue(target, adapter, pattern), "legacy receipt drains before prefetch");
    }

    /** Exercises the server-side link action on disposable real GT controllers, leaving the user's lines untouched. */
    static void checkInterruption(net.minecraft.world.World world, EntityPlayer player) {
        int x = 18, y = 20, z = -8;
        require(world.isAirBlock(x, y, z), "interruption fixture position is empty");
        for (boolean advanced : new boolean[] { false, true }) {
            ItemStack controller = advanced ? ggfab.GGItemList.AdvAssLine.get(1)
                : gregtech.api.enums.ItemList.Machine_Multi_Assemblyline.get(1);
            world.setBlock(x, y, z, gregtech.api.GregTechAPI.sBlockMachines, 0, 3);
            var base = (gregtech.api.metatileentity.BaseMetaTileEntity) world.getTileEntity(x, y, z);
            try {
                base.setInitialValuesAsNBT(null, (short) controller.getItemDamage());
                base.setOwnerName(player.getCommandSenderName());
                base.setOwnerUuid(player.getUniqueID());
                var line = (gregtech.api.metatileentity.implementations.MTEMultiBlockBase) base.getMetaTileEntity();
                line.mProgresstime = 10;
                line.mMaxProgresstime = 100;
                line.setInventorySlotContents(1, new ItemStack(Items.paper));
                base.enableWorking();
                var provider = new TilePackagedProvider();
                var tag = new net.minecraft.nbt.NBTTagCompound();
                var target = new PackagedTarget(0, x, y, z, 2);
                var bindings = new net.minecraft.nbt.NBTTagList();
                bindings.appendTag(target.write());
                tag.setTag("WirelessConnections", bindings);
                var receipts = new net.minecraft.nbt.NBTTagList();
                for (int i = 0; i < 2; i++) {
                    var entry = target.write();
                    entry.setString("Core", advanced ? "advanced_assembly_line" : "assembly_line");
                    entry.setTag(
                        "Expected",
                        new ItemStack(Items.diamond).writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
                    receipts.appendTag(entry);
                }
                tag.setTag("GTNGJobs", receipts);
                provider.readFromNBT(tag);
                provider.setWorldObj(world);
                provider.setOwnerName(player.getCommandSenderName());
                require(provider.releaseInterrupted(0), "explicit interruption accepted");
                require(
                    provider.queuedJobs() == 0 && provider.connections()
                        .size() == 1,
                    "all prefetched receipts released while binding survives");
                require(
                    !base.isAllowedToWork() && line.mMaxProgresstime == 0 && line.mProgresstime == 0,
                    "real GT shutdown stops controller");
                require(line.getStackInSlot(1) != null, "shutdown preserves unconsumed inventory");
                require(!provider.releaseInterrupted(0), "repeated release does not change state");
            } finally {
                base.setInventorySlotContents(1, null);
                world.setBlockToAir(x, y, z);
            }
        }
        System.out.println("ASSEMBLY_QA: interruption checks passed");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof CoreScreen)) return;
        if (++frames == 40) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "assembly-cores-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            java.nio.file.Files.write(
                new File("assembly-qa-result.txt").toPath(),
                "PASS".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("ASSEMBLY_QA: PASS");
            mc.shutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("ASSEMBLY_QA: " + message);
    }

    /** Uses the actual registered item renderers in a client GUI. */
    private static final class CoreScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Assembly Line Packaged Cores", width / 2, height / 2 - 55, 0xffffff);
            RenderItem renderer = new RenderItem();
            var entries = new GTNGItemList[] { GTNGItemList.AssemblyLineCore, GTNGItemList.AdvancedAssemblyLineCore };
            for (int i = 0; i < entries.length; i++) {
                ItemStack stack = entries[i].get(1);
                int y = height / 2 - 20 + i * 32;
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                renderer.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, width / 2 - 145, y);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
                drawString(fontRendererObj, stack.getDisplayName(), width / 2 - 120, y + 4, 0xffffff);
            }
        }
    }
}
