package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.me.ChatComponentInterfaceNameSuffix;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.config.OptionList;
import codechicken.nei.config.OptionToggleButton;
import cpw.mods.fml.common.Loader;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
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
    private static String chemicalPlantRawName;
    private static String chemicalPlantSuffix;
    private static ItemStack chemicalPlantRep;
    private static final List<ControllerName> controllerNames = new ArrayList<>();

    static void checkServer(EntityPlayerMP player) throws Exception {
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
        checkControllerRoutes(player, ItemList.Machine_Multi_LargeChemicalReactor.get(1), "GT5 chemical reactor", 8);
        if (Loader.isModLoaded(ModList.GTNotLeisure.getID())) {
            checkGtnlController(hatch);
            checkControllerRoutes(player, registeredController("chemicalplant"), "GTNL chemical plant", 16);
            checkGtnlModes(player, "largeextractor", 24, 2);
            checkGtnlModes(player, "largebrewer", 26, 3);
            checkGtnlModes(player, "largedistillery", 28, 2);
        }
        hatch.setCustomName("Named Assembly");
        require(
            hatch.getRawName()
                .equals("Named Assembly"),
            "custom name preserved");
        require(!(hatch.getNameSuffix() instanceof ChatComponentInterfaceNameSuffix), "custom name bypasses policy");
        require(hatch.getDisplayRep() == null, "custom name bypasses controller display-name fallback");
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
        checkGtnlDisplayName(terminal);
        checkControllerRecipeNames(terminal);
        System.out.println(
            "TERMINAL_NAME_QA: default no, native and combined names, GTNL suffix, circuits/manual/mold, custom name PASS");
    }

    private static void checkGtnlController(SuperMTEHatchCraftingInputME hatch) {
        for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
            if (machine == null || !machine.getMetaName()
                .equals("chemicalplant")) continue;
            hatch.mRecipeMap = null;
            hatch.setControllerRecipeMap(null);
            hatch.updateCraftingIcon(machine.getStackForm(1));
            hatch.setInventorySlotContents(hatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(9));
            hatch.setInventorySlotContents(hatch.getManualSlotStart(), null);
            hatch.setInventorySlotContents(hatch.getMoldSlot(), null);
            chemicalPlantRep = hatch.getDisplayRep();
            require(
                chemicalPlantRep != null && chemicalPlantRep.isItemEqual(machine.getStackForm(1)),
                "GTNL controller display representation supplied to AE2");
            chemicalPlantRawName = hatch.getRawName();
            chemicalPlantSuffix = IChatComponent.Serializer.func_150696_a(hatch.getNameSuffix());
            return;
        }
        throw new AssertionError("GTNL chemical plant fixture registered");
    }

    /** Uses placed instances so hatch ownership and watcher state cannot leak into registered prototypes. */
    private static void checkControllerRoutes(EntityPlayerMP player, ItemStack stack, String label, int x)
        throws Exception {
        for (int route = 0; route < 3; route++) {
            var owner = (MTEMultiBlockBase) place(player, x + route * 2, stack).getMetaTileEntity();
            var hatch = (SuperMTEHatchCraftingInputME) place(player, x + route * 2 + 1, selfRep).getMetaTileEntity();
            hatch.setInventorySlotContents(hatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(9));
            boolean accepted = switch (route) {
                case 0 -> owner.addInputHatchToMachineList(hatch.getBaseMetaTileEntity(), 0);
                case 1 -> owner.addInputBusToMachineList(hatch.getBaseMetaTileEntity(), 0);
                default -> owner.addToMachineList(hatch.getBaseMetaTileEntity(), 0);
            };
            require(accepted && owner.mDualInputHatches.contains(hatch), label + " accepts input route " + route);
            require(owner.isValid(), label + " is a live placed controller");
            Field capturedMap = SuperMTEHatchCraftingInputME.class.getDeclaredField("controllerRecipeMap");
            capturedMap.setAccessible(true);
            require(
                capturedMap.get(hatch) == owner.getRecipeMap(),
                label + " captures controller cache on route " + route);
            String expected = owner.getRecipeMap()
                .getDefaultRecipeCategory().unlocalizedName;
            require(
                hatch.getRawName()
                    .equals(expected),
                label + " captures recipe category on route " + route);
            hatch.mRecipeMap = RecipeMaps.assemblerRecipes;
            hatch.setControllerRecipeMap(RecipeMaps.assemblerRecipes);
            require(
                hatch.getRawName()
                    .equals(expected),
                label + " current owner overrides stale cache on route " + route);
            require(
                hatch.getDisplayRep() != null && hatch.getDisplayRep()
                    .isItemEqual(owner.getMachineCraftingIcon()),
                label + " retains its controller display representation");
            controllerNames.add(new ControllerName(label + " route " + route, hatch));
        }
    }

    private static void checkGtnlModes(EntityPlayerMP player, String metaName, int x, int modes) {
        var owner = (MTEMultiBlockBase) place(player, x, registeredController(metaName)).getMetaTileEntity();
        var hatch = (SuperMTEHatchCraftingInputME) place(player, x + 1, selfRep).getMetaTileEntity();
        hatch.setInventorySlotContents(hatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(9));
        require(
            owner.addInputHatchToMachineList(hatch.getBaseMetaTileEntity(), 0),
            metaName + " accepts fluid input route");
        String initial = owner.getRecipeMap()
            .getDefaultRecipeCategory().unlocalizedName;
        for (int mode = 0; mode <= modes; mode++) {
            String expected = owner.getRecipeMap()
                .getDefaultRecipeCategory().unlocalizedName;
            require(
                hatch.getRawName()
                    .equals(expected),
                metaName + " current recipe category after screwdriver " + mode);
            if (mode == 0) {
                hatch.mRecipeMap = RecipeMaps.assemblerRecipes;
                hatch.setControllerRecipeMap(RecipeMaps.assemblerRecipes);
            } else if (mode == modes) {
                require(expected.equals(initial), metaName + " screwdriver cycles back to its initial recipe map");
            } else {
                require(!expected.equals(initial), metaName + " screwdriver selects another recipe map");
            }
            controllerNames.add(new ControllerName("GTNL " + metaName + " mode " + mode, hatch));
            if (mode < modes) owner.onScrewdriverRightClick(ForgeDirection.NORTH, player, 0, 0, 0, null);
        }
    }

    private static ItemStack registeredController(String metaName) {
        for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
            if (machine instanceof MTEMultiBlockBase && machine.getMetaName()
                .equals(metaName)
                && machine.getClass()
                    .getName()
                    .startsWith("com.science.gtnl."))
                return machine.getStackForm(1);
        }
        throw new AssertionError("GTNL controller fixture registered: " + metaName);
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x, ItemStack stack) {
        player.worldObj.setBlock(x, 10, 8, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 10, 8);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static void checkControllerRecipeNames(Object terminal) throws Exception {
        Method translate = GuiInterfaceTerminal.class
            .getDeclaredMethod("translateRawName", String.class, String.class, ItemStack.class);
        translate.setAccessible(true);
        var tag = NEIClientConfig.global.config.getTag(ButtonConstants.PREFER_OWN_INTERFACE_NAMES);
        try {
            for (int index = 0; index < controllerNames.size(); index++) {
                ControllerName name = controllerNames.get(index);
                long id = 9879L + index;
                String localized = StatCollector.translateToLocal(name.rawName);
                require(!localized.equals(name.rawName), name.label + " recipe category is localized");
                for (boolean preferred : new boolean[] { false, true, false }) {
                    tag.setBooleanValue(preferred);
                    String nativeName = (String) translate.invoke(null, name.rawName, name.suffix, name.rep);
                    require(
                        nativeName.startsWith(localized + " ") && nativeName.contains("9"),
                        name.label + " native recipe name and circuit");
                    var update = new PacketInterfaceTerminalUpdate();
                    if (!entries(terminal).containsKey(id)) {
                        update.addNewEntry(id, name.rawName, true)
                            .setTerminalVisible(true)
                            .setItems(1, 9, 9, patternItems)
                            .setSupportedStackTypes(new IAEStackType<?>[] { ITEM_STACK_TYPE })
                            .setReps(selfRep, name.rep)
                            .setSuffix(name.suffix);
                    } else {
                        update.addRenamedEntry(id, name.rawName, name.suffix, name.rep);
                    }
                    Field commands = PacketInterfaceTerminalUpdate.class.getDeclaredField("commands");
                    commands.setAccessible(true);
                    ((GuiInterfaceTerminal) terminal)
                        .postUpdate((List<PacketInterfaceTerminalUpdate.PacketEntry>) commands.get(update), 0);
                    String normalized = IChatComponent.Serializer
                        .func_150699_a(InterfaceTerminalSuffix.normalize(name.suffix))
                        .getUnformattedText();
                    require(
                        entryName(terminal, id).equals(localized + normalized),
                        name.label + " combined add/rename and grouping");
                    if (preferred) {
                        require(
                            entryName(terminal, id).equals(localized + " 9"),
                            name.label + " own naming rule uses recipe pool plus circuit");
                        if (name.label.equals("GTNL chemical plant route 0")) {
                            require(
                                name.rawName.equals(
                                    RecipeMaps.multiblockChemicalReactorRecipes
                                        .getDefaultRecipeCategory().unlocalizedName),
                                "GTNL chemical plant uses the chemical reactor pool");
                            System.out.println("TERMINAL_RECIPE_NAME_QA: chemical plant displays " + localized + " 9");
                            screenshotRecipeName(terminal, localized, id);
                        }
                    }
                }
            }
        } finally {
            tag.setBooleanValue(false);
        }
        System.out
            .println(
                "TERMINAL_RECIPE_NAME_QA: GT5 attachment routes; "
                    + (Loader.isModLoaded(ModList.GTNotLeisure.getID())
                        ? "GTNL chemicalplant, largeextractor, largebrewer, largedistillery and screwdriver modes; "
                        : "GTNL absent; ")
                    + "native/combined add/rename PASS ("
                    + controllerNames.size()
                    + " fixtures)");
    }

    private static void screenshotRecipeName(Object terminal, String query, long id) throws Exception {
        Field field = GuiInterfaceTerminal.class.getDeclaredField("searchFieldNames");
        field.setAccessible(true);
        MEGuiTextField search = (MEGuiTextField) field.get(terminal);
        String previous = search.getText();
        try {
            search.setText(query);
            Minecraft.getMinecraft().currentScreen.drawScreen(-10000, -10000, 0);
            require(sectionVisible(terminal, id), "chemical reactor recipe category is visible after name search");
            screenshot("gtnl-chemical-plant-recipe-pool-own-name.png");
        } finally {
            search.setText(previous);
        }
    }

    private static void checkGtnlDisplayName(Object terminal) throws Exception {
        if (chemicalPlantRep == null) return;
        Method translate = GuiInterfaceTerminal.class
            .getDeclaredMethod("translateRawName", String.class, String.class, ItemStack.class);
        translate.setAccessible(true);
        require(
            translate.invoke(null, chemicalPlantRawName, "", null)
                .equals(chemicalPlantRawName),
            "GTNL raw key reproduces the untranslated name without its display representation");
        String localized = chemicalPlantRep.getDisplayName();
        require(
            localized.equals(StatCollector.translateToLocal("gtnl.machine.chemical_plant.name")),
            "GTNL controller name resolved in the client's language");
        var tag = NEIClientConfig.global.config.getTag(ButtonConstants.PREFER_OWN_INTERFACE_NAMES);
        try {
            for (boolean preferred : new boolean[] { false, true, false }) {
                tag.setBooleanValue(preferred);
                String nativeName = (String) translate
                    .invoke(null, chemicalPlantRawName, chemicalPlantSuffix, chemicalPlantRep);
                require(
                    nativeName.startsWith(localized + " ") && nativeName.contains("9"),
                    "native GTNL name and circuit");
                var update = new PacketInterfaceTerminalUpdate();
                if (!entries(terminal).containsKey(9878L)) {
                    update.addNewEntry(9878L, chemicalPlantRawName, true)
                        .setTerminalVisible(true)
                        .setItems(1, 9, 9, patternItems)
                        .setSupportedStackTypes(new IAEStackType<?>[] { ITEM_STACK_TYPE })
                        .setReps(selfRep, chemicalPlantRep)
                        .setSuffix(chemicalPlantSuffix);
                } else {
                    update.addRenamedEntry(9878L, chemicalPlantRawName, chemicalPlantSuffix, chemicalPlantRep);
                }
                Field commands = PacketInterfaceTerminalUpdate.class.getDeclaredField("commands");
                commands.setAccessible(true);
                ((GuiInterfaceTerminal) terminal)
                    .postUpdate((List<PacketInterfaceTerminalUpdate.PacketEntry>) commands.get(update), 0);
                String combined = entryName(terminal, 9878L);
                require(
                    combined.startsWith(localized + " ") && combined.contains("9")
                        && !combined.contains(chemicalPlantRawName),
                    "combined GTNL name, circuit and grouping");
                if (preferred) require(combined.equals(localized + " 9"), "own GTNL name retains only circuit 9");
                Minecraft.getMinecraft().currentScreen.drawScreen(-10000, -10000, 0);
                screenshot(preferred ? "gtnl-chemical-plant-own-name.png" : "gtnl-chemical-plant-default-name.png");
            }
        } finally {
            tag.setBooleanValue(false);
        }
        System.out
            .println("TERMINAL_GTNL_NAME_QA: real controller, client localization, add/rename and circuit 9 PASS");
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
        return entryName(terminal, 9877L);
    }

    private static String entryName(Object terminal, long id) throws Exception {
        Object entry = entries(terminal).get(id);
        Field name = entry.getClass()
            .getDeclaredField("dispName");
        name.setAccessible(true);
        return (String) name.get(entry);
    }

    private static boolean entryMissing(Object terminal) throws Exception {
        return !entries(terminal).containsKey(9877L);
    }

    private static boolean sectionVisible(Object terminal) throws Exception {
        return sectionVisible(terminal, 9877L);
    }

    private static boolean sectionVisible(Object terminal, long id) throws Exception {
        Object entry = entries(terminal).get(id);
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

    private static final class ControllerName {

        private final String label;
        private final String rawName;
        private final String suffix;
        private final ItemStack rep;

        private ControllerName(String label, SuperMTEHatchCraftingInputME hatch) {
            this.label = label;
            rawName = hatch.getRawName();
            suffix = IChatComponent.Serializer.func_150696_a(hatch.getNameSuffix());
            rep = hatch.getDisplayRep();
        }
    }
}
