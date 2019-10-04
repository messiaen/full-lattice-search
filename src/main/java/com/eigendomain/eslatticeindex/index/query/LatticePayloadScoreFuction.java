package com.eigendomain.eslatticeindex.index.query;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.spans.Spans;

public abstract class LatticePayloadScoreFuction {
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
    public abstract float spanScore(int docId, String field, int start, int end, int width, int numPayloadsSeen, float currentScore, float currentSpanScore);

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
    public abstract float currentLeafScore(int docId, String field, int start, int end, int numPayloadsSeen, float currentScore, float currentPayloadScore);

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

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);
}
