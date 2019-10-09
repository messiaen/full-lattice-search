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

import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Arrays;

abstract class LatticeTokenParts<T extends LatticeTokenParts<T>> {
    private static final PayloadEncoder encoder =  new FloatEncoder();

    private final char fieldDelimiter;

    private int pos;
    private int rank;
    private int tokenLen;
    private BytesRef encodedScore;
    private Float score;

    LatticeTokenParts(char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
        this.reset();
    }

    public char delimiter() {
        return fieldDelimiter;
    }

    public int numFields() {
        return 3;
    }

    public int rank() {
        return rank;
    }

    public int pos() {
        return pos;
    }

    public int tokenLen() {
        return tokenLen;
    }

    public BytesRef encodedScore() {
        return encodedScore;
    }

    public Float score() {
        return score;
    }

    public int positionIncrement(T lastTokenParts) {
        if (lastTokenParts.pos() == pos()) {
            return 0;
        }
        return 1;
    }

    public int firstTokenIncrement() {
        return 1;
    }

    protected boolean parseFields(char[] token, int len, int[] delimiterLocs) {
        this.tokenLen = getTokenLen(delimiterLocs);
        this.pos = parseInteger(token, len, delimiterLocs, 1);
        this.rank = parseInteger(token, len, delimiterLocs, 2);
        this.score = parseFloat(token, len, delimiterLocs, 3);
        this.encodedScore = encodeFloat(token, len, delimiterLocs, 3);
        return true;
    }

    public final boolean parseToken(char[] token, int len) throws IOException {
        final int[] delimiterLocs = findDelimiters(token, len);
        if (delimiterLocs == null) {
            return false;
        }
        return parseFields(token, len, delimiterLocs);
    }

    protected static int getTokenLen(int[] delimiterLocs) {
        return delimiterLocs[0];
    }

    protected static int parseInteger(char[] token, int len, int[] delimiterLocs, int fieldNum) {
        int end = fieldNum < delimiterLocs.length ? delimiterLocs[fieldNum] : len;
        return Integer.parseInt(String.valueOf(Arrays.copyOfRange(token, delimiterLocs[fieldNum-1]+1, end)));
    }

    protected static Float parseFloat(char[] token, int len, int[] delimiterLocs, int fieldNum) {
        int end = fieldNum < delimiterLocs.length ? delimiterLocs[fieldNum] : len;
        return Float.parseFloat(String.valueOf(Arrays.copyOfRange(token, delimiterLocs[fieldNum-1]+1, end)));
    }

    protected static BytesRef encodeFloat(char[] token, int len, int[] delimiterLocs, int fieldNum) {
        int end = fieldNum < delimiterLocs.length ? delimiterLocs[fieldNum] : len;
        return encoder.encode(token, delimiterLocs[fieldNum-1]+1, end - (delimiterLocs[fieldNum-1]+1));
    }

    public void reset() {
        pos = 0;
        rank = 0;
        tokenLen = 0;
        encodedScore = null;
        score = null;
    }

    private int[] findDelimiters(char[] token, int len) throws IOException {
        int nFields = numFields();
        char d = delimiter();
        final int[] locs = new int[nFields];
        int i = 0;
        for (int j = 0; j < len && i < nFields; j++) {
            if (d == token[j]) {
                locs[i++] = j;
            }
        }
        if (i == 0) {
            return null;
        } else if (i == nFields) {
            return locs;
        } else {
            throw new IOException("Failed to parse token: " + String.valueOf(token));
        }
    }
}
