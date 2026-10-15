package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;

import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptSnippets.Snippet;

/**
 * Holds the editor's list of calls and the highlighter's idea of the API together.
 *
 * <p>
 * The failure this exists to catch is quiet: renaming a function in {@code ScriptApi} and updating one of the two lists
 * that mention it, leaving the player with a list that writes a call nothing answers.
 */
public class ScriptSnippetsTest {

    private static final Pattern MEMBER_ACCESS = Pattern.compile("(\\w+)\\.(\\w+)");

    @Test
    public void everyMemberASnippetWritesIsOneTheApiHas() {
        for (Snippet snippet : ScriptSnippets.all()) {
            Matcher matcher = MEMBER_ACCESS.matcher(snippet.text);
            while (matcher.find()) {
                String owner = matcher.group(1);
                String member = matcher.group(2);
                if (!LuaSyntax.isGlobal(owner)) {
                    continue;
                }
                assertTrue(snippet.label + " writes " + owner + "." + member + ", which the API does not have",
                    LuaSyntax.isMember(member));
            }
        }
    }

    @Test
    public void everySnippetHasSomethingToShowAndSomethingToWrite() {
        for (Snippet snippet : ScriptSnippets.all()) {
            assertFalse(snippet.label.trim().isEmpty());
            assertFalse(snippet.label, snippet.text.trim().isEmpty());
        }
    }

    @Test
    public void aSnippetMarksAtMostOneCaret() {
        // Two would leave the editor to guess which the player meant, and it has no way to.
        for (Snippet snippet : ScriptSnippets.all()) {
            int first = snippet.text.indexOf(ScriptSnippets.CARET);
            assertEquals(snippet.label + " marks the caret twice", first,
                snippet.text.lastIndexOf(ScriptSnippets.CARET));
        }
    }

    @Test
    public void theCaretMarkIsNotSomethingAPlayerCouldType() {
        // It is stripped on insertion; anything that forgot to would be obvious rather than subtly wrong.
        assertTrue(ScriptSnippets.CARET < ' ');
    }

    /**
     * Every snippet compiles.
     *
     * <p>
     * The one that matters: a missing quote in a list of nineteen is invisible to a reader and fatal to the player who
     * clicks it. The caret becomes an identifier and the fragment is tried in each of the shapes a snippet can take - a
     * statement, an expression, a table field, the right-hand side of an assignment - because a snippet is a piece of a
     * script rather than a script.
     */
    @Test
    public void everySnippetIsLuaThatCompiles() {
        Globals globals = ScriptSandbox.newGlobals();

        for (Snippet snippet : ScriptSnippets.all()) {
            boolean compiled = false;
            String firstError = null;

            // Two stand-ins for the caret, because Lua has no token that is valid in both places a caret can sit: a
            // name where a value goes, a call where a statement goes. A snippet is fine if any reading of it parses.
            for (String placeholder : new String[] { "X", "X()" }) {
                String text = snippet.text.replace(String.valueOf(ScriptSnippets.CARET), placeholder);
                for (String shape : new String[] { text, "return " + text, "local _ = {" + text + "}", text + "nil" }) {
                    try {
                        globals.load(shape, "snippet");
                        compiled = true;
                        break;
                    } catch (LuaError e) {
                        if (firstError == null) {
                            firstError = e.getMessage();
                        }
                    }
                }
                if (compiled) {
                    break;
                }
            }
            assertTrue(snippet.label + " does not compile: " + firstError, compiled);
        }
    }

    @Test
    public void snippetsAreWrittenInTheHouseStyle() {
        for (Snippet snippet : ScriptSnippets.all()) {
            // One quote style throughout: a file that mixes them is where an unterminated string hides.
            assertFalse(snippet.label + " uses a single quote", snippet.text.indexOf('\'') >= 0);
            assertFalse(snippet.label + " leaves a space inside a brace", snippet.text.contains("{ "));
            assertFalse(snippet.label + " leaves a space inside a brace", snippet.text.contains(" }"));
        }
    }

    @Test
    public void whatASnippetWritesIsLuaTheHighlighterCanRead() {
        for (Snippet snippet : ScriptSnippets.all()) {
            String text = snippet.text.replace(String.valueOf(ScriptSnippets.CARET), "");
            for (java.util.List<LuaSyntax.Token> line : LuaSyntax.tokenize(text)) {
                int at = 0;
                for (LuaSyntax.Token token : line) {
                    assertEquals(snippet.label + " does not tokenize cleanly", at, token.start);
                    at = token.end;
                }
            }
        }
    }
}
