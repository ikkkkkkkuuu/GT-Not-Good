package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.util.GTUtil;
import com.xyp.gtnotgood.config.Config;

import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.slot.AppEngSlot;
import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.ConfigTag;
import codechicken.nei.NEIClientConfig;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;

final class InterfaceSuffixButtonClientChecks {

    private InterfaceSuffixButtonClientChecks() {}

    static void run(GuiQuickEncodingTerminal gui) throws Exception {
        require(Boolean.getBoolean("gtng.terminalScroll.qa"), "suffix checks require the isolated QA client");
        ConfigTag suffix = NEIClientConfig.global.config
            .getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE);
        ConfigTag ignoreCircuit = NEIClientConfig.global.config
            .getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT);
        String originalSuffix = suffix.getValue();
        String originalIgnoreCircuit = ignoreCircuit.getValue();
        String originalTheme = Config.quickTerminalButtonTheme;
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack originalCursor = mc.thePlayer.inventory.getItemStack();
        try {
            ignoreCircuit.setBooleanValue(false);
            NEI_TH_Config.setConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE, true);
            GuiTabButton button = button(gui);
            checkLayout(gui, button);
            String onMessage = checkMessage(button, true);
            checkRecipeNames(true);
            checkDiskValue(true);

            ItemStack carried = new ItemStack(Items.diamond, 17);
            carried.setStackDisplayName("Suffix button cursor check");
            mc.thePlayer.inventory.setItemStack(carried);
            click(gui, button, 0);
            require(!NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE),
                "suffix button disables the shared global setting");
            String offMessage = checkMessage(button, false);
            require(!onMessage.equals(offMessage), "tooltip changes with the suffix setting");
            checkRecipeNames(false);
            checkDiskValue(false);
            screenshot(gui, button, "suffix-off.png");

            click(gui, button, 1);
            require(NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE),
                "second suffix click restores the enabled state");
            require(button.getMessage().equals(onMessage), "second click restores the enabled tooltip");
            checkDiskValue(true);
            screenshot(gui, button, "suffix-on.png");

            click(gui, button, 0, true);
            click(gui, button, 1, true);
            require(NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE),
                "left and right drag-release checks restore the enabled suffix state");

            gui.setWorldAndResolution(Minecraft.getMinecraft(), gui.width, gui.height);
            GuiTabButton rebuilt = button(gui);
            require(rebuilt != button, "GUI initialization recreates the suffix button");
            checkLayout(gui, rebuilt);
            require(rebuilt.getMessage().equals(onMessage), "rebuilt GUI retains the global suffix state");
            suffix.setBooleanValue(false);
            require(rebuilt.getMessage().equals(offMessage), "NEI changes immediately update the existing button");
            suffix.setBooleanValue(true);
            require(rebuilt.getMessage().equals(onMessage), "NEI changes restore the existing button state");
            System.out.println(
                "TERMINAL_SUFFIX_QA: layout, left/right click, cursor, tooltip, rebuild, NEI, disk and NC suffixes PASS");
        } finally {
            Config.quickTerminalButtonTheme = originalTheme;
            mc.thePlayer.inventory.setItemStack(originalCursor);
            suffix.setValue(originalSuffix);
            ignoreCircuit.setValue(originalIgnoreCircuit);
        }
    }

    private static void checkLayout(GuiQuickEncodingTerminal gui, GuiTabButton button) throws Exception {
        GuiButton switchButton = (GuiButton) field(gui, "switchToCraftingButton");
        require(button.visible && button.isVisible() && button.enabled, "suffix button is visible and enabled");
        require(button.width == 16 && button.height == 16 && button.getWidth() == 16 && button.getHeight() == 16,
            "suffix button uses a 16 by 16 hitbox and tooltip area");
        require(button.xPosition == switchButton.xPosition && button.yPosition + 18 == switchButton.yPosition,
            "suffix button is above the interface column crafting button");
        require(button.xPosition >= 0 && button.yPosition >= 0
            && button.xPosition + 16 <= gui.width
            && button.yPosition + 16 <= gui.height, "suffix button stays within the GUI viewport");
    }

    private static String checkMessage(GuiTabButton button, boolean enabled) {
        String message = button.getMessage();
        String status = StatCollector
            .translateToLocal("gtnotgood.terminal.quick.append_suffix." + (enabled ? "on" : "off"));
        require(message.contains((enabled ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + status),
            "suffix tooltip displays the current colored state");
        require(message.contains(StatCollector.translateToLocal("gtnotgood.terminal.quick.append_suffix.detail")),
            "suffix tooltip describes the recipe import behavior");
        return message;
    }

    private static void checkRecipeNames(boolean enabled) {
        ItemStack circuit = GTUtility.getIntegratedCircuit(2);
        ItemStack manualCircuit = GTUtility.getIntegratedCircuit(32);
        ItemStack mold = ItemList.Shape_Mold_Ingot.get(0);
        circuit.stackSize = 0;
        manualCircuit.stackSize = 0;
        ItemStack ingredient = new ItemStack(Items.dye, 3, 11);
        GTRecipe recipe = new GTRecipe(false, new ItemStack[] { circuit, manualCircuit, mold, ingredient },
            new ItemStack[] { new ItemStack(Items.apple) }, null, null, null, null, null, null, null, 20, 8, 0);
        GTNEIDefaultHandler handler = new GTNEIDefaultHandler(RecipeMaps.assemblerRecipes.getDefaultRecipeCategory());
        handler.arecipes.add(handler.new CachedDefaultRecipe(recipe));
        List<OrderStack<?>> inputs = new ArrayList<>();
        for (int i = 0; i < recipe.mInputs.length; i++) inputs.add(new OrderStack<>(recipe.mInputs[i], i));
        inputs.add(new OrderStack<>(new FluidStack(FluidRegistry.WATER, 1234), inputs.size()));
        String baseName = handler.getRecipeName();
        String suffix = " 2 32 " + mold.getItemDamage();
        require(GTUtil.getRecipeName(handler, inputs).equals(baseName + (enabled ? suffix : "")),
            "recipe name includes only the requested non-consumed item numbers");
        require(
            circuit.stackSize == 0 && manualCircuit.stackSize == 0 && mold.stackSize == 0 && ingredient.stackSize == 3,
            "recipe naming preserves catalyst and ingredient quantities");
    }

    private static void checkDiskValue(boolean expected) {
        ConfigFile reloaded = new ConfigFile(((ConfigFile) NEIClientConfig.global.config).file);
        ConfigTag persisted = reloaded.getTag(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE, false);
        require(persisted != null && persisted.getBooleanValue() == expected,
            "a fresh NEI config reads the suffix setting from disk");
    }

    private static void click(GuiQuickEncodingTerminal gui, GuiTabButton button, int mouseButton) throws Exception {
        click(gui, button, mouseButton, false);
    }

    private static void click(GuiQuickEncodingTerminal gui, GuiTabButton button, int mouseButton,
        boolean releaseOverInventory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack expected = mc.thePlayer.inventory.getItemStack().copy();
        boolean expectedState = !NEI_TH_Config
            .getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE);
        Slot releaseSlot = releaseOverInventory ? playerSlot(gui, expected) : null;
        ItemStack originalSlot = releaseSlot == null || releaseSlot.getStack() == null ? null
            : releaseSlot.getStack().copy();
        int releaseX = releaseSlot == null ? button.xPosition + 3
            : (Integer) field(gui, "guiLeft") + releaseSlot.xDisplayPosition + 8;
        int releaseY = releaseSlot == null ? button.yPosition + 3
            : (Integer) field(gui, "guiTop") + releaseSlot.yDisplayPosition + 8;
        PlayerControllerMP originalController = mc.playerController;
        mc.playerController = new PlayerControllerMP(mc, mc.getNetHandler()) {

            @Override
            public ItemStack windowClick(int windowId, int slotId, int clickedButton, int clickType,
                EntityPlayer player) {
                throw new AssertionError("Suffix button dispatched a vanilla inventory click to slot " + slotId);
            }
        };
        try {
            gui.mouseClicked(button.xPosition + 3, button.yPosition + 3, mouseButton);
            require(NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
                == expectedState, "suffix press toggles exactly once for mouse button " + mouseButton);
            require(ItemStack.areItemStacksEqual(expected, mc.thePlayer.inventory.getItemStack()),
                "suffix press preserves the carried item stack for mouse button " + mouseButton);
            if (releaseOverInventory) {
                gui.mouseClickMove(releaseX, releaseY, mouseButton, 100L);
                require(NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
                    == expectedState, "dragging from the suffix button does not toggle again");
                require(ItemStack.areItemStacksEqual(expected, mc.thePlayer.inventory.getItemStack()),
                    "dragging from the suffix button preserves the carried item stack");
            }
            gui.mouseMovedOrUp(releaseX, releaseY, mouseButton);
            require(NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)
                == expectedState, "suffix release does not toggle again for mouse button " + mouseButton);
            require(ItemStack.areItemStacksEqual(expected, mc.thePlayer.inventory.getItemStack()),
                "suffix release preserves the carried item stack for mouse button " + mouseButton);
            if (releaseSlot != null) require(ItemStack.areItemStacksEqual(originalSlot, releaseSlot.getStack()),
                "releasing a suffix click over the player inventory leaves the real slot unchanged");
        } finally {
            mc.playerController = originalController;
        }
    }

    private static Slot playerSlot(GuiQuickEncodingTerminal gui, ItemStack carried) {
        Minecraft mc = Minecraft.getMinecraft();
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (
                value instanceof AppEngSlot slot && slot.inventory == mc.thePlayer.inventory
                    && slot.isPlayerSide()
                    && slot.isEnabled()
                    && slot.isItemValid(carried)
                    && slot.canTakeStack(mc.thePlayer)
                    && slot.xDisplayPosition >= 0
                    && slot.yDisplayPosition >= 0
            ) {
                return slot;
            }
        }
        throw new AssertionError("No enabled real player slot is available for the suffix drag-release check");
    }

    private static void screenshot(GuiQuickEncodingTerminal gui, GuiTabButton button, String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack carried = mc.thePlayer.inventory.getItemStack();
        String originalTheme = Config.quickTerminalButtonTheme;
        mc.thePlayer.inventory.setItemStack(null);
        try {
            saveScreenshot(gui, button, name);
            for (String theme : new String[] { "light", "dark" }) {
                Config.quickTerminalButtonTheme = theme;
                saveScreenshot(gui, button, name.replace(".png", "-" + theme + ".png"));
            }
        } finally {
            Config.quickTerminalButtonTheme = originalTheme;
            mc.thePlayer.inventory.setItemStack(carried);
        }
    }

    private static void saveScreenshot(GuiQuickEncodingTerminal gui, GuiTabButton button, String name) {
        Minecraft mc = Minecraft.getMinecraft();
        gui.drawScreen(button.xPosition + 3, button.yPosition + 3, 0);
        ScreenShotHelper.saveScreenshot(new File(System.getProperty("gtng.terminalScroll.qa.output")), name,
            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static GuiTabButton button(GuiQuickEncodingTerminal gui) throws Exception {
        return (GuiTabButton) field(gui, "appendSuffixButton");
    }

    private static Object field(GuiQuickEncodingTerminal gui, String name) throws Exception {
        for (Class<?> type = gui.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(gui);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
