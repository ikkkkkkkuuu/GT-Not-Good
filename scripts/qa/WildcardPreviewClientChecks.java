package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.PageButton;
import com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardDropWidget;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

/** Native prefix regression and real GUI rendering; excluded from the release artifact. */
@Mod(
    modid = "wildcardpreviewqa",
    name = "Wildcard Preview QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WildcardPreviewClientChecks {

    private static final OrePrefixes[][] BATCHES = {
        { OrePrefixes.pipeTiny, OrePrefixes.pipeSmall, OrePrefixes.pipeMedium, OrePrefixes.pipeLarge,
            OrePrefixes.pipeHuge, OrePrefixes.pipeQuadruple },
        { OrePrefixes.pipeNonuple, OrePrefixes.wireGt01, OrePrefixes.cableGt01, OrePrefixes.gem, OrePrefixes.lens,
            OrePrefixes.ingot },
        { OrePrefixes.pipeRestrictiveTiny, OrePrefixes.pipeRestrictiveSmall, OrePrefixes.pipeRestrictiveMedium,
            OrePrefixes.pipeRestrictiveLarge, OrePrefixes.pipeRestrictiveHuge } };

    private boolean started;
    private volatile boolean finished;
    private volatile int stage;
    private volatile int batch;
    private int ticks;
    private int closedAt;
    private int frames;
    private GuiContainerWrapper lastGui;
    private GuiContainerWrapper activeGui;
    private PageButton outputTab;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.wildcardPreview.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        if (stage >= 1 && mc.theWorld == null) {
            fail(new AssertionError("Wildcard preview disconnected during GUI stage " + stage));
            return;
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "wildcard-preview-qa-" + System.currentTimeMillis(),
                "Wildcard Preview QA",
                new WorldSettings(38L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++ticks > 1200)
                throw new AssertionError("Wildcard preview timed out: stage=" + stage + ", batch=" + batch);
            if (ticks == 1) verifyRegistry();
            if (stage == 0 && ticks >= 40 || stage == 3 && ticks - closedAt >= 20) {
                ItemStack pattern = GTNGItemList.WildcardPattern.get(1);
                WildcardModelState.ensureInitialized(pattern);
                WildcardModelState
                    .setInputs(pattern, Collections.singletonList(new PrefixIOComponent(OrePrefixes.ingot, 1)));
                List<IWildcardIOComponent> outputs = new ArrayList<>();
                for (int index = 0; index < BATCHES[batch].length; index++)
                    outputs.add(new PrefixIOComponent(BATCHES[batch][index], index + 1));
                WildcardModelState.setOutputs(pattern, outputs);
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, pattern);
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
                closedAt = ticks;
                stage = 4;
            } else if (stage == 4 && ticks - closedAt >= 20) {
                PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
                stage = 1;
            } else if (stage == 2) {
                player.closeScreen();
                if (++batch == BATCHES.length) finish("PASS");
                else {
                    closedAt = ticks;
                    stage = 3;
                }
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    /** Fails on the original Iron-only pipe preview before any cached or GUI results can hide the regression. */
    private static void verifyRegistry() {
        require(
            new PrefixIOComponent(OrePrefixes.pipeMedium, 1).getDisplayStack() != null,
            "pipeMedium has a representative preview in the native registry");
        for (OrePrefixes[] prefixes : BATCHES) {
            for (OrePrefixes prefix : prefixes) {
                PrefixIOComponent component = new PrefixIOComponent(prefix, 1);
                for (int amount : new int[] { 1, 7, 64, 129, Integer.MAX_VALUE }) {
                    component.setAmount(amount);
                    ItemStack display = component.getDisplayStack();
                    require(
                        display != null && display.getItem() != null && display.stackSize == amount,
                        prefix.name() + " preview preserves " + amount);
                    require(
                        WildcardMaterials.parseItem(display).prefix == prefix,
                        prefix.name() + " preview resolves its registered form");
                    ItemStack iron = component.apply(Materials.Iron);
                    if (iron != null) require(
                        ItemStack.areItemStacksEqual(iron, display),
                        prefix.name() + " keeps Iron when this form is available");
                    ItemStack expected = display.copy();
                    display.stackSize = 0;
                    display.setItemDamage(-1);
                    NBTTagCompound altered = new NBTTagCompound();
                    altered.setBoolean("qaAltered", true);
                    display.setTagCompound(altered);
                    require(
                        ItemStack.areItemStacksEqual(expected, component.getDisplayStack()),
                        prefix.name() + " returns independent preview copies");
                }
            }
        }
        PrefixIOComponent switched = new PrefixIOComponent(OrePrefixes.ingot, 2);
        switched.getDisplayStack();
        switched.setPrefix(OrePrefixes.pipeMedium);
        require(
            WildcardMaterials.parseItem(switched.getDisplayStack()).prefix == OrePrefixes.pipeMedium,
            "changing prefix invalidates the previous representative");
        switched.setRawText("gem");
        require(
            WildcardMaterials.parseItem(switched.getDisplayStack()).prefix == OrePrefixes.gem,
            "editing prefix text updates the representative");
        switched.setRawText("noSuchPrefix");
        require(
            switched.getDisplayStack() == null && switched.getRawText()
                .equals("noSuchPrefix"),
            "unknown prefix stays blank and keeps entered text");
        switched.setPrefix(null);
        require(
            switched.getDisplayStack() == null && PrefixIOComponent.empty()
                .getDisplayStack() == null,
            "empty prefixes stay blank");
        System.out.println("WILDCARD_PREVIEW_QA: native registry checks PASS");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage != 1) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper gui) || gui == lastGui) return;
        try {
            ModularPanel panel = gui.getScreen()
                .getMainPanel();
            if (!panel.getName()
                .equals("wildcard_pattern")) return;
            if (activeGui != gui) {
                activeGui = gui;
                frames = 0;
                outputTab = collect(panel, PageButton.class).stream()
                    .filter(tab -> tab.getIndex() == 2)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing actual OUT tab"));
                outputTab.onMousePressed(0);
            }
            if (++frames < 40) return;
            require(outputTab.isActive(), "actual wildcard OUT tab is selected");
            List<WildcardDropWidget> icons = collect(
                outputTab.getController()
                    .getActivePage(),
                WildcardDropWidget.class);
            require(icons.size() == BATCHES[batch].length, "actual output tab contains every configured icon");
            Field getter = WildcardDropWidget.class.getDeclaredField("getter");
            getter.setAccessible(true);
            for (int index = 0; index < icons.size(); index++) {
                ItemStack icon = (ItemStack) ((Supplier<?>) getter.get(icons.get(index))).get();
                require(
                    icon != null && icon.stackSize == index + 1
                        && WildcardMaterials.parseItem(icon).prefix == BATCHES[batch][index],
                    "actual output widget renders " + BATCHES[batch][index].name());
            }
            ScreenShotHelper.saveScreenshot(
                outputDirectory(),
                "wildcard-output-" + batch + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            lastGui = gui;
            stage = 2;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static <T> List<T> collect(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collect(root, type, matches);
        return matches;
    }

    private static <T> void collect(IWidget root, Class<T> type, List<T> matches) {
        if (type.isInstance(root)) matches.add(type.cast(root));
        for (IWidget child : root.getChildren()) collect(child, type, matches);
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.wildcardPreview.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("WILDCARD_PREVIEW_QA: " + message);
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
        System.out.println("WILDCARD_PREVIEW_QA: " + result);
        finished = true;
    }
}
