package com.xyp.gtnotgood.commandtree;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.commandtree.accessor.NetHandlerPlayClientExtras;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.gui.CommandSuggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.ParseResults;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.ArgumentCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.util.SuggestionProviders;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "commandtreeqa",
    name = "Command Tree QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CommandTreeClientChecks {

    private static final String ValidCommand = "/bq_admin edit";
    private static final String MissingCommand = "/gtng_qa_missing_command";
    private boolean started;
    private volatile boolean finished;
    private volatile boolean initialPermissionsVerified;
    private volatile boolean grantRequested;
    private volatile boolean granted;
    private volatile Throwable serverFailure;
    private int ticks;
    private int serverTicks;
    private int stage;
    private int frames;
    private GuiChat chat;
    private CommandSuggestions suggestions;
    private CommandNode<ISuggestionProvider> preservedNode;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.commandtree.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null) mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        try {
            if (serverFailure != null) throw new AssertionError("Server fixture failed", serverFailure);
            if (++ticks > 6000) throw new AssertionError("Command tree QA timed out at stage " + stage);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("command-tree-qa-" + System.currentTimeMillis(), "Command Tree QA",
                    new WorldSettings(83171L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || granted) return;
        try {
            var server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null || server.getConfigurationManager().playerEntityList.isEmpty() || ++serverTicks < 100)
                return;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            ICommand command = (ICommand) server.getCommandManager().getCommands().get("bq_admin");
            require(command != null, "real BetterQuesting admin command registered");
            if (!initialPermissionsVerified) {
                require(!command.canCommandSenderUseCommand(player), "login starts without admin permission");
                initialPermissionsVerified = true;
            }
            if (grantRequested) {
                server.getConfigurationManager().setCommandsAllowedForAll(true);
                require(command.canCommandSenderUseCommand(player), "permission granted after login");
                player.worldObj.setWorldTime(6000);
                player.worldObj.getWorldInfo().setRaining(false);
                granted = true;
            }
        } catch (Throwable failure) {
            serverFailure = failure;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !initialPermissionsVerified) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;
        try {
            NetHandlerPlayClientExtras connection = (NetHandlerPlayClientExtras) mc.thePlayer.sendQueue;
            CommandDispatcher<ISuggestionProvider> dispatcher = connection.brigo$commands();
            if (stage == 0) {
                if (dispatcher.getRoot().getChildren().isEmpty()) return;
                require(dispatcher.getRoot().getChild("bq_admin") == null,
                    "login tree omits unavailable admin command");
                preservedNode = LiteralArgumentBuilder.<ISuggestionProvider>literal("qakeep")
                    .then(LiteralArgumentBuilder.<ISuggestionProvider>literal("detail")).build();
                dispatcher.getRoot().addChild(preservedNode);
                log("LOGIN restricted roots=" + dispatcher.getRoot().getChildren().size());
                grantRequested = true;
                advance(1);
            } else if (stage == 1) {
                if (!granted) return;
                require(dispatcher.getRoot().getChild("bq_admin") == null,
                    "permission change leaves login snapshot stale");
                openChat(mc, ValidCommand);
                advance(2);
            } else if (stage == 2 || stage == 5) {
                if (!validParse() || !pending().isDone() || ++frames < 20) return;
                verifyValid(dispatcher);
                screenshot(mc, stage == 2 ? "command-tree-promoted.png" : "command-tree-reopened.png");
                if (stage == 2) {
                    setText("/bq_admin e");
                    advance(3);
                } else {
                    setText(MissingCommand);
                    advance(6);
                }
            } else if (stage == 3) {
                if (!pending().isDone() || !input().getText().equals("/bq_admin e") || ++frames < 20) return;
                Suggestions completed = pending().join();
                require(completed.getList().stream().anyMatch(value -> value.getText().equals("edit")),
                    "real server supplies edit subcommand");
                log("SERVER_COMPLETION " + completed.getList());
                screenshot(mc, "command-tree-subcommand.png");
                mc.displayGuiScreen(null);
                CommandDispatcher<ISuggestionProvider> stale = new CommandDispatcher<>();
                stale.getRoot().addChild(preservedNode);
                connection.brigo$setCommands(stale);
                require(connection.brigo$commands().getRoot().getChild("bq_admin") == null,
                    "reopen starts with nonempty stale tree");
                advance(4);
            } else if (stage == 4) {
                if (++frames < 20) return;
                openChat(mc, ValidCommand);
                advance(5);
            } else if (stage == 6) {
                ParseResults<ISuggestionProvider> parsed = parse();
                if (!parsed.getReader().getString().equals(MissingCommand) || !pending().isDone() || ++frames < 20)
                    return;
                require(parsed.getReader().canRead(), "unknown command remains unparsed");
                require(CommandSuggestions.getParseException(parsed) != null, "unknown command retains parse error");
                require(formatted(parsed).contains(EnumChatFormatting.RED.toString()), "unknown command stays red");
                require(!usage().isEmpty(), "unknown command shows error usage");
                log("UNKNOWN remaining=" + parsed.getReader().getRemaining() + " usage=" + usage());
                screenshot(mc, "command-tree-unknown.png");
                Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println(
                    "COMMAND_TREE_QA_PASS permission change, chat refresh, server completion, reopen, unknown command");
                finished = true;
                mc.shutdown();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyValid(CommandDispatcher<ISuggestionProvider> dispatcher) throws Exception {
        ParseResults<ISuggestionProvider> parsed = parse();
        require(!parsed.getReader().canRead() && parsed.getExceptions().isEmpty(), "admin command parses completely");
        require(CommandSuggestions.getParseException(parsed) == null, "admin command has no parse error");
        require(!formatted(parsed).contains(EnumChatFormatting.RED.toString()), "admin command is not highlighted red");
        for (String line : usage())
            require(!line.contains("Unknown") && !line.contains(" at position "), "admin command usage has no error");
        CommandNode<ISuggestionProvider> admin = dispatcher.getRoot().getChild("bq_admin");
        require(admin != null && admin.getChild("params") instanceof ArgumentCommandNode,
            "discovered admin command has generic parameters");
        ArgumentCommandNode<ISuggestionProvider, ?> params = (ArgumentCommandNode<ISuggestionProvider, ?>) admin
            .getChild("params");
        require(params.getCustomSuggestions() == SuggestionProviders.ASK_SERVER,
            "generic parameters delegate suggestions to server");
        require(dispatcher.getRoot().getChild("qakeep") == preservedNode && preservedNode.getChild("detail") != null,
            "existing detailed command node is preserved");
        log("VALID stage=" + stage
            + " roots="
            + dispatcher.getRoot().getChildren().size()
            + " cursor="
            + parsed.getReader().getCursor()
            + " errors="
            + parsed.getExceptions().size()
            + " usage="
            + usage());
    }

    private void openChat(Minecraft mc, String text) throws Exception {
        chat = new GuiChat(text);
        mc.displayGuiScreen(chat);
        suggestions = (CommandSuggestions) field(chat, "brigo$commandSuggestions");
        require(suggestions != null, "real GuiChat initializes command suggestions");
        log("CHAT_OPEN " + text);
    }

    private void setText(String text) throws Exception {
        input().setText(text);
        input().setCursorPositionEnd();
    }

    private boolean validParse() throws Exception {
        ParseResults<ISuggestionProvider> parsed = parse();
        return parsed != null && parsed.getReader().getString().equals(ValidCommand)
            && !parsed.getReader().canRead()
            && parsed.getExceptions().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private ParseResults<ISuggestionProvider> parse() throws Exception {
        return (ParseResults<ISuggestionProvider>) field(suggestions, "currentParse");
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Suggestions> pending() throws Exception {
        return (CompletableFuture<Suggestions>) field(suggestions, "pendingSuggestions");
    }

    @SuppressWarnings("unchecked")
    private List<String> usage() throws Exception {
        return (List<String>) field(suggestions, "commandUsage");
    }

    private GuiTextField input() throws Exception {
        return (GuiTextField) field(suggestions, "input");
    }

    private String formatted(ParseResults<ISuggestionProvider> parsed) throws Exception {
        Method formatter = CommandSuggestions.class.getDeclaredMethod("formatParsedCommand", ParseResults.class,
            String.class, int.class);
        formatter.setAccessible(true);
        return (String) formatter.invoke(null, parsed, input().getText(), 0);
    }

    private static Object field(Object instance, String name) throws Exception {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field value = type.getDeclaredField(name);
                value.setAccessible(true);
                return value.get(instance);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private void advance(int next) {
        stage = next;
        frames = 0;
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.commandtree.qa.output"));
        folder.mkdirs();
        return folder;
    }

    private static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void log(String message) throws Exception {
        Files.write(new File(output(), "structure.log").toPath(),
            (message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
        System.out.println("COMMAND_TREE_QA " + message);
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("COMMAND_TREE_QA: " + message);
    }
}
