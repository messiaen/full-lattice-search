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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

public class LatticeTokenFilter<T extends LatticeTokenParts<T>> extends TokenFilter {
    private final char delimiter;
    private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

    private final LatticeTokenPartsFactory<T> tokenPartsFactory;

    private T currTokParts;
    private T lastTokParts;
    private T tmpTok;
    private boolean firstTok;

    private final ArrayList<Map.Entry<Float, Integer>> bucketEntries;
    private int repeatTok;

    public LatticeTokenFilter(TokenStream input, SortedMap<Float, Integer> buckets, char fieldDelimiter,
                              LatticeTokenPartsFactory<T> tokenPartsFactory) {
        super(input);
        this.tokenPartsFactory = tokenPartsFactory;

        currTokParts = this.tokenPartsFactory.create(fieldDelimiter);
        lastTokParts = this.tokenPartsFactory.create(fieldDelimiter);
        tmpTok = null;
        firstTok = true;
        repeatTok = 0;

        delimiter = fieldDelimiter;

        this.bucketEntries = new ArrayList<>(buckets.entrySet());
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (repeatTok > 0) {
            posIncAtt.setPositionIncrement(0);
            repeatTok--;
            return true;
        } else if (input.incrementToken()) {
            if (currTokParts.parseToken(termAtt.buffer(), termAtt.length())) {
                payAtt.setPayload(currTokParts.encodedScore());
                termAtt.setLength(currTokParts.tokenLen());

                if (firstTok) {
                    posIncAtt.setPositionIncrement(currTokParts.firstTokenIncrement());
                } else {
                   posIncAtt.setPositionIncrement(currTokParts.positionIncrement(lastTokParts));
                }

                firstTok = false;
                repeatTok = tokRepeats(currTokParts.score()) - 1;

                tmpTok = lastTokParts;
                lastTokParts = currTokParts;
                currTokParts = tmpTok;
                currTokParts.reset();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        currTokParts.reset();
        lastTokParts.reset();
        tmpTok = null;

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
}
