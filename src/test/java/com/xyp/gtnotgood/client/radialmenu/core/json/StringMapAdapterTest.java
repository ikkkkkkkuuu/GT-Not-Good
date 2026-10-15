package com.xyp.gtnotgood.client.radialmenu.core.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;

/** A parameter written as an array of lines, which is the only way a long script is readable in a file. */
public class StringMapAdapterTest {

    private static Profile read(String params) {
        return ConfigCodec.readProfile(
            "{ \"name\": \"p\", \"root\": { \"children\": [ { \"title\": \"e\", \"action\": { \"type\": \"script\","
                + " \"params\": "
                + params
                + " } } ] } }");
    }

    private static ActionSpec firstAction(Profile profile) {
        return profile.root.childrenOrEmpty().get(0).action;
    }

    @Test
    public void anArrayOfLinesReadsAsOneStringWithNewlines() {
        ActionSpec action = firstAction(read("{ \"script\": [\"local me = player.name\", \"chat.send('/home')\"] }"));
        assertEquals("local me = player.name\nchat.send('/home')", action.getString(ActionTypes.PARAM_SCRIPT, null));
    }

    @Test
    public void aPlainStringStillReadsTheWayItAlwaysDid() {
        ActionSpec action = firstAction(read("{ \"script\": \"notify('hi')\" }"));
        assertEquals("notify('hi')", action.getString(ActionTypes.PARAM_SCRIPT, null));
    }

    @Test
    public void aNumberOrBooleanReadsAsItsText() {
        ActionSpec action = firstAction(read("{ \"timeoutTicks\": 200, \"cycle\": true }"));
        assertEquals("200", action.getString(ActionTypes.PARAM_TIMEOUT_TICKS, null));
        assertEquals("true", action.getString(ActionTypes.PARAM_CYCLE, null));
    }

    @Test
    public void anEmptyArrayIsAnEmptyValueRatherThanAMissingOne() {
        ActionSpec action = firstAction(read("{ \"script\": [] }"));
        assertEquals("", action.getString(ActionTypes.PARAM_SCRIPT, null));
    }

    @Test
    public void aNullValueIsLeftOutSoTheDefaultApplies() {
        ActionSpec action = firstAction(read("{ \"script\": null }"));
        assertNull(action.getString(ActionTypes.PARAM_SCRIPT, null));
    }

    @Test
    public void aMultiLineValueIsWrittenBackAsAnArray() {
        Profile profile = new Profile();
        profile.name = "p";
        profile.root = MenuNode.category("p", null, null);
        profile.root.children.add(MenuNode.leaf("e", null, new ActionSpec(ActionTypes.SCRIPT)
            .set(ActionTypes.PARAM_SCRIPT, "first\nsecond").set(ActionTypes.PARAM_TIMEOUT_TICKS, "200")));

        String json = ConfigCodec.writeProfile(profile);

        assertTrue(json, json.contains("\"first\""));
        assertTrue(json, json.contains("\"second\""));
        // A single-line value is left exactly as it was: every profile written before this holds plain strings.
        assertTrue(json, json.contains("\"timeoutTicks\": \"200\""));
    }

    @Test
    public void roundTripsThroughBothForms() {
        Profile profile = new Profile();
        profile.name = "p";
        profile.root = MenuNode.category("p", null, null);
        profile.root.children
            .add(MenuNode.leaf("e", null, new ActionSpec(ActionTypes.SCRIPT).set(ActionTypes.PARAM_SCRIPT, "a\nb\nc")));

        Profile reread = ConfigCodec.readProfile(ConfigCodec.writeProfile(profile));

        assertEquals("a\nb\nc", firstAction(reread).getString(ActionTypes.PARAM_SCRIPT, null));
    }
}
