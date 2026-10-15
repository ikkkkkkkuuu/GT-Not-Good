package com.xyp.gtnotgood.client.radialmenu.client.script;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.action.ActionExecutors;
import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiRadialWheel;
import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiTextPrompt;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptLimits;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptRequest;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptTask;

public final class ScriptHost {

    private static final int MAX_STEPS_PER_TICK = 32;

    private static final int DEFAULT_TIMEOUT_TICKS = 600;

    private static final List<Run> RUNS = new ArrayList<>();

    private static final Map<ActionSpec, String> LAST_ERRORS = new WeakHashMap<>();

    private static long nextToken;

    private ScriptHost() {}

    private static final class Run {

        final String token;

        final ActionSpec owner;

        final ScriptTask task;

        int ticksLeft;

        long cursor;

        boolean waiting;
        int waitTicksLeft;
        int waitedTicks;

        GuiRadialWheel wheel;

        Integer choice;

        MenuNode shownMenu;

        boolean collapsed;

        GuiTextPrompt prompt;

        String typed;

        boolean answered;

        Run(String token, ActionSpec owner, ScriptTask task) {
            this.token = token;
            this.owner = owner;
            this.task = task;
        }
    }

    public static boolean start(ActionSpec spec) {
        String source = spec.getString(ActionTypes.PARAM_SCRIPT, "");
        if (source.trim().isEmpty()) {
            return false;
        }

        cancelOwnedBy(spec, null);

        String token = "script-" + ++nextToken;

        ScriptTask task = ScriptTask.start(source, "script", token, ClientScriptContext.INSTANCE, ScriptLimits.DEFAULT);

        Run run = new Run(token, spec, task);
        run.ticksLeft = Math.max(0, spec.getInt(ActionTypes.PARAM_TIMEOUT_TICKS, DEFAULT_TIMEOUT_TICKS));
        run.cursor = ChatCapture.end();
        RUNS.add(run);

        pump(run);
        return true;
    }

    public static void choose(String token, int choice) {
        for (Run run : RUNS) {
            if (run.token.equals(token)) {
                run.choice = choice;
                return;
            }
        }

        RadialMenuMod.LOG.debug("No running script for " + token + "; the choice was discarded");
    }

    public static void wheelClosed(GuiRadialWheel wheel) {
        for (Run run : RUNS) {
            if (run.wheel == wheel) {
                run.wheel = null;
                return;
            }
        }
    }

    public static void inlineCollapsed(GuiRadialWheel wheel) {
        for (Run run : RUNS) {
            if (run.wheel == wheel) {
                run.collapsed = true;
                return;
            }
        }
    }

    public static boolean hasRunning() {
        return !RUNS.isEmpty();
    }

    public static void cancelAll(String reason) {
        for (Run run : new ArrayList<>(RUNS)) {
            cancel(run, reason);
        }
        RUNS.clear();
        ChatCapture.clear();
    }

    public static void onClientTick() {
        ChatCapture.drain();
        if (RUNS.isEmpty()) {
            ScriptStore.flush();
            return;
        }

        for (Run run : new ArrayList<>(RUNS)) {
            if (run.waiting && run.waitTicksLeft > 0) {
                run.waitTicksLeft--;
                run.waitedTicks++;
            }

            if (run.ticksLeft > 0 && --run.ticksLeft == 0) {
                cancel(run, "the script ran out of time");
                RUNS.remove(run);
                continue;
            }

            pump(run);
        }

        ScriptStore.flush();
    }

    private static void pump(Run run) {
        for (int step = 0; step < MAX_STEPS_PER_TICK; step++) {
            if (run.task.isFinished()) {
                finish(run);
                return;
            }
            ScriptRequest request = run.task.pending();
            if (request == null) {
                finish(run);
                return;
            }
            if (!dispatch(run, request)) {
                return;
            }
        }
    }

    private static boolean dispatch(Run run, ScriptRequest request) {
        switch (request.kind) {
            case Send:

                ActionExecutors
                    .runNow(new ActionSpec(ActionTypes.COMMAND).set(ActionTypes.PARAM_COMMAND, request.text));
                run.task.resumeVoid();
                return true;

            case Notify:
                tell(request.text, EnumChatFormatting.GRAY);
                run.task.resumeVoid();
                return true;

            case Log:
                RadialMenuMod.LOG.info("[" + run.token + "] " + request.text);
                run.task.resumeVoid();
                return true;

            case Action:
                runAction(run, request.action);
                run.task.resumeVoid();
                return true;

            case Sleep:
                if (!run.waiting) {
                    run.waiting = true;
                    run.waitTicksLeft = Math.max(1, request.ticks);
                    return false;
                }
                if (run.waitTicksLeft > 0) {
                    return false;
                }
                run.waiting = false;
                run.task.resumeVoid();
                return true;

            case AwaitLine:
                return awaitLine(run, request);

            case Menu:
                return openOrResolveMenu(run, request.menu);

            case MenuUpdate:
                if (run.wheel != null) {
                    run.wheel.replaceRoot(request.menu);
                }
                run.task.resumeVoid();
                return true;

            case MenuClose:
                closeWheel(run);
                run.task.resumeVoid();
                return true;

            case StoreSet:
                ScriptStore.set(request.key, request.text);
                run.task.resumeVoid();
                return true;

            case Prompt:
                return openOrResolvePrompt(run, request);

            default:
                RadialMenuMod.LOG.warn("Unknown script request " + request.kind);
                cancel(run, "the script asked for something this version does not know about");
                RUNS.remove(run);
                return false;
        }
    }

    private static void runAction(Run run, ActionSpec spec) {
        if (spec == null || spec.type == null) {
            return;
        }
        if (ActionTypes.SCRIPT.equals(spec.type) || ActionTypes.SCRIPT_RESUME.equals(spec.type)) {
            RadialMenuMod.LOG.warn("[" + run.token + "] refused to run a '" + spec.type + "' action from a script");
            return;
        }
        ActionExecutors.runNow(spec);
    }

    private static boolean awaitLine(Run run, ScriptRequest request) {
        if (!run.waiting) {
            run.waiting = true;
            run.waitTicksLeft = Math.max(1, request.ticks);
            run.waitedTicks = 0;
        }

        run.cursor = ChatCapture.clampCursor(run.cursor);
        String line = ChatCapture.at(run.cursor);
        if (line != null) {
            run.cursor++;
            int waited = run.waitedTicks;
            run.waiting = false;
            run.task.resumeLine(line, waited);
            return true;
        }

        if (run.waitTicksLeft > 0) {
            return false;
        }

        run.waiting = false;
        run.task.resumeLine(null, request.ticks);
        return true;
    }

    private static boolean openOrResolveMenu(Run run, MenuNode menu) {
        if (run.choice != null) {
            int choice = run.choice;
            run.choice = null;
            run.collapsed = false;
            run.shownMenu = null;
            run.task.resumeChoice(choice);
            return true;
        }

        Minecraft mc = Minecraft.getMinecraft();
        boolean onScreen = run.wheel != null && mc.currentScreen == run.wheel;

        if (run.shownMenu == menu) {
            if (onScreen && !run.collapsed) {
                return false;
            }
            run.collapsed = false;
            run.shownMenu = null;
            run.task.resumeChoice(0);
            return true;
        }

        if (mc.thePlayer == null) {
            run.task.resumeChoice(0);
            return true;
        }

        if (onScreen) {
            if (menu.layoutOrDefault().isInline() && run.wheel.attachInline(run.wheel.lastChosen(), menu)) {
                shown(run, menu);
                return false;
            }
            run.wheel.replaceRoot(menu);
            shown(run, menu);
            return false;
        }

        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiRadialWheel)) {
            RadialMenuMod.LOG.debug("[" + run.token + "] a screen was open, so the script's menu was not shown");
            run.task.resumeChoice(0);
            return true;
        }

        GuiRadialWheel wheel = new GuiRadialWheel(menu);

        run.wheel = wheel;
        shown(run, menu);
        mc.displayGuiScreen(wheel);
        return false;
    }

    private static void shown(Run run, MenuNode menu) {
        run.shownMenu = menu;
        run.collapsed = false;
    }

    private static boolean openOrResolvePrompt(Run run, ScriptRequest request) {
        if (run.answered) {
            String typed = run.typed;
            run.answered = false;
            run.typed = null;
            run.prompt = null;
            run.task.resumeText(typed);
            return true;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (run.prompt != null) {
            if (mc.currentScreen == run.prompt) {
                return false;
            }
            run.prompt = null;
            run.task.resumeText(null);
            return true;
        }

        if (mc.thePlayer == null) {
            run.task.resumeText(null);
            return true;
        }

        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiRadialWheel)) {
            RadialMenuMod.LOG.debug("[" + run.token + "] a screen was open, so the script's prompt was not shown");
            run.task.resumeText(null);
            return true;
        }

        final Run owner = run;

        String title = request.text == null ? "" : request.text.replace("%", "%%");
        GuiTextPrompt prompt = new GuiTextPrompt(title, request.key, new GuiTextPrompt.Result() {

            @Override
            public String onConfirm(String value) {
                owner.typed = value;
                owner.answered = true;

                return null;
            }
        });
        run.prompt = prompt;

        closeWheel(run);
        GuiStack.push(prompt);
        return false;
    }

    private static void closeWheel(Run run) {
        Minecraft mc = Minecraft.getMinecraft();
        if (run.wheel != null && mc.currentScreen == run.wheel) {
            mc.displayGuiScreen(null);
        }
        run.wheel = null;
        run.shownMenu = null;
        run.collapsed = false;
    }

    public static String lastError(ActionSpec spec) {
        return spec == null ? null : LAST_ERRORS.get(spec);
    }

    private static void finish(Run run) {
        RUNS.remove(run);
        closeWheel(run);

        String error = run.task.error();
        if (error == null) {
            LAST_ERRORS.remove(run.owner);
            return;
        }
        LAST_ERRORS.put(run.owner, error);
        RadialMenuMod.LOG.warn("[" + run.token + "] " + error);
        // #tr radialmenu.script.error
        // # Script error: %s
        // # zh_CN 脚本错误：%s
        tell(StatCollector.translateToLocalFormatted("radialmenu.script.error", error), EnumChatFormatting.RED);
    }

    private static void cancel(Run run, String reason) {
        run.task.cancel(reason);
        closeWheel(run);
        RadialMenuMod.LOG.warn("[" + run.token + "] cancelled: " + reason);
    }

    private static void cancelOwnedBy(ActionSpec owner, String reason) {
        Iterator<Run> iterator = RUNS.iterator();
        while (iterator.hasNext()) {
            Run run = iterator.next();
            if (run.owner == owner) {
                iterator.remove();
                run.task.cancel(reason == null ? "restarted" : reason);
                closeWheel(run);
            }
        }
    }

    private static void tell(String text, EnumChatFormatting colour) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || text == null) {
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(colour + text));
    }
}
