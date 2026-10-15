package com.xyp.gtnotgood.client.radialmenu.client.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;

public class CommandRadialMenu extends CommandBase {

    @Override
    public String getCommandName() {
        return "radialmenu";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/radialmenu <edit|profile|profiles|reload>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            reply(sender, getCommandUsage(sender));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("edit".equals(sub)) {
            GuiStack.requestOpen(new GuiProfileManager());
            return;
        }

        if ("profiles".equals(sub)) {
            List<String> names = ProfileStorage.listProfileNames();
            // #tr radialmenu.command.profiles
            // # Profiles: %s
            // # zh_CN 轮盘配置：%s
            reply(sender, I18n.format("radialmenu.command.profiles", names.isEmpty() ? "-" : join(names)));
            // #tr radialmenu.command.active
            // # Active profile: %s
            // # zh_CN 当前轮盘配置：%s
            reply(sender, I18n.format("radialmenu.command.active", ProfileManager.activeName()));
            return;
        }

        if ("profile".equals(sub)) {
            if (args.length < 2) {
                reply(sender, I18n.format("radialmenu.command.active", ProfileManager.activeName()));
                return;
            }
            String target = args[1];
            if (ProfileManager.switchTo(target)) {
                // #tr radialmenu.command.switched
                // # Switched to profile %s
                // # zh_CN 已切换到轮盘配置 %s
                reply(sender, I18n.format("radialmenu.command.switched", target));
            } else {
                // #tr radialmenu.command.missing
                // # Profile not found: %s
                // # zh_CN 找不到轮盘配置：%s
                replyError(sender, I18n.format("radialmenu.command.missing", target));
            }
            return;
        }

        if ("reload".equals(sub)) {
            ProfileManager.load();
            // #tr radialmenu.command.reloaded
            // # Reloaded radial profiles
            // # zh_CN 已重新加载轮盘配置
            reply(sender, I18n.format("radialmenu.command.reloaded"));
            return;
        }

        reply(sender, getCommandUsage(sender));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "edit", "profile", "profiles", "reload");
        }
        if (args.length == 2 && "profile".equalsIgnoreCase(args[0])) {
            List<String> names = ProfileStorage.listProfileNames();
            return getListOfStringsMatchingLastWord(args, names.toArray(new String[0]));
        }
        return new ArrayList<>();
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static void reply(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[RadialMenu] " + message));
    }

    private static void replyError(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[RadialMenu] " + message));
    }
}
