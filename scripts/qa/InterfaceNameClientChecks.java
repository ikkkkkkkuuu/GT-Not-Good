package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.me.ChatComponentInterfaceNameSuffix;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.config.OptionList;
import codechicken.nei.config.OptionToggleButton;
import cpw.mods.fml.common.Loader;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;

/** Exercises actual hatch suffixes, GTNHLib serialization and AE2 grouping under both NEI preferences. */
final class InterfaceNameClientChecks {

    private static String rawName;
    private static String suffix;
    private static String preferredText;
    private static String standardText;
    private static ItemStack selfRep;
    private static NBTTagList patternItems;

    static void checkServer(EntityPlayerMP player) {
        player.worldObj.setBlock(6, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(6, 10, 0);
        selfRep = GTNGItemList.SuperMTEHatchCraftingInputME.get(1);
        tile.setInitialValuesAsNBT(null, (short) selfRep.getItemDamage());
        var hatch = (SuperMTEHatchCraftingInputME) tile.getMetaTileEntity();
        hatch.mRecipeMap = RecipeMaps.assemblerRecipes;
        hatch.setInventorySlotContents(hatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(2));
        hatch.setInventorySlotContents(hatch.getManualSlotStart(), GTUtility.getIntegratedCircuit(32));
        ItemStack mold = ItemList.Shape_Mold_Ingot.get(1);
        hatch.setInventorySlotContents(hatch.getMoldSlot(), mold);
        rawName = hatch.getRawName();
        var component = (ChatComponentInterfaceNameSuffix) hatch.getNameSuffix();
        standardText = component.getText(false);
        preferredText = component.getText(true);
        require(
            preferredText.contains("2") && preferredText.contains("32")
                && preferredText.contains(String.valueOf(mold.getItemDamage())),
            "own circuit, manual and mold retained");
        require(!preferredText.contains(" - "), "own name ignores foreign category");
        if (Loader.isModLoaded(ModList.GTNotLeisure.getID())) {
            require(standardText.contains(" - "), "real GTNL category retained in default policy");
        }
        require(
            component.getUnformattedText()
                .equals(standardText),
            "server uses standard policy");
        suffix = IChatComponent.Serializer.func_150696_a(component);
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound patternTag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound input = new NBTTagCompound(), output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple))
            .writeToNBTGeneric(input);
        AEItemStack.create(new ItemStack(Items.feather))
            .writeToNBTGeneric(output);
        inputs.appendTag(input);
        outputs.appendTag(output);
        patternTag.setTag("in", inputs);
        patternTag.setTag("out", outputs);
        patternTag.setBoolean("crafting", false);
        pattern.setTagCompound(patternTag);
        patternItems = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            patternItems.appendTag(i == 0 ? pattern.writeToNBT(new NBTTagCompound()) : new NBTTagCompound());
        }
        hatch.setCustomName("Named Assembly");
        require(
            hatch.getRawName()
                .equals("Named Assembly"),
            "custom name preserved");
        require(!(hatch.getNameSuffix() instanceof ChatComponentInterfaceNameSuffix), "custom name bypasses policy");
    }

    static void run(Object terminal) throws Exception {
        require(!NEI_TH_Config.getConfigValue(ButtonConstants.PREFER_OWN_INTERFACE_NAMES, false), "default is no");
        var tag = NEIClientConfig.global.config.getTag(ButtonConstants.PREFER_OWN_INTERFACE_NAMES);
        Method resolve = GuiInterfaceTerminal.class.getDeclaredMethod("resolveSuffix", String.class);
        resolve.setAccessible(true);
        String baseName = StatCollector.translateToLocal(rawName);
        checkOption();
        try {
            for (boolean preferred : new boolean[] { false, true, false }) {
                tag.setBooleanValue(preferred);
                String selected = preferred ? preferredText : standardText;
                require(
                    resolve.invoke(null, suffix)
                        .equals(selected),
                    "native terminal policy " + preferred);
                String normalized = IChatComponent.Serializer.func_150699_a(InterfaceTerminalSuffix.normalize(suffix))
                    .getUnformattedText();
                String expected = selected.replace("[", "")
                    .replace("]", "")
                    .trim()
                    .replaceAll("\\s+", " ");
                expected = baseName + (expected.isEmpty() ? "" : " " + expected);
                require(
                    normalized.equals(expected.substring(baseName.length())),
                    "combined suffix policy " + preferred);
                var update = new PacketInterfaceTerminalUpdate();
                if (!preferred && entryMissing(terminal)) {
                    update.addNewEntry(9877L, rawName, true)
                        .setTerminalVisible(true)
                        .setItems(1, 9, 9, patternItems)
                        .setSupportedStackTypes(new IAEStackType<?>[] { ITEM_STACK_TYPE })
                        .setReps(selfRep, null)
                        .setSuffix(suffix);
                } else {
                    update.addRenamedEntry(9877L, rawName, suffix, null);
                }
                Field commands = PacketInterfaceTerminalUpdate.class.getDeclaredField("commands");
                commands.setAccessible(true);
                ((GuiInterfaceTerminal) terminal)
                    .postUpdate((List<PacketInterfaceTerminalUpdate.PacketEntry>) commands.get(update), 0);
                require(entryName(terminal).equals(expected), "group/search name policy " + preferred);
                Minecraft.getMinecraft().currentScreen.drawScreen(-10000, -10000, 0);
                require(sectionVisible(terminal), "name section rendered in the viewport");
                screenshot(preferred ? "own-interface-name.png" : "default-interface-name.png");
            }
        } finally {
            tag.setBooleanValue(false);
        }
        System.out.println(
            "TERMINAL_NAME_QA: default no, native and combined names, GTNL suffix, circuits/manual/mold, custom name PASS");
    }

    private static void checkOption() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen parent = mc.currentScreen;
        OptionList list = (OptionList) NEIClientConfig.getOptionList()
            .getOption(ModList.GTNotGood.getID());
        var option = (OptionToggleButton) list.getOption("prefer_own_interface_names");
        require(option != null, "NEI option registered");
        GuiScreen screen = list.getGui(parent, list, false);
        screen.setWorldAndResolution(mc, parent.width, parent.height);
        require(
            !option.state() && option.getButtonText()
                .equals("否"),
            "NEI default localized to no");
        screen.drawScreen(-10000, -10000, 0);
        screenshot("nei-name-option-no.png");
        option.onClick(0);
        require(
            NEI_TH_Config.getConfigValue(ButtonConstants.PREFER_OWN_INTERFACE_NAMES, false),
            "NEI click enables preference");
        require(
            option.getButtonText()
                .equals("是"),
            "NEI enabled state localized to yes");
        screen.drawScreen(-10000, -10000, 0);
        screenshot("nei-name-option-yes.png");
        option.onClick(0);
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            new File(System.getProperty("gtng.terminalScroll.qa.output")),
            name,
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
    }

    private static String entryName(Object terminal) throws Exception {
        Object entry = entries(terminal).get(9877L);
        Field name = entry.getClass()
            .getDeclaredField("dispName");
        name.setAccessible(true);
        return (String) name.get(entry);
    }

    private static boolean entryMissing(Object terminal) throws Exception {
        return !entries(terminal).containsKey(9877L);
    }

    private static boolean sectionVisible(Object terminal) throws Exception {
        Object entry = entries(terminal).get(9877L);
        Field sectionField = entry.getClass()
            .getDeclaredField("section");
        sectionField.setAccessible(true);
        Object section = sectionField.get(entry);
        Field visible = section.getClass()
            .getDeclaredField("visible");
        visible.setAccessible(true);
        return visible.getBoolean(section);
    }

    private static Map<?, ?> entries(Object terminal) throws Exception {
        Field masterField = GuiInterfaceTerminal.class.getDeclaredField("masterList");
        masterField.setAccessible(true);
        Object master = masterField.get(terminal);
        Field listField = master.getClass()
            .getDeclaredField("list");
        listField.setAccessible(true);
        return (Map<?, ?>) listField.get(master);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
