package com.xyp.gtnotgood.client.radialmenu.core.json;

import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;

public final class ConfigCodec {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeAdapterFactory(new LowercaseEnumAdapterFactory())
        .registerTypeAdapterFactory(new StringMapAdapterFactory()).create();

    private ConfigCodec() {}

    public static String writeProfile(Profile profile) {
        return GSON.toJson(profile);
    }

    public static Profile readProfile(Reader reader) {
        Profile profile = GSON.fromJson(reader, Profile.class);
        if (profile == null) {
            throw new JsonParseException("Profile file is empty");
        }
        profile.normalize();
        return profile;
    }

    public static Profile readProfile(String json) {
        Profile profile = GSON.fromJson(json, Profile.class);
        if (profile == null) {
            throw new JsonParseException("Profile file is empty");
        }
        profile.normalize();
        return profile;
    }

    public static String writeSettings(Settings settings) {
        return GSON.toJson(settings);
    }

    public static Settings readSettings(Reader reader) {
        Settings settings = GSON.fromJson(reader, Settings.class);
        if (settings == null) {
            settings = new Settings();
        }
        settings.normalize();
        return settings;
    }
}
