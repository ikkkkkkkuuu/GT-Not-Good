package com.xyp.gtnotgood.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.Display;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Keeps this client's Windows taskbar button in sync with the live client option. */
public final class WindowsTaskbarButton {

    private volatile boolean hidden;
    private volatile boolean running;
    private volatile long nextAttemptAt;

    /**
     * Applies a changed setting off the render thread after the game window exists.
     * A failed operation retries later without running a helper process every tick.
     *
     * @param event client tick delivered after the display may have been created
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Display.isCreated()
            || running
            || Config.hideWindowsTaskbarButton == hidden
            || System.nanoTime() < nextAttemptAt) {
            return;
        }
        if (!System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("windows")) {
            return;
        }
        boolean desiredHidden = Config.hideWindowsTaskbarButton;
        running = true;
        Thread worker = new Thread(() -> setButtonHidden(desiredHidden), "GTNG-taskbar-button");
        worker.setDaemon(true);
        worker.start();
    }

    private void setButtonHidden(boolean desiredHidden) {
        try {
            String runtimeName = ManagementFactory.getRuntimeMXBean()
                .getName();
            String processId = runtimeName.substring(0, runtimeName.indexOf('@'));
            String script = readScript().replace("__PID__", processId)
                .replace("__VISIBLE__", desiredHidden ? "$false" : "$true");
            String command = Base64.getEncoder()
                .encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-WindowStyle",
                "Hidden",
                "-EncodedCommand",
                command).redirectErrorStream(true)
                    .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                GTNotGood.LOG.warn("Timed out changing the Windows taskbar button");
            } else if (process.exitValue() != 0) {
                GTNotGood.LOG.warn("Could not change the Windows taskbar button (exit {})", process.exitValue());
            } else {
                hidden = desiredHidden;
            }
        } catch (IOException | RuntimeException e) {
            GTNotGood.LOG.warn("Could not change the Windows taskbar button", e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        } finally {
            nextAttemptAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            running = false;
        }
    }

    private static String readScript() throws IOException {
        String path = "/assets/" + ModList.GTNotGood.getResourceLocation() + "/scripts/hide_taskbar_button.ps1";
        try (InputStream stream = WindowsTaskbarButton.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing taskbar script: " + path);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
