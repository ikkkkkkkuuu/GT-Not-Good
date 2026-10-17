package com.xyp.gtnotgood.client.webscreen;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenResolution;

public final class ChromiumSession implements AutoCloseable {

    public static final int DEFAULT_WIDTH = 1920;
    public static final int DEFAULT_HEIGHT = 1080;
    private static final int VIEWPORT_WIDTH = 960;
    private static final int VIEWPORT_HEIGHT = 540;
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
    private static final ConcurrentMap<Path, SettingsIntent> latestSettings = new ConcurrentHashMap<>();
    private final File profile;
    private final Path profilePath;
    private final String executable;
    private final WebScreenSettings settings;
    private final boolean loadSavedZoom;
    private final SettingsIntent settingsIntent;
    private final boolean muted;
    private final int width;
    private final int height;
    private final int quality;
    private final int frameRate;
    private final long frameIntervalNanos;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger decoding = new AtomicInteger();
    private final AtomicBoolean updatingViewport = new AtomicBoolean();
    private final AtomicBoolean capturePending = new AtomicBoolean();
    private volatile boolean metricsInFlight;
    private final AtomicBoolean zoomSaveQueued = new AtomicBoolean();
    private final AtomicBoolean zoomDirty = new AtomicBoolean();
    private final AtomicLong nextFrameNanos = new AtomicLong();
    private final AtomicLong frameSequence = new AtomicLong();
    private final AtomicReference<EncodedFrame> pendingImage = new AtomicReference<>();
    private final AtomicReference<BufferedImage> latestImage = new AtomicReference<>();
    private final Object frameLock = new Object();
    private final int decoderThreads;
    private final ThreadLocal<FrameDecoder> frameDecoder = new ThreadLocal<>();
    private long publishedSequence;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WebScreen-Browser");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService decoder;
    private final Thread shutdownHook;
    private volatile String status = "starting";
    private volatile String url = "about:blank";
    private volatile String requestedUrl = "about:blank";
    private volatile int zoomPercent = 100;
    private volatile int appliedZoomPercent = 100;
    private boolean zoomChanged;
    private volatile String targetId;
    private volatile String mainFrameId;
    private volatile DevToolsConnection connection;
    private volatile Process process;
    private RandomAccessFile profileLockFile;
    private FileLock profileLock;

    public ChromiumSession(File profile, String executable, boolean muted) {
        this(profile, executable, muted, DEFAULT_WIDTH, DEFAULT_HEIGHT, 90, 100);
    }

    public ChromiumSession(File profile, String executable, boolean muted, int width, int height, int quality,
        int frameRate) {
        this(profile, executable, muted, legacySettings(width, quality, frameRate), width, height, quality, frameRate,
            true);
    }

    public ChromiumSession(File profile, String executable, boolean forcedMuted, WebScreenSettings settings) {
        this(profile, executable, forcedMuted, settings, settings.getResolution().width,
            settings.getResolution().height, settings.getJpegQuality(), settings.getFrameRate(), false);
    }

    private ChromiumSession(File profile, String executable, boolean forcedMuted, WebScreenSettings settings, int width,
        int height, int quality, int frameRate, boolean loadSavedZoom) {
        if ((width != 1280 && width != 1920 && width != 2560 && width != 3840) || height != width / 16 * 9) {
            throw new IllegalArgumentException("Unsupported web screen resolution");
        }
        if (quality < 60 || quality > 100) throw new IllegalArgumentException("JPEG quality must be 60 to 100");
        if (frameRate < 15 || frameRate > 100) throw new IllegalArgumentException("Frame rate must be 15 to 100");
        this.profile = profile;
        profilePath = profile.toPath().toAbsolutePath().normalize();
        this.executable = executable == null ? "" : executable.trim();
        this.settings = settings;
        this.loadSavedZoom = loadSavedZoom;
        settingsIntent = loadSavedZoom ? null : new SettingsIntent(settings);
        if (settingsIntent != null) latestSettings.put(profilePath, settingsIntent);
        this.muted = forcedMuted || settings.isMuted();
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.frameRate = frameRate;
        zoomPercent = settings.getZoomPercent();
        appliedZoomPercent = zoomPercent;
        frameIntervalNanos = TimeUnit.SECONDS.toNanos(1) / frameRate;
        int targetThreads = width >= 3840 ? 4 : width >= 2560 ? 2 : 1;
        decoderThreads = Math.min(targetThreads, Math.max(1, (Runtime.getRuntime().availableProcessors() - 2) / 2));
        decoder = Executors.newFixedThreadPool(decoderThreads, runnable -> {
            Thread thread = new Thread(() -> {
                try {
                    runnable.run();
                } finally {
                    FrameDecoder current = frameDecoder.get();
                    if (current != null) current.close();
                    frameDecoder.remove();
                }
            }, "WebScreen-Frames");
            thread.setDaemon(true);
            return thread;
        });
        shutdownHook = new Thread(() -> {
            close();
            try {
                awaitClosed(5000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "WebScreen-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static WebScreenSettings legacySettings(int width, int quality, int frameRate) {
        WebScreenSettings settings = new WebScreenSettings();
        settings.setResolution(width == 3840 ? WebScreenResolution.UltraHd
            : width == 2560 ? WebScreenResolution.QuadHd : WebScreenResolution.FullHd);
        settings.setJpegQuality(quality);
        settings.setFrameRate(frameRate);
        return settings;
    }

    public WebScreenSettings getSettings() {
        return settings;
    }

    static WebScreenSettings getLatestSettings(File profile) {
        SettingsIntent intent = latestSettings.get(profile.toPath().toAbsolutePath().normalize());
        return intent == null ? null : intent.settings.copy();
    }

    public void start(String initialUrl) {
        if (closed.get() || !started.compareAndSet(false, true)) return;
        url = initialUrl;
        requestedUrl = initialUrl;
        execute(() -> launch(true));
    }

    public void persistSettings() {
        if (!started.compareAndSet(false, true)) return;
        execute(() -> launch(false));
    }

    public BufferedImage pollFrame() {
        return latestImage.getAndSet(null);
    }

    public String getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getViewportWidth() {
        return Math.round(VIEWPORT_WIDTH * 100F / appliedZoomPercent);
    }

    public int getViewportHeight() {
        return Math.round(VIEWPORT_HEIGHT * 100F / appliedZoomPercent);
    }

    public int getZoomPercent() {
        return zoomPercent;
    }

    public synchronized void setZoomPercent(int percent) {
        int bounded = Math.max(25, Math.min(300, percent));
        if (closed.get()) return;
        boolean changed = zoomPercent != bounded;
        zoomChanged = true;
        zoomPercent = bounded;
        settings.setZoomPercent(bounded);
        zoomDirty.set(true);
        scheduleZoomSave();
        if (changed) updateViewport();
    }

    // Screenshot completion restores device metrics, so both requests must finish in order.
    private void updateViewport() {
        DevToolsConnection current = connection;
        if (current == null || closed.get() || !"ready".equals(status) || !updatingViewport.compareAndSet(false, true))
            return;
        int percent = zoomPercent;
        if (percent != appliedZoomPercent) {
            metricsInFlight = true;
            callViewport(current, "Emulation.setDeviceMetricsOverride", createMetrics(percent))
                .whenComplete((result, failure) -> {
                    try {
                        if (failure != null) fail(failure);
                        else if (!closed.get()) {
                            appliedZoomPercent = percent;
                            if (zoomPercent == percent) capturePending.set(true);
                        }
                    } finally {
                        metricsInFlight = false;
                        updatingViewport.set(false);
                        updateViewport();
                    }
                });
        } else if (capturePending.getAndSet(false)) {
            JsonObject parameters = new JsonObject();
            parameters.addProperty("format", "jpeg");
            parameters.addProperty("quality", quality);
            parameters.addProperty("captureBeyondViewport", false);
            callViewport(current, "Page.captureScreenshot", parameters).whenComplete((result, failure) -> {
                try {
                    if (failure != null) fail(failure);
                    else if (result != null && result.has("data") && !closed.get()) {
                        queueFrame(result.get("data").getAsString(), true);
                    }
                } finally {
                    updatingViewport.set(false);
                    updateViewport();
                }
            });
        } else {
            updatingViewport.set(false);
            if (zoomPercent != appliedZoomPercent || capturePending.get()) updateViewport();
        }
    }

    private CompletableFuture<JsonObject> callViewport(DevToolsConnection current, String method,
        JsonObject parameters) {
        CompletableFuture<JsonObject> request = current.call(method, parameters);
        try {
            ScheduledFuture<?> timeout = worker.schedule(
                () -> request.completeExceptionally(new IOException("Browser viewport command timed out")), 5,
                TimeUnit.SECONDS);
            request.whenComplete((result, failure) -> timeout.cancel(false));
        } catch (RejectedExecutionException exception) {
            request.completeExceptionally(exception);
        }
        return request;
    }

    public void adjustZoom(int change) {
        setZoomPercent(zoomPercent + change);
    }

    public void resetZoom() {
        setZoomPercent(100);
    }

    public int getQuality() {
        return quality;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public synchronized void navigate(String targetUrl) {
        url = targetUrl;
        requestedUrl = targetUrl;
        JsonObject parameters = new JsonObject();
        parameters.addProperty("url", targetUrl);
        send("Page.navigate", parameters);
    }

    public void reload() {
        send("Page.reload", new JsonObject());
    }

    public void back() {
        DevToolsConnection current = connection;
        if (closed.get() || current == null || !"ready".equals(status)) return;
        current.call("Page.getNavigationHistory", new JsonObject()).thenAccept(history -> {
            int index = history.get("currentIndex").getAsInt();
            JsonArray entries = history.getAsJsonArray("entries");
            if (index > 0 && index <= entries.size()) {
                JsonObject parameters = new JsonObject();
                parameters.add("entryId", entries.get(index - 1).getAsJsonObject().get("id"));
                send("Page.navigateToHistoryEntry", parameters);
            }
        });
    }

    public void mouse(String type, int x, int y, String button, int buttons, double deltaY, int modifiers) {
        if (metricsInFlight || zoomPercent != appliedZoomPercent) return;
        if (!Arrays.asList("mousePressed", "mouseReleased", "mouseMoved", "mouseWheel").contains(type)) return;
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", type);
        parameters.addProperty("x", Math.max(0, Math.min(getViewportWidth() - 1, x)));
        parameters.addProperty("y", Math.max(0, Math.min(getViewportHeight() - 1, y)));
        parameters.addProperty("button", Arrays.asList("left", "right", "middle").contains(button) ? button : "none");
        parameters.addProperty("buttons", buttons & 7);
        parameters.addProperty("modifiers", modifiers & 15);
        if ("mousePressed".equals(type) || "mouseReleased".equals(type)) parameters.addProperty("clickCount", 1);
        if ("mouseWheel".equals(type)) {
            parameters.addProperty("deltaX", 0);
            parameters.addProperty("deltaY", Double.isFinite(deltaY) ? deltaY : 0);
        }
        send("Input.dispatchMouseEvent", parameters);
    }

    public void key(String type, String key, int windowsVirtualKeyCode, int modifiers) {
        if (!Arrays.asList("keyDown", "rawKeyDown", "keyUp", "char").contains(type)) return;
        JsonObject parameters = new JsonObject();
        boolean enterText = windowsVirtualKeyCode == 13 && (modifiers & 7) == 0
            && ("rawKeyDown".equals(type) || "keyDown".equals(type));
        parameters.addProperty("type", enterText ? "keyDown" : type);
        parameters.addProperty("key", key);
        parameters.addProperty("windowsVirtualKeyCode", windowsVirtualKeyCode);
        parameters.addProperty("modifiers", modifiers & 15);
        if (enterText) {
            parameters.addProperty("text", "\r");
            parameters.addProperty("unmodifiedText", "\r");
        }
        send("Input.dispatchKeyEvent", parameters);
    }

    public void insertText(String text) {
        if (text == null || text.isEmpty() || text.length() > 8192) return;
        JsonObject parameters = new JsonObject();
        parameters.addProperty("text", text);
        send("Input.insertText", parameters);
    }

    private void launch(boolean browserRequested) {
        try {
            Files.createDirectories(profilePath);
            acquireProfileLock();
            if (profileLock == null || !profileLock.isValid()) {
                stop("closed");
                return;
            }
            if (loadSavedZoom) {
                if (closed.get()) return;
                readZoomSettings();
            } else savePendingSettings();
            if (closed.get()) return;
            if (!browserRequested || isSettingsSuperseded()) {
                stop("closed");
                return;
            }
            File browser = findBrowser();
            if (browser == null) {
                stop("missingBrowser");
                return;
            }
            File portFile = new File(profile, "DevToolsActivePort");
            Files.deleteIfExists(portFile.toPath());
            List<String> command = new ArrayList<>(
                Arrays.asList(browser.getAbsolutePath(), "--headless=new", "--remote-debugging-address=127.0.0.1",
                    "--remote-debugging-port=0", "--user-data-dir=" + profile.getCanonicalPath(), "--no-first-run",
                    "--no-default-browser-check", "--force-device-scale-factor=" + width / (double) VIEWPORT_WIDTH,
                    "--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT,
                    "--autoplay-policy=no-user-gesture-required"));
            if (muted) command.add("--mute-audio");
            command.add("about:blank");
            if (closed.get()) return;
            process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(new File(profile, "browser.log"))).start();
            process.getOutputStream().close();
            int port = waitForPort(portFile);
            if (closed.get()) return;
            URI endpoint = waitForPage(port);
            if (closed.get()) return;
            targetId = endpoint.getPath().substring(endpoint.getPath().lastIndexOf('/') + 1);
            DevToolsConnection created = new DevToolsConnection(this::onEvent, this::fail);
            connection = created;
            created.connect(endpoint);
            if (closed.get()) return;
            int initialZoom = zoomPercent;
            created.call("Emulation.setDeviceMetricsOverride", createMetrics(initialZoom)).get(5, TimeUnit.SECONDS);
            appliedZoomPercent = initialZoom;
            created.call("Page.enable", new JsonObject()).get(5, TimeUnit.SECONDS);
            JsonObject discover = new JsonObject();
            discover.addProperty("discover", true);
            created.call("Target.setDiscoverTargets", discover).get(5, TimeUnit.SECONDS);
            JsonObject screencast = new JsonObject();
            screencast.addProperty("format", "jpeg");
            screencast.addProperty("quality", quality);
            screencast.addProperty("maxWidth", width);
            screencast.addProperty("maxHeight", height);
            screencast.addProperty("everyNthFrame", 1);
            created.call("Page.startScreencast", screencast).get(5, TimeUnit.SECONDS);
            synchronized (this) {
                if (closed.get()) return;
                status = "ready";
                updateViewport();
                navigate(requestedUrl);
            }
            worker.scheduleWithFixedDelay(() -> {
                Process current = process;
                if (!closed.get() && (current == null || !current.isAlive())) {
                    fail(new IOException("Owned browser process exited"));
                }
            }, 1, 1, TimeUnit.SECONDS);
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private JsonObject createMetrics(int percent) {
        double factor = percent / 100D;
        JsonObject metrics = new JsonObject();
        metrics.addProperty("width", Math.round(VIEWPORT_WIDTH / factor));
        metrics.addProperty("height", Math.round(VIEWPORT_HEIGHT / factor));
        metrics.addProperty("deviceScaleFactor", width / (double) VIEWPORT_WIDTH * factor);
        metrics.addProperty("mobile", false);
        JsonObject viewport = new JsonObject();
        viewport.addProperty("x", 0);
        viewport.addProperty("y", 0);
        viewport.addProperty("width", VIEWPORT_WIDTH / factor);
        viewport.addProperty("height", VIEWPORT_HEIGHT / factor);
        viewport.addProperty("scale", 1);
        metrics.add("viewport", viewport);
        return metrics;
    }

    private void readZoomSettings() {
        WebScreenSettings saved = WebScreenSettings.load(profile);
        synchronized (this) {
            if (!zoomChanged) {
                zoomPercent = saved.getZoomPercent();
                settings.setZoomPercent(zoomPercent);
            }
        }
    }

    private void scheduleZoomSave() {
        if (!zoomSaveQueued.compareAndSet(false, true)) return;
        try {
            worker.schedule(() -> {
                zoomSaveQueued.set(false);
                saveZoomSettings();
            }, 200, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
            zoomSaveQueued.set(false);
        }
    }

    private void saveZoomSettings() {
        if (profileLock == null || !profileLock.isValid() || !zoomDirty.getAndSet(false)) return;
        try {
            settings.save(profile);
        } catch (IOException exception) {
            zoomDirty.set(true);
            GTNotGood.LOG.warn("Unable to save web screen settings", exception);
        }
    }

    private File findBrowser() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) return null;
        if (!executable.isEmpty()) {
            File configured = new File(executable);
            return configured.isFile() ? configured : null;
        }
        String[] locations = { "ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA" };
        String[] applications = { "Microsoft/Edge/Application/msedge.exe", "Google/Chrome/Application/chrome.exe" };
        for (String application : applications) {
            for (String location : locations) {
                String directory = System.getenv(location);
                if (directory != null) {
                    File candidate = new File(directory, application);
                    if (candidate.isFile()) return candidate;
                }
            }
        }
        return null;
    }

    private void acquireProfileLock() throws Exception {
        if (profileLock != null && profileLock.isValid()) return;
        if (profileLockFile == null) profileLockFile = new RandomAccessFile(new File(profile, "webscreen.lock"), "rw");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((!closed.get() || hasPendingSettings()) && System.nanoTime() < deadline) {
            if (isSettingsSuperseded()) return;
            try {
                profileLock = profileLockFile.getChannel().tryLock();
                if (profileLock != null) return;
            } catch (OverlappingFileLockException ignored) {}
            Thread.sleep(100);
        }
        if (!closed.get() || hasPendingSettings()) throw new IOException("Browser profile is already in use");
    }

    private boolean hasPendingSettings() {
        return settingsIntent != null && !settingsIntent.saved && latestSettings.get(profilePath) == settingsIntent;
    }

    private boolean isSettingsSuperseded() {
        return settingsIntent != null && !settingsIntent.saved && !hasPendingSettings();
    }

    private void savePendingSettings() throws IOException {
        if (!hasPendingSettings() || profileLock == null || !profileLock.isValid()) return;
        settingsIntent.settings.save(profile);
        settingsIntent.saved = true;
    }

    private int waitForPort(File portFile) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!closed.get() && System.nanoTime() < deadline) {
            if (!process.isAlive()) throw new IOException("Owned browser process exited during startup");
            if (portFile.isFile() && portFile.length() <= 4096) {
                List<String> lines = Files.readAllLines(portFile.toPath(), StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    try {
                        int port = Integer.parseInt(lines.get(0).trim());
                        if (port > 0 && port <= 65535) return port;
                    } catch (NumberFormatException ignored) {}
                }
            }
            Thread.sleep(100);
        }
        throw new IOException("Browser DevTools startup timed out");
    }

    private URI waitForPage(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!closed.get() && System.nanoTime() < deadline) {
            HttpURLConnection request = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/json/list")
                .openConnection(Proxy.NO_PROXY);
            request.setConnectTimeout(1500);
            request.setReadTimeout(1500);
            request.setInstanceFollowRedirects(false);
            try {
                if (request.getResponseCode() != 200) throw new IOException("Browser target discovery failed");
                try (InputStream input = request.getInputStream()) {
                    JsonArray targets = new JsonParser()
                        .parse(new InputStreamReader(new ByteArrayInputStream(readBounded(input, 262144)),
                            StandardCharsets.UTF_8))
                        .getAsJsonArray();
                    for (JsonElement element : targets) {
                        JsonObject target = element.getAsJsonObject();
                        if (!"page".equals(target.get("type").getAsString()) || !target.has("webSocketDebuggerUrl")) {
                            continue;
                        }
                        URI endpoint = new URI(target.get("webSocketDebuggerUrl").getAsString());
                        if (
                            "ws".equals(endpoint.getScheme()) && endpoint.getPort() == port
                                && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equals(endpoint.getHost()))
                        ) {
                            return new URI("ws", null, "127.0.0.1", port, endpoint.getPath(), endpoint.getQuery(),
                                null);
                        }
                    }
                }
            } catch (IOException exception) {
                if (!process.isAlive())
                    throw new IOException("Owned browser process exited during discovery", exception);
            } finally {
                request.disconnect();
            }
            Thread.sleep(100);
        }
        throw new IOException("Browser page discovery timed out");
    }

    private static byte[] readBounded(InputStream input, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > maximum) throw new IOException("Browser response exceeds limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void onEvent(JsonObject event) {
        if (closed.get()) return;
        String method = event.get("method").getAsString();
        JsonObject parameters = event.getAsJsonObject("params");
        if ("Page.screencastFrame".equals(method)) {
            JsonObject acknowledgement = new JsonObject();
            acknowledgement.add("sessionId", parameters.get("sessionId"));
            DevToolsConnection current = connection;
            if (current != null) current.send("Page.screencastFrameAck", acknowledgement);
            String data = parameters.get("data").getAsString();
            queueFrame(data, false);
        } else if ("Page.frameNavigated".equals(method)) {
            JsonObject frame = parameters.getAsJsonObject("frame");
            if (!frame.has("parentId")) {
                mainFrameId = frame.get("id").getAsString();
                url = frame.get("url").getAsString();
            }
        } else if (
            "Page.navigatedWithinDocument".equals(method) && parameters.has("url")
                && parameters.has("frameId")
                && parameters.get("frameId").getAsString().equals(mainFrameId)
        ) {
            url = parameters.get("url").getAsString();
        } else if ("Page.loadEventFired".equals(method)) {
            captureStillFrame();
        } else if ("Page.windowOpen".equals(method) && parameters.has("url")) {
            String target = parameters.get("url").getAsString();
            if (target.startsWith("https://") || target.startsWith("http://")) navigate(target);
        } else if ("Target.targetCreated".equals(method)) {
            JsonObject target = parameters.getAsJsonObject("targetInfo");
            if (
                "page".equals(target.get("type").getAsString())
                    && !targetId.equals(target.get("targetId").getAsString())
            ) {
                JsonObject closeTarget = new JsonObject();
                closeTarget.add("targetId", target.get("targetId"));
                DevToolsConnection current = connection;
                if (current != null) current.send("Target.closeTarget", closeTarget);
            }
        }
    }

    private void captureStillFrame() {
        if (closed.get()) return;
        // Static pages may paint before the screencast starts emitting frames.
        capturePending.set(true);
        updateViewport();
    }

    private void queueFrame(String data, boolean still) {
        if (closed.get() || data.length() > ((MAX_IMAGE_BYTES + 2) / 3) * 4) return;
        long now = System.nanoTime();
        long previous;
        long next;
        do {
            previous = nextFrameNanos.get();
            if (!still && previous != 0 && now - previous < 0) return;
            long delay = still || previous == 0 ? frameIntervalNanos
                : Math.max(0, frameIntervalNanos - (now - previous));
            next = now + delay;
        } while (!nextFrameNanos.compareAndSet(previous, next));
        pendingImage.set(new EncodedFrame(frameSequence.incrementAndGet(), data));
        scheduleDecode();
    }

    private void scheduleDecode() {
        while (!closed.get() && pendingImage.get() != null) {
            int active = decoding.get();
            if (active >= decoderThreads) return;
            if (!decoding.compareAndSet(active, active + 1)) continue;
            try {
                decoder.execute(this::decodeFrames);
            } catch (RejectedExecutionException ignored) {
                decoding.decrementAndGet();
                return;
            }
        }
    }

    private void decodeFrames() {
        try {
            FrameDecoder current = frameDecoder.get();
            if (current == null) {
                current = new FrameDecoder();
                frameDecoder.set(current);
            }
            EncodedFrame frame;
            while (!closed.get() && (frame = pendingImage.getAndSet(null)) != null) {
                BufferedImage image = current.decode(frame.data);
                if (image != null) publishFrame(frame.sequence, image);
            }
        } catch (Exception exception) {
            fail(exception);
        } finally {
            decoding.decrementAndGet();
            if (pendingImage.get() != null && !closed.get()) scheduleDecode();
        }
    }

    private void publishFrame(long sequence, BufferedImage image) {
        synchronized (frameLock) {
            if (closed.get() || sequence <= publishedSequence) return;
            publishedSequence = sequence;
            latestImage.set(image);
        }
    }

    private static BufferedImage convertFrame(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Raster raster = image.getRaster();
        if (
            image.getType() == BufferedImage.TYPE_3BYTE_BGR && raster.getDataBuffer() instanceof DataBufferByte
                && raster.getSampleModel() instanceof ComponentSampleModel
                && raster.getSampleModelTranslateX() == 0
                && raster.getSampleModelTranslateY() == 0
        ) {
            DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
            ComponentSampleModel samples = (ComponentSampleModel) raster.getSampleModel();
            int[] bands = samples.getBandOffsets();
            int[] banks = samples.getBankIndices();
            byte[] source = buffer.getData();
            if (
                buffer.getOffset() == 0 && buffer.getNumBanks() == 1
                    && samples.getPixelStride() == 3
                    && samples.getScanlineStride() == width * 3
                    && bands.length == 3
                    && bands[0] == 2
                    && bands[1] == 1
                    && bands[2] == 0
                    && banks.length == 3
                    && banks[0] == 0
                    && banks[1] == 0
                    && banks[2] == 0
                    && source.length >= width * height * 3
            ) {
                int[] pixels = ((DataBufferInt) converted.getRaster().getDataBuffer()).getData();
                for (int pixel = 0, offset = 0; pixel < pixels.length; pixel++, offset += 3) {
                    pixels[pixel] = 0xFF000000 | (source[offset + 2] & 255) << 16
                        | (source[offset + 1] & 255) << 8
                        | source[offset] & 255;
                }
                return converted;
            }
        }
        int[] pixels = ((DataBufferInt) converted.getRaster().getDataBuffer()).getData();
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return converted;
    }

    private void send(String method, JsonObject parameters) {
        DevToolsConnection current = connection;
        if (!closed.get() && "ready".equals(status) && current != null) current.send(method, parameters);
    }

    private void fail(Throwable failure) {
        if (closed.get()) return;
        GTNotGood.LOG.warn("Web screen browser session failed", failure);
        stop("failed");
    }

    private synchronized void stop(String finalStatus) {
        if (!closed.compareAndSet(false, true)) return;
        status = finalStatus;
        execute(this::release);
    }

    private void execute(Runnable task) {
        try {
            worker.execute(task);
        } catch (RejectedExecutionException ignored) {}
    }

    @Override
    public void close() {
        stop("closed");
    }

    public boolean awaitClosed(long timeoutMs) throws InterruptedException {
        return worker.awaitTermination(Math.max(0, timeoutMs), TimeUnit.MILLISECONDS);
    }

    private void release() {
        DevToolsConnection current = connection;
        Process owned = process;
        try {
            if (current != null && owned != null && owned.isAlive()) {
                current.send("Browser.close", new JsonObject());
                if (!owned.waitFor(3, TimeUnit.SECONDS)) {
                    owned.destroy();
                    if (!owned.waitFor(2, TimeUnit.SECONDS)) {
                        owned.destroyForcibly();
                        owned.waitFor();
                    }
                }
            } else if (owned != null && owned.isAlive()) {
                owned.destroy();
                if (!owned.waitFor(2, TimeUnit.SECONDS)) {
                    owned.destroyForcibly();
                    owned.waitFor();
                }
            }
        } catch (InterruptedException exception) {
            if (owned != null && owned.isAlive()) {
                owned.destroyForcibly();
                while (owned.isAlive()) {
                    try {
                        owned.waitFor();
                    } catch (InterruptedException ignored) {}
                }
            }
            Thread.currentThread().interrupt();
        } finally {
            if (current != null) current.close();
            connection = null;
            process = null;
            pendingImage.set(null);
            synchronized (frameLock) {
                latestImage.set(null);
            }
            decoder.shutdownNow();
            try {
                decoder.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            try {
                if (hasPendingSettings()) {
                    try {
                        Files.createDirectories(profilePath);
                        acquireProfileLock();
                        savePendingSettings();
                    } catch (Exception exception) {
                        GTNotGood.LOG.warn("Unable to persist pending web screen settings", exception);
                    }
                }
                saveZoomSettings();
                if (profileLock != null) profileLock.release();
                if (profileLockFile != null) profileLockFile.close();
            } catch (IOException exception) {
                GTNotGood.LOG.warn("Unable to release the web screen profile lock", exception);
            }
            if (settingsIntent != null) latestSettings.remove(profilePath, settingsIntent);
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {}
            worker.shutdownNow();
        }
    }

    private static final class SettingsIntent {

        private final WebScreenSettings settings;
        private volatile boolean saved;

        private SettingsIntent(WebScreenSettings settings) {
            this.settings = settings;
        }
    }

    private static final class EncodedFrame {

        private final long sequence;
        private final String data;

        private EncodedFrame(long sequence, String data) {
            this.sequence = sequence;
            this.data = data;
        }
    }

    private final class FrameDecoder implements AutoCloseable {

        private final ImageReader reader;
        private BufferedImage destination;

        private FrameDecoder() {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
            if (!readers.hasNext()) throw new IllegalStateException("JPEG decoder unavailable");
            reader = readers.next();
        }

        private BufferedImage decode(String data) throws IOException {
            byte[] bytes = Base64.getDecoder().decode(data);
            if (bytes.length > MAX_IMAGE_BYTES) return null;
            try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                reader.setInput(input, true, true);
                if (reader.getWidth(0) != width || reader.getHeight(0) != height) return null;
                if (destination == null) destination = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setDestination(destination);
                return convertFrame(reader.read(0, parameters));
            } finally {
                reader.setInput(null);
            }
        }

        @Override
        public void close() {
            reader.dispose();
            destination = null;
        }
    }
}
