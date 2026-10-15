package com.xyp.gtnotgood.client.radialmenu.client.profile;

import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.core.json.Settings;
import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;

public final class ProfileManager {

    private static Settings settings = new Settings();

    private static Profile active;

    private ProfileManager() {}

    public static void load() {
        settings = ProfileStorage.loadSettings();

        if (ProfileStorage.listProfileNames().isEmpty()) {
            Profile starter = DefaultProfile.create();
            ProfileStorage.saveProfile(starter);
            settings.activeProfile = starter.name;
            ProfileStorage.saveSettings(settings);
        }

        if (!switchTo(settings.activeProfile)) {
            List<String> names = ProfileStorage.listProfileNames();
            if (!names.isEmpty()) {
                switchTo(names.get(0));
            } else {
                active = DefaultProfile.create();
            }
        }
    }

    public static Profile active() {
        if (active == null) {
            active = DefaultProfile.create();
        }
        return active;
    }

    public static String activeName() {
        return active().name;
    }

    public static boolean switchTo(String name) {
        Profile loaded = ProfileStorage.loadProfile(name);
        if (loaded == null) {
            return false;
        }
        active = loaded;
        ToolBeltMenu.attach(active.root);
        if (!name.equals(settings.activeProfile)) {
            settings.activeProfile = name;
            ProfileStorage.saveSettings(settings);
        }
        RadialMenuMod.LOG.info("Active profile: " + name);
        return true;
    }

    public static boolean cycle(int direction) {
        List<String> names = ProfileStorage.listProfileNames();
        if (names.size() < 2) {
            return false;
        }
        int current = names.indexOf(activeName());
        int next = current < 0 ? 0 : Math.floorMod(current + direction, names.size());
        return switchTo(names.get(next));
    }

    public static void saveActive() {
        ProfileStorage.saveProfile(active());
    }

    public static boolean create(String name) {
        String clean = ProfileStorage.sanitize(name);
        if (ProfileStorage.exists(clean)) {
            return false;
        }
        Profile profile = Profile.empty(clean);
        ToolBeltMenu.attach(profile.root);
        return ProfileStorage.saveProfile(profile);
    }

    public static boolean duplicate(String source, String newName) {
        String clean = ProfileStorage.sanitize(newName);
        if (ProfileStorage.exists(clean)) {
            return false;
        }
        Profile loaded = ProfileStorage.loadProfile(source);
        if (loaded == null) {
            return false;
        }
        loaded.name = clean;
        return ProfileStorage.saveProfile(loaded);
    }

    public static boolean rename(String oldName, String newName) {
        String clean = ProfileStorage.sanitize(newName);
        if (clean.equals(oldName)) {
            return true;
        }

        boolean sameFile = ProfileStorage.isSameFile(oldName, clean);
        if (!sameFile && ProfileStorage.exists(clean)) {
            return false;
        }

        Profile loaded = ProfileStorage.loadProfile(oldName);
        if (loaded == null) {
            return false;
        }
        loaded.name = clean;

        if (sameFile) {
            if (!ProfileStorage.renameFile(oldName, clean)) {
                return false;
            }
            ProfileStorage.saveProfile(loaded);
        } else {
            if (!ProfileStorage.saveProfile(loaded)) {
                return false;
            }
            ProfileStorage.deleteProfile(oldName);
        }

        if (oldName.equals(settings.activeProfile)) {
            switchTo(clean);
        }
        return true;
    }

    public static boolean delete(String name) {
        if (!ProfileStorage.exists(name)) {
            return false;
        }
        boolean wasActive = name.equals(activeName());
        if (!ProfileStorage.deleteProfile(name)) {
            return false;
        }

        if (wasActive) {
            List<String> remaining = ProfileStorage.listProfileNames();
            if (remaining.isEmpty()) {
                Profile starter = DefaultProfile.create();
                ProfileStorage.saveProfile(starter);
                switchTo(starter.name);
            } else {
                switchTo(remaining.get(0));
            }
        }
        return true;
    }

    public static boolean applyAutoBind(String serverAddress, String worldName) {
        for (String name : ProfileStorage.listProfileNames()) {
            if (name.equals(activeName())) {
                continue;
            }
            Profile candidate = ProfileStorage.loadProfile(name);
            if (candidate != null && candidate.matches(serverAddress, worldName)) {
                RadialMenuMod.LOG.info(
                    "Auto-binding to profile " + name + " for " + (serverAddress == null ? worldName : serverAddress));
                return switchTo(name);
            }
        }
        return false;
    }
}
