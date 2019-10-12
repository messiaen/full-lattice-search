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

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.spans.Spans;

public abstract class LatticePayloadScoreFunction {
    private final float lenNormFactor;

    public LatticePayloadScoreFunction(float lengthNormalizationFactor) {
        this.lenNormFactor = lengthNormalizationFactor;
    }

    /**
     * Calculate the score up to this point for this doc and field
     * @param docId The current doc
     * @param field The field
     * @param start The start position of the matching Span
     * @param end The end position of the matching Span
     * @param width the width of the current span
     * @param numPayloadsSeen The number of payloads seen so far
     * @param currentScore The current score so far
     * @param currentSpanScore The score for the current payload
     * @return The new current Score
     *
     * @see Spans
     */
    public abstract float spanScore(int docId, String field, int start, int end, int width, int numPayloadsSeen,
                                    float currentScore, float currentSpanScore);

    /**
     * Calculate the score up to this point for this span doc and field
     * @param docId The current doc
     * @param field The field
     * @param start The start position of the matching Span
     * @param end The end position of the matching Span
     * @param numPayloadsSeen The number of payloads seen so far within the span
     * @param currentScore The current score so far
     * @param currentPayloadScore The score for the current payload
     * @return The new current Score
     *
     * @see Spans
     */
    public abstract float currentLeafScore(int docId, String field, int start, int end, int numPayloadsSeen,
                                           float currentScore, float currentPayloadScore);

    /**
     * Calculate the final score for all the payloads seen so far for this doc/field
     * @param docId The current doc
     * @param field The current field
     * @param numSpansSeen The total number of matching spans seen on this document
     * @param payloadScore The raw score for those payloads
     * @return The final score for the payloads
     */
    public abstract float docScore(int docId, String field, int numSpansSeen, float payloadScore);

    public Explanation explain(int docId, String field, int numSpansSeen, float payloadScore){
        return Explanation.match(
                docScore(docId, field, numSpansSeen, payloadScore),
                getClass().getSimpleName() + ".docScore()");
    }

    public float lengthNormalizationFactor() {
        return lenNormFactor;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);
}
