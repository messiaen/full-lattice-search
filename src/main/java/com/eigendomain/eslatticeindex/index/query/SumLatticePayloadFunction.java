/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eigendomain.eslatticeindex.index.query;

public class SumLatticePayloadFunction extends LatticePayloadScoreFunction {
    protected static final float MIN_SCORE = 10e-7f;
    protected static final float MIN_LOG_SCORE = (float)Math.log(MIN_SCORE);
    protected static final float SCORE_MULT = (float)Math.log(10e4);

    public SumLatticePayloadFunction(float lengthNormalizationFactor) {
        super(lengthNormalizationFactor);
    }

    @Override
    public float spanScore(int docId, String field, int start, int end, int width, int numPayloadsSeen,
                           float currentScore, float currentSpanScore) {
        // the scores are normalized by the length of the span
        // this incorporates that number of tokens in the query plus the number of skipped tokens
        if (lengthNormalizationFactor() == 0.0) {
            return currentScore + (float) Math.exp(SCORE_MULT + currentSpanScore);
        } else {
            return currentScore
                    + (float) Math.exp(
                    (SCORE_MULT + currentSpanScore) - Math.log((end - start) * lengthNormalizationFactor()));
        }
    }

    @Override
    public float currentLeafScore(int docId, String field, int start, int end, int numPayloadsSeen, float currentScore,
                                  float currentPayloadScore) {
        float logPayload = (float)Math.log(currentPayloadScore);
        float newScore = currentScore + logPayload;
        return Math.max(newScore, MIN_LOG_SCORE);
    }

    @Override
    public float docScore(int docId, String field, int numSpansSeen, float payloadScore) {
        return numSpansSeen > 0 ? payloadScore : MIN_SCORE;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof SumLatticePayloadFunction)) {
            return false;
        }

        SumLatticePayloadFunction other = (SumLatticePayloadFunction) o;
        if (this.lengthNormalizationFactor() != other.lengthNormalizationFactor()) {
            return false;
        }
        return true;
    }
}
