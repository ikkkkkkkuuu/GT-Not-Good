package com.xyp.gtnotgood.mixins;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/**
 * Keeps the Thaumcraft auto-research mixins client-only while allowing the
 * regular mixin configuration to be loaded in both client and server JVMs.
 */
public final class GTNotGoodMixinConfigPlugin implements IMixinConfigPlugin {

    private static final String CLIENT_RESEARCH_MIXIN_PACKAGE = "com.xyp.gtnotgood.mixins.tcautores.";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.startsWith(CLIENT_RESEARCH_MIXIN_PACKAGE) || FMLLaunchHandler.side()
            .isClient();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
