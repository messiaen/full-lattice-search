package com.eigendomain.eslatticeindex.index.query;

import org.apache.lucene.queries.payloads.PayloadFunction;

public class LatticePayloadFunction extends PayloadFunction {
    public static final float MIN_POST = 0.001f;
    // TODO use log base 2 if possible
    public static final float LOG_MIN_POST = log2(MIN_POST);
    private final boolean normalize;
    private final boolean normProb;

    public LatticePayloadFunction() {
        this(true, false);
    }

    public LatticePayloadFunction(boolean normalize, boolean normProb) {
        this.normalize = normalize;
        this.normProb = normProb;
    }

    @Override
    public float currentScore(int docId, String field, int start, int end, int numPayloadsSeen, float currentScore, float currentPayloadScore) {
        System.out.println("currentScore=" + currentScore + "; currentPayloadScore=" + currentPayloadScore + ";");
        return currentScore + log2(currentPayloadScore < MIN_POST ? MIN_POST : currentPayloadScore);
    }

    @Override
    public float docScore(int docId, String field, int numPayloadsSeen, float payloadScore) {
        System.out.println("payloadScore=" + payloadScore + ";");
        System.out.println("numPayloadsSeen=" + numPayloadsSeen + ";");
        System.out.println("LOG_MIN_POST=" + LOG_MIN_POST + ";");
        if (numPayloadsSeen > 0 && normProb) {
            return (float)Math.pow(2, payloadScore  - log2(numPayloadsSeen));
        }
        if (numPayloadsSeen > 0 && normalize) {
            return (payloadScore - LOG_MIN_POST) / (float) numPayloadsSeen + 1.0f;
        } else if (numPayloadsSeen > 0) {
            return payloadScore - LOG_MIN_POST + 1.0f;
        } else {
            return 1.0f;
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.getClass().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

    private static float log2(float f) {
        return (float)(Math.log(f)/Math.log(2));
    }
}
