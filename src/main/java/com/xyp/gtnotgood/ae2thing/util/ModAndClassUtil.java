package com.xyp.gtnotgood.ae2thing.util;

import java.lang.reflect.Field;

import com.xyp.gtnotgood.ae2thing.integration.Mods;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.core.localization.ButtonToolTips;

public class ModAndClassUtil {

    public static boolean isTypeFilter;
    public static boolean isCraftStatus;
    public static boolean isDoubleButton;
    public static boolean isBeSubstitutionsButton;

    @SuppressWarnings("all")
    public static void init() {
        isTypeFilter = Mods.hasAe2TypeFilter();
        try {
            Field d = Settings.class.getDeclaredField("CRAFTING_STATUS");
            if (d == null) isCraftStatus = false;
            isCraftStatus = true;
        } catch (NoSuchFieldException e) {
            isCraftStatus = false;
        }
        try {
            Field d = ActionItems.class.getDeclaredField("DOUBLE");
            if (d == null) isDoubleButton = false;
            isDoubleButton = true;
        } catch (NoSuchFieldException e) {
            isDoubleButton = false;
        }
        try {
            Field d = ButtonToolTips.class.getDeclaredField("BeSubstitutionsDescEnabled");
            isBeSubstitutionsButton = true;
        } catch (NoSuchFieldException e) {
            isBeSubstitutionsButton = false;
        }
    }
}
