package com.eigendomain.eslatticeindex.index.query;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.queries.payloads.PayloadDecoder;
import org.apache.lucene.util.BytesRef;

public class FloatDecoder implements PayloadDecoder {

    private final float scale;

    public FloatDecoder(float scale) {
        this.scale = scale;
    }

    public FloatDecoder() {
        this(1.0f);
    }

    @Override
    public float computePayloadFactor(BytesRef payload) {
        if (payload == null) {
            return 0.00001f;
        } else {
            return PayloadHelper.decodeFloat(payload.bytes, payload.offset) * scale;
        }
    }
}
