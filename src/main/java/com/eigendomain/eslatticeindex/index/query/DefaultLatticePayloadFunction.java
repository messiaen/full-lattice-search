package com.eigendomain.eslatticeindex.index.query;

public class DefaultLatticePayloadFunction extends LatticePayloadScoreFuction {
    private static final float MIN_LOG_SCORE = (float)Math.log(10e-7);
    private static final float SCORE_MULT = (float)Math.log(10e4);

    @Override
    public float currentSpanScore(int docId, String field, int start, int end, int width, int numPayloadsSeen, float currentScore, float currentSpanScore) {
        // the scores are normalized by the length of the span
        // this incorporates that number of tokens in the query plus the number of skipped tokens
        return currentScore + (float)Math.exp((SCORE_MULT + currentSpanScore) - Math.log(end - start));
    }

    @Override
    public float currentLeafScore(int docId, String field, int start, int end, int numPayloadsSeen, float currentScore, float currentPayloadScore) {
        float logPayload = (float)Math.log(currentPayloadScore);
        float newScore = currentScore + logPayload;
        return newScore < MIN_LOG_SCORE ? MIN_LOG_SCORE : newScore;
    }

    @Override
    public float docScore(int docId, String field, int numSpansSeen, float payloadScore) {
        return numSpansSeen > 0 ? payloadScore : 1;
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
        if (!(o instanceof DefaultLatticePayloadFunction)) {
            return false;
        }
		return true;
    }
}
