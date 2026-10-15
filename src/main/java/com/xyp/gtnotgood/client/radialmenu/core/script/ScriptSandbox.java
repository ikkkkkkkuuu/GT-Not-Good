package com.xyp.gtnotgood.client.radialmenu.core.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseMathLib;

final class ScriptSandbox {

    private ScriptSandbox() {}

    static Globals newGlobals() {
        Globals globals = new Globals();

        // Library registration needs this table; loading PackageLib would expose Java modules.
        LuaTable packages = new LuaTable();
        packages.set("loaded", new LuaTable());
        globals.set("package", packages);

        globals.load(new BaseLib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new JseMathLib());
        globals.load(new Bit32Lib());
        globals.load(new DebugLib());

        LoadState.install(globals);
        LuaC.install(globals);

        // Keep DebugLib's watchdog active without exposing hooks to scripts.
        globals.set("debug", LuaValue.NIL);

        globals.set("package", LuaValue.NIL);

        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);

        return globals;
    }
}
