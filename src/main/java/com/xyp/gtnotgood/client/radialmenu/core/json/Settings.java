package com.xyp.gtnotgood.client.radialmenu.core.json;

public class Settings {

    public static final int CURRENT_FORMAT_VERSION = 1;

    public static final String DEFAULT_PROFILE = "default";

    public int formatVersion = CURRENT_FORMAT_VERSION;

    public String activeProfile = DEFAULT_PROFILE;

    public void normalize() {
        if (formatVersion <= 0) {
            formatVersion = CURRENT_FORMAT_VERSION;
        }
        if (activeProfile == null || activeProfile.trim().isEmpty()) {
            activeProfile = DEFAULT_PROFILE;
        }
    }
}
