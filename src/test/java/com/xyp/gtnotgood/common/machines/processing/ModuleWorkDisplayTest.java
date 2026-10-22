package com.xyp.gtnotgood.common.machines.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gtnewhorizons.modularui.config.Config;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class ModuleWorkDisplayTest {

    private static Locale previousLocale;
    private static Map<String, String> translations;
    private static String previousProgressTranslation;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void bootstrapNativeDisplay() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        HeadlessBlockRegistry.bootstrap();
        previousLocale = Config.locale;
        Config.locale = Locale.US;
        Field localizedName = StatCollector.class.getDeclaredField("localizedName");
        localizedName.setAccessible(true);
        Field languageList = StringTranslate.class.getDeclaredField("languageList");
        languageList.setAccessible(true);
        translations = (Map<String, String>) languageList.get(localizedName.get(null));
        previousProgressTranslation = translations.put("GT5U.gui.text.progress_recipe", "%s|%s|%s");
    }

    @AfterClass
    public static void restoreDisplayConfiguration() {
        Config.locale = previousLocale;
        if (translations != null) {
            if (previousProgressTranslation == null) translations.remove("GT5U.gui.text.progress_recipe");
            else translations.put("GT5U.gui.text.progress_recipe", previousProgressTranslation);
        }
    }

    @Test
    public void advancingProgressDoesNotChangeTheTaskStructure() {
        ModuleWork work = work(128, new WirelessOutputs(null, null));
        work.advance(17);
        NBTTagCompound initial = ModuleWorkDisplay.task(work.core, work.coreType, 1, work, false, 128);

        work.advance(31);
        NBTTagCompound advanced = ModuleWorkDisplay.task(work.core, work.coreType, 1, work, false, 128);

        assertEquals(initial, advanced);
        assertFalse(advanced.hasKey("progress"));
        assertEquals("2.40|6.40|37.5", ModuleWorkDisplay.progress(work.progress(), work.duration));
    }

    @Test
    public void progressFormattingClampsBoundsAndAvoidsIntegerOverflow() {
        assertEquals("0.00|6.40|0.0", ModuleWorkDisplay.progress(Integer.MIN_VALUE, 128));
        assertEquals("6.40|6.40|100.0", ModuleWorkDisplay.progress(Integer.MAX_VALUE, 128));
        assertEquals("0.00|0.00|0.0", ModuleWorkDisplay.progress(17, 0));
        assertEquals("0.00|0.00|0.0", ModuleWorkDisplay.progress(17, -1));
        assertEquals("107,374,182.35|107,374,182.35|100.0",
            ModuleWorkDisplay.progress(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void pausedAndFinishedTasksKeepIndependentOutputSnapshotsAtZeroRate() {
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, 4096);
        fluid.tag = new NBTTagCompound();
        fluid.tag.setString("variant", "original");
        ModuleWork work = work(100, new WirelessOutputs(null, new FluidStack[] { fluid }));
        work.advance(17);
        NBTTagCompound originalOutputs = work.outputs.save();

        NBTTagCompound paused = ModuleWorkDisplay.task(work.core, work.coreType, 2, work, true, 128);
        NBTTagList pausedOutputs = paused.getTagList("outputs", 10);
        assertTrue(paused.getBoolean("paused"));
        assertFalse(paused.getBoolean("finished"));
        assertEquals(1, pausedOutputs.tagCount());
        assertEquals("4096", pausedOutputs.getCompoundTagAt(0).getString("amount"));
        assertEquals(0, pausedOutputs.getCompoundTagAt(0).getInteger("ticks"));
        pausedOutputs.getCompoundTagAt(0).getCompoundTag("fluid").getCompoundTag("Tag").setString("variant",
            "snapshot");
        assertEquals(originalOutputs, work.outputs.save());
        assertEquals(17, work.progress());

        work.advance(Integer.MAX_VALUE);
        NBTTagCompound finished = ModuleWorkDisplay.task(work.core, work.coreType, 2, work, false, 128);
        assertTrue(finished.getBoolean("finished"));
        assertFalse(finished.getBoolean("paused"));
        assertEquals(0, finished.getTagList("outputs", 10).getCompoundTagAt(0).getInteger("ticks"));
        assertEquals("original", finished.getTagList("outputs", 10).getCompoundTagAt(0).getCompoundTag("fluid")
            .getCompoundTag("Tag").getString("variant"));
    }

    @Test
    public void idleCoresHaveNoProgressOrOutputs() {
        NBTTagCompound idle = ModuleWorkDisplay.task("controller", CoreType.Normal, 0, null, true, 128);

        assertEquals("controller", idle.getString("core"));
        assertEquals(CoreType.Normal.name(), idle.getString("type"));
        assertEquals(0, idle.getInteger("duration"));
        assertFalse(idle.getBoolean("paused"));
        assertFalse(idle.getBoolean("finished"));
        assertFalse(idle.hasKey("progress"));
        assertEquals(0, idle.getTagList("outputs", 10).tagCount());
    }

    private static ModuleWork work(int duration, WirelessOutputs outputs) {
        return new ModuleWork("Normal:12:64:-8", CoreType.Normal, new UUID(0, 1), 128, 0, duration, 1, true,
            BigInteger.ZERO, outputs);
    }
}
