package com.xyp.gtnotgood.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xyp.gtnotgood.common.packet.ServerConfigMessage;
import com.xyp.gtnotgood.config.ServerConfigOptions.Option;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Checks untrusted input, stale edits, persistence and the separation between live and restart-only options. */
public class ServerConfigTest {

    private Configuration oldConfig;
    private boolean oldLoaded;
    private boolean oldGas;
    private boolean oldWater;
    private Path directory;
    private Configuration config;
    private Object oldMinecraftHome;
    private final Map<String, Option> options = new LinkedHashMap<>();

    @Before
    public void setup() throws Exception {
        oldConfig = (Configuration) field("configuration").get(null);
        oldLoaded = field("configLoaded").getBoolean(null);
        oldGas = MainConfig.GasInPut;
        oldWater = MainConfig.Water;
        directory = Files.createTempDirectory("server-config-test");
        Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        oldMinecraftHome = home.get(null);
        home.set(null, directory.toFile());
        config = new Configuration(
            directory.resolve("main.cfg")
                .toFile());
        config.getBoolean("GasInPut", "鸿蒙之眼", true, "test");
        config.getBoolean("Water", "净化水", true, "test");
        config.save();
        field("configuration").set(null, config);
        field("configLoaded").setBoolean(null, true);
        MainConfig.GasInPut = true;
        MainConfig.Water = true;
        ServerConfigOptions.options()
            .forEach(
                (id, option) -> {
                    if (id.equals("main/鸿蒙之眼/GasInPut") || id.equals("main/净化水/Water")) options.put(id, option);
                });
    }

    @After
    public void cleanup() throws Exception {
        field("configuration").set(null, oldConfig);
        field("configLoaded").setBoolean(null, oldLoaded);
        MainConfig.GasInPut = oldGas;
        MainConfig.Water = oldWater;
        Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, oldMinecraftHome);
        Files.deleteIfExists(directory.resolve("main.cfg"));
        Files.deleteIfExists(directory);
    }

    private static Field field(String name) throws Exception {
        Field field = MainConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private ServerConfigMessage request() {
        ServerConfigMessage message = new ServerConfigMessage();
        message.status = ServerConfigService.APPLY;
        options.forEach(
            (id, option) -> message.expected.put(
                id,
                option.property()
                    .getString()));
        return message;
    }

    private int apply(ServerConfigMessage message) throws Exception {
        Method method = ServerConfigService.class.getDeclaredMethod("apply", ServerConfigMessage.class, Map.class);
        method.setAccessible(true);
        return (Integer) method.invoke(null, message, options);
    }

    @Test
    public void liveChangesPersistAndStaleEditsCannotOverwriteThem() throws Exception {
        ServerConfigMessage message = request();
        message.values.put("main/鸿蒙之眼/GasInPut", "false");
        assertEquals(ServerConfigService.SAVED, apply(message));
        assertFalse(MainConfig.GasInPut);
        Configuration reloaded = new Configuration(config.getConfigFile());
        assertFalse(reloaded.getBoolean("GasInPut", "鸿蒙之眼", true, "test"));
        assertEquals(ServerConfigService.CONFLICT, apply(message));
    }

    @Test
    public void restartOptionsAreSavedWithoutChangingRuntimeFields() throws Exception {
        ServerConfigMessage message = request();
        message.values.put("main/净化水/Water", "false");
        assertEquals(ServerConfigService.RESTART, apply(message));
        assertTrue(MainConfig.Water);
        assertEquals(
            "false",
            options.get("main/净化水/Water")
                .property()
                .getString());
    }

    @Test
    public void unknownOrInvalidEntriesRejectTheEntireBatch() throws Exception {
        ServerConfigMessage message = request();
        message.values.put("main/鸿蒙之眼/GasInPut", "false");
        message.values.put("arbitrary/path", "true");
        assertEquals(ServerConfigService.INVALID, apply(message));
        assertTrue(MainConfig.GasInPut);
        assertEquals(
            "true",
            options.get("main/鸿蒙之眼/GasInPut")
                .property()
                .getString());
        message.values.remove("arbitrary/path");
        message.values.put("main/净化水/Water", "maybe");
        assertEquals(ServerConfigService.INVALID, apply(message));
        assertTrue(MainConfig.GasInPut);
    }

    @Test
    public void saveFailureLeavesRuntimeAndPropertiesUnchanged() throws Exception {
        ServerConfigMessage message = request();
        message.values.put("main/鸿蒙之眼/GasInPut", "false");
        Files.delete(
            config.getConfigFile()
                .toPath());
        assertEquals(ServerConfigService.FAILED, apply(message));
        assertTrue(MainConfig.GasInPut);
        assertEquals(
            "true",
            options.get("main/鸿蒙之眼/GasInPut")
                .property()
                .getString());
    }

    @Test
    public void numericValidationRejectsNonFiniteFractionalAndOutOfRangeValues() {
        Property integer = new Property("count", "1", Property.Type.INTEGER).setMinValue(1)
            .setMaxValue(10);
        Property decimal = new Property("chance", "1", Property.Type.DOUBLE).setMinValue(0.0)
            .setMaxValue(100.0);
        for (String value : new String[] { "0", "11", "1.5", "2147483648" }) reject(integer, value);
        for (String value : new String[] { "NaN", "Infinity", "-1", "101" }) reject(decimal, value);
        ServerConfigOptions.validate(integer, "10");
        ServerConfigOptions.validate(decimal, "0.5");
    }

    private static void reject(Property property, String value) {
        try {
            ServerConfigOptions.validate(property, value);
            fail("Accepted " + value);
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void packetRoundTripAndOversizedMapRejection() {
        ServerConfigMessage sent = request();
        sent.requestId = 123;
        sent.values.put("main/鸿蒙之眼/GasInPut", "false");
        ByteBuf buffer = Unpooled.buffer();
        try {
            sent.toBytes(buffer);
            ServerConfigMessage received = new ServerConfigMessage();
            received.fromBytes(buffer);
            assertEquals(sent.requestId, received.requestId);
            assertEquals(sent.values, received.values);
            assertEquals(sent.expected, received.expected);
            buffer.clear()
                .writeLong(0)
                .writeByte(0)
                .writeByte(65);
            try {
                received.fromBytes(buffer);
                fail("Accepted oversized map");
            } catch (IllegalArgumentException expected) {}
        } finally {
            buffer.release();
        }
    }
}
