package com.xyp.gtnotgood.loader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.xyp.gtnotgood.loader.EarlyMixinsLoader")
public final class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtnotgood.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // GTNHMixins replaces the JSON's common list with this return value; sided lists stay in the JSON.
        try (InputStreamReader reader = new InputStreamReader(
            EarlyMixinsLoader.class.getResourceAsStream("/" + getMixinConfig()), StandardCharsets.UTF_8)) {
            List<String> mixins = new ArrayList<>();
            for (JsonElement mixin : new Gson().fromJson(reader, JsonObject.class).getAsJsonArray("mixins")) {
                mixins.add(mixin.getAsString());
            }
            return mixins;
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read early mixin configuration", exception);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
