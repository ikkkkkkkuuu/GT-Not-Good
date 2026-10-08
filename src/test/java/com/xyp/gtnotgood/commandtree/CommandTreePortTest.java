package com.xyp.gtnotgood.commandtree;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import org.junit.Test;

import com.xyp.gtnotgood.commandtree.client.ClientCommandTree;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.network.CommandsPacket;
import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentTypes;
import com.xyp.gtnotgood.commandtree.commodore.file.CommodoreFileReader;
import com.xyp.gtnotgood.commandtree.compat.CompatMod;
import com.xyp.gtnotgood.commandtree.compat.mods.BaublesMod;
import com.xyp.gtnotgood.commandtree.compat.mods.MinecraftMod;
import com.xyp.gtnotgood.commandtree.compat.mods.ThaumcraftMod;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.RootCommandNode;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Verifies the imported command definitions and command-tree wire format. */
public class CommandTreePortTest {

    /** Client commands and aliases must appear even when the server did not advertise them. */
    @Test
    public void clientCommandsMergeIntoSuggestions() {
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        ICommand allowed = new CommandBase() {

            @Override
            public String getCommandName() {
                return "gtngtexteffects";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/gtngtexteffects";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] arguments) {}

            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender) {
                return true;
            }
        };
        Map<String, ICommand> commands = new HashMap<>();
        commands.put("gtngtexteffects", allowed);
        commands.put("textfx", allowed);
        ClientCommandTree.merge(dispatcher, commands, null);
        assertNotNull(dispatcher.getRoot().getChild("gtngtexteffects"));
        assertNotNull(dispatcher.getRoot().getChild("textfx"));
    }

    /** Every bundled definition must parse from the final resource path. */
    @Test
    public void bundledDefinitionsParse() throws Exception {
        for (CompatMod mod : new CompatMod[] { new MinecraftMod(), new BaublesMod(), new ThaumcraftMod() }) {
            for (String commandClass : mod.commands()) {
                String resource = "/assets/" + ModList.GTNotGood
                    .getID() + "/commandtree/commands/" + mod.identifier() + "/" + commandClass + ".commodore";
                try (InputStream input = getClass().getResourceAsStream(resource)) {
                    assertNotNull(resource, input);
                    LiteralCommandNode<Object> node = CommodoreFileReader.INSTANCE.parse(input);
                    assertFalse(resource, node.getChildren().isEmpty());
                    if ("gamemode".equals(node.getLiteral())) {
                        assertNotNull(node.getChild("creative"));
                        assertNotNull(node.getChild("0"));
                        assertNull(node.getChild("spectator"));
                        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
                        dispatcher.getRoot().addChild(node);
                        assertFalse(dispatcher.getCompletionSuggestions(dispatcher.parse("gamemode c", new Object()))
                            .join().isEmpty());
                    }
                }
            }
        }
    }

    /** Argument nodes and the registered server-completion marker survive serialization. */
    @Test
    public void argumentTreeSurvivesPacketRoundTrip() {
        ArgumentTypes.init();
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<ISuggestionProvider>literal("example")
            .then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("player", StringArgumentType.word())));
        byte[] encoded = CommandsPacket.create(dispatcher.getRoot());
        RootCommandNode<ISuggestionProvider> decoded = CommandsPacket.read(encoded);
        assertNotNull(decoded.getChild("example"));
        assertNotNull(decoded.getChild("example").getChild("player"));
    }
}
