package com.xyp.gtnotgood.client.radialmenu.client.script;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class ChatCapture {

    private static final int MAX_LINES = 200;

    private static final ConcurrentLinkedQueue<String> INCOMING = new ConcurrentLinkedQueue<>();

    private static final List<String> LINES = new ArrayList<>();

    private static long firstIndex;

    private ChatCapture() {}

    public static final class Listener {

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
        public void onChat(ClientChatReceivedEvent event) {
            if (event.message == null) {
                return;
            }
            String text = event.message.getUnformattedText();
            if (text == null) {
                return;
            }

            INCOMING.add(StringUtils.stripControlCodes(text));
        }
    }

    static void drain() {
        String line;
        while ((line = INCOMING.poll()) != null) {
            LINES.add(line);
        }
        while (LINES.size() > MAX_LINES) {
            LINES.remove(0);
            firstIndex++;
        }
    }

    static long end() {
        return firstIndex + LINES.size();
    }

    static String at(long index) {
        int offset = (int) (index - firstIndex);
        return offset >= 0 && offset < LINES.size() ? LINES.get(offset) : null;
    }

    static long clampCursor(long cursor) {
        return Math.max(cursor, firstIndex);
    }

    static void clear() {
        INCOMING.clear();
        LINES.clear();
        firstIndex = 0;
    }
}
