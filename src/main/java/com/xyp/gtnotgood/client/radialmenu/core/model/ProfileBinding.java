package com.xyp.gtnotgood.client.radialmenu.core.model;

import com.google.gson.annotations.SerializedName;

public class ProfileBinding {

    public enum Type {
        @SerializedName("server")
        Server,

        @SerializedName("world")
        World,

        @SerializedName("singleplayer")
        Singleplayer
    }

    public Type type;

    public String value;

    public static ProfileBinding server(String address) {
        ProfileBinding binding = new ProfileBinding();
        binding.type = Type.Server;
        binding.value = address;
        return binding;
    }

    public static ProfileBinding world(String worldName) {
        ProfileBinding binding = new ProfileBinding();
        binding.type = Type.World;
        binding.value = worldName;
        return binding;
    }

    public boolean matches(String serverAddress, String worldName) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case Server:
                return serverAddress != null && serverAddress.equalsIgnoreCase(trimmedValue());
            case World:
                return worldName != null && worldName.equalsIgnoreCase(trimmedValue());
            case Singleplayer:
                return serverAddress == null;
            default:
                return false;
        }
    }

    private String trimmedValue() {
        return value == null ? "" : value.trim();
    }
}
