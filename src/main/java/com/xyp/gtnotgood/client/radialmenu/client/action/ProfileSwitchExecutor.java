package com.xyp.gtnotgood.client.radialmenu.client.action;

import com.xyp.gtnotgood.client.radialmenu.client.input.KeybindStateTracker;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;

public class ProfileSwitchExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.PROFILE_SWITCH;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String target = spec.getString(ActionTypes.PARAM_PROFILE, null);

        KeybindStateTracker.releaseAll();

        if (target == null || target.trim().isEmpty()) {
            return ProfileManager.cycle(1);
        }
        return ProfileManager.switchTo(target.trim());
    }
}
