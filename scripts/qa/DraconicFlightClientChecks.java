package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.brandon3055.draconicevolution.client.gui.componentguis.GUIToolConfig;
import com.brandon3055.draconicevolution.client.gui.guicomponents.ComponentFieldAdjuster;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.container.ContainerAdvTool;
import com.brandon3055.draconicevolution.common.inventory.InventoryTool;
import com.brandon3055.draconicevolution.common.items.armor.DraconicArmor;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "draconicflightclientqa",
    name = "Draconic Flight Client QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class DraconicFlightClientChecks {

    private boolean started;
    private volatile boolean equipped;
    private volatile int serverStage;
    private volatile boolean finished;
    private int ticks;
    private int serverTicks;
    private int stage;
    private int frames;
    private int checks;
    private FlightGui gui;
    private ItemConfigField speed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.draconicflight.qa") && !Boolean.getBoolean("gtng.draconicflight.serverQA"))
            FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("draconic-flight-qa-" + System.currentTimeMillis(), "Draconic Flight QA",
                    new WorldSettings(74110L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (++ticks > 3000) throw new AssertionError("draconic flight client QA timed out");
            if (!equipped || mc.thePlayer == null || mc.theWorld == null) return;
            ItemStack chest = mc.thePlayer.inventory.armorInventory[2];
            if (chest == null || !(chest.getItem() instanceof DraconicArmor)) return;
            if (stage == 0 && mc.thePlayer.capabilities.getFlySpeed() > 0.3f) {
                equal(0.35, mc.thePlayer.capabilities.getFlySpeed(), "600 percent native flight speed");
                speed = DraconicFlightServerChecks.field(chest, 38, "ArmorFlightSpeedMult");
                equal(10, speed.castMaxToDouble(), "GUI field limit is 1000 percent");
                gui = new FlightGui(mc);
                mc.displayGuiScreen(gui);
                gui.setEditingItem(chest, 38);
                gui.setFieldBeingEdited(speed);
                gui.adjuster().mouseClicked(180, 40, 0);
                equal(10, speed.castToDouble(), "native GUI +++ reaches new maximum");
                require(speed.getFormattedValue().replace(",", "").replace(" ", "").equals("+1000%"),
                    "native GUI formats +1000 percent: " + speed.getFormattedValue());
                stage = 1;
            } else if (
                stage == 1 && serverStage >= 1 && Math.abs(mc.thePlayer.capabilities.getFlySpeed() - 0.55f) < 0.000001
            ) {
                equal(0.55, mc.thePlayer.capabilities.getFlySpeed(), "1000 percent native flight speed");
                gui.adjuster().mouseClicked(60, 40, 0);
                equal(9.9, speed.castToDouble(), "native GUI decreases by ten percent");
                stage = 2;
            } else if (stage == 2 && serverStage >= 2) {
                gui.adjuster().mouseClicked(125, 40, 0);
                equal(10, speed.castToDouble(), "native GUI increases back to maximum");
                gui.adjuster().mouseClicked(180, 40, 0);
                equal(10, speed.castToDouble(), "native GUI caps repeated increments at maximum");
                stage = 3;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (!equipped) {
                if (++serverTicks < 60) return;
                int verified = new DraconicFlightServerChecks().verify(server);
                System.out.println("DRACONIC_FLIGHT_INTEGRATED_SERVER_QA: PASS (" + verified + " checks)");
                ItemStack chest = new ItemStack(ModItems.draconicChest);
                IConfigurableItem.ProfileHelper.setFloat(chest, "ArmorFlightSpeedMult", 6f);
                player.inventory.armorInventory[2] = chest;
                player.inventoryContainer.detectAndSendChanges();
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                equipped = true;
                return;
            }
            ItemStack chest = player.inventory.armorInventory[2];
            float value = IConfigurableItem.ProfileHelper.getFloat(chest, "ArmorFlightSpeedMult", -1);
            if (serverStage == 0 && value == 10f) serverStage = 1;
            else if (serverStage == 1 && Math.abs(value - 9.9f) < 0.000001) serverStage = 2;
            else if (serverStage == 2 && value == 10f) {
                ItemStack restored = ItemStack.loadItemStackFromNBT(chest.writeToNBT(new NBTTagCompound()));
                equal(10, IConfigurableItem.ProfileHelper.getFloat(restored, "ArmorFlightSpeedMult", -1),
                    "actual network-adjusted server item persists 1000 percent");
                serverStage = 3;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || stage != 3 || serverStage != 3 || finished) return;
        try {
            if (++frames < 15) return;
            Minecraft mc = Minecraft.getMinecraft();
            require(mc.currentScreen == gui, "native tool configuration screen remains open");
            equal(0.55, mc.thePlayer.capabilities.getFlySpeed(), "native flight speed retains new setting");
            ScreenShotHelper.saveScreenshot(new File(System.getProperty("gtng.draconicflight.qa.output")),
                "draconic-flight-1000.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            writeResult("PASS");
            System.out.println("DRACONIC_FLIGHT_CLIENT_QA: PASS (" + checks + " client checks)");
            finished = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static final class FlightGui extends GUIToolConfig {

        private FlightGui(Minecraft mc) {
            super(mc.thePlayer, new ContainerAdvTool(mc.thePlayer.inventory, new InventoryTool(mc.thePlayer, null)));
        }

        private ComponentFieldAdjuster adjuster() {
            return (ComponentFieldAdjuster) collection.getComponent("FIELD_CONFIG_BUTTON_ARRAY");
        }
    }

    private static void writeResult(String value) throws Exception {
        Files.write(Path.of(System.getProperty("gtng.draconicflight.qa.output"), "result.txt"),
            value.getBytes(StandardCharsets.UTF_8));
    }

    private static void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            writeResult("FAIL: " + failure);
        } catch (Exception writeFailure) {
            failure.addSuppressed(writeFailure);
        }
        FMLCommonHandler.instance().exitJava(1, false);
    }

    private void equal(double expected, double actual, String message) {
        require(Math.abs(expected - actual) < 0.000001, message + ": " + actual);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
