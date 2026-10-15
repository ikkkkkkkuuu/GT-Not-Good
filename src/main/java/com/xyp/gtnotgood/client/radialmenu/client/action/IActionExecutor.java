package com.xyp.gtnotgood.client.radialmenu.client.action;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public interface IActionExecutor {

    String typeId();

    boolean execute(ActionSpec spec);
}
