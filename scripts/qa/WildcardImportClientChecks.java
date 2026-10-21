package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardPatternGui;
import com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardPatternGui.RecipeImportHandler;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardRecipeTemplate;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "wildcardimportqa",
    name = "Wildcard Import QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WildcardImportClientChecks {

    private static final Fluid FIXED_FLUID = new Fluid("wildcard_import_qa_fixed");

    private boolean started;
    private volatile boolean finished;
    private volatile int stage;
    private volatile boolean unsavedChecked;
    private int ticks;
    private int transitionTick;
    private int frames;
    private GuiContainerWrapper origin;
    private GuiOverlayButton importButton;
    private GTRecipe fixture;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.wildcardImport.qa")) return;
        FluidRegistry.registerFluid(FIXED_FLUID);
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
        mc.gameSettings.pauseOnLostFocus = false;
        if (finished) {
            mc.shutdown();
            return;
        }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("wildcard-import-qa-" + System.currentTimeMillis(), "Wildcard Import QA",
                new WorldSettings(38L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++ticks > 1200) throw new AssertionError("Wildcard import timed out: stage=" + stage);
            if (stage == 0 && ticks >= 40) {
                verifyTemplates();
                ItemStack pattern = GTNGItemList.WildcardPattern.get(1);
                WildcardModelState.ensureInitialized(pattern);
                WildcardModelState.setInputs(pattern,
                    Collections.singletonList(new SimpleIOComponent(new ItemStack(Items.stick), 7)));
                WildcardModelState.setOutputs(pattern,
                    Collections.singletonList(new SimpleIOComponent(new ItemStack(Items.apple), 9)));
                WildcardModelState.setFilters(pattern,
                    Collections.singletonList(new StringFilterComponent(Materials.Iron.mName, true, true)));
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, pattern);
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
                transitionTick = ticks;
                stage = 10;
            } else if (stage == 10 && ticks - transitionTick >= 20) {
                PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
                stage = 1;
            } else if (stage == 4 && !unsavedChecked) {
                List<IWildcardIOComponent> original = WildcardModelState.getInputs(player.getHeldItem());
                require(
                    original.size() == 1 && original.get(0) instanceof SimpleIOComponent
                        && ((SimpleIOComponent) original.get(0)).getAmount() == 7,
                    "NEI import edits the client draft without replacing the server pattern before Save");
                unsavedChecked = true;
            } else if (stage == 6) {
                if (ticks - transitionTick < 20) return;
                verifyComponents(WildcardModelState.getInputs(player.getHeldItem()),
                    WildcardModelState.getOutputs(player.getHeldItem()));
                require(WildcardModelState.getFilters(player.getHeldItem()).size() == 1,
                    "Save retains the existing material filter");
                player.closeScreen();
                transitionTick = ticks;
                stage = 7;
            } else if (stage == 7 && ticks - transitionTick >= 20) {
                PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
                frames = 0;
                stage = 8;
            } else if (stage == 9) {
                player.closeScreen();
                finish("PASS");
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void buttons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        for (GuiRecipeButton button : event.buttonList) {
            if (
                button instanceof GuiOverlayButton overlay && overlay.firstGui == origin
                    && overlay.getClass().getName().contains("WildcardRecipeImport")
            ) importButton = overlay;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 1 && mc.currentScreen instanceof GuiContainerWrapper gui) {
                if (++frames < 30) return;
                origin = gui;
                fixture = fixture();
                GTNEIDefaultHandler handler = new GTNEIDefaultHandler(
                    RecipeMaps.assemblerRecipes.getDefaultRecipeCategory());
                handler.arecipes.add(handler.new CachedDefaultRecipe(fixture));
                Constructor<GuiCraftingRecipe> constructor = GuiCraftingRecipe.class
                    .getDeclaredConstructor(ArrayList.class);
                constructor.setAccessible(true);
                ArrayList<ICraftingHandler> handlers = new ArrayList<>();
                handlers.add(handler);
                stage = 2;
                frames = 0;
                mc.displayGuiScreen(constructor.newInstance(handlers));
            } else if (stage == 2 && ++frames >= 30) {
                require(importButton != null && importButton.enabled,
                    "actual NEI recipe page replaces and enables its + button for the bound wildcard GUI");
                require(!importButton.handleTooltip(new ArrayList<>()).isEmpty(), "NEI import has a localized tooltip");
                screenshot("wildcard-nei-import.png");
                importButton.mouseReleased(importButton.xPosition + 2, importButton.yPosition + 2);
                require(mc.currentScreen == origin, "NEI + returns to the originating wildcard GUI");
                frames = 0;
                stage = 3;
            } else if (stage == 3 && ++frames >= 30) {
                verifyDraft(origin);
                require(tab(origin, 1).isActive(), "import selects the actual IN tab");
                screenshot("wildcard-import-input.png");
                stage = 4;
                frames = 0;
            } else if (stage == 4 && unsavedChecked) {
                tab(origin, 2).onMousePressed(0);
                frames = 0;
                stage = 5;
            } else if (stage == 5 && ++frames >= 30) {
                screenshot("wildcard-import-output.png");
                IWidget page = tab(origin, 2).getController().getActivePage();
                ButtonWidget<?> save = collect(page, ButtonWidget.class).stream()
                    .filter(button -> button.getOverlay() instanceof IKey key
                        && key.get().equals(StatCollector.translateToLocal("gui.wildcardpattern.save")))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing actual Save button"));
                save.onMousePressed(0);
                transitionTick = ticks;
                stage = 6;
            } else if (stage == 8 && mc.currentScreen instanceof GuiContainerWrapper gui && gui != origin) {
                if (++frames < 30) return;
                verifyDraft(gui);
                require(fixture.mInputs[1].stackSize == 0, "NEI import preserves the original non-consumable circuit");
                stage = 9;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static GTRecipe fixture() {
        FIXED_FLUID.setIcons(FluidRegistry.WATER.getIcon(), FluidRegistry.WATER.getIcon());
        ItemStack circuit = GTUtility.getIntegratedCircuit(2);
        circuit.stackSize = 0;
        FluidStack auxiliary = new FluidStack(FIXED_FLUID, 1234);
        auxiliary.tag = new NBTTagCompound();
        auxiliary.tag.setString("wildcardImportQA", "preserved");
        return new GTRecipe(false,
            new ItemStack[] { WildcardMaterials.makePrefixStack(OrePrefixes.ingot, Materials.Iron, 2), circuit },
            new ItemStack[] { WildcardMaterials.makePrefixStack(OrePrefixes.plate, Materials.Iron, 3) }, null, null,
            null, null, null, new FluidStack[] { auxiliary, Materials.Iron.getMolten(288) },
            new FluidStack[] { Materials.Iron.getMolten(432) }, 20, 8, 0);
    }

    private static void verifyTemplates() {
        ItemStack ingot = WildcardMaterials.makePrefixStack(OrePrefixes.ingot, Materials.Iron, 3);
        ItemStack dust = WildcardMaterials.makePrefixStack(OrePrefixes.dust, Materials.Copper, 7);
        ItemStack plate = WildcardMaterials.makePrefixStack(OrePrefixes.plate, Materials.Iron, 2);
        WildcardRecipeTemplate template = WildcardRecipeTemplate.create(Arrays.asList(ingot, dust),
            Collections.singletonList(plate));
        requirePrefix(template.getInputs().get(0), OrePrefixes.ingot, 3);
        requirePrefix(template.getInputs().get(1), OrePrefixes.dust, 7);
        requirePrefix(template.getOutputs().get(0), OrePrefixes.plate, 2);
        require(true, "all material slots are wildcards even when source materials differ");

        String addonMaterial = "WildcardImportQATestium";
        OreDictionary.registerOre("ingot" + addonMaterial, new ItemStack(Items.apple));
        OreDictionary.registerOre("ingot" + addonMaterial, new ItemStack(Items.carrot));
        OreDictionary.registerOre("plate" + addonMaterial, new ItemStack(Items.paper));
        template = WildcardRecipeTemplate.create(Collections.singletonList(new ItemStack(Items.carrot, 5)),
            Collections.singletonList(new ItemStack(Items.paper, 2)));
        requirePrefix(template.getInputs().get(0), OrePrefixes.ingot, 5);
        requirePrefix(template.getOutputs().get(0), OrePrefixes.plate, 2);
        require(WildcardMaterials.findByName(addonMaterial) == Materials._NULL,
            "addon material without a GT Materials entry accepts an equivalent ore dictionary alternative");

        ItemStack nonConsumable = ingot.copy();
        nonConsumable.stackSize = 0;
        template = WildcardRecipeTemplate.create(Collections.singletonList(nonConsumable),
            Collections.singletonList(plate));
        require(
            template.getInputs().get(0) instanceof SimpleIOComponent sample && sample.getAmount() == 1
                && sample.getStack().stackSize == 1
                && sample.getStack().isItemEqual(ingot)
                && nonConsumable.stackSize == 0,
            "non-consumable material samples stay fixed without changing the recipe");

        ItemStack tagged = ingot.copy();
        tagged.stackSize = 11;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("grade", "special");
        tagged.setTagCompound(tag);
        template = WildcardRecipeTemplate.create(Collections.singletonList(tagged), Collections.singletonList(plate));
        require(template.getInputs().get(0) instanceof SimpleIOComponent,
            "material items with essential NBT remain fixed");
        SimpleIOComponent fixed = (SimpleIOComponent) template.getInputs().get(0);
        tagged.getTagCompound().setString("grade", "changed");
        require(fixed.getAmount() == 11 && fixed.getStack().getTagCompound().getString("grade").equals("special"),
            "fixed sample keeps an independent NBT copy and its exact quantity");

        expectRejected(Collections.emptyList(), Collections.singletonList(plate));
        expectRejected(Collections.singletonList(ingot), Collections.emptyList());
        expectRejected(Collections.nCopies(7, ingot), Collections.singletonList(plate));
        expectRejected(Collections.singletonList(ingot), Collections.nCopies(7, plate));
        ItemStack zeroOutput = plate.copy();
        zeroOutput.stackSize = 0;
        expectRejected(Collections.singletonList(ingot), Collections.singletonList(zeroOutput));
        ItemStack negativeInput = ingot.copy();
        negativeInput.stackSize = -1;
        expectRejected(Collections.singletonList(negativeInput), Collections.singletonList(plate));
        require(true, "empty, oversized and invalid recipes are rejected without truncation");
    }

    private static void requirePrefix(IWildcardIOComponent component, OrePrefixes prefix, int amount) {
        require(component instanceof PrefixIOComponent form && form.getPrefix() == prefix && form.getAmount() == amount,
            "material wildcard retains " + prefix.name() + " x" + amount);
    }

    private static void expectRejected(List<ItemStack> inputs, List<ItemStack> outputs) {
        try {
            WildcardRecipeTemplate.create(inputs, outputs);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected an invalid recipe to be rejected");
    }

    @SuppressWarnings("unchecked")
    private static void verifyDraft(GuiContainerWrapper gui) throws Exception {
        RecipeImportHandler target = gui.getScreen().getSyncManager().getMainPSM()
            .findSyncHandlerNullable("wildcardImport", RecipeImportHandler.class);
        require(target != null && target.isValid(), "wildcard import target remains bound");
        Field owner = RecipeImportHandler.class.getDeclaredField("gui");
        owner.setAccessible(true);
        WildcardPatternGui config = (WildcardPatternGui) owner.get(target);
        Field inputs = WildcardPatternGui.class.getDeclaredField("inputs");
        Field outputs = WildcardPatternGui.class.getDeclaredField("outputs");
        Field filters = WildcardPatternGui.class.getDeclaredField("filters");
        inputs.setAccessible(true);
        outputs.setAccessible(true);
        filters.setAccessible(true);
        verifyComponents((List<IWildcardIOComponent>) inputs.get(config),
            (List<IWildcardIOComponent>) outputs.get(config));
        require(((List<?>) filters.get(config)).size() == 1, "NEI import retains the material filter in the draft");
    }

    private static void verifyComponents(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs) {
        require(inputs.size() == 4 && outputs.size() == 2, "all item and fluid inputs and outputs are retained");
        require(inputs.get(0) instanceof PrefixIOComponent ingot && ingot.getPrefix() == OrePrefixes.ingot
            && ingot.getAmount() == 2, "material input becomes an ingot wildcard with its exact count");
        require(
            inputs.get(1) instanceof SimpleIOComponent circuit && circuit.getAmount() == 1
                && circuit.getStack().isItemEqual(GTUtility.getIntegratedCircuit(2)),
            "zero-sized integrated circuit becomes one fixed circuit with its original number");
        require(outputs.get(0) instanceof PrefixIOComponent plate && plate.getPrefix() == OrePrefixes.plate
            && plate.getAmount() == 3, "material output becomes a plate wildcard with its exact count");
        require(amount(inputs.get(2)) == 1234 && amount(inputs.get(3)) == 288 && amount(outputs.get(1)) == 432,
            "native fluid amounts survive NEI import and saving without conversion to item counts");
        require(
            inputs.get(2) instanceof SimpleIOComponent fixed
                && FluidDropCompat.getFluidStack(fixed.getStack()).getFluid() == FIXED_FLUID,
            "unknown auxiliary fluid remains fixed");
        FluidStack auxiliary = FluidDropCompat.getFluidStack(inputs.get(2).apply(Materials.Iron));
        require(
            auxiliary != null && auxiliary.tag != null
                && auxiliary.tag.getString("wildcardImportQA").equals("preserved"),
            "fixed auxiliary fluid retains its native fluid NBT after expansion and saving");
        require(inputs.get(3) instanceof FluidIOComponent && outputs.get(1) instanceof FluidIOComponent,
            "molten material inputs and outputs become fluid wildcards");
    }

    private static long amount(IWildcardIOComponent component) {
        if (component instanceof FluidIOComponent fluid) return fluid.getAmount();
        if (component instanceof SimpleIOComponent fixed) {
            require(FluidDropCompat.getFluidStack(fixed.getStack()) != null, "fixed fluid keeps its fluid identity");
            return fixed.getAmount();
        }
        throw new AssertionError("Expected a fluid component");
    }

    private static PageButton tab(GuiContainerWrapper gui, int index) {
        return collect(gui.getScreen().getMainPanel(), PageButton.class).stream()
            .filter(button -> button.getIndex() == index).findFirst()
            .orElseThrow(() -> new AssertionError("Missing tab " + index));
    }

    private static <T> List<T> collect(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) matches.add(type.cast(root));
        for (IWidget child : root.getChildren()) matches.addAll(collect(child, type));
        return matches;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(outputDirectory(), name, mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.wildcardImport.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("WILDCARD_IMPORT_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finish("FAIL: " + failure);
    }

    private void finish(String result) {
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("WILDCARD_IMPORT_QA: " + result);
        finished = true;
    }
}
