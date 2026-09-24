package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.Config;
import com.rtsbuilding.rtsbuilding.server.plugin.RtsPluginService;
import com.rtsbuilding.rtsbuilding.server.progression.RtsFeature;
import com.rtsbuilding.rtsbuilding.server.progression.RtsProgressionManager;
import com.rtsbuilding.rtsbuilding.server.service.mining.RtsMiningValidator;

/** Checks GTNG's built-in capabilities independently of legacy plugin saves and held tools. */
public class OfficialRtsUnlockedTest {

    @Test
    public void legacyProgressionSettingCannotRelockCapabilities() {
        boolean previous = Config.ENABLE_SURVIVAL_PROGRESSION.getAsBoolean();
        try {
            Config.ENABLE_SURVIVAL_PROGRESSION.set(true);
            assertFalse(RtsProgressionManager.isEnabled());
            for (RtsFeature feature : RtsFeature.values()) {
                assertTrue(RtsPluginService.canUse(null, feature));
            }
            assertEquals(Integer.MAX_VALUE, RtsMiningValidator.rangeMiningMaxRequiredLevel(null, false));
        } finally {
            Config.ENABLE_SURVIVAL_PROGRESSION.set(previous);
        }
    }

    @Test
    public void emptyHandDoesNotRequireBorrowedTools() {
        assertTrue(RtsMiningValidator.canHarvestWithTool(null, null, false));
        assertFalse(
            RtsMiningValidator.isSelectedMiningToolRequested(
                "minecraft:diamond_pickaxe",
                new net.minecraft.item.ItemStack(new net.minecraft.item.Item(), 1)));
        assertFalse(RtsMiningValidator.isToolNearBreak(null, null, 0));
    }
}
