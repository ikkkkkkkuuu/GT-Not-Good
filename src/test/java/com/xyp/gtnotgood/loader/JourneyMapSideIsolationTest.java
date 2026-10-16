package com.xyp.gtnotgood.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

public class JourneyMapSideIsolationTest {

    @Test
    public void clientWithoutJourneyMapDoesNotLoadIntegration() throws Exception {
        assertIntegrationSkipped(Side.CLIENT, Collections.emptySet());
    }

    @Test
    public void serverWithJourneyMapDoesNotLoadIntegration() throws Exception {
        assertIntegrationSkipped(Side.SERVER, Collections.singleton(ModList.JourneyMap.getID()));
    }

    private static synchronized void assertIntegrationSkipped(Side side, Set<String> mods) throws Exception {
        Field launchSide = FMLLaunchHandler.class.getDeclaredField("side");
        launchSide.setAccessible(true);
        Object previousSide = launchSide.get(null);
        try {
            launchSide.set(null, side);
            ClassLoader loader = new ClassLoader(JourneyMapSideIsolationTest.class.getClassLoader()) {

                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (
                        name.startsWith("journeymap.") || name.startsWith("com.xyp.gtnotgood.client.journeymap.")
                            || name.startsWith("com.xyp.gtnotgood.utils.journeymap.")
                    ) throw new AssertionError("Optional client integration was resolved: " + name);
                    if (!name.equals("com.xyp.gtnotgood.loader.LateMixinsLoader")) {
                        return super.loadClass(name, resolve);
                    }
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            byte[] bytes = classBytes(name);
                            type = defineClass(name, bytes, 0, bytes.length);
                        }
                        if (resolve) resolveClass(type);
                        return type;
                    }
                }
            };
            Class<?> type = Class.forName("com.xyp.gtnotgood.loader.LateMixinsLoader", true, loader);
            Object instance = type.getDeclaredConstructor().newInstance();
            List<?> mixins = (List<?>) type.getMethod("getMixins", Set.class).invoke(instance, mods);
            assertEquals(Collections.emptyList(), mixins);
        } finally {
            launchSide.set(null, previousSide);
        }
    }

    private static byte[] classBytes(String name) throws ClassNotFoundException {
        try (
            InputStream stream = JourneyMapSideIsolationTest.class.getClassLoader()
                .getResourceAsStream(name.replace('.', '/') + ".class");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            assertNotNull(name, stream);
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }
}
