package com.xyp.gtnotgood.common.machines.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import sun.misc.Unsafe;

/** Tests native AE definition isolation while leaving unrelated mod/ore registration outside the fixture. */
public class VaultStackSnapshotsTest {

    private static final Item item = new Item();
    private static Unsafe unsafe;
    private static Field blazeRodField;
    private static Item originalBlazeRod;
    private static Map<Object, Object> oreReferences;
    private static Object oreKey;
    private static Method createItem;
    private static Method createSnapshot;
    private static Constructor<?> createList;
    private static Constructor<?> createSharedTag;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void prepareNativeItemStack() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);
        originalBlazeRod = Items.blaze_rod;
        blazeRodField = Items.class.getDeclaredField("blaze_rod");
        if (originalBlazeRod == null) setBlazeRod(new Item());

        NativeItemLoader loader = new NativeItemLoader();
        createItem = loader.loadClass("appeng.util.item.AEItemStack").getMethod("create", ItemStack.class);
        createSnapshot = loader.loadClass(VaultStackSnapshots.class.getName()).getMethod("item", IAEItemStack.class);
        createList = loader.loadClass("appeng.util.item.ItemList").getConstructor();
        createSharedTag = loader.loadClass("appeng.util.item.AESharedNBT").getConstructor(int.class);

        // This test item has no ores. Seed only its cached lookup to avoid bootstrapping all vanilla recipes.
        Class<?> oreHelper = loader.loadClass("appeng.util.item.OreHelper");
        Field references = oreHelper.getDeclaredField("references");
        references.setAccessible(true);
        oreReferences = (Map<Object, Object>) references.get(oreHelper.getField("INSTANCE").get(null));
        Constructor<?> itemRef = loader.loadClass("appeng.util.item.OreHelper$ItemRef")
            .getDeclaredConstructor(ItemStack.class);
        itemRef.setAccessible(true);
        oreKey = itemRef.newInstance(new ItemStack(item));
        oreReferences.put(oreKey, null);
    }

    @AfterClass
    public static void restoreNativeEnvironment() {
        if (oreReferences != null) oreReferences.remove(oreKey);
        if (blazeRodField != null && originalBlazeRod == null) setBlazeRod(null);
    }

    @Test
    public void changingExportedDefinitionCannotCorruptStoredLookupOrQuantity() throws Exception {
        IItemList<IAEItemStack> stored = storedItems();
        IAEItemStack original = sample(7_000_000_000L);
        stored.addStorage(original);
        IAEItemStack storedEntry = stored.findPrecise(sample(1));
        IAEItemStack snapshot = snapshot(storedEntry);
        assertNotSame(storedEntry, snapshot);

        snapshot.setTagCompound(changedTag());
        snapshot.setStackSize(3);

        assertSame(storedEntry, stored.findPrecise(sample(1)));
        assertEquals(7_000_000_000L, storedEntry.getStackSize());
        assertFalse(storedEntry.hasTagCompound());
        assertEquals(1, stored.size());
    }

    @Test
    public void changingCallerDefinitionCannotCorruptInsertedSnapshot() throws Exception {
        IAEItemStack caller = sample(9_000_000_000L);
        IAEItemStack snapshot = snapshot(caller);
        IItemList<IAEItemStack> stored = storedItems();
        stored.addStorage(snapshot);
        IAEItemStack storedEntry = stored.findPrecise(sample(1));

        caller.setTagCompound(changedTag());
        caller.setStackSize(0);

        assertSame(storedEntry, stored.findPrecise(sample(1)));
        assertFalse(snapshot.hasTagCompound());
        assertEquals(9_000_000_000L, snapshot.getStackSize());
        assertEquals(9_000_000_000L, storedEntry.getStackSize());
    }

    @Test
    public void preservesLongQuantitiesBeyondNativeItemStackCapacity() throws Exception {
        for (long amount : new long[] { 0, 1, Integer.MAX_VALUE, (long) Integer.MAX_VALUE + 1, Long.MAX_VALUE }) {
            IAEItemStack original = sample(amount);
            original.setCraftable(true);
            original.setCountRequestable(12);
            IAEItemStack snapshot = snapshot(original);
            assertEquals(amount, snapshot.getStackSize());
            assertSame(item, snapshot.getItem());
            assertFalse(snapshot.isCraftable());
            assertEquals(0, snapshot.getCountRequestable());
        }
    }

    private static IAEItemStack sample(long amount) throws ReflectiveOperationException {
        return ((IAEItemStack) createItem.invoke(null, new ItemStack(item))).setStackSize(amount);
    }

    private static IAEItemStack snapshot(IAEItemStack stack) throws ReflectiveOperationException {
        return (IAEItemStack) createSnapshot.invoke(null, stack);
    }

    @SuppressWarnings("unchecked")
    private static IItemList<IAEItemStack> storedItems() throws ReflectiveOperationException {
        return (IItemList<IAEItemStack>) createList.newInstance();
    }

    private static NBTTagCompound changedTag() throws ReflectiveOperationException {
        // Use AE's native shared tag directly; constructing the entire AE API is unrelated to definition sharing.
        NBTTagCompound tag = (NBTTagCompound) createSharedTag.newInstance(713);
        tag.setString("variant", "changed");
        return tag;
    }

    private static void setBlazeRod(Item value) {
        unsafe.putObject(unsafe.staticFieldBase(blazeRodField), unsafe.staticFieldOffset(blazeRodField), value);
    }

    /**
     * AEStack eagerly creates a GUI pattern icon through the entire AE mod registry. Remove only that initializer
     * in an isolated loader; all item-definition, NBT, quantity and ItemList methods remain the dependency bytecode.
     */
    private static final class NativeItemLoader extends ClassLoader {

        private NativeItemLoader() {
            super(VaultStackSnapshotsTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("appeng.util.item.") && !name.equals(VaultStackSnapshots.class.getName()))
                return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        ClassNode node = new ClassNode();
                        new ClassReader(input).accept(node, 0);
                        if (name.equals("appeng.util.item.AEStack")) {
                            for (MethodNode method : node.methods) {
                                if (!method.name.equals("<clinit>")) continue;
                                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                                    if (
                                        instruction instanceof FieldInsnNode field
                                            && field.getOpcode() == Opcodes.PUTSTATIC
                                    ) assertEquals("PATTERN", field.name);
                                }
                                method.instructions.clear();
                                method.instructions.add(new InsnNode(Opcodes.RETURN));
                                method.tryCatchBlocks.clear();
                                if (method.localVariables != null) method.localVariables.clear();
                                method.maxStack = 0;
                                method.maxLocals = 0;
                            }
                        }
                        ClassWriter writer = new ClassWriter(0);
                        node.accept(writer);
                        byte[] bytes = writer.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        }
    }
}
