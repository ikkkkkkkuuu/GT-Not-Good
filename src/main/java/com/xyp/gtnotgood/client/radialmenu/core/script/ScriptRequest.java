package com.xyp.gtnotgood.client.radialmenu.core.script;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;

/** Lua coroutines exchange requests; the client thread performs all game operations. */
public final class ScriptRequest {

    public enum Kind {
        Send,

        AwaitLine,

        Menu,

        MenuUpdate,

        MenuClose,

        Sleep,

        Action,

        Notify,

        Log,

        StoreSet,

        Prompt
    }

    public final Kind kind;

    public final String text;

    public final int ticks;

    public final MenuNode menu;

    public final ActionSpec action;

    public final String key;

    private ScriptRequest(Kind kind, String text, int ticks, MenuNode menu, ActionSpec action, String key) {
        this.kind = kind;
        this.text = text;
        this.ticks = ticks;
        this.menu = menu;
        this.action = action;
        this.key = key;
    }

    public static ScriptRequest send(String text) {
        return new ScriptRequest(Kind.Send, text, 0, null, null, null);
    }

    public static ScriptRequest notify(String text) {
        return new ScriptRequest(Kind.Notify, text, 0, null, null, null);
    }

    public static ScriptRequest log(String text) {
        return new ScriptRequest(Kind.Log, text, 0, null, null, null);
    }

    public static ScriptRequest awaitLine(int timeoutTicks) {
        return new ScriptRequest(Kind.AwaitLine, null, timeoutTicks, null, null, null);
    }

    public static ScriptRequest sleep(int ticks) {
        return new ScriptRequest(Kind.Sleep, null, ticks, null, null, null);
    }

    public static ScriptRequest menu(MenuNode menu) {
        return new ScriptRequest(Kind.Menu, null, 0, menu, null, null);
    }

    public static ScriptRequest menuUpdate(MenuNode menu) {
        return new ScriptRequest(Kind.MenuUpdate, null, 0, menu, null, null);
    }

    public static ScriptRequest menuClose() {
        return new ScriptRequest(Kind.MenuClose, null, 0, null, null, null);
    }

    public static ScriptRequest action(ActionSpec action) {
        return new ScriptRequest(Kind.Action, null, 0, null, action, null);
    }

    public static ScriptRequest storeSet(String key, String value) {
        return new ScriptRequest(Kind.StoreSet, value, 0, null, null, key);
    }

    public static ScriptRequest prompt(String title, String initial) {
        return new ScriptRequest(Kind.Prompt, title, 0, null, null, initial);
    }

    @Override
    public String toString() {
        switch (kind) {
            case Send:
            case Notify:
            case Log:
                return kind + "(" + text + ")";
            case AwaitLine:
            case Sleep:
                return kind + "(" + ticks + " ticks)";
            case Menu:
            case MenuUpdate:
                return kind + "(" + menu.childrenOrEmpty().size() + " entries)";
            case Action:
                return kind + "(" + action.type + ")";
            case StoreSet:
                return kind + "(" + key + " = " + text + ")";
            case Prompt:
                return kind + "(" + text + ")";
            default:
                return kind.toString();
        }
    }
}
