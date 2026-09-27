package com.xyp.gtnotgood.ae2thing.coremod;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

/** Discovered by GTNHMixins through its late-loader annotation rather than a Java constructor call. */
@LateMixin
@SuppressWarnings("unused")
public class AE2LatePlugin implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.ae2thing.late.json";
    }

    @Override
    @Nonnull
    public List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
