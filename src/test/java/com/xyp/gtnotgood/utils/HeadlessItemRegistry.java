package com.xyp.gtnotgood.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

import sun.misc.Unsafe;

public final class HeadlessItemRegistry {

    private HeadlessItemRegistry() {}

    /** Keep vanilla final fields stable for the test JVM; compiled GT matching code can retain their values. */
    public static synchronized void bootstrap() throws ReflectiveOperationException {
        String name = "minecraft:feather";
        Item feather = (Item) Item.itemRegistry.getObject(name);
        if (feather == null) {
            feather = new Item().setUnlocalizedName("feather");
            Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
                Object.class);
            register.setAccessible(true);
            register.invoke(Item.itemRegistry, 288, name, feather);
        }
        Field field = Items.class.getDeclaredField("feather");
        if (field.get(null) == null) {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), feather);
        }
    }
}
