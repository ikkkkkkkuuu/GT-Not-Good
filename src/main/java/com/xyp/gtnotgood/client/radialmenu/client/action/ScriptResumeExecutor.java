package com.xyp.gtnotgood.client.radialmenu.client.action;

import com.xyp.gtnotgood.client.radialmenu.client.script.ScriptHost;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;

public class ScriptResumeExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.SCRIPT_RESUME;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String token = spec.getString(ActionTypes.PARAM_TOKEN, null);
        int choice = spec.getInt(ActionTypes.PARAM_CHOICE, 0);
        if (token == null || choice <= 0) {
            return false;
        }

        ScriptHost.choose(token, choice);
        return true;
    }
}
