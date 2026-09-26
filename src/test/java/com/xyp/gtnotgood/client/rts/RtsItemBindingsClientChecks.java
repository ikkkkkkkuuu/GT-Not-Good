package com.xyp.gtnotgood.client.rts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.common.RtsItems;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Opt-in startup regression checking every RTS registry item has exactly one populated host catalog entry. */
@Mod(
    modid = "rtsitembindingsqa",
    name = "RTS Item Bindings QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class RtsItemBindingsClientChecks {

    private boolean complete;

    @Mod.EventHandler
    public void loaded(FMLLoadCompleteEvent event) {
        if (!Boolean.getBoolean("gtng.rts.itemBindings.qa")) return;
        if (!RtsbuildingMod.INSTANCE.isInitialized()) throw new AssertionError("RTS must be enabled for this test");
        int checked = 0;
        for (RtsItems.Handle<?> handle : RtsItems.getAllItems()) {
            if (GameRegistry.findItem(ModList.ModIds.GT_NOT_GOOD, handle.id()) != handle.get()) {
                throw new AssertionError("RTS registry identity mismatch: " + handle.id());
            }
            int matches = 0;
            for (GTNGItemList entry : GTNGItemList.values()) {
                if (entry.name()
                    .startsWith("RTS_") && entry.mStack != null
                    && entry.mStack.getItem() == handle.get()) matches++;
            }
            if (matches != 1) throw new AssertionError("RTS catalog binding count for " + handle.id() + ": " + matches);
            checked++;
        }
        if (checked != 17) throw new AssertionError("Expected 17 RTS items, found " + checked);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (complete || event.phase != TickEvent.Phase.END || minecraft.currentScreen == null) return;
        complete = true;
        Files.write(
            new File("rts-item-bindings-qa-result.txt").toPath(),
            "PASS: 17 RTS registry items bound to the host catalog".getBytes(StandardCharsets.UTF_8));
        System.out.println("RTS_ITEM_BINDINGS_QA: PASS");
        minecraft.shutdown();
    }
}
