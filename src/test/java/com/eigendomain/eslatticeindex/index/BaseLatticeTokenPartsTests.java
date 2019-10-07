package com.eigendomain.eslatticeindex.index;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BaseLatticeTokenPartsTests extends ESTestCase {
    @Test
    public void testParse() throws IOException {
        BaseLatticeTokenParts parts = new BaseLatticeTokenParts('|');
        char[] token = "cat|3|10|0.5".toCharArray();
        int len = token.length;

        boolean success = parts.parseToken(token, len);
        Assert.assertTrue(success);
        Assert.assertEquals(3, parts.tokenLen());
        Assert.assertEquals(3, parts.pos());
        Assert.assertEquals(10, parts.rank());
        Assert.assertEquals(0.5f, parts.score(), 0.000001);

        parts.reset();

        Assert.assertEquals(0, parts.tokenLen());
        Assert.assertEquals(0, parts.pos());
        Assert.assertEquals(0, parts.rank());
        Assert.assertNull(parts.score());
        Assert.assertNull(parts.encodedScore());
    }

    @Test
    public void testPositionIncrement() throws IOException {
        BaseLatticeTokenParts last = new BaseLatticeTokenParts('|');
        char[] lastTok  = "cat|3|34|0.01".toCharArray();
        int lastLen = lastTok.length;
        last.parseToken(lastTok, lastLen);

        BaseLatticeTokenParts curr = new BaseLatticeTokenParts('|');
        char[] currTok = "dog|3|10|0.5".toCharArray();
        int currLen = currTok.length;
        curr.parseToken(currTok, currLen);

        Assert.assertEquals(0, curr.positionIncrement(last));

        last.reset();
        last = new BaseLatticeTokenParts('|');
        lastTok  = "cat|9|34|0.01".toCharArray();
        lastLen = lastTok.length;
        last.parseToken(lastTok, lastLen);

        Assert.assertEquals(1, curr.positionIncrement(last));
    }
}
