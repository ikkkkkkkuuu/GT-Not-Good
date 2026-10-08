package com.xyp.gtnotgood.client.text;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.command.ICommand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.ClientCommandHandler;

import com.xyp.gtnotgood.client.text.preview.TextEffectPreview;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorPreferences;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;
import com.xyp.gtnotgood.utils.text.TextEffectsCompat;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

import codechicken.nei.FormattedTextField;
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
    private int commandStage;
    private GuiScreen commandSource;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.text.qa")) {
            System.out.println("TEXT_EFFECTS_QA_START");
            FMLCommonHandler.instance().bus().register(this);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (screen == null && mc.theWorld == null && mc.currentScreen != null) {
            ICommand command = (ICommand) ClientCommandHandler.instance.getCommands().get("gtngtexteffects");
            require(command != null, "GTNG preview command registered");
            if (TextEffectsCompat.hasUpstreamRenderer())
                require(ClientCommandHandler.instance.getCommands().containsKey("gtnltexteffects"),
                    "GTNL preview command still registered");
            if (commandStage == 0) {
                commandSource = mc.currentScreen;
                command.processCommand(null, new String[] { "wireless" });
                commandStage = 1;
                return;
            }
            if (mc.currentScreen == commandSource || !(mc.currentScreen instanceof TextEffectPreview)) return;
            Field wireless = TextEffectPreview.class.getDeclaredField("wirelessMonitor");
            wireless.setAccessible(true);
            require(wireless.getBoolean(mc.currentScreen) == (commandStage == 1), "command opens requested preview");
            if (commandStage == 1) {
                commandSource = mc.currentScreen;
                command.processCommand(null, new String[0]);
                commandStage = 2;
                return;
            }
            System.out.println("TEXT_EFFECTS_QA_SCREEN " + mc.currentScreen.getClass().getName());
            mc.gameSettings.pauseOnLostFocus = false;
            screen = new Preview();
            mc.displayGuiScreen(screen);
            require(TextEffectRegistry.identifiers().size() == 12, "all twelve presets registered");
            verifyImmediateSelection();
            String plain = "GOLD";
            require(
                mc.fontRenderer.getStringWidth(plain)
                    == mc.fontRenderer.getStringWidth(TextEffectsCompat.apply(plain, TextEffects.BURNISHED_AURIC)),
                "effect markers must have zero display width");
            verifyRendererPresets(mc);
            GuiTextField field = new FormattedTextField(mc.fontRenderer, 0, 0, 150, 20);
            field.setMaxStringLength(100);
            field.setText("&{ba}GOLD");
            require(field.getText().equals("&{ba}GOLD"), "NEI preserves editable declarations");
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || screen == null) return;
        frames++;
        if (frames == 60 || frames == 120 || frames == 180) {
            Class<?> renderer = TextEffectsCompat.hasUpstreamRenderer()
                ? Class.forName("com.science.gtnl.client.text.EffectTextRenderer")
                : EffectTextRenderer.class;
            Field failed = renderer.getDeclaredField("failed");
            failed.setAccessible(true);
            Object instance = renderer.getField("INSTANCE").get(null);
            require(((Set<?>) failed.get(instance)).isEmpty(), "shader compilation/rendering");
            Minecraft mc = Minecraft.getMinecraft();
            String variant = TextEffectsCompat.hasUpstreamRenderer() ? "gtnl"
                : ModList.Angelica.isModLoaded() ? "angelica" : "vanilla";
            File output = new File(System.getProperty("gtng.text.qa.output"));
            output.mkdirs();
            ScreenShotHelper.saveScreenshot(output, variant + "-" + frames + ".png", mc.displayWidth, mc.displayHeight,
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
                Files.write(new File(output, "result.txt").toPath(), Collections.singletonList("PASS"));
                mc.shutdown();
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyRendererPresets(Minecraft mc) throws Exception {
        String plain = "GT-Not-Good";
        for (String identifier : TextEffectRegistry.identifiers()) {
            TextEffectStyle style = new TextEffectStyle(identifier, Collections.emptyList(), 1);
            String span = TextEffectsCompat.apply(plain, style);
            require(mc.fontRenderer.getStringWidth(span) == mc.fontRenderer.getStringWidth(plain), "preset parsed");
            String inline = TextEffectsCompat.format(style) + plain + "\u00a7r";
            require(mc.fontRenderer.getStringWidth(inline) == mc.fontRenderer.getStringWidth(plain), "inline parsed");
            if (TextEffectsCompat.hasUpstreamRenderer()) {
                String upstream = identifier.replace(ModList.GTNotGood.getID() + ":",
                    ModList.GTNotLeisure.getID() + ":");
                Object effect = Class.forName("com.science.gtnl.client.text.TextEffectRegistry")
                    .getMethod("get", String.class).invoke(null, upstream);
                require(effect != null && span.contains(upstream), "GTNL owns the displayed preset");
            }
        }
        String hud = WirelessMonitorPreferences.decorate(plain);
        require(mc.fontRenderer.getStringWidth(hud) == mc.fontRenderer.getStringWidth(plain), "wireless HUD parsed");
    }

    /** Exercises the actual row and apply button, then restores the developer's client config byte-for-byte. */
    private void verifyImmediateSelection() throws Exception {
        File file = new File(Config.getConfigDirectory(), ModList.GTNotGood.getID() + "-text-effects.cfg");
        byte[] original = file.isFile() ? Files.readAllBytes(file.toPath()) : null;
        var oldStyle = AnimatedText.creditStyle();
        boolean oldBold = AnimatedText.creditBold();
        boolean oldItalic = AnimatedText.creditItalic();
        try {
            screen.selectFirstAndApply();
            String first = TextEffectRegistry.identifiers().get(0);
            require(AnimatedText.creditStyle().rendererId().equals(first), "clicked row takes effect immediately");
            require(
                AnimatedText.GT_NOT_GOOD.get()
                    .contains(TextEffectsCompat.apply(ModList.GTNotGood.getDisplayName(), AnimatedText.creditStyle())),
                "existing tooltip supplier sees new effect");
            TextEffectPreferences.load();
            require(AnimatedText.creditStyle().rendererId().equals(first), "clicked row persists after reload");
        } finally {
            if (original == null) Files.deleteIfExists(file.toPath());
            else Files.write(file.toPath(), original);
            AnimatedText.configureCredit(oldStyle, oldBold, oldItalic);
        }
    }

    /** Adds the actual MachineLoader credit supplier to the upstream preview. */
    public static class Preview extends TextEffectPreview {

        /** Selects the first visible row and activates the same button used by a player. */
        public void selectFirstAndApply() throws Exception {
            Field page = TextEffectPreview.class.getDeclaredField("page");
            page.setAccessible(true);
            page.setInt(this, 0);
            mouseClicked(80, 80, 0);
            for (Object entry : buttonList) {
                GuiButton button = (GuiButton) entry;
                if (button.id == 5) {
                    actionPerformed(button);
                    return;
                }
            }
            throw new AssertionError("machine credit apply button is missing");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            fontRendererObj.drawStringWithShadow(WirelessMonitorPreferences.decorate("EU/t +29,500,080 EU / 无线电网"), 18,
                height - 56, 0xFFFFFF);
            fontRendererObj.drawStringWithShadow(AnimatedText.GT_NOT_GOOD.get(), 18, height - 42, 0xFFFFFF);
        }
    }
}
