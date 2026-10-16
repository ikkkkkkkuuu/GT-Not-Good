package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;

public class GuiProfileBackups extends UiScreen {

    private static final int ID_RESTORE_BASE = 100;

    private static final int RESTORE_WIDTH = 60;

    /** Absolute rather than "ten minutes ago". */
    private static final SimpleDateFormat WHEN = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final String profile;

    private final List<Integer> ages = new ArrayList<>();

    /** What each kept version holds, read once per rebuild rather than per frame. */
    private final List<String> summaries = new ArrayList<>();

    private final List<int[]> rowPositions = new ArrayList<>();

    public GuiProfileBackups(String profile) {
        this.profile = profile;
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.backups.title
        // # Kept versions of %s
        // # zh_CN %s 的保留版本
        return "radialmenu.backups.title";
    }

    @Override
    protected Object[] titleArgs() {
        return new Object[] { profile };
    }

    @Override
    protected int panelWidth() {
        return 320;
    }

    @Override
    protected boolean framedViewport() {
        return true;
    }

    @Override
    protected void buildControls() {
        ages.clear();
        summaries.clear();
        rowPositions.clear();

        int y = scrolledTop();

        // The profile as it stands, with no button beside it: there is nothing to restore it to.
        rowPositions.add(new int[] { contentLeft(), y });
        ages.add(Integer.valueOf(0));
        summaries.add(summary(0));
        y += Ui.STEP;

        for (int age = 1; age <= ProfileStorage.keptVersions(); age++) {
            if (!ProfileStorage.hasBackup(profile, age)) {
                continue;
            }
            this.buttonList
                .add(new GuiButton(ID_RESTORE_BASE + age, contentRight() - RESTORE_WIDTH, y, RESTORE_WIDTH, Ui.ROW,
                    // #tr radialmenu.backups.restore
                    // # Restore
                    // # zh_CN 恢复
                    I18n.format("radialmenu.backups.restore")));
            // #tr radialmenu.backups.restore.tip
            // # Restore this version and back up the current one
            // # zh_CN 恢复此版本，并备份当前版本
            tooltip(ID_RESTORE_BASE + age, describe("radialmenu.backups.restore.tip"));
            rowPositions.add(new int[] { contentLeft(), y });
            ages.add(Integer.valueOf(age));
            summaries.add(summary(age));
            y += Ui.STEP;
        }

        // Room for the line that says there is nothing kept yet, which sits under the one row there is.
        setContentHeight(Math.max(Ui.STEP, y - scrolledTop()) + (ages.size() <= 1 ? Ui.STEP : 0) + Ui.PAD);
        addBottomBar("gui.done", null, null);
    }

    @Override
    protected int panelHeightHint() {
        return Ui.STEP * (ProfileStorage.keptVersions() + 1);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            onCancel();
            return;
        }
        final int age = button.id - ID_RESTORE_BASE;
        if (age < 1 || !ProfileStorage.hasBackup(profile, age)) {
            return;
        }
        GuiStack.push(new GuiConfirm(
            // #tr radialmenu.backups.confirmRestore
            // # Restore this version?
            // # zh_CN 恢复此版本？
            "radialmenu.backups.confirmRestore",
            // #tr radialmenu.backups.confirmRestore.subject
            // # The profile goes back to how it was at %s
            // # zh_CN 配置将恢复到 %s 时的状态
            I18n.format("radialmenu.backups.confirmRestore.subject", when(age)), "radialmenu.backups.restore",
            new GuiConfirm.Result() {

                @Override
                public void onConfirmed() {
                    restore(age);
                }
            }));
    }

    private void restore(int age) {
        if (!ProfileStorage.restoreBackup(profile, age)) {
            return;
        }
        ProfileManager.load();
        requestRebuild();
    }

    @Override
    protected void onCancel() {
        GuiStack.pop();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < ages.size() && i < rowPositions.size(); i++) {
            int[] position = rowPositions.get(i);
            if (!isVisibleRow(position[1])) {
                continue;
            }
            int age = ages.get(i).intValue();
            int textY = position[1] + (Ui.ROW - 8) / 2;
            boolean current = age == 0;

            this.fontRendererObj.drawString(when(age), position[0], textY, current ? Ui.TEXT : Ui.TEXT_MUTED);
            this.fontRendererObj.drawString(
                Ui.fit(summaries.get(i), contentRight() - RESTORE_WIDTH - Ui.GAP - (position[0] + 92)),
                position[0] + 92, textY, current ? Ui.TEXT : Ui.TEXT_MUTED);
            if (current) {
                // #tr radialmenu.backups.current
                // # now
                // # zh_CN 现在
                String label = I18n.format("radialmenu.backups.current");
                this.fontRendererObj.drawString(label, contentRight() - this.fontRendererObj.getStringWidth(label),
                    textY, Ui.TEXT_MUTED);
            }
        }

        if (ages.size() <= 1) {
            this.fontRendererObj.drawString(
                // #tr radialmenu.backups.none
                // # Nothing has been saved over yet, so there is no earlier version to go back to
                // # zh_CN 尚未保存过任何版本，因此没有可恢复的旧版本
                I18n.format("radialmenu.backups.none"), contentLeft(), scrolledTop() + Ui.STEP + 6, Ui.TEXT_MUTED);
        }
    }

    private String when(int age) {
        long time = age == 0 ? ProfileStorage.profileFile(profile).lastModified()
            : ProfileStorage.backupTime(profile, age);
        return time == 0L ? "" : WHEN.format(new Date(time));
    }

    private String summary(int age) {
        Profile kept = age == 0 ? ProfileStorage.loadProfile(profile) : ProfileStorage.loadBackup(profile, age);
        if (kept == null || kept.root == null) {
            // #tr radialmenu.backups.unreadable
            // # could not be read
            // # zh_CN 无法读取
            return I18n.format("radialmenu.backups.unreadable");
        }
        // The whole tree, not the first ring of it. A submenu emptied out leaves the wheel above it the same
        // length, and a number that could not see that would be silent about the very loss this screen is for.
        // #tr radialmenu.backups.entries
        // # %d entries
        // # zh_CN %d 个条目
        return I18n.format("radialmenu.backups.entries", Integer.valueOf(kept.root.deepCount()));
    }

    private static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }
}
