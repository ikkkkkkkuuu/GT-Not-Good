package com.xyp.gtnotgood.client.radialmenu;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.radialmenu.client.action.ActionExecutors;
import com.xyp.gtnotgood.client.radialmenu.client.action.CommandActionExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.action.KeyActionExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.action.ProfileSwitchExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.action.ScriptActionExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.action.ScriptResumeExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.action.SequenceExecutor;
import com.xyp.gtnotgood.client.radialmenu.client.command.CommandRadialMenu;
import com.xyp.gtnotgood.client.radialmenu.client.gui.WheelOverlayHandler;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconResourceReloadHandler;
import com.xyp.gtnotgood.client.radialmenu.client.input.WheelInputHandler;
import com.xyp.gtnotgood.client.radialmenu.client.input.WheelKeyBindings;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.client.script.ChatCapture;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.config.AccentConfig;
import com.xyp.gtnotgood.client.radialmenu.config.AnimationConfig;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;
import com.xyp.gtnotgood.client.radialmenu.config.WheelConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;

public final class RadialMenuMod {

    public static final String MODID = ModList.ModIds.GT_NOT_GOOD;
    public static final String MODNAME = ModList.Names.GT_NOT_GOOD;
    public static final Logger LOG = GTNotGood.LOG;

    private RadialMenuMod() {}

    public static void preInit() {
        try {
            ConfigurationManager.registerConfig(RadialMenuConfig.class);
            ConfigurationManager.registerConfig(WheelConfig.class);
            ConfigurationManager.registerConfig(ColorConfig.class);
            ConfigurationManager.registerConfig(AccentConfig.class);
            ConfigurationManager.registerConfig(AnimationConfig.class);
        } catch (ConfigException e) {
            throw new IllegalStateException("Cannot load radial menu settings", e);
        }
        ProfileStorage.init(Minecraft.getMinecraft().mcDataDir);
        ActionTypes.registerDefaults();
        ActionExecutors.register(new KeyActionExecutor());
        ActionExecutors.register(new ProfileSwitchExecutor());
        ActionExecutors.register(new CommandActionExecutor());
        ActionExecutors.register(new SequenceExecutor());
        ActionExecutors.register(new ScriptActionExecutor());
        ActionExecutors.register(new ScriptResumeExecutor());
        ActionExecutors.register(new ToolBeltMenu());
    }

    public static void init() {
        ProfileManager.load();
        ClientCommandHandler.instance.registerCommand(new CommandRadialMenu());
        IconResourceReloadHandler.register();
        MinecraftForge.EVENT_BUS.register(new WheelOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new ChatCapture.Listener());
        WheelKeyBindings.register();
        FMLCommonHandler.instance().bus().register(new WheelInputHandler());
    }
}
