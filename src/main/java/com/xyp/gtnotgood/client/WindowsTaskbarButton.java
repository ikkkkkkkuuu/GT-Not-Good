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
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Removes this client's taskbar button once its Windows window has been created. */
public final class WindowsTaskbarButton {

    /**
     * Starts the native taskbar operation off the render thread and unregisters after one attempt.
     * The bundled script runs in a short-lived child process and makes no persistent Windows changes.
     *
     * @param event client tick delivered after the display may have been created
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Display.isCreated()) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .unregister(this);
        if (!System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("windows")) {
            return;
        }
        Thread worker = new Thread(this::hideButton, "GTNG-taskbar-button");
        worker.setDaemon(true);
        worker.start();
    }

    private void hideButton() {
        try {
            String runtimeName = ManagementFactory.getRuntimeMXBean()
                .getName();
            String processId = runtimeName.substring(0, runtimeName.indexOf('@'));
            String script = readScript().replace("__PID__", processId);
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
                GTNotGood.LOG.warn("Timed out hiding the Windows taskbar button");
            } else if (process.exitValue() != 0) {
                GTNotGood.LOG.warn("Could not hide the Windows taskbar button (exit {})", process.exitValue());
            }
        } catch (IOException | RuntimeException e) {
            GTNotGood.LOG.warn("Could not hide the Windows taskbar button", e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
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
