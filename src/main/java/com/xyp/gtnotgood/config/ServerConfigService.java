package com.xyp.gtnotgood.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.packet.ServerConfigMessage;
import com.xyp.gtnotgood.config.ServerConfigOptions.Option;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Applies administrator edits on the server thread, with validation, stale-edit detection and durable writes. */
public final class ServerConfigService {

    public static final int READ = 0, APPLY = 1, SNAPSHOT = 2, SAVED = 3, RESTART = 4, DENIED = 5, CONFLICT = 6,
        INVALID = 7, FAILED = 8;
    private static final Map<EntityPlayerMP, ServerConfigMessage> PENDING = new ConcurrentHashMap<>();

    private static boolean allowed(EntityPlayerMP player) {
        return player.canCommandSenderUseCommand(2, "gtnotgood.config");
    }

    public static void enqueue(EntityPlayerMP player, ServerConfigMessage message) {
        // One outstanding request per connection bounds queue growth even for repeated packets.
        PENDING.putIfAbsent(player, message);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        MinecraftServer server = MinecraftServer.getServer();
        PENDING.forEach((player, message) -> {
            if (!PENDING.remove(player, message)) return;
            if (server == null || !server.getConfigurationManager().playerEntityList.contains(player)) return;
            if (!allowed(player)) {
                GTNotGood.channel
                    .sendTo(new ServerConfigMessage(message.requestId, DENIED, Collections.emptyMap()), player);
                return;
            }
            Map<String, Option> options = ServerConfigOptions.options();
            int status = message.status == READ ? SNAPSHOT : apply(message, options);
            GTNotGood.channel.sendTo(new ServerConfigMessage(message.requestId, status, snapshot(options)), player);
        });
    }

    private static Map<String, String> snapshot(Map<String, Option> options) {
        Map<String, String> values = new LinkedHashMap<>();
        options.forEach(
            (id, option) -> values.put(
                id,
                option.property()
                    .getString()));
        return values;
    }

    private static int apply(ServerConfigMessage message, Map<String, Option> options) {
        if (message.status != APPLY) return INVALID;
        Map<String, String> before = snapshot(options);
        if (!before.equals(message.expected)) return CONFLICT;
        Map<String, Object> parsed = new LinkedHashMap<>();
        try {
            message.values.forEach((id, value) -> {
                Option option = options.get(id);
                if (option == null) throw new IllegalArgumentException();
                parsed.put(id, option.parse(value));
            });
        } catch (IllegalArgumentException e) {
            return INVALID;
        }
        Map<Configuration, byte[]> backups = new LinkedHashMap<>();
        try {
            for (String id : parsed.keySet()) {
                Configuration config = options.get(id)
                    .configuration();
                if (!backups.containsKey(config)) backups.put(
                    config,
                    Files.readAllBytes(
                        config.getConfigFile()
                            .toPath()));
            }
            message.values.forEach(
                (id, value) -> options.get(id)
                    .property()
                    .set(value));
            for (Configuration config : backups.keySet()) save(config);
        } catch (IOException | RuntimeException e) {
            before.forEach(
                (id, value) -> options.get(id)
                    .property()
                    .set(value));
            backups.forEach((config, bytes) -> {
                try {
                    Files.write(
                        config.getConfigFile()
                            .toPath(),
                        bytes);
                } catch (IOException rollbackFailure) {
                    GTNotGood.LOG.error("Could not restore configuration file", rollbackFailure);
                }
            });
            GTNotGood.LOG.error("Could not save server configuration", e);
            return FAILED;
        }
        boolean restart = false;
        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            Option option = options.get(entry.getKey());
            option.applyRuntime(entry.getValue());
            restart |= !option.live && !before.get(option.id)
                .equals(message.values.get(option.id));
        }
        return restart ? RESTART : SAVED;
    }

    /**
     * Writes via a sibling temporary file. Forge's save method swallows IO errors, so it cannot acknowledge a remote
     * save.
     *
     * @param config existing configuration with accepted values
     * @throws IOException if serialization or file replacement fails
     */
    private static void save(Configuration config) throws IOException {
        Path destination = config.getConfigFile()
            .toPath();
        Path temporary = Files.createTempFile(destination.getParent(), "server-config-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write("# Configuration file\n\n");
                for (String name : config.getCategoryNames()) {
                    ConfigCategory category = config.getCategory(name);
                    if (!category.isChild()) {
                        category.write(writer, 0);
                        writer.newLine();
                    }
                }
            }
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
