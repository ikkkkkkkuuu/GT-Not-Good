package com.xyp.gtnotgood.common.blocks.webscreen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class WebScreenAddressTest {

    @Test
    public void normalizesHostAndPreservesEncodedNavigation() {
        assertEquals("https://www.bilibili.com", WebScreenAddress.normalize("  WWW.BILIBILI.COM  "));
        assertEquals("http://example.com:8080/a%2Fb?next=https://other.example#part",
            WebScreenAddress.normalize("HTTP://EXAMPLE.COM:8080/a%2Fb?next=https://other.example#part"));
        assertEquals("https://example.com/?next=https://other.example",
            WebScreenAddress.normalize("example.com/?next=https://other.example"));
    }

    @Test
    public void rejectsCredentialsAndNonWebSchemes() {
        rejects("https://user:password@example.com");
        rejects("https://user%40example.com@example.net");
        rejects("file:///C:/Windows/win.ini");
        rejects("javascript:alert(1)");
        rejects("data:text/html,hello");
        rejects("ftp://example.com");
    }

    @Test
    public void rejectsMissingHostsAndInvalidBounds() {
        rejects(null);
        rejects(" ");
        rejects("https://");
        rejects("https://example.com:65536");
        rejects("https://example.com:0");
        rejects("https://example.com/a b");
        rejects("https://example.com/\nhello");
        rejects("https://example.com/" + new String(new char[WebScreenAddress.MAX_LENGTH]).replace('\0', 'a'));
    }

    private static void rejects(String address) {
        try {
            WebScreenAddress.normalize(address);
            fail("Accepted invalid address: " + address);
        } catch (IllegalArgumentException expected) {}
    }
}
