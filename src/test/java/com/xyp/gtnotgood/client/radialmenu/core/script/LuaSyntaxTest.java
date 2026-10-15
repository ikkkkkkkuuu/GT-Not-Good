package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.script.LuaSyntax.Kind;
import com.xyp.gtnotgood.client.radialmenu.core.script.LuaSyntax.Token;

/** What the editor paints, asked as a question about ranges rather than looked at on a screen. */
public class LuaSyntaxTest {

    private static List<Token> line(String source) {
        return LuaSyntax.tokenize(source).get(0);
    }

    /** The text of a token, which is what makes a failure readable. */
    private static String text(String source, Token token) {
        return source.substring(token.start, token.end);
    }

    private static Token find(String source, String wanted) {
        for (Token token : line(source)) {
            if (text(source, token).equals(wanted)) {
                return token;
            }
        }
        throw new AssertionError("no token reads '" + wanted + "' in: " + source);
    }

    @Test
    public void tokensTileEveryLineCompletely() {
        // The renderer walks them in order and draws each one; a gap would be text nobody painted.
        String source = "local x = 1 -- a note";
        int at = 0;
        for (Token token : line(source)) {
            assertEquals("tokens must be contiguous", at, token.start);
            at = token.end;
        }
        assertEquals(source.length(), at);
    }

    @Test
    public void everyLineGetsAnEntryIncludingBlankOnes() {
        List<List<Token>> lines = LuaSyntax.tokenize("a\n\nb");
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).isEmpty());
    }

    @Test
    public void readsKeywords() {
        assertEquals(Kind.Keyword, find("if x then end", "if").kind);
        assertEquals(Kind.Keyword, find("if x then end", "then").kind);
        assertEquals(Kind.Plain, find("if x then end", "x").kind);
    }

    @Test
    public void readsTheModsOwnGlobals() {
        assertEquals(Kind.Api, find("chat.send('hi')", "chat").kind);
        assertEquals(Kind.Api, find("menu.open(items)", "menu").kind);
        assertEquals(Kind.Plain, find("menu.open(items)", "items").kind);
    }

    @Test
    public void readsAMemberOnlyAfterADot() {
        // The point of the whole exercise: chat.send is coloured twice over and a misspelling is not - not even with
        // a bracket after it, which on any other name would be enough.
        assertEquals(Kind.Function, find("chat.send('x')", "send").kind);
        assertEquals(Kind.Plain, find("chat.sned('x')", "sned").kind);
        // "name" is a member of player, and a bare local called name is not one.
        assertEquals(Kind.Field, find("player.name", "name").kind);
        assertEquals(Kind.Plain, find("local name = 1", "name").kind);
    }

    @Test
    public void aTableAndWhatIsCalledOnItAreDifferentColours() {
        assertEquals(Kind.Api, find("chat.send('x')", "chat").kind);
        assertEquals(Kind.Function, find("chat.send('x')", "send").kind);
        // A space before the bracket is still a call.
        assertEquals(Kind.Function, find("chat.send ('x')", "send").kind);
    }

    @Test
    public void anythingWithABracketAfterItIsAFunction() {
        assertEquals(Kind.Function, find("notify('done')", "notify").kind);
        assertEquals(Kind.Function, find("local function pickHome(list)", "pickHome").kind);
        assertEquals(Kind.Function, find("pickHome(list)", "pickHome").kind);
        assertEquals(Kind.Function, find("list:gmatch('%a+')", "gmatch").kind);
        assertEquals(Kind.Plain, find("list:gmatch('%a+')", "list").kind);
        // A keyword is a keyword whatever follows it.
        assertEquals(Kind.Keyword, find("if (ready) then", "if").kind);
        assertEquals(Kind.Keyword, find("x = function(a) end", "function").kind);
    }

    @Test
    public void luasOwnLibrariesAreNotHeldToTheModsMemberList() {
        assertEquals(Kind.Api, find("string.format('%d', 1)", "string").kind);
        assertEquals(Kind.Function, find("string.format('%d', 1)", "format").kind);
        assertEquals(Kind.Function, find("table.insert(homes, home)", "insert").kind);
        assertEquals(Kind.Plain, find("x = math.pi", "pi").kind);
    }

    @Test
    public void aKnownMemberOfAnyValueIsAField() {
        assertEquals(Kind.Field, find("picture = stack.icon", "icon").kind);
        assertEquals(Kind.Field, find("player.held.name", "name").kind);
        assertEquals(Kind.Plain, find("home.distance", "distance").kind);
        assertEquals(Kind.Function, find("home.distance()", "distance").kind);
    }

    @Test
    public void wordsThatStandForAValueAreNotKeywords() {
        assertEquals(Kind.Constant, find("return nil", "nil").kind);
        assertEquals(Kind.Keyword, find("return nil", "return").kind);
        assertEquals(Kind.Constant, find("ok = true", "true").kind);
        assertEquals(Kind.Constant, find("ok = false", "false").kind);
        assertEquals(Kind.Self, find("self.count = 1", "self").kind);
        assertEquals(Kind.Declaration, find("local ok = true", "local").kind);
        assertEquals(Kind.Keyword, find("local function go() end", "function").kind);
        // Inside a longer name they are just letters.
        assertEquals(Kind.Plain, find("nilCount = 1", "nilCount").kind);
    }

    @Test
    public void aWordAfterConcatenationIsNotAMember() {
        // ".." is a two-character operator, so the word after it is a name of its own.
        assertEquals(Kind.Plain, find("'a' .. name", "name").kind);
    }

    @Test
    public void aWordAfterABracketIsNotAMember() {
        assertEquals(Kind.Plain, find("f(name)", "name").kind);
    }

    @Test
    public void readsStringsWithEitherQuote() {
        assertEquals(Kind.String, find("x = 'one'", "'one'").kind);
        assertEquals(Kind.String, find("x = \"two\"", "\"two\"").kind);
    }

    @Test
    public void anEscapedQuoteDoesNotEndAString() {
        String source = "x = 'it\\'s'";
        assertEquals(Kind.String, find(source, "'it\\'s'").kind);
    }

    @Test
    public void anUnterminatedStringStopsAtTheEndOfItsLine() {
        // Lua would refuse the file; the editor has to keep drawing, and a missing quote must not recolour the rest.
        List<List<Token>> lines = LuaSyntax.tokenize("x = 'oops\nlocal y = 1");
        assertEquals(Kind.String, lines.get(0).get(lines.get(0).size() - 1).kind);
        assertEquals(Kind.Declaration, lines.get(1).get(0).kind);
    }

    @Test
    public void readsNumbers() {
        assertEquals(Kind.Number, find("x = 20", "20").kind);
        assertEquals(Kind.Number, find("x = 1.5", "1.5").kind);
        assertEquals(Kind.Number, find("x = 0xFF", "0xFF").kind);
        assertEquals(Kind.Number, find("x = 1e-3", "1e-3").kind);
    }

    @Test
    public void readsLineComments() {
        String source = "code -- the rest 'including' quotes";
        assertEquals(Kind.Comment, find(source, "-- the rest 'including' quotes").kind);
    }

    @Test
    public void aLongCommentRunsAcrossLines() {
        List<List<Token>> lines = LuaSyntax.tokenize("--[[ start\nstill a comment\nend ]] local x = 1");
        for (Token token : lines.get(1)) {
            assertEquals(Kind.Comment, token.kind);
        }
        // And the code after the closing bracket is code again.
        List<Token> third = lines.get(2);
        assertEquals(Kind.Comment, third.get(0).kind);
        assertTrue(hasKind(third, Kind.Declaration));
    }

    @Test
    public void aLongStringRunsAcrossLinesAndMatchesItsOwnLevel() {
        List<List<Token>> lines = LuaSyntax.tokenize("x = [==[ one\n]] still open\n]==] local y = 1");
        for (Token token : lines.get(1)) {
            assertEquals("a ]] does not close a [==[", Kind.String, token.kind);
        }
        assertTrue(hasKind(lines.get(2), Kind.Declaration));
    }

    @Test
    public void readsOperators() {
        assertEquals(Kind.Operator, find("a == b", "==").kind);
        assertEquals(Kind.Operator, find("a .. b", "..").kind);
    }

    @Test
    public void whitespaceIsPlainRatherThanAbsent() {
        List<Token> tokens = line("a  b");
        assertEquals(3, tokens.size());
        assertEquals(Kind.Plain, tokens.get(1).kind);
        assertEquals("  ", text("a  b", tokens.get(1)));
    }

    @Test
    public void handlesTheWholeHomeScript() {
        // The script from the docs, as a canary: no exception, every line tiled, nothing left uncoloured.
        List<String> source = Arrays.asList("local me = player.name", "chat.send('/home list ' .. me)",
            "local list = chat.await('^' .. me .. ': %d+ / %d+: (.+)$', 60)",
            "if not list then notify('no answer') return end", "local homes = {}",
            "for name in list:gmatch('[^,%s]+') do homes[#homes + 1] = name end",
            "local pick = menu.open(homes, { title = 'Homes', slots = 8 })",
            "if pick then chat.send('/home ' .. pick .. ' ' .. me) end");

        List<List<Token>> lines = LuaSyntax.tokenize(source);
        assertEquals(source.size(), lines.size());
        for (int i = 0; i < source.size(); i++) {
            int at = 0;
            for (Token token : lines.get(i)) {
                assertEquals("line " + i + " is not tiled", at, token.start);
                at = token.end;
            }
            assertEquals("line " + i + " is not fully covered", source.get(i).length(), at);
        }
    }

    private static boolean hasKind(List<Token> tokens, Kind kind) {
        for (Token token : tokens) {
            if (token.kind == kind) {
                return true;
            }
        }
        return false;
    }
}
