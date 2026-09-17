package com.xyp.gtnotgood.loader;

import com.hfstudio.bqapi.BQApi;

public class QuestLoader {

    public static void init() {
        BQApi.registerImportedFolder("gtnotgood", "quests");
    }
}
