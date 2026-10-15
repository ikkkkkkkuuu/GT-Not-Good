package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileBindingTest {

    @Test
    public void serverRuleMatchesTheAddressIgnoringCase() {
        ProfileBinding rule = ProfileBinding.server("GTNH.Example.com");
        assertTrue(rule.matches("gtnh.example.com", null));
        assertFalse(rule.matches("other.example.com", null));
    }

    @Test
    public void serverRuleNeverMatchesSinglePlayer() {
        assertFalse(ProfileBinding.server("gtnh.example.com").matches(null, "MyWorld"));
    }

    @Test
    public void worldRuleMatchesTheFolderName() {
        ProfileBinding rule = ProfileBinding.world("MyWorld");
        assertTrue(rule.matches(null, "myworld"));
        assertFalse(rule.matches(null, "OtherWorld"));
        assertFalse(rule.matches("gtnh.example.com", null));
    }

    @Test
    public void singleplayerRuleMatchesAnyLocalWorld() {
        ProfileBinding rule = new ProfileBinding();
        rule.type = ProfileBinding.Type.Singleplayer;

        assertTrue(rule.matches(null, "anything"));
        assertFalse(rule.matches("gtnh.example.com", null));
    }

    @Test
    public void surroundingWhitespaceInAHandEditedValueIsIgnored() {
        ProfileBinding rule = ProfileBinding.server("  gtnh.example.com  ");
        assertTrue(rule.matches("gtnh.example.com", null));
    }

    @Test
    public void aRuleWithNoTypeMatchesNothing() {
        ProfileBinding rule = new ProfileBinding();
        rule.value = "gtnh.example.com";
        assertFalse(rule.matches("gtnh.example.com", null));
    }

    @Test
    public void profileMatchesIfAnyOfItsRulesDoes() {
        Profile profile = Profile.empty("multi");
        profile.bindings.add(ProfileBinding.world("Other"));
        profile.bindings.add(ProfileBinding.server("gtnh.example.com"));

        assertTrue(profile.matches("gtnh.example.com", null));
        assertFalse(profile.matches("another.example.com", null));
    }

    @Test
    public void profileWithoutRulesIsManualOnly() {
        assertFalse(Profile.empty("manual").matches("gtnh.example.com", null));
        assertFalse(Profile.empty("manual").matches(null, "MyWorld"));
    }
}
