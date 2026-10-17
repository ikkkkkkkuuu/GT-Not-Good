package com.xyp.gtnotgood.client.webscreen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnotgood.GTNotGood;

public final class WebScreenProfiles implements AutoCloseable {

    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private final Path directory;
    private final Path file;
    private final LinkedHashMap<UUID, UUID> aliases = new LinkedHashMap<>(16, 0.75f, true);
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WebScreen-Profiles");
        thread.setDaemon(true);
        return thread;
    });
    private Map<UUID, UUID> pendingSave;
    private boolean saving;
    private boolean closed;

    public WebScreenProfiles(File scopeDirectory) {
        directory = scopeDirectory.toPath();
        file = directory.resolve("screens.json");
        load();
    }

    public synchronized UUID find(UUID memberId) {
        return aliases.get(memberId);
    }

    public synchronized void bind(Collection<UUID> members, UUID profileId) {
        if (closed || profileId == null || members == null) return;
        boolean changed = false;
        for (UUID member : members) {
            if (member != null && !profileId.equals(aliases.put(member, profileId))) changed = true;
            if (aliases.size() > MAX_ENTRIES) aliases.remove(aliases.keySet().iterator().next());
        }
        if (changed) scheduleSave();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
                throw new IOException("Profile aliases exceed size limit");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (output.size() + read > MAX_FILE_BYTES) {
                        throw new IOException("Profile aliases exceed size limit");
                    }
                    output.write(buffer, 0, read);
                }
            }
            JsonElement parsed = new JsonParser().parse(new String(output.toByteArray(), StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("Invalid profile aliases");
            JsonObject object = parsed.getAsJsonObject();
            if (object.entrySet().size() > MAX_ENTRIES) throw new IOException("Too many profile aliases");
            LinkedHashMap<UUID, UUID> loaded = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IOException("Invalid profile alias UUID");
                }
                loaded.put(parseUuid(entry.getKey()), parseUuid(value.getAsString()));
            }
            aliases.putAll(loaded);
        } catch (IOException | RuntimeException exception) {
            GTNotGood.LOG.warn("Ignoring invalid web screen profile aliases");
        }
    }

    private static UUID parseUuid(String text) {
        UUID id = UUID.fromString(text);
        if (!id.toString().equalsIgnoreCase(text)) throw new IllegalArgumentException("Invalid profile alias UUID");
        return id;
    }

    private void scheduleSave() {
        pendingSave = new LinkedHashMap<>(aliases);
        if (saving) return;
        saving = true;
        writer.execute(this::savePending);
    }

    private void savePending() {
        while (true) {
            Map<UUID, UUID> snapshot;
            synchronized (this) {
                snapshot = pendingSave;
                pendingSave = null;
                if (snapshot == null) {
                    saving = false;
                    return;
                }
            }
            save(snapshot);
        }
    }

    private void save(Map<UUID, UUID> snapshot) {
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            JsonObject object = new JsonObject();
            snapshot.forEach((member, profile) -> object.addProperty(member.toString(), profile.toString()));
            temporary = Files.createTempFile(directory, "screens-", ".json.tmp");
            Files.write(temporary, object.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            GTNotGood.LOG.warn("Unable to save web screen profile aliases", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    GTNotGood.LOG.warn("Unable to remove temporary web screen profile aliases");
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        scheduleSave();
        writer.shutdown();
    }
}
