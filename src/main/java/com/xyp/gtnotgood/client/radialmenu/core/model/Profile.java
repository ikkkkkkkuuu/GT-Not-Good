package com.xyp.gtnotgood.client.radialmenu.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Profile {

    public static final int CURRENT_FORMAT_VERSION = 1;

    public int formatVersion = CURRENT_FORMAT_VERSION;

    public String name;

    public List<ProfileBinding> bindings;

    public MenuStyle style;

    public MenuNode root;

    public static Profile empty(String name) {
        Profile profile = new Profile();
        profile.name = name;
        profile.bindings = new ArrayList<>();
        profile.root = MenuNode.category(name, null, SlotLayout.fixed(SlotLayout.DEFAULT_SLOTS));
        profile.root.ensureSlotCapacity();
        return profile;
    }

    public List<ProfileBinding> bindingsOrEmpty() {
        return bindings == null ? Collections.<ProfileBinding>emptyList() : bindings;
    }

    public boolean matches(String serverAddress, String worldName) {
        for (ProfileBinding binding : bindingsOrEmpty()) {
            if (binding != null && binding.matches(serverAddress, worldName)) {
                return true;
            }
        }
        return false;
    }

    public void normalize() {
        if (formatVersion <= 0) {
            formatVersion = CURRENT_FORMAT_VERSION;
        }
        if (root == null) {
            root = MenuNode.category(name, null, SlotLayout.fixed(SlotLayout.DEFAULT_SLOTS));
        }
        if (root.children == null) {
            root.children = new ArrayList<>();
        }
        root.normalize();
    }
}
