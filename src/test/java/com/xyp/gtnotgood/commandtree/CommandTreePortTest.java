package com.xyp.gtnotgood.commandtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import com.xyp.gtnotgood.commandtree.shadow.brigadier.ParseResults;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
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

    @Test
    public void serverCommandsRepairMissingRootsAndRequestArgumentCompletions() {
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        ServerSuggestionProvider provider = new ServerSuggestionProvider();
        assertTrue(parseCommand(dispatcher, "/bq_admin edit", provider).getReader().canRead());

        ClientCommandTree.mergeServer(dispatcher, new SuggestionsBuilder("/", 0).suggest("/bq_admin").build());

        ParseResults<ISuggestionProvider> parsed = parseCommand(dispatcher, "/bq_admin edit", provider);
        assertFalse(parsed.getReader().canRead());
        assertTrue(parsed.getExceptions().isEmpty());
        Suggestions suggestions = dispatcher.getCompletionSuggestions(parseCommand(dispatcher, "/bq_admin e", provider))
            .join();
        assertEquals("/bq_admin e", provider.requestedInput);
        assertEquals(1, suggestions.getList().size());
        assertEquals("/bq_admin edit", suggestions.getList().get(0).apply("/bq_admin e"));
    }

    @Test
    public void serverRootFallbackPreservesSpecificSyntax() {
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        LiteralCommandNode<ISuggestionProvider> gamemode = dispatcher
            .register(LiteralArgumentBuilder.<ISuggestionProvider>literal("gamemode")
                .then(LiteralArgumentBuilder.<ISuggestionProvider>literal("creative")));

        ClientCommandTree.mergeServer(dispatcher, new SuggestionsBuilder("/", 0).suggest("/gamemode").build());

        assertSame(gamemode, dispatcher.getRoot().getChild("gamemode"));
        assertNotNull(gamemode.getChild("creative"));
        assertNull(gamemode.getChild("params"));
        ServerSuggestionProvider provider = new ServerSuggestionProvider();
        assertFalse(parseCommand(dispatcher, "/gamemode creative", provider).getReader().canRead());
        assertTrue(parseCommand(dispatcher, "/gamemode invalid", provider).getReader().canRead());
        Suggestions suggestions = dispatcher.getCompletionSuggestions(parseCommand(dispatcher, "/gamemode c", provider))
            .join();
        assertEquals("creative", suggestions.getList().get(0).getText());
        assertNull(provider.requestedInput);
    }

    @Test
    public void serverRootsAcceptNamesWithOrWithoutSlashAndMergeRepeatedly() {
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        Suggestions roots = new SuggestionsBuilder("/", 0).suggest("bq_client").suggest("/bq_admin").build();

        ClientCommandTree.mergeServer(dispatcher, roots);
        ClientCommandTree.mergeServer(dispatcher, roots);

        assertNotNull(dispatcher.getRoot().getChild("bq_client"));
        assertNotNull(dispatcher.getRoot().getChild("bq_admin"));
        assertNull(dispatcher.getRoot().getChild("q_client"));
        assertEquals(2, dispatcher.getRoot().getChildren().size());
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

    private static ParseResults<ISuggestionProvider> parseCommand(CommandDispatcher<ISuggestionProvider> dispatcher,
        String command, ISuggestionProvider provider) {
        StringReader reader = new StringReader(command);
        reader.skip();
        return dispatcher.parse(reader, provider);
    }

    private static final class ServerSuggestionProvider implements ISuggestionProvider {

        private String requestedInput;

        @Override
        public Collection<String> getPlayerNames() {
            return Collections.emptyList();
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context,
            SuggestionsBuilder builder) {
            requestedInput = context.getInput();
            return builder.suggest("edit").buildFuture();
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestionsFromServer() {
            return Suggestions.empty();
        }
    }
}
