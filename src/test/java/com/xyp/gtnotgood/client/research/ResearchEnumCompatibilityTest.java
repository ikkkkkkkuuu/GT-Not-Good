package com.xyp.gtnotgood.client.research;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.xyp.gtnotgood.client.research.Config.SolveMode;

public class ResearchEnumCompatibilityTest {

    @Test
    public void preservesLegacyConfigurationValues() {
        assertEquals(SolveMode.Normal, SolveMode.fromConfig("NORMAL"));
        assertEquals(SolveMode.Weighted, SolveMode.fromConfig("weighted"));
        assertEquals("NORMAL", SolveMode.Normal.configValue);
        assertEquals("WEIGHTED", SolveMode.Weighted.configValue);
        assertThrows(IllegalArgumentException.class, () -> SolveMode.fromConfig("unknown"));
    }

    @Test
    public void preservesTranslationKeysWithWordSeparators() {
        assertEquals("tcautores.queue_status.has_note", ResearchCatalog.Status.HasNote.translationKey);
        assertEquals("tcautores.plan_action.learn_discovery", ResearchPlan.Action.LearnDiscovery.translationKey);
        assertEquals("tcautores.plan_action.wait_for_prerequisites",
            ResearchPlan.Action.WaitForPrerequisites.translationKey);
        assertEquals("tcautores.generate_reason.no_ink",
            ResearchNoteGenerationController.EndReason.NoInk.translationKey);
        assertEquals("tcautores.generate_reason.workspace_required",
            ResearchNoteGenerationController.EndReason.WorkspaceRequired.translationKey);
    }
}
