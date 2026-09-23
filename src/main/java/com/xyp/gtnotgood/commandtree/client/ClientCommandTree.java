package com.xyp.gtnotgood.commandtree.client;

import java.util.Map;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;

/** Adds Forge client commands to the suggestions sent by the server. */
public final class ClientCommandTree {

    private ClientCommandTree() {}

    /**
     * Adds registered client command names and aliases that the local player may use.
     * Calling this again is safe; it also picks up commands registered after login.
     *
     * @param dispatcher the current suggestions tree
     * @param commands   Forge's client command map, including aliases
     * @param sender     the local player whose permissions should be checked
     */
    public static void merge(CommandDispatcher<ISuggestionProvider> dispatcher, Map<String, ICommand> commands,
        ICommandSender sender) {
        for (Map.Entry<String, ICommand> entry : commands.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isEmpty()
                || dispatcher.getRoot()
                    .getChild(name) != null
                || !entry.getValue()
                    .canCommandSenderUseCommand(sender)) {
                continue;
            }
            dispatcher.register(
                LiteralArgumentBuilder.<ISuggestionProvider>literal(name)
                    .then(
                        RequiredArgumentBuilder
                            .<ISuggestionProvider, String>argument("params", StringArgumentType.greedyString())));
        }
    }
}
