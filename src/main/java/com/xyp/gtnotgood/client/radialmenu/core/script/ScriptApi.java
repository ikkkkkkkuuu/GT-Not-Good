package com.xyp.gtnotgood.client.radialmenu.core.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

final class ScriptApi {

    private static final String PRELUDE = "/assets/" + RadialMenuMod.MODID + "/radialmenu/scripts/prelude.lua";

    private static final int DEFAULT_WAIT_TICKS = 60;

    private ScriptApi() {}

    static void install(final ScriptTask task, Globals globals) {
        LuaTable chat = new LuaTable();
        chat.set("send", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.send(args.checkjstring(1)));
                return LuaValue.NONE;
            }
        });

        chat.set("line", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return task.yieldRequest(ScriptRequest.awaitLine(args.optint(1, DEFAULT_WAIT_TICKS)));
            }
        });
        globals.set("chat", chat);

        LuaTable menu = new LuaTable();
        menu.set("open", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return task.yieldRequest(ScriptRequest.menu(
                    ScriptMenus.build(args.checktable(1), args.arg(2), task.token(), task.context(), task.limits())));
            }
        });
        menu.set("update", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return task.yieldRequest(ScriptRequest.menuUpdate(
                    ScriptMenus.build(args.checktable(1), args.arg(2), task.token(), task.context(), task.limits())));
            }
        });
        menu.set("close", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.menuClose());
                return LuaValue.NONE;
            }
        });

        menu.set("maxEntries", LuaValue.valueOf(task.limits().maxMenuItems));
        globals.set("menu", menu);

        globals.set("sleep", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.sleep(args.optint(1, 1)));
                return LuaValue.NONE;
            }
        });

        globals.set("notify", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.notify(join(args)));
                return LuaValue.NONE;
            }
        });

        LuaValue log = new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.log(join(args)));
                return LuaValue.NONE;
            }
        };
        globals.set("log", log);

        globals.set("print", log);

        LuaTable action = new LuaTable();
        action.set("run", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                task.yieldRequest(ScriptRequest.action(specOf(args.checktable(1))));
                return LuaValue.NONE;
            }
        });
        globals.set("action", action);

        globals.set("chunk", chunkTable());

        globals.set("world", worldTable(task));

        globals.set("inventory", inventoryTable(task));

        globals.set("store", storeTable(task));

        globals.set("prompt", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return task.yieldRequest(
                    // #tr radialmenu.script.prompt
                    // # Type a value
                    // # zh_CN 输入一个值
                    ScriptRequest.prompt(args.optjstring(1, "radialmenu.script.prompt"), args.optjstring(2, "")));
            }
        });

        globals.set("player", playerTable(task));

        globals.load(prelude(), "radialmenu-prelude").call();
    }

    private static LuaTable worldTable(final ScriptTask task) {
        LuaTable world = new LuaTable();
        world.set("lookingAt", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                ScriptContext context = task.context();
                LookTarget target = context == null ? null : context.lookingAt();
                if (target == null) {
                    return LuaValue.NIL;
                }
                LuaTable table = new LuaTable();
                table.set("kind", LuaValue.valueOf(target.kind));
                table.set("id", LuaValue.valueOf(target.id));
                table.set("label", LuaValue.valueOf(target.label));
                table.set("meta", LuaValue.valueOf(target.meta));
                table.set("x", LuaValue.valueOf(target.x));
                table.set("y", LuaValue.valueOf(target.y));
                table.set("z", LuaValue.valueOf(target.z));
                return table;
            }
        });

        LuaTable meta = new LuaTable();
        meta.set("__index", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                ScriptContext context = task.context();
                if (context == null) {
                    return LuaValue.NIL;
                }
                String key = args.arg(2).tojstring();
                if ("time".equals(key)) {
                    return LuaValue.valueOf(WorldTime.timeOfDay(context.worldTime()));
                }
                if ("day".equals(key)) {
                    return LuaValue.valueOf(WorldTime.day(context.worldTime()));
                }
                if ("isDay".equals(key)) {
                    return LuaValue.valueOf(WorldTime.isDay(context.worldTime()));
                }
                if ("name".equals(key)) {
                    return LuaValue.valueOf(context.worldName() == null ? "" : context.worldName());
                }
                return LuaValue.NIL;
            }
        });
        world.setmetatable(meta);
        return world;
    }

    private static LuaTable inventoryTable(final ScriptTask task) {
        LuaTable inventory = new LuaTable();
        inventory.set("items", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                LuaTable list = new LuaTable();
                int index = 1;
                for (ScriptItem item : items(task)) {
                    list.set(index++, itemTable(item));
                }
                return list;
            }
        });
        inventory.set("count", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(total(task, args));
            }
        });
        inventory.set("has", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(total(task, args) > 0);
            }
        });
        return inventory;
    }

    private static int total(ScriptTask task, Varargs args) {
        String id = args.checkjstring(1);
        boolean byMeta = args.narg() >= 2 && !args.isnil(2);
        int meta = args.optint(2, 0);
        int total = 0;
        for (ScriptItem item : items(task)) {
            if (item.id.equals(id) && (!byMeta || item.meta == meta)) {
                total += item.count;
            }
        }
        return total;
    }

    private static List<ScriptItem> items(ScriptTask task) {
        ScriptContext context = task.context();
        return context == null ? Collections.<ScriptItem>emptyList() : context.inventory();
    }

    private static LuaTable itemTable(ScriptItem item) {
        LuaTable table = new LuaTable();
        table.set("id", LuaValue.valueOf(item.id));
        table.set("meta", LuaValue.valueOf(item.meta));
        table.set("count", LuaValue.valueOf(item.count));
        table.set("label", LuaValue.valueOf(item.label));
        table.set("slot", LuaValue.valueOf(item.slot));

        table.set("icon", LuaValue.valueOf(item.id + ":" + item.meta));
        return table;
    }

    private static LuaTable storeTable(final ScriptTask task) {
        LuaTable store = new LuaTable();
        store.set("get", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                ScriptContext context = task.context();
                String value = context == null ? null : context.storeGet(args.checkjstring(1));
                if (value != null) {
                    return LuaValue.valueOf(value);
                }

                return args.arg(2);
            }
        });
        store.set("set", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                String key = args.checkjstring(1);
                LuaValue value = args.arg(2);
                task.yieldRequest(ScriptRequest.storeSet(key, value.isnil() ? null : value.tojstring()));
                return LuaValue.NONE;
            }
        });
        return store;
    }

    private static LuaTable chunkTable() {
        LuaTable chunk = new LuaTable();
        chunk.set("of", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(ChunkCoords.chunkOf(args.checkint(1)));
            }
        });
        chunk.set("offset", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(ChunkCoords.offsetOf(args.checkint(1)));
            }
        });
        chunk.set("toWorld", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(ChunkCoords.toWorld(args.checkint(1), args.optint(2, 0)));
            }
        });
        chunk.set("size", LuaValue.valueOf(ChunkCoords.SIZE));
        return chunk;
    }

    private static LuaTable playerTable(final ScriptTask task) {
        LuaTable player = new LuaTable();
        LuaTable meta = new LuaTable();
        meta.set("__index", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
                ScriptContext context = task.context();
                if (context == null) {
                    return LuaValue.NIL;
                }
                String key = args.arg(2).tojstring();
                if ("name".equals(key)) {
                    return LuaValue.valueOf(context.playerName() == null ? "" : context.playerName());
                }
                if ("dim".equals(key)) {
                    return LuaValue.valueOf(context.dimension());
                }
                if ("x".equals(key)) {
                    return LuaValue.valueOf(context.blockX());
                }
                if ("y".equals(key)) {
                    return LuaValue.valueOf(context.blockY());
                }
                if ("z".equals(key)) {
                    return LuaValue.valueOf(context.blockZ());
                }

                if ("chunkX".equals(key)) {
                    return LuaValue.valueOf(ChunkCoords.chunkOf(context.blockX()));
                }
                if ("chunkZ".equals(key)) {
                    return LuaValue.valueOf(ChunkCoords.chunkOf(context.blockZ()));
                }
                if ("xInChunk".equals(key)) {
                    return LuaValue.valueOf(ChunkCoords.offsetOf(context.blockX()));
                }
                if ("zInChunk".equals(key)) {
                    return LuaValue.valueOf(ChunkCoords.offsetOf(context.blockZ()));
                }

                if ("yaw".equals(key)) {
                    return LuaValue.valueOf(Facing.normalizeYaw(context.yaw()));
                }
                if ("pitch".equals(key)) {
                    return LuaValue.valueOf(context.pitch());
                }
                if ("facing".equals(key)) {
                    return LuaValue.valueOf(Facing.of(context.yaw()));
                }
                if ("health".equals(key)) {
                    return LuaValue.valueOf(context.health());
                }
                if ("food".equals(key)) {
                    return LuaValue.valueOf(context.food());
                }
                if ("air".equals(key)) {
                    return LuaValue.valueOf(context.air());
                }
                if ("held".equals(key)) {
                    ScriptItem held = context.heldItem();
                    return held == null ? LuaValue.NIL : itemTable(held);
                }
                return LuaValue.NIL;
            }
        });
        player.setmetatable(meta);
        return player;
    }

    private static ActionSpec specOf(LuaValue table) {
        ActionSpec spec = new ActionSpec(table.get("type").optjstring(null));

        LuaValue params = table.get("params");
        if (params.istable()) {
            LuaValue key = LuaValue.NIL;
            while (true) {
                Varargs next = params.next(key);
                key = next.arg1();
                if (key.isnil()) {
                    break;
                }
                spec.set(key.tojstring(), next.arg(2).tojstring());
            }
        }

        LuaValue steps = table.get("steps");
        if (steps.istable()) {
            for (int i = 1; i <= steps.length(); i++) {
                LuaValue step = steps.get(i);
                if (step.istable()) {
                    spec.stepsOrEmpty().add(specOf(step));
                }
            }
        }
        return spec;
    }

    private static String join(Varargs args) {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= args.narg(); i++) {
            if (i > 1) {
                text.append(' ');
            }
            text.append(args.arg(i).tojstring());
        }
        return text.toString();
    }

    private static String prelude() {
        InputStream stream = ScriptApi.class.getResourceAsStream(PRELUDE);
        if (stream == null) {
            throw new IllegalStateException("The script prelude is missing from the jar: " + PRELUDE);
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                bytes.write(buffer, 0, read);
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException("The script prelude could not be read", e);
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {}
        }
    }
}
