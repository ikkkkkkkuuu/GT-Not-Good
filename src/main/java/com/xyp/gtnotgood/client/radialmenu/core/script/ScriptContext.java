package com.xyp.gtnotgood.client.radialmenu.core.script;

import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.core.model.AccentCoefficients;

public interface ScriptContext {

    String playerName();

    int dimension();

    int blockX();

    int blockY();

    int blockZ();

    double yaw();

    double pitch();

    double health();

    int food();

    int air();

    ScriptItem heldItem();

    List<ScriptItem> inventory();

    LookTarget lookingAt();

    long worldTime();

    String worldName();

    String storeGet(String key);

    AccentCoefficients accentCoefficients();
}
