package com.xyp.gtnotgood.common.items.electrode;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.Language;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.ITexture;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import kubatech.api.arcfurnace.ArcFurnaceProcessingEvent;
import kubatech.loaders.ArcFurnaceElectrode;
import kubatech.loaders.ArcFurnaceLoader;
import kubatech.tileentity.gregtech.hatch.MTEHatchElectrode;
import kubatech.tileentity.gregtech.multiblock.MTEIndustrialArcFurnace;

@Mod(
    modid = "ironelectrodeqa",
    name = "Iron Electrode QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class IronElectrodeClientChecks {

    private final StringBuilder report = new StringBuilder();
    private int stage, ticks;
    private boolean loaded, finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.ironElectrode.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        loaded = true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 0) {
                if (!loaded || mc.theWorld != null || mc.currentScreen == null) return;
                checkIntegration();
                showTooltip("zh_CN");
                stage = 1;
                ticks = 0;
            } else if (++ticks == 12) {
                capture(stage == 1 ? "tooltip-zh_CN.png" : "tooltip-en_US.png");
                if (stage == 1) {
                    showTooltip("en_US");
                    stage = 2;
                    ticks = 0;
                } else {
                    write("report.txt", report.toString());
                    write("result.txt", "PASS");
                    System.out.println("IRON_ELECTRODE_QA_PASS\n" + report);
                    finished = true;
                    mc.shutdown();
                }
            }
        } catch (Throwable failure) {
            finished = true;
            failure.printStackTrace();
            try {
                StringWriter trace = new StringWriter();
                failure.printStackTrace(new PrintWriter(trace));
                write("report.txt", report.toString());
                write("result.txt", "FAIL\n" + trace);
            } catch (Exception reportingFailure) {
                reportingFailure.printStackTrace();
            }
            mc.shutdown();
        }
    }

    private void checkIntegration() throws Exception {
        ItemStack stack = GTNGItemList.IronElectrode.get(1);
        ArcFurnaceElectrode electrode = IronElectrode.getDefinition();
        require(stack.getItem() instanceof IronElectrode, "Item registered in GTNGItemList");
        require(ArcFurnaceElectrode.getByStack(stack) == electrode, "Native singleton recognizes the custom item");
        require(ArcFurnaceElectrode.getById(32766) == electrode && electrode.ordinal() == 32766,
            "Stable custom ID and native persisted ordinal agree");
        require(ArcFurnaceElectrode.getById(0) == ArcFurnaceElectrode.GraphiteElectrode,
            "Existing native IDs retain their meaning");
        require(electrode.durability == Integer.MAX_VALUE && electrode.speedModifier == 50d
            && electrode.parallelLimit == 4096
            && electrode.OCSpeedFactor == 12d
            && electrode.OCPowerFactor == 4d
            && electrode.euModifier == 0.5d
            && electrode.startupPenalty == 0d, "All requested electrode attributes");
        Method amperage = ArcFurnaceElectrode.class.getDeclaredMethod("getStartupAmperage");
        amperage.setAccessible(true);
        require(((Long) amperage.invoke(electrode)) == 1L, "Native startup amperage is 1 A");

        MTEHatchElectrode hatch = new MTEHatchElectrode("iron_electrode_qa_hatch", 5, new String[0],
            new ITexture[0][][]);
        require(hatch.isItemValidForSlot(0, stack), "Native electrode hatch accepts the custom item");
        require(hatch.isItemValidForSlot(0, ArcFurnaceElectrode.GraphiteElectrode.getElectrodeItem(1))
            && !hatch.isItemValidForSlot(0, new ItemStack(Items.iron_ingot))
            && !hatch.isItemValidForSlot(0, null), "Hatch preserves native electrodes and rejects ordinary items");

        require(ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.remainingDurability(stack) == Integer.MAX_VALUE,
            "Fresh durability reaches signed integer maximum");
        require(!ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.damageElectrode(stack, Integer.MAX_VALUE - 5),
            "Native damage API handles durability near the maximum");
        require(ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.damageElectrode(stack, 100),
            "Damage across integer maximum consumes the electrode without wrapping");
        require(
            !ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.damageElectrode(stack, Integer.MIN_VALUE)
                && ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.usedDurability(stack) == 0,
            "Large negative repair saturates at zero");
        ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.damageElectrode(stack, 7);
        ItemStack restored = ItemStack.loadItemStackFromNBT(stack.writeToNBT(new NBTTagCompound()));
        require(
            ArcFurnaceElectrode.getByStack(restored) == electrode
                && ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.usedDurability(restored) == 7,
            "Item save/load preserves custom identity and durability");

        QaFurnace furnace = new QaFurnace();
        Field installed = MTEIndustrialArcFurnace.class.getDeclaredField("electrode");
        installed.setAccessible(true);
        installed.set(furnace, electrode);
        furnace.getEffectState().setInteger("infinityTargetParallel", 4096);
        NBTTagCompound saved = new NBTTagCompound();
        furnace.saveNBTData(saved);
        require(saved.getInteger("electrode") == 32766, "Real arc furnace saves the stable custom ID");
        QaFurnace loaded = new QaFurnace();
        loaded.loadNBTData(saved);
        require(
            installed.get(loaded) == electrode && loaded.getEffectState().getInteger("infinityTargetParallel") == 4096,
            "Real arc furnace restores the electrode and parallel state");
        checkEffects(furnace, electrode);
    }

    private void checkEffects(QaFurnace furnace, ArcFurnaceElectrode electrode) {
        furnace.resetEffectState();
        QaLogic logic = new QaLogic();
        electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventConfigureProcessing(furnace, logic));
        require(logic.parallels() == 4096, "To Infinity begins at 4096 parallels");
        for (int i = 0; i < 22; i++) {
            electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventRunCompleted(furnace));
            int expected = (int) Math.min(Integer.MAX_VALUE, 4096L << (i + 1));
            require(furnace.getEffectState().getInteger("infinityTargetParallel") == expected,
                "Completed run " + (i + 1) + " ramps to " + expected + " parallels");
        }
        furnace.getEffectState().setInteger("infinityTargetParallel", Integer.MAX_VALUE);
        electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventConfigureProcessing(furnace, logic));
        require(logic.parallels() == Integer.MAX_VALUE, "Native parallel maximum remains available after growth");
        QaParallelHelper helper = new QaParallelHelper();
        electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventCreateParallelHelper(furnace, helper));
        require(helper.availablePower() == Long.MAX_VALUE, "Native To Infinity parallel power behavior retained");
        QaOverclockCalculator calculator = new QaOverclockCalculator();
        calculator.setAmperage(64).setEUt(512);
        electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventConfigureOverclock(furnace, calculator));
        require(calculator.voltage() == Long.MAX_VALUE && calculator.amperage() == 1,
            "Native To Infinity overclock behavior retained");
        electrode.specialEffect.accept(new ArcFurnaceProcessingEvent.EventReset(furnace));
        require(furnace.getEffectState().getInteger("infinityTargetParallel") == 4096,
            "Shutdown resets To Infinity to its 4096 starting parallels");

        ArcFurnaceProcessingEvent.EventStartShutdown shutdown = new ArcFurnaceProcessingEvent.EventStartShutdown(
            furnace, 600);
        electrode.specialEffect.accept(shutdown);
        require(shutdown.duration == 1, "Native Hot Restart removes shutdown cooldown");
        furnace.lastStartup = -1;
        furnace.voltage = 8192;
        ArcFurnaceProcessingEvent.EventStartIgnition cold = ignition(furnace, electrode);
        require(cold.duration == 600 && cold.eut == 8192, "Cold ignition uses one input ampere");
        furnace.voltage = Long.MAX_VALUE;
        require(ignition(furnace, electrode).eut == Integer.MAX_VALUE, "Cold ignition clamps high input voltage");
        furnace.lastStartup = 100;
        furnace.setTotalRunTime(179);
        ArcFurnaceProcessingEvent.EventStartIgnition hot = ignition(furnace, electrode);
        require(hot.duration == 1 && hot.eut == 1, "Native Hot Restart retains its one tick at 1 EU/t");
        furnace.voltage = 512;
        furnace.setTotalRunTime(180);
        ArcFurnaceProcessingEvent.EventStartIgnition expired = ignition(furnace, electrode);
        require(expired.duration == 600 && expired.eut == 512, "Hot Restart expires exactly at four seconds");
    }

    private static ArcFurnaceProcessingEvent.EventStartIgnition ignition(QaFurnace furnace,
        ArcFurnaceElectrode electrode) {
        ArcFurnaceProcessingEvent.EventStartIgnition event = new ArcFurnaceProcessingEvent.EventStartIgnition(furnace,
            600, 10000000);
        electrode.specialEffect.accept(event);
        return event;
    }

    private void showTooltip(String locale) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        boolean switched = false;
        for (Language language : mc.getLanguageManager().getLanguages()) {
            if (!language.getLanguageCode().equals(locale)) continue;
            mc.getLanguageManager().setCurrentLanguage(language);
            mc.gameSettings.language = locale;
            mc.refreshResources();
            switched = true;
            break;
        }
        require(switched, "Language available: " + locale);
        ItemStack stack = GTNGItemList.IronElectrode.get(1);
        List<String> tooltip = new ArrayList<>();
        tooltip.add(stack.getDisplayName());
        stack.getItem().addInformation(stack, null, tooltip, false);
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", tooltip));
        require(!text.contains("item.") && !text.contains("{\\"), "Tooltip translations and colors resolve: " + locale);
        String compact = text.replace(" ", "");
        for (String value : new String[] { "2147483647/2147483647", "5000%", "4096", "12/4", "0.5x", "x1", "1A" }) {
            require(compact.contains(value), "Tooltip contains " + value + ": " + locale);
        }
        String[] labels = locale.equals("zh_CN") ? new String[] { "耐久", "速度", "并行", "超频系数", "EU修正", "启动惩罚", "启动电流" }
            : new String[] { "Durability:", "Speed:", "Parallels:", "OC:", "EU Modifier:", "Startup Penalty:",
                "Startup Amperage:" };
        for (String label : labels) require(text.contains(label), "Localized stat label " + label + ": " + locale);
        require(
            text.contains(locale.equals("zh_CN") ? "铁电极" : "Iron Electrode")
                && text.contains(locale.equals("zh_CN") ? "迈向无尽" : "To Infinity")
                && text.contains(locale.equals("zh_CN") ? "热重启" : "Hot Restart"),
            "Name and both native effects are localized: " + locale);
        write("tooltip-" + locale + ".txt", text);
        mc.displayGuiScreen(new TooltipGui(stack, tooltip));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        report.append("PASS ").append(message).append('\n');
    }

    private static void capture(String filename) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), filename, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.ironElectrode.qa.output"));
        output.mkdirs();
        return output;
    }

    private static void write(String filename, String text) throws Exception {
        Files.write(new File(output(), filename).toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static final class QaFurnace extends MTEIndustrialArcFurnace {

        private long voltage = 8192;
        private long lastStartup = -1;

        private QaFurnace() {
            super("iron_electrode_qa");
            setBaseMetaTileEntity(new BaseMetaTileEntity());
        }

        @Override
        public long getAverageInputVoltage() {
            return voltage;
        }

        @Override
        public long getLastCompletedStartupTick() {
            return lastStartup;
        }
    }

    private static final class QaLogic extends ProcessingLogic {

        private int parallels() {
            return maxParallel;
        }
    }

    private static final class QaParallelHelper extends ParallelHelper {

        private long availablePower() {
            return availableEUt;
        }
    }

    private static final class QaOverclockCalculator extends OverclockCalculator {

        private long voltage() {
            return machineVoltage;
        }

        private long amperage() {
            return machineAmperage;
        }
    }

    private final class TooltipGui extends GuiScreen {

        private final ItemStack stack;
        private final List<String> lines;
        private int tooltipX, tooltipY;

        private TooltipGui(ItemStack stack, List<String> lines) {
            this.stack = stack;
            this.lines = lines;
        }

        @Override
        public void initGui() {
            int textWidth = lines.stream().mapToInt(fontRendererObj::getStringWidth).max().orElse(0);
            int textHeight = 10 * lines.size();
            require(textWidth + 60 <= width && textHeight + 40 <= height,
                "Tooltip fits rendered viewport: " + mc.gameSettings.language);
            require(stack.getIconIndex() != null, "Electrode icon resolves: " + mc.gameSettings.language);
            tooltipX = (width - textWidth) / 2 - 12;
            tooltipY = (height - textHeight) / 2 + 12;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            RenderHelper.enableGUIStandardItemLighting();
            new RenderItem().renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, tooltipX - 22,
                tooltipY - 15);
            RenderHelper.disableStandardItemLighting();
            drawHoveringText(lines, tooltipX, tooltipY, fontRendererObj);
        }
    }
}
