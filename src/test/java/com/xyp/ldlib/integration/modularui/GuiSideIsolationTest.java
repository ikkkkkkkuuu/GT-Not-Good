package com.xyp.ldlib.integration.modularui;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import cpw.mods.fml.common.asm.transformers.SideTransformer;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

/** Checks the dedicated-server class transformation without loading a client screen or starting Minecraft. */
public class GuiSideIsolationTest {

    @Test
    public void wildcardBuilderHasNoClientScreenDependency() throws Exception {
        assertServerSafe("com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardPatternGui");
    }

    @Test
    public void itemAndBlockFactoriesStripClientScreenCreation() throws Exception {
        assertServerSafe("com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem");
        assertServerSafe("com.xyp.gtnotgood.common.blocks.mecontainer.TileMEContainer");
    }

    @Test
    public void pixelScreenIsRejectedOnDedicatedServer() throws Exception {
        try {
            transformOnServer("com.xyp.ldlib.integration.modularui.PixelFontModularScreen");
            fail("The pixel screen must be client-only");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("invalid side SERVER"));
        }
    }

    /** Rebuilds the constant pool so stripped client methods cannot leave false-positive references. */
    private static void assertServerSafe(String name) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(transformOnServer(name)).accept(writer, 0);
        String bytecode = new String(writer.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(name + " still depends on ModularScreen", bytecode.contains("/ModularScreen"));
        assertFalse(name + " still creates the pixel screen", bytecode.contains("/PixelFontModularScreen"));
        assertTrue(name + " lost its shared UI builder", bytecode.contains("buildUI"));
    }

    /** Runs Forge's real side stripper and restores its process-wide launch state after each check. */
    private static synchronized byte[] transformOnServer(String name) throws Exception {
        Field launchSide = FMLLaunchHandler.class.getDeclaredField("side");
        launchSide.setAccessible(true);
        Object previousLaunchSide = launchSide.get(null);
        Field transformerSide = null;
        Object previousTransformerSide = null;
        try {
            launchSide.set(null, Side.SERVER);
            SideTransformer transformer = new SideTransformer();
            transformerSide = SideTransformer.class.getDeclaredField("SIDE");
            transformerSide.setAccessible(true);
            previousTransformerSide = transformerSide.get(null);
            transformerSide.set(null, "SERVER");
            try (InputStream stream = GuiSideIsolationTest.class.getClassLoader()
                .getResourceAsStream(name.replace('.', '/') + ".class")) {
                assertNotNull(name, stream);
                ClassWriter writer = new ClassWriter(0);
                new ClassReader(stream).accept(writer, 0);
                return transformer.transform(name, name, writer.toByteArray());
            }
        } finally {
            if (transformerSide != null) transformerSide.set(null, previousTransformerSide);
            launchSide.set(null, previousLaunchSide);
        }
    }
}
