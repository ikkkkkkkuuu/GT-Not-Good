package com.xyp.gtnotgood.client.radialmenu.core.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ScriptSnippets {

    public static final char CARET = (char) 1;

    public static final class Snippet {

        public final String label;

        public final String text;

        Snippet(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    private static final List<Snippet> ALL = Collections.unmodifiableList(Arrays.asList(
        new Snippet("chat.send", "chat.send(\"" + CARET + "\")"),
        new Snippet("chat.await", "chat.await(\"^" + CARET + "(.+)$\")"),
        new Snippet("chat.awaitAll", "chat.awaitAll(\"^" + CARET + "(.+)$\", 40)"),
        new Snippet("menu.open", "menu.open({" + CARET + "})"),
        new Snippet("menu.open opts", "menu.open(items, {title = \"" + CARET + "\", slots = 8})"),
        new Snippet("menu entry", "{key = \"" + CARET + "\", label = \"\", icon = \"minecraft:stone\"}"),
        new Snippet("menu submenu", "{label = \"" + CARET + "\", opening = \"inline\", items = {}}"),
        new Snippet("menu.open inline", "menu.open(" + CARET + ", {opening = \"inline\"})"),
        new Snippet("onPick", "onPick = function(key, item)\n  " + CARET + "\nend"),
        new Snippet("menu.update", "menu.update(items)"), new Snippet("menu.close", "menu.close()"),
        new Snippet("player.name", "player.name"), new Snippet("player.facing", "player.facing"),
        new Snippet("player.chunkX", "player.chunkX"), new Snippet("chunk.toWorld", "chunk.toWorld(player.chunkX, 8)"),
        new Snippet("chunk.of", "chunk.of(player.x)"), new Snippet("world.lookingAt", "world.lookingAt()"),
        new Snippet("inventory.count", "inventory.count(\"minecraft:coal\")"),
        new Snippet("inventory.items", "inventory.items()"), new Snippet("store.get", "store.get(\"key\", \"\")"),
        new Snippet("store.set", "store.set(\"key\", value)"), new Snippet("prompt", "prompt(\"Name\")"),
        new Snippet("notify", "notify(\"" + CARET + "\")"), new Snippet("log", "log(\"" + CARET + "\")"),
        new Snippet("sleep", "sleep(20)"),
        new Snippet("action.run", "action.run({type = \"keybind\", params = {binding = \"" + CARET + "\"}})"),
        new Snippet("local", "local " + CARET + " = "), new Snippet("if", "if " + CARET + " then\n  \nend"),
        new Snippet("for", "for i = 1, 10 do\n  " + CARET + "\nend"),
        new Snippet("while", "while true do\n  " + CARET + "\nend")));

    private ScriptSnippets() {}

    public static List<Snippet> all() {
        return ALL;
    }
}
