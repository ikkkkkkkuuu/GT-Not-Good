package com.xyp.gtnotgood.client.radialmenu.client.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;

public final class ScriptStore {

    private static final String FILE = "script-store.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, String> values;

    private static boolean dirty;

    private ScriptStore() {}

    public static synchronized String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        load();
        return values.get(key);
    }

    public static synchronized void set(String key, String value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        load();
        String previous = value == null ? values.remove(key) : values.put(key, value);
        if (value == null ? previous != null : !value.equals(previous)) {
            dirty = true;
        }
    }

    public static synchronized void flush() {
        if (!dirty || values == null) {
            return;
        }
        dirty = false;

        File file = new File(ProfileStorage.rootDir(), FILE);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            GSON.toJson(values, writer);
        } catch (Exception failure) {
            RadialMenuMod.LOG.warn("Could not write the script store", failure);
        } finally {
            close(writer);
        }
    }

    public static synchronized void reload() {
        flush();
        values = null;
    }

    private static void load() {
        if (values != null) {
            return;
        }
        values = new TreeMap<>();

        File file = new File(ProfileStorage.rootDir(), FILE);
        if (!file.isFile()) {
            return;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            Map<String, String> read = GSON.fromJson(reader,
                new TypeToken<LinkedHashMap<String, String>>() {}.getType());
            if (read != null) {
                for (Map.Entry<String, String> entry : read.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception failure) {
            RadialMenuMod.LOG.warn("Could not read the script store; starting from empty", failure);
        } finally {
            close(reader);
        }
    }

    private static void close(java.io.Closeable stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception ignored) {}
    }
}
