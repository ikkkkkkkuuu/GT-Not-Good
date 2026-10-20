package com.xyp.gtnotgood.qa;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.config.ServerConfigOptions;
import com.xyp.gtnotgood.config.ServerConfigOptions.Option;

public class MultiAmpMachinesTest {

    private static final String MACHINE_PACKAGE = "gregtech/common/tileentities/machines/multi/";
    private static final String MIXIN_PACKAGE = "com/xyp/gtnotgood/mixins/late/gregtech/";
    private static final String BASE = "gregtech/api/metatileentity/implementations/MTEMultiBlockBase";
    private static final String ENHANCED = "gregtech/api/metatileentity/implementations/MTEEnhancedMultiBlockBase";
    private static final String EXTENDED = "gregtech/api/metatileentity/implementations/MTEExtendedPowerMultiBlockBase";
    private static final String ELEMENT = "com/xyp/gtnotgood/utils/machine/MultiAmpEnergyHatchElement";
    private static final String MULTI_AMP = "tectech/thing/metaTileEntity/hatch/MTEHatchEnergyMulti";
    private static final String LASER = "tectech/thing/metaTileEntity/hatch/MTEHatchEnergyTunnel";
    private static final String WIRELESS = "com/xyp/gtnotgood/common/machines/hatch/WirelessMultiAmpEnergyHatch";
    private static final String FIXTURE = "com/xyp/gtnotgood/qa/MultiAmpMachinesFixture";
    private static final String ARRAY = "[Lgregtech/api/interfaces/IHatchElement;";
    private static final String AT_LEAST = "Lgregtech/api/util/HatchElementBuilder;atLeast(" + ARRAY
        + ")Lgregtech/api/util/HatchElementBuilder;";
    private static final String FINISHER = "Lgregtech/api/util/MultiblockTooltipBuilder;toolTipFinisher("
        + "[Ljava/lang/String;)Lgregtech/api/util/MultiblockTooltipBuilder;";
    private static final String[] MACHINES = { "ElectricBlastFurnace", "VacuumFreezer" };
    private final boolean[] previousEnabled = new boolean[MACHINES.length];
    private boolean previousLoaded;
    private Field loaded;

    @Before
    public void isolateConfiguration() throws Exception {
        loaded = MainConfig.class.getDeclaredField("configLoaded");
        loaded.setAccessible(true);
        previousLoaded = loaded.getBoolean(null);
        loaded.setBoolean(null, true);
        for (int index = 0; index < MACHINES.length; index++) {
            previousEnabled[index] = switchField(MACHINES[index]).getBoolean(null);
            enable(MACHINES[index], true);
        }
    }

    @After
    public void restoreConfiguration() throws Exception {
        for (int index = 0; index < MACHINES.length; index++) enable(MACHINES[index], previousEnabled[index]);
        loaded.setBoolean(null, previousLoaded);
    }

    @Test
    public void injectionPointsSelectTheNativeEnergyArrayAndChecksExactlyOnce() throws Exception {
        for (String machine : MACHINES) {
            String target = MACHINE_PACKAGE + "MTE" + machine;
            ClassNode nativeType = read(target);
            ClassNode mixin = read(MIXIN_PACKAGE + machine + "MultiAmpMixin");
            AnnotationNode mixinAnnotation = annotation(mixin.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/Mixin;");
            assertEquals(Arrays.asList(Type.getObjectType(target)), value(mixinAnnotation, "value"));
            assertEquals(Boolean.FALSE, value(mixinAnnotation, "remap"));
            boolean furnace = machine.equals("ElectricBlastFurnace");
            String structureMethod = furnace ? "<clinit>" : "getStructureDefinition";
            int ordinal = furnace ? 1 : 0;
            List<MethodInsnNode> builders = calls(method(nativeType, structureMethod), AT_LEAST);
            assertEquals(ordinal + 1, builders.size());
            MethodInsnNode selected = builders.get(ordinal);
            int energyReferences = 0;
            for (AbstractInsnNode instruction = selected.getPrevious(); instruction
                != null; instruction = instruction.getPrevious()) {
                if (instruction instanceof MethodInsnNode) break;
                if (instruction instanceof FieldInsnNode field && field.name.equals("Energy")) energyReferences++;
            }
            assertEquals(1, energyReferences);
            MethodNode handler = method(mixin, "gtnotgood$allowMultiAmpEnergy");
            assertEquals("(" + ARRAY + ")" + ARRAY, handler.desc);
            assertEquals(furnace, (handler.access & Opcodes.ACC_STATIC) != 0);
            assertInjection(handler, "ModifyArg", structureMethod, AT_LEAST, ordinal);
            assertEquals(Integer.valueOf(0), value(
                annotation(handler.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;"), "index"));
            String check = "L" + target + ";checkHasEnergyHatch(Ljava/util/List;)V";
            assertEquals(1, calls(method(nativeType, "checkMachine"), check).size());
            assertInjection(method(mixin, "gtnotgood$checkEnergy"), "Redirect", "checkMachine", check, null);
            assertEquals(1, calls(method(nativeType, "createTooltip"), FINISHER).size());
            assertInjection(method(mixin, "gtnotgood$describeMultiAmpEnergy"), "Redirect", "createTooltip", FINISHER,
                null);
        }
    }

    @Test
    public void eachRestartOnlyOptionKeepsItsRuntimeValueUntilRestart() throws Exception {
        for (String machine : MACHINES) {
            String category = machine.equals("ElectricBlastFurnace") ? "工业高炉" : "真空冷冻机";
            Option option = ServerConfigOptions.options()
                .get("main/" + category + "/" + switchField(machine).getName());
            assertNotNull(option);
            assertFalse(option.live);
            option.applyRuntime(false);
            assertTrue(switchField(machine).getBoolean(null));
        }
    }

    @Test
    public void actualHandlersAcceptMultiAmpHatchesAndKeepOtherStructureElements() throws Exception {
        for (String machine : MACHINES) {
            Class<?> fixture = fixture(machine);
            Object instance = fixture.getConstructor().newInstance();
            ElementAccess[] elements = { ElementProbe.OutputHatch, ElementProbe.Maintenance, ElementProbe.Energy };
            ElementAccess[] result = structure(instance, elements);
            assertSame(ElementProbe.OutputHatch, result[0]);
            assertSame(ElementProbe.Maintenance, result[1]);
            assertTrue(result[2].matchesHatch(new NormalEnergyProbe()));
            assertTrue(result[2].matchesHatch(new MultiAmpHatchProbe()));
            assertTrue(result[2].matchesHatch(wirelessHatch()));
            assertFalse(result[2].matchesHatch(new LaserProbe()));
            assertTrue(result[2].mteClasses().contains(MultiAmpHatchProbe.class));
            enable(machine, false);
            ElementAccess[] original = { ElementProbe.Energy, ElementProbe.OutputHatch };
            assertSame(original, structure(instance, original));
            assertSame(ElementProbe.Energy, original[0]);
            assertSame(ElementProbe.OutputHatch, original[1]);
        }
    }

    @Test
    public void nativeMultiAmpAdderRejectsLaserAndEnergyChecksRespectBothSwitches() throws Exception {
        assertEquals(MULTI_AMP, read(LASER).superName);
        assertEquals(Opcodes.ICONST_1, method(read(MULTI_AMP), "getHatchType").instructions.getFirst().getOpcode());
        assertEquals(Opcodes.ICONST_2, method(read(LASER), "getHatchType").instructions.getFirst().getOpcode());
        for (String machine : MACHINES) {
            Object instance = fixture(machine).getConstructor().newInstance();
            MachineProbe probe = (MachineProbe) instance;
            ElementAccess energy = structure(instance, new ElementAccess[] { ElementProbe.Energy })[0];
            assertFalse(energy.adder().apply(probe, new TileProbe(new LaserProbe()), 11));
            assertTrue(probe.mExoticEnergyHatches.isEmpty());
            assertFalse(checkEnergy(instance).isEmpty());
            MultiAmpHatchProbe hatch = new MultiAmpHatchProbe();
            assertTrue(energy.adder().apply(probe, new TileProbe(hatch), 11));
            assertEquals(11, hatch.texture);
            assertSame(probe.icon, hatch.icon);
            assertSame(probe.tile, hatch.controller);
            assertEquals(Collections.singletonList(hatch), probe.mExoticEnergyHatches);
            assertTrue(checkEnergy(instance).isEmpty());
            MultiAmpHatchProbe wireless = wirelessHatch();
            assertEquals(1, wireless.getHatchType());
            assertTrue(energy.adder().apply(probe, new TileProbe(wireless), 12));
            assertEquals(12, wireless.texture);
            assertSame(probe.icon, wireless.icon);
            assertSame(probe.tile, wireless.controller);
            assertTrue(probe.mExoticEnergyHatches.contains(wireless));
            assertTrue(checkEnergy(instance).isEmpty());
            enable(machine, false);
            assertFalse(checkEnergy(instance).isEmpty());
            probe.mEnergyHatches.add(new NormalEnergyProbe());
            assertTrue(checkEnergy(instance).isEmpty());
            enable(machine, true);
            assertTrue(checkEnergy(instance).isEmpty());
            probe.mEnergyHatches.clear();
            probe.mExoticEnergyHatches.clear();
            assertFalse(checkEnergy(instance).isEmpty());
        }
    }

    private static MultiAmpHatchProbe wirelessHatch() throws Exception {
        ClassNode node = read(WIRELESS);
        node.access = Opcodes.ACC_PUBLIC;
        node.superName = Type.getInternalName(MultiAmpHatchProbe.class);
        node.fields.clear();
        node.methods.removeIf(method -> !method.name.equals("getHatchType"));
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        node.methods.add(constructor);
        String name = FIXTURE + "$Wireless";
        byte[] bytes = remap(node, Collections.singletonMap(WIRELESS, name));
        Class<?> type = new ClassLoader(MultiAmpMachinesTest.class.getClassLoader()) {

            Class<?> defineWireless() {
                return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.defineWireless();
        return (MultiAmpHatchProbe) type.getConstructor().newInstance();
    }

    private static Class<?> fixture(String machine) throws Exception {
        String mixinName = MIXIN_PACKAGE + machine + "MultiAmpMixin";
        ClassNode node = read(mixinName);
        node.access = Opcodes.ACC_PUBLIC;
        node.superName = Type.getInternalName(MachineProbe.class);
        node.methods.removeIf(method -> !method.name.equals("gtnotgood$allowMultiAmpEnergy")
            && !method.name.equals("gtnotgood$checkEnergy"));
        ClassNode enhanced = read(ENHANCED);
        for (String name : new String[] { "checkHasEnergyHatch", "checkHasAnyEnergy", "checkHatchMin" }) {
            node.methods.add(method(enhanced, name));
        }
        node.methods.add(method(read(BASE), "addMultiAmpEnergyInputToMachineList"));
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        node.methods.add(constructor);
        Map<String, String> names = new HashMap<>();
        names.put(mixinName, FIXTURE);
        names.put(MACHINE_PACKAGE + "MTE" + machine, FIXTURE);
        names.put(ENHANCED, FIXTURE);
        names.put(EXTENDED, FIXTURE);
        names.put(BASE, Type.getInternalName(MachineProbe.class));
        names.put(ELEMENT, FIXTURE + "$Element");
        names.put(MULTI_AMP, Type.getInternalName(MultiAmpHatchProbe.class));
        names.put("gregtech/api/interfaces/IHatchElement", Type.getInternalName(ElementAccess.class));
        names.put("gregtech/api/enums/HatchElement", Type.getInternalName(ElementProbe.class));
        names.put("gregtech/api/util/IGTHatchAdder", Type.getInternalName(AdderAccess.class));
        names.put("gregtech/api/interfaces/metatileentity/IMetaTileEntity", Type.getInternalName(HatchProbe.class));
        names.put("gregtech/api/interfaces/tileentity/IGregTechTileEntity", Type.getInternalName(TileAccess.class));
        names.put("net/minecraft/item/ItemStack", Type.getInternalName(IconProbe.class));
        names.put("gregtech/api/structure/error/ErrorType", Type.getInternalName(ErrorTypeProbe.class));
        names.put("gregtech/api/structure/error/StructureErrors", Type.getInternalName(ErrorProbe.class));
        names.put("gregtech/api/structure/error/StructureError", Type.getInternalName(ErrorProbe.class));
        names.put("gregtech/api/structure/error/TranslatableStructureError", Type.getInternalName(ErrorProbe.class));
        Map<String, byte[]> classes = new HashMap<>();
        classes.put(FIXTURE.replace('/', '.'), remap(node, names));
        classes.put((FIXTURE + "$Element").replace('/', '.'), remap(read(ELEMENT), names));
        return new ClassLoader(MultiAmpMachinesTest.class.getClassLoader()) {

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        }.loadClass(FIXTURE.replace('/', '.'));
    }

    private static byte[] remap(ClassNode node, Map<String, String> names) {
        node.signature = null;
        node.innerClasses.clear();
        node.outerClass = null;
        node.visibleAnnotations = null;
        node.invisibleAnnotations = null;
        for (MethodNode method : node.methods) {
            method.visibleAnnotations = null;
            method.invisibleAnnotations = null;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        return writer.toByteArray();
    }

    private static ElementAccess[] structure(Object instance, ElementAccess[] elements) throws Exception {
        Method handler = instance.getClass().getDeclaredMethod("gtnotgood$allowMultiAmpEnergy", ElementAccess[].class);
        handler.setAccessible(true);
        return (ElementAccess[]) handler.invoke(instance, (Object) elements);
    }

    private static List<ErrorProbe> checkEnergy(Object instance) throws Exception {
        List<ErrorProbe> errors = new ArrayList<>();
        Method handler = instance.getClass().getDeclaredMethod("gtnotgood$checkEnergy", instance.getClass(),
            List.class);
        handler.setAccessible(true);
        handler.invoke(instance, instance, errors);
        return errors;
    }

    private static void enable(String machine, boolean enabled) throws Exception {
        switchField(machine).setBoolean(null, enabled);
    }

    private static Field switchField(String machine) throws Exception {
        return MainConfig.class
            .getField(Character.toLowerCase(machine.charAt(0)) + machine.substring(1) + "MultiAmpEnergy");
    }

    private static void assertInjection(MethodNode handler, String annotation, String method, String target,
        Integer ordinal) {
        AnnotationNode injection = annotation(handler.visibleAnnotations,
            "Lorg/spongepowered/asm/mixin/injection/" + annotation + ";");
        assertEquals(Arrays.asList(method), value(injection, "method"));
        assertEquals(Integer.valueOf(1), value(injection, "require"));
        assertEquals(Integer.valueOf(1), value(injection, "allow"));
        AnnotationNode at = value(injection, "at");
        assertEquals("INVOKE", value(at, "value"));
        assertEquals(target, value(at, "target"));
        if (ordinal != null) assertEquals(ordinal, value(at, "ordinal"));
    }

    private static List<MethodInsnNode> calls(MethodNode method, String target) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (
                instruction instanceof MethodInsnNode call
                    && target.equals("L" + call.owner + ";" + call.name + call.desc)
            ) result.add(call);
        }
        return result;
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream bytes = MultiAmpMachinesTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(name, bytes);
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError(name));
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        assertNotNull(descriptor, annotations);
        return annotations.stream().filter(annotation -> annotation.desc.equals(descriptor)).findFirst()
            .orElseThrow(() -> new AssertionError(descriptor));
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(AnnotationNode annotation, String name) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(name)) return (T) annotation.values.get(index + 1);
        }
        throw new AssertionError(name);
    }

    public interface HatchProbe {
    }

    public interface TileAccess {

        HatchProbe getMetaTileEntity();
    }

    public static final class TileProbe implements TileAccess {

        private final HatchProbe hatch;

        public TileProbe(HatchProbe hatch) {
            this.hatch = hatch;
        }

        public HatchProbe getMetaTileEntity() {
            return hatch;
        }
    }

    public interface AdderAccess {

        boolean apply(Object machine, TileAccess tile, int texture);
    }

    public interface ElementAccess {

        List<Class<? extends HatchProbe>> mteClasses();

        boolean matchesHatch(HatchProbe hatch);

        AdderAccess adder();

        long count(Object machine);

        default ElementAccess or(ElementAccess other) {
            ElementAccess first = this;
            return new ElementAccess() {

                public List<Class<? extends HatchProbe>> mteClasses() {
                    List<Class<? extends HatchProbe>> classes = new ArrayList<>(first.mteClasses());
                    classes.addAll(other.mteClasses());
                    return classes;
                }

                public boolean matchesHatch(HatchProbe hatch) {
                    return first.matchesHatch(hatch) || other.matchesHatch(hatch);
                }

                public AdderAccess adder() {
                    return (machine, tile, texture) -> first.adder().apply(machine, tile, texture)
                        || other.adder().apply(machine, tile, texture);
                }

                public long count(Object machine) {
                    return first.count(machine) + other.count(machine);
                }
            };
        }
    }

    public enum ElementProbe implements ElementAccess {

        Energy,
        MultiAmpEnergy,
        OutputHatch,
        Maintenance;

        public List<Class<? extends HatchProbe>> mteClasses() {
            return this == Energy ? Collections.singletonList(NormalEnergyProbe.class) : Collections.emptyList();
        }

        public boolean matchesHatch(HatchProbe hatch) {
            return this == Energy && hatch instanceof NormalEnergyProbe;
        }

        public AdderAccess adder() {
            return (machine, tile, texture) -> this == MultiAmpEnergy
                && ((MachineProbe) machine).addMultiAmpEnergyInputToMachineList(tile, texture);
        }

        public long count(Object machine) {
            MachineProbe probe = (MachineProbe) machine;
            return this == Energy ? probe.mEnergyHatches.size() : probe.mExoticEnergyHatches.size();
        }

        public String getDisplayName() {
            return name();
        }

        public String getDescriptionLangKey() {
            return name();
        }
    }

    public static class MachineProbe {

        public final ArrayList<NormalEnergyProbe> mEnergyHatches = new ArrayList<>();
        public final List<HatchProbe> mExoticEnergyHatches = new ArrayList<>();
        private final IconProbe icon = new IconProbe();
        private final TileAccess tile = new TileProbe(null);

        public TileAccess getBaseMetaTileEntity() {
            return tile;
        }

        public IconProbe getMachineCraftingIcon() {
            return icon;
        }

        public boolean addMultiAmpEnergyInputToMachineList(TileAccess tile, int texture) {
            throw new AssertionError("Native method must override this fixture stub");
        }
    }

    public static class NormalEnergyProbe implements HatchProbe {
    }

    public static class MultiAmpHatchProbe implements HatchProbe {

        private int texture;
        private IconProbe icon;
        private TileAccess controller;

        public int getHatchType() {
            return 1;
        }

        public void updateTexture(int texture) {
            this.texture = texture;
        }

        public void updateCraftingIcon(IconProbe icon, TileAccess controller) {
            this.icon = icon;
            this.controller = controller;
        }
    }

    public static class LaserProbe extends MultiAmpHatchProbe {

        @Override
        public int getHatchType() {
            return 2;
        }
    }

    public static class IconProbe {
    }

    public enum ErrorTypeProbe {

        TOO_FEW
    }

    public static final class ErrorProbe {

        public static ErrorProbe hatchCount(ErrorTypeProbe type, ElementProbe element, int current, int target) {
            return new ErrorProbe();
        }
    }
}
