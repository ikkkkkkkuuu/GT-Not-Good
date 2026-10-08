package com.xyp.gtnotgood.client.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.config.Config;
import thaumcraft.common.lib.research.ResearchManager;

public final class ResearchPlan {

    public enum Action {

        Completed("tcautores.plan_action.completed"),
        LearnDiscovery("tcautores.plan_action.learn_discovery"),
        SolveExisting("tcautores.plan_action.solve_existing"),
        GenerateAndSolve("tcautores.plan_action.generate_and_solve"),
        WaitForPrerequisites("tcautores.plan_action.wait_for_prerequisites"),
        Hidden("tcautores.plan_action.hidden"),
        Direct("tcautores.plan_action.direct"),
        Unsupported("tcautores.plan_action.unsupported");

        public final String translationKey;

        Action(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    public static final class Entry {

        public final ResearchItem research;
        public final Action action;
        public final boolean target;

        private Entry(ResearchItem research, Action action, boolean target) {
            this.research = research;
            this.action = action;
            this.target = target;
        }
    }

    public final ResearchItem target;
    public final List<Entry> entries;

    private ResearchPlan(ResearchItem target, List<Entry> entries) {
        this.target = target;
        this.entries = Collections.unmodifiableList(entries);
    }

    public static ResearchPlan create(EntityPlayer player, GuiResearchTableHelperInterface helper,
        ResearchItem target) {
        if (player == null || helper == null || target == null)
            return new ResearchPlan(target, Collections.emptyList());
        String username = player.getCommandSenderName();
        List<String> completedResearch = ResearchManager.getResearchForPlayerSafe(username);
        Set<String> completed = completedResearch == null ? Collections.emptySet() : new HashSet<>(completedResearch);
        List<Entry> result = new ArrayList<>();
        for (ResearchItem research : ResearchCatalog.prerequisitePlan(target, true)) {
            result.add(
                new Entry(research, classify(helper, username, completed, research), research.key.equals(target.key)));
        }
        return new ResearchPlan(target, result);
    }

    public int count(Action action) {
        int count = 0;
        for (Entry entry : entries) if (entry.action == action) count++;
        return count;
    }

    public boolean canExecute() {
        return count(Action.Hidden) == 0 && count(Action.Unsupported) == 0;
    }

    private static Action classify(GuiResearchTableHelperInterface helper, String username, Set<String> completed,
        ResearchItem research) {
        if (completed.contains(research.key)) return Action.Completed;
        if (!ResearchCatalog.isRevealed(research, completed)) return Action.Hidden;
        if (helper.findCompletedResearchNoteSlot(research.key) >= 0) return Action.LearnDiscovery;
        if (helper.countIncompleteResearchNotes(research.key) > 0) return Action.SolveExisting;
        if (
            research.isVirtual() || research.isStub()
                || research.isAutoUnlock()
                || research.tags == null
                || research.tags.size() == 0
                || research.getResearchPrimaryTag() == null
        ) return Action.Unsupported;
        if (ResearchCatalog.completesDirectly(research, Config.researchDifficulty)) return Action.Direct;
        if (!ResearchManager.doesPlayerHaveRequisites(username, research.key)) return Action.WaitForPrerequisites;
        return Action.GenerateAndSolve;
    }
}
