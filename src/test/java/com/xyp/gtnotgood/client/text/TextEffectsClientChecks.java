package com.xyp.gtnotgood.client.text;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtnotgood.client.text.preview.TextEffectPreview;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in GL smoke test: all presets, machine credit, input mapping and resource reload, without opening a world. */
@Mod(
    modid = "texteffectsqa",
    name = "Text Effects QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public class TextEffectsClientChecks {

    private Preview screen;
    private int frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.text.qa")) {
            System.out.println("TEXT_EFFECTS_QA_START");
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (screen == null && mc.theWorld == null && mc.currentScreen != null) {
            System.out.println(
                "TEXT_EFFECTS_QA_SCREEN " + mc.currentScreen.getClass()
                    .getName());
            mc.gameSettings.pauseOnLostFocus = false;
            screen = new Preview();
            mc.displayGuiScreen(screen);
            require(
                TextEffectRegistry.identifiers()
                    .size() == 12,
                "all twelve presets registered");
            String plain = "GOLD";
            require(
                mc.fontRenderer.getStringWidth(plain)
                    == mc.fontRenderer.getStringWidth(TextEffects.apply(plain, TextEffects.BURNISHED_AURIC)),
                "effect markers must have zero display width");
            GuiTextField field = new codechicken.nei.FormattedTextField(mc.fontRenderer, 0, 0, 150, 20);
            field.setMaxStringLength(100);
            field.setText("&{ba}GOLD");
            require(
                field.getText()
                    .equals("&{ba}GOLD"),
                "NEI preserves editable declarations");
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || screen == null) return;
        frames++;
        if (frames == 60 || frames == 120 || frames == 180) {
            Field failed = EffectTextRenderer.class.getDeclaredField("failed");
            failed.setAccessible(true);
            require(((Set<?>) failed.get(EffectTextRenderer.INSTANCE)).isEmpty(), "shader compilation/rendering");
            Minecraft mc = Minecraft.getMinecraft();
            String variant = ModList.Angelica.isModLoaded() ? "angelica" : "vanilla";
            File output = new File(System.getProperty("gtng.text.qa.output"));
            output.mkdirs();
            ScreenShotHelper.saveScreenshot(
                output,
                variant + "-" + frames + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (frames == 60) {
                Field page = TextEffectPreview.class.getDeclaredField("page");
                page.setAccessible(true);
                page.setInt(screen, 1);
                Field bold = TextEffectPreview.class.getDeclaredField("bold");
                bold.setAccessible(true);
                bold.setBoolean(screen, true);
                Field italic = TextEffectPreview.class.getDeclaredField("italic");
                italic.setAccessible(true);
                italic.setBoolean(screen, true);
            } else if (frames == 120) {
                mc.refreshResources();
            } else {
                System.out.println("TEXT_EFFECTS_QA_PASS " + variant);
                mc.shutdown();
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Adds the actual MachineLoader credit supplier to the upstream preview. */
    public static class Preview extends TextEffectPreview {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            fontRendererObj.drawStringWithShadow(AnimatedText.GT_NOT_GOOD.get(), 18, height - 42, 0xFFFFFF);
        }
    }
}
