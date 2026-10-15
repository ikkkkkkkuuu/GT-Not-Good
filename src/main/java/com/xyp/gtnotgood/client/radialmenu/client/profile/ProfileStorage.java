package com.xyp.gtnotgood.client.radialmenu.client.profile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.core.json.ConfigCodec;
import com.xyp.gtnotgood.client.radialmenu.core.json.Settings;
import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;
import com.xyp.gtnotgood.utils.enums.ModList;

public final class ProfileStorage {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final String PROFILES_DIR = "profiles";
    private static final String BACKUPS_DIR = "backups";
    private static final String ICONS_DIR = "icons";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String PROFILE_SUFFIX = ".json";

    private static final int KEEP_BACKUPS = 3;

    private static File rootDir;

    private ProfileStorage() {}

    public static void init(File gameDir) {
        rootDir = new File(new File(gameDir, ModList.GTNotGood.getDisplayName()), "RadialMenu");
        mkdirs(rootDir);
        mkdirs(profilesDir());
        mkdirs(iconsDir());
    }

    public static File rootDir() {
        return rootDir;
    }

    public static File profilesDir() {
        return new File(rootDir, PROFILES_DIR);
    }

    public static File backupsDir() {
        return new File(rootDir, BACKUPS_DIR);
    }

    public static File iconsDir() {
        return new File(rootDir, ICONS_DIR);
    }

    public static File profileFile(String name) {
        return new File(profilesDir(), sanitize(name) + PROFILE_SUFFIX);
    }

    public static List<String> listProfileNames() {
        List<String> names = new ArrayList<>();
        File[] files = profilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile() && fileName.endsWith(PROFILE_SUFFIX)) {
                    names.add(fileName.substring(0, fileName.length() - PROFILE_SUFFIX.length()));
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    public static boolean exists(String name) {
        return profileFile(name).isFile();
    }

    public static Profile loadProfile(String name) {
        return readProfile(profileFile(name), name);
    }

    private static Profile readProfile(File file, String name) {
        if (file == null || !file.isFile()) {
            return null;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF_8);
            Profile profile = ConfigCodec.readProfile(reader);
            if (profile.name == null || profile.name.trim().isEmpty()) {
                profile.name = name;
            }
            return profile;
        } catch (Exception e) {
            RadialMenuMod.LOG.error("Failed to read profile " + file.getName() + ", ignoring it", e);
            return null;
        } finally {
            close(reader);
        }
    }

    public static boolean saveProfile(Profile profile) {
        if (profile == null || profile.name == null) {
            return false;
        }
        File target = profileFile(profile.name);
        String json = ConfigCodec.writeProfile(profile);

        if (json.equals(read(target))) {
            return true;
        }
        rotateBackups(profile.name);

        File temp = new File(target.getParentFile(), target.getName() + ".tmp");
        Writer writer = null;
        try {
            mkdirs(target.getParentFile());
            writer = new OutputStreamWriter(new FileOutputStream(temp), UTF_8);
            writer.write(json);
            writer.close();
            writer = null;
            return replace(temp, target);
        } catch (IOException e) {
            RadialMenuMod.LOG.error("Failed to write profile " + target.getName(), e);
            return false;
        } finally {
            close(writer);
        }
    }

    private static void rotateBackups(String name) {
        File live = profileFile(name);
        if (!live.isFile() || KEEP_BACKUPS <= 0) {
            return;
        }
        mkdirs(backupsDir());

        File oldest = backupFile(name, KEEP_BACKUPS);
        if (oldest.exists() && !oldest.delete()) {
            RadialMenuMod.LOG.warn("Could not drop the oldest backup " + oldest.getName());
            return;
        }
        for (int age = KEEP_BACKUPS - 1; age >= 1; age--) {
            File from = backupFile(name, age);
            if (from.isFile()) {
                from.renameTo(backupFile(name, age + 1));
            }
        }
        copy(live, backupFile(name, 1));
    }

    private static File backupFile(String name, int age) {
        return new File(backupsDir(), sanitize(name) + "." + age + PROFILE_SUFFIX);
    }

    public static int keptVersions() {
        return KEEP_BACKUPS;
    }

    public static boolean hasBackup(String name, int age) {
        return backupFile(name, age).isFile();
    }

    public static long backupTime(String name, int age) {
        File file = backupFile(name, age);
        return file.isFile() ? file.lastModified() : 0L;
    }

    public static Profile loadBackup(String name, int age) {
        return readProfile(backupFile(name, age), name);
    }

    public static boolean restoreBackup(String name, int age) {
        Profile kept = loadBackup(name, age);
        if (kept == null) {
            return false;
        }
        kept.name = name;
        kept.normalize();
        return saveProfile(kept);
    }

    private static void copy(File from, File to) {
        String content = read(from);
        if (content == null) {
            return;
        }
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(to), UTF_8);
            writer.write(content);
        } catch (IOException e) {
            RadialMenuMod.LOG.warn("Could not write the backup " + to.getName(), e);
        } finally {
            close(writer);
        }
    }

    private static String read(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF_8);
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[4096];
            int got;
            while ((got = reader.read(buffer)) > 0) {
                text.append(buffer, 0, got);
            }
            return text.toString();
        } catch (IOException e) {
            RadialMenuMod.LOG.warn("Could not read " + file.getName(), e);
            return null;
        } finally {
            close(reader);
        }
    }

    public static boolean deleteProfile(String name) {
        return profileFile(name).delete();
    }

    public static boolean renameFile(String from, String to) {
        File source = profileFile(from);
        File target = profileFile(to);
        if (!source.isFile()) {
            return false;
        }

        File temp = new File(profilesDir(), sanitize(from) + ".rename.tmp");
        if (temp.exists() && !temp.delete()) {
            RadialMenuMod.LOG.error("Could not clear " + temp.getName());
            return false;
        }
        if (!source.renameTo(temp)) {
            RadialMenuMod.LOG.error("Could not rename " + source.getName());
            return false;
        }
        if (!temp.renameTo(target)) {

            temp.renameTo(source);
            RadialMenuMod.LOG.error("Could not rename " + temp.getName() + " to " + target.getName());
            return false;
        }

        for (int age = 1; age <= KEEP_BACKUPS; age++) {
            File backup = backupFile(from, age);
            if (backup.isFile()) {
                backup.renameTo(backupFile(to, age));
            }
        }
        return true;
    }

    public static boolean isSameFile(String a, String b) {
        return sanitize(a).equalsIgnoreCase(sanitize(b));
    }

    public static Settings loadSettings() {
        File file = new File(rootDir, SETTINGS_FILE);
        if (!file.isFile()) {
            return new Settings();
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF_8);
            return ConfigCodec.readSettings(reader);
        } catch (Exception e) {
            RadialMenuMod.LOG.error("Failed to read settings, falling back to defaults", e);
            return new Settings();
        } finally {
            close(reader);
        }
    }

    public static void saveSettings(Settings settings) {
        File target = new File(rootDir, SETTINGS_FILE);
        File temp = new File(rootDir, SETTINGS_FILE + ".tmp");
        Writer writer = null;
        try {
            mkdirs(rootDir);
            writer = new OutputStreamWriter(new FileOutputStream(temp), UTF_8);
            writer.write(ConfigCodec.writeSettings(settings));
            writer.close();
            writer = null;
            replace(temp, target);
        } catch (IOException e) {
            RadialMenuMod.LOG.error("Failed to write settings", e);
        } finally {
            close(writer);
        }
    }

    public static String sanitize(String name) {
        if (name == null) {
            return "default";
        }
        String cleaned = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.isEmpty() ? "default" : cleaned;
    }

    private static boolean replace(File temp, File target) {
        if (target.exists() && !target.delete()) {
            RadialMenuMod.LOG.error("Could not replace " + target.getName());
            return false;
        }
        if (!temp.renameTo(target)) {
            RadialMenuMod.LOG.error("Could not rename " + temp.getName() + " to " + target.getName());
            return false;
        }
        return true;
    }

    private static void mkdirs(File dir) {
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            RadialMenuMod.LOG.error("Could not create directory " + dir.getAbsolutePath());
        }
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {

            }
        }
    }
}
