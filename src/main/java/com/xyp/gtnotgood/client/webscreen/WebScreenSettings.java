package com.xyp.gtnotgood.client.webscreen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenResolution;

public final class WebScreenSettings {

    private WebScreenResolution resolution = WebScreenResolution.FullHd;
    private int frameRate = 100;
    private int jpegQuality = 90;
    private boolean muted;
    private int zoomPercent = 100;

    public synchronized WebScreenResolution getResolution() {
        return resolution;
    }

    public synchronized void setResolution(WebScreenResolution resolution) {
        this.resolution = resolution == null ? WebScreenResolution.FullHd : resolution;
    }

    public synchronized int getFrameRate() {
        return frameRate;
    }

    public synchronized void setFrameRate(int frameRate) {
        this.frameRate = Math.max(15, Math.min(100, frameRate));
    }

    public synchronized int getJpegQuality() {
        return jpegQuality;
    }

    public synchronized void setJpegQuality(int jpegQuality) {
        this.jpegQuality = Math.max(60, Math.min(100, jpegQuality));
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
    }

    public synchronized int getZoomPercent() {
        return zoomPercent;
    }

    public synchronized void setZoomPercent(int zoomPercent) {
        this.zoomPercent = Math.max(25, Math.min(300, zoomPercent));
    }

    public synchronized WebScreenSettings copy() {
        WebScreenSettings copied = new WebScreenSettings();
        copied.resolution = resolution;
        copied.frameRate = frameRate;
        copied.jpegQuality = jpegQuality;
        copied.muted = muted;
        copied.zoomPercent = zoomPercent;
        return copied;
    }

    public static WebScreenSettings load(File profile) {
        WebScreenSettings latest = ChromiumSession.getLatestSettings(profile);
        if (latest != null) return latest;
        WebScreenSettings loaded = new WebScreenSettings();
        Path settings = new File(profile, "webscreen-settings.json").toPath();
        try {
            if (!Files.isRegularFile(settings) || Files.size(settings) > 16384) return loaded;
            JsonObject saved = new JsonParser().parse(new String(Files.readAllBytes(settings), StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonElement resolution = saved.get("resolution");
            if (resolution != null && resolution.isJsonPrimitive()) {
                loaded.setResolution(WebScreenResolution.parse(resolution.getAsString()));
            }
            loaded.setFrameRate(readNumber(saved, "frameRate", 100, 15, 100));
            loaded.setJpegQuality(readNumber(saved, "jpegQuality", 90, 60, 100));
            loaded.setZoomPercent(readNumber(saved, "zoomPercent", 100, 25, 300));
            JsonElement muted = saved.get("muted");
            if (muted != null && muted.isJsonPrimitive()) loaded.setMuted(muted.getAsBoolean());
        } catch (Exception exception) {
            GTNotGood.LOG.warn("Unable to read web screen settings", exception);
        }
        return loaded;
    }

    private static int readNumber(JsonObject saved, String key, int fallback, int minimum, int maximum) {
        JsonElement value = saved.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            double parsed = value.getAsDouble();
            return Double.isFinite(parsed) ? (int) Math.max(minimum, Math.min(maximum, parsed)) : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public void save(File profile) throws IOException {
        JsonObject saved;
        synchronized (this) {
            saved = new JsonObject();
            saved.addProperty("resolution", resolution.option);
            saved.addProperty("frameRate", frameRate);
            saved.addProperty("jpegQuality", jpegQuality);
            saved.addProperty("muted", muted);
            saved.addProperty("zoomPercent", zoomPercent);
        }
        Files.createDirectories(profile.toPath());
        Path target = new File(profile, "webscreen-settings.json").toPath();
        Path pending = Files.createTempFile(profile.toPath(), "webscreen-settings-", ".tmp");
        try {
            Files.write(pending, saved.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
