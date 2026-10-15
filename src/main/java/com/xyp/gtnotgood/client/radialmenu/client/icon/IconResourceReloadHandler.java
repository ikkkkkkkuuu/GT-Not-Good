package com.xyp.gtnotgood.client.radialmenu.client.icon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;

public class IconResourceReloadHandler implements IResourceManagerReloadListener {

    public static void register() {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();

        if (manager instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager) manager).registerReloadListener(new IconResourceReloadHandler());
        } else {
            RadialMenuMod.LOG.warn("Resource manager is not reloadable; icon caches will not refresh on pack changes");
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        SpriteAtlas.invalidate();
        IconRenderer.clearCache();
    }
}
