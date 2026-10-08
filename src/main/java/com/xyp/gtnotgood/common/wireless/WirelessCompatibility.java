package com.xyp.gtnotgood.common.wireless;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.xyp.gtnotgood.GTNotGood;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * Only entry points replaced by the scheduler require an adapter. Native validators, controller checks and
 * completion hooks remain virtual calls and must not disqualify machines merely because they override them.
 */
public final class WirelessCompatibility {

    private static final Set<String> CONTROLLERS = new HashSet<>(Arrays.asList(MTEMultiBlockBase.class.getName(),
        "com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.GTCM_MultiMachineBase",
        "com.science.gtnl.common.machine.multiMachineBase.MultiMachineBase",
        "com.science.gtnl.common.machine.multiMachineBase.GTMMultiMachineBase"));
    private static final Set<String> HELPERS = new HashSet<>(Arrays.asList(ProcessingLogic.class.getName(),
        "com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.processingLogics.GTCM_ProcessingLogic",
        "com.science.gtnl.utils.recipes.GTNLProcessingLogic"));
    private static final ClassValue<Boolean> CONTROLLER_SUPPORT = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            String owner = declaring(type, "runMachine", IGregTechTileEntity.class, long.class);
            boolean supported = CONTROLLERS.contains(owner);
            if (!supported)
                GTNotGood.LOG.warn("Wireless controller rejected: {} runMachine owner={}", type.getName(), owner);
            return supported;
        }
    };
    private static final ClassValue<Boolean> LOGIC_SUPPORT = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            String process = declaring(type, "process");
            boolean supported = HELPERS.contains(process) || process
                .equals("com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.GTCM_MultiMachineBase$1");
            if (!supported)
                GTNotGood.LOG.warn("Wireless processing rejected: {} process owner={}", type.getName(), process);
            return supported;
        }
    };

    private WirelessCompatibility() {}

    public static boolean supports(MTEMultiBlockBase machine, ProcessingLogic logic) {
        return logic != null && CONTROLLER_SUPPORT.get(machine.getClass()) && LOGIC_SUPPORT.get(logic.getClass());
    }

    static String declaring(Class<?> type, String name, Class<?>... args) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, args);
                return method.getDeclaringClass().getName();
            } catch (NoSuchMethodException ignored) {} catch (LinkageError unavailableOptionalApi) {
                // Reflection resolves every method signature, including unrelated optional integration types.
                // Reading the declaration avoids turning an absent optional API into a controller rejection.
                GTNotGood.LOG.warn("Wireless declaration reflection failed for {}.{}; reading class bytes: {}",
                    current.getName(), name, unavailableOptionalApi.toString());
                return declaringFromBytes(current, name, args);
            }
        }
        return "";
    }

    private static String declaringFromBytes(Class<?> type, String name, Class<?>... args) {
        StringBuilder signature = new StringBuilder("(");
        for (Class<?> arg : args) signature.append(Type.getDescriptor(arg));
        signature.append(')');
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            boolean[] found = { false };
            try (
                InputStream bytes = current.getResourceAsStream("/" + current.getName().replace('.', '/') + ".class")) {
                if (bytes == null) return "";
                new ClassReader(bytes).accept(new ClassVisitor(asmApi()) {

                    @Override
                    public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                        String methodSignature, String[] exceptions) {
                        if (methodName.equals(name) && descriptor.startsWith(signature.toString())) found[0] = true;
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (found[0]) return current.getName();
            } catch (IOException | RuntimeException unavailableClassBytes) {
                GTNotGood.LOG.warn("Cannot inspect wireless entry point {}.{}", current.getName(), name,
                    unavailableClassBytes);
                return "";
            }
        }
        return "";
    }

    /** Forge's compile API is ASM 5; the Java 17+ launcher supplies ASM 9 for multi-release class files. */
    private static int asmApi() {
        try {
            return Opcodes.class.getField("ASM9").getInt(null);
        } catch (ReflectiveOperationException olderAsm) {
            return Opcodes.ASM5;
        }
    }
}
