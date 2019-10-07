package com.eigendomain.eslatticeindex.index;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class AudioLatticeTokenPartsTests extends ESTestCase {
    @Test
    public void testParse() throws IOException {
        AudioLatticeTokenParts parts = new AudioLatticeTokenParts('|', 0.01f);
        char[] token = "cat|3|10|0.5|1.26|2.27".toCharArray();
        int len = token.length;

        boolean success = parts.parseToken(token, len);
        Assert.assertTrue(success);
        Assert.assertEquals(3, parts.tokenLen());
        Assert.assertEquals(3, parts.pos());
        Assert.assertEquals(10, parts.rank());
        Assert.assertEquals(0.5f, parts.score(), 0.000001);
        Assert.assertEquals(1.26, parts.startTime(), 0.0001);
        Assert.assertEquals(2.27, parts.stopTime(), 0.0001);

        parts.reset();

        Assert.assertEquals(0, parts.tokenLen());
        Assert.assertEquals(0, parts.pos());
        Assert.assertEquals(0, parts.rank());
        Assert.assertNull(parts.score());
        Assert.assertNull(parts.encodedScore());
        Assert.assertEquals(0.0, parts.startTime(), 0.0001);
        Assert.assertEquals(0.0, parts.stopTime(), 0.0001);
    }

    @Test
    public void testPositionIncrement() throws IOException {
        AudioLatticeTokenParts last = new AudioLatticeTokenParts('|', 0.1f);
        char[] lastTok  = "cat|3|34|0.01|1.26|2.27".toCharArray();
        int lastLen = lastTok.length;
        last.parseToken(lastTok, lastLen);

        AudioLatticeTokenParts curr = new AudioLatticeTokenParts('|', 0.1f);
        char[] currTok = "dog|3|10|0.5|2.28|3.0".toCharArray();
        int currLen = currTok.length;
        curr.parseToken(currTok, currLen);

        Assert.assertEquals(0, curr.positionIncrement(last));

        last = new AudioLatticeTokenParts('|', 0.1f);
        lastTok  = "cat|9|34|0.01|1.26|2.27".toCharArray();
        lastLen = lastTok.length;
        last.parseToken(lastTok, lastLen);

        Assert.assertEquals(11, curr.positionIncrement(last));

        curr = new AudioLatticeTokenParts('|', 0.01f);
        currTok = "dog|3|10|0.5|2.28|3.0".toCharArray();
        currLen = currTok.length;
        curr.parseToken(currTok, currLen);

        Assert.assertEquals(102, curr.positionIncrement(last));
    }
}
