package com.xyp.gtnotgood.common.machines.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

public class ModuleWorkTest {

    private static final UUID OWNER = UUID.fromString("72b1b5ea-78ce-42dd-aa33-df5c63ff7f0e");

    @Test
    public void differentExecutionCoresFinishIndependently() {
        ModuleWork shortWork = work("short", 20, BigInteger.ZERO);
        ModuleWork longWork = work("long", 200, BigInteger.ZERO);
        shortWork.advance(20);
        longWork.advance(20);

        assertTrue(shortWork.finished());
        assertFalse(longWork.finished());
        assertEquals(180, longWork.remaining());
        shortWork.advance(Integer.MAX_VALUE);
        assertEquals(20, shortWork.progress());
        longWork.advance(-1);
        assertEquals(20, longWork.progress());
    }

    @Test
    public void restoresCoreIdentityProgressAndExactPrepaidEnergy() {
        BigInteger prepaid = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(12345));
        ModuleWork original = work("dimension:0:12:64:-8", 200, prepaid);
        original.advance(73);
        ModuleWork restored = ModuleWork.load(original.save());

        assertEquals(original.core, restored.core);
        assertEquals(original.coreType, restored.coreType);
        assertEquals(OWNER, restored.owner);
        assertEquals(original.eut, restored.eut);
        assertEquals(original.computation, restored.computation);
        assertEquals(original.parallels, restored.parallels);
        assertEquals(original.controllerPower, restored.controllerPower);
        assertEquals(prepaid, restored.prepaidEU);
        assertEquals(2, restored.timeReduction);
        assertEquals(4, restored.powerIncrease);
        assertEquals(73, restored.progress());
        assertEquals(127, restored.remaining());
        restored.advance(127);
        assertTrue(restored.finished());
        assertTrue(ModuleWork.load(restored.save()).finished());
    }

    @Test
    public void clampsSavedProgressAndDoesNotOverflowOnLargeTickAdvances() {
        ModuleWork original = work("bounded", Integer.MAX_VALUE, BigInteger.ZERO);
        original.advance(Integer.MAX_VALUE - 1);
        original.advance(Integer.MAX_VALUE);
        assertTrue(original.finished());
        NBTTagCompound saved = work("bounded", 20, BigInteger.ZERO).save();
        saved.setInteger("progress", -100);
        assertEquals(0, ModuleWork.load(saved).progress());
        saved.setInteger("progress", Integer.MAX_VALUE);
        assertTrue(ModuleWork.load(saved).finished());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativePrepaidEnergy() {
        work("invalid", 20, BigInteger.valueOf(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingExecutionTime() {
        work("invalid", 0, BigInteger.ZERO);
    }

    private static ModuleWork work(String core, int duration, BigInteger prepaid) {
        return new ModuleWork(core, CoreType.values()[0], OWNER, 128, 37, duration, 3, true, prepaid,
            new WirelessOutputs(null, null));
    }
}
