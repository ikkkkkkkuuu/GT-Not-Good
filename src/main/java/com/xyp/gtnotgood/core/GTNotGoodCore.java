package com.xyp.gtnotgood.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * FML coremod entry used only to register GT Not Good's early mixins.
 * <p>
 * The normal and optional late mixins are still selected by
 * {@link com.xyp.gtnotgood.loader.LateMixinsLoader}; this class exists because mixins that target early-loaded
 * Minecraft or GregTech classes must be visible before the regular mod-loading phase.
 */
@IFMLLoadingPlugin.Name("GTNotGoodCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.xyp.gtnotgood.core")
public class GTNotGoodCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    /**
     * This coremod only registers mixins and does not provide raw ASM transformers.
     *
     * @return empty transformer list
     */
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.gtnotgood.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Arrays.asList("Gregtech.BaseMetaTileEntityAccelerationMixin", "Gregtech.BlockLeftClickDataStickMixin");
    }
}
