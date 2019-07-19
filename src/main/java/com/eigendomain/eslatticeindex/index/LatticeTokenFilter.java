/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eigendomain.eslatticeindex.index;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;

public class LatticeTokenFilter extends TokenFilter {
    public static final char DELIMITER = '|';
    public static final char NUM_FIELDS = 3;
    private final int numFields;
    private final PayloadEncoder encoder;
    private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final TokenParts tokenParts;
    private int lastPos;
    private boolean firstTok;

    private final ArrayList<Map.Entry<Float, Integer>> bucketEntries;
    private int repeatTok;

    public LatticeTokenFilter(TokenStream input, SortedMap<Float, Integer> buckets, int numExtraFields) {
        super(input);
        encoder = new FloatEncoder();
        tokenParts = new TokenParts();
        lastPos = 0;
        firstTok = true;
        repeatTok = 0;

        this.bucketEntries = new ArrayList<>(buckets.entrySet());
        this.numFields = NUM_FIELDS + numExtraFields;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (repeatTok > 0) {
            posIncAtt.setPositionIncrement(0);
            repeatTok--;
            return true;
        } else if (input.incrementToken()) {
            if (splitToken(termAtt.buffer(), termAtt.length())) {
                payAtt.setPayload(tokenParts.score);
                termAtt.setLength(tokenParts.tokenLen);

                if (firstTok) {
                    posIncAtt.setPositionIncrement(1);
                } else if (lastPos == tokenParts.pos) {
                    posIncAtt.setPositionIncrement(0);
                } else {
                    posIncAtt.setPositionIncrement(1);
                }

                lastPos = tokenParts.pos;
                firstTok = false;

                repeatTok = tokRepeats(tokenParts.scoreValue) - 1;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        tokenParts.reset();
        lastPos = 0;
        firstTok = true;
        repeatTok = 0;
    }

    private int tokRepeats(float score) {
        for (Map.Entry<Float, Integer> e : bucketEntries) {
            if (score >= e.getKey()) {
                return e.getValue();
            }
        }
        return 1;
    }

    private boolean splitToken(char[] token, int len) throws IOException {
        final int[] delimiterLocs = findDelimiters(token, len);
        if (delimiterLocs == null) {
            return false;
        }

        int scoreEnd = delimiterLocs.length > NUM_FIELDS - 1 ? delimiterLocs[2] : len;

        tokenParts.tokenLen = delimiterLocs[0];
        tokenParts.pos = Integer.parseInt(String.valueOf(Arrays.copyOfRange(token, delimiterLocs[0]+1, delimiterLocs[1])));
        tokenParts.scoreValue = Float.parseFloat(String.copyValueOf(Arrays.copyOfRange(token, delimiterLocs[1]+1 , scoreEnd)));
        tokenParts.score = encoder.encode(token, delimiterLocs[1]+1, scoreEnd - (delimiterLocs[1]+1));
        return true;
    }

    private int[] findDelimiters(char[] token, int len) throws IOException {
        final int[] locs = new int[numFields-1];
        int i = 0;
        for (int j = 0; j < len && i < numFields-1; j++) {
            if (DELIMITER == token[j]) {
                locs[i++] = j;
            }
        }
        if (i == 0) {
            return null;
        } else if (i == numFields-1) {
            return locs;
        } else {
            throw new IOException("Failed to parse token");
        }
    }

    private class TokenParts {
        private int pos;
        private int tokenLen;
        private BytesRef score;
        private Float scoreValue;

        private void reset() {
            pos = 0;
            tokenLen = 0;
            score = null;
            scoreValue = null;
        }
    }
}
