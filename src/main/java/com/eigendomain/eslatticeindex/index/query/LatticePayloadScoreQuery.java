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

import java.io.IOException;
import java.util.*;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.queries.payloads.PayloadDecoder;
import org.apache.lucene.queries.payloads.PayloadFunction;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.spans.FilterSpans;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BytesRef;

/**
 * A Query class that uses a {@link PayloadFunction} to modify the score of a wrapped SpanQuery
 */
public class LatticePayloadScoreQuery extends SpanQuery {

    private final SpanQuery wrappedQuery;
    private final LatticePayloadScoreFuction function;
    private final PayloadDecoder decoder;
    private final boolean includeSpanScore;

    /**
     * Creates a new LatticePayloadScoreQuery
     * @param wrappedQuery the query to wrap
     * @param function a PayloadFunction to use to modify the scores
     * @param decoder a PayloadDecoder to convert payloads into float values
     * @param includeSpanScore include both span score and payload score in the scoring algorithm
     */
    public LatticePayloadScoreQuery(SpanQuery wrappedQuery, LatticePayloadScoreFuction function, PayloadDecoder decoder, boolean includeSpanScore) {
        this.wrappedQuery = Objects.requireNonNull(wrappedQuery);
        this.function = Objects.requireNonNull(function);
        this.decoder = Objects.requireNonNull(decoder);
        this.includeSpanScore = includeSpanScore;
    }

    /**
     * Creates a new LatticePayloadScoreQuery that includes the underlying span scores
     * @param wrappedQuery the query to wrap
     * @param function a PayloadFunction to use to modify the scores
	 * @param decoder a PayloadDecoder to use to decode each payload
     */
    public LatticePayloadScoreQuery(SpanQuery wrappedQuery, LatticePayloadScoreFuction function, PayloadDecoder decoder) {
        this(wrappedQuery, function, decoder, true);
    }

    @Override
    public String getField() {
        return wrappedQuery.getField();
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query matchRewritten = wrappedQuery.rewrite(reader);
        if (wrappedQuery != matchRewritten && matchRewritten instanceof SpanQuery) {
            return new LatticePayloadScoreQuery((SpanQuery)matchRewritten, function, decoder, includeSpanScore);
        }
        return super.rewrite(reader);
    }

    @Override
    public void visit(QueryVisitor visitor) {
        wrappedQuery.visit(visitor.getSubVisitor(BooleanClause.Occur.MUST, this));
    }


    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("LatticePayloadScoreQuery(");
        buffer.append(wrappedQuery.toString(field));
        buffer.append(", function: ");
        buffer.append(function.getClass().getSimpleName());
        buffer.append(", includeSpanScore: ");
        buffer.append(includeSpanScore);
        buffer.append(")");
        return buffer.toString();
    }

    @Override
    public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        SpanWeight innerWeight = wrappedQuery.createWeight(searcher, scoreMode, boost);
        if (!scoreMode.needsScores())
            return innerWeight;
        return new PayloadSpanWeight(searcher, innerWeight, boost);
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
                equalsTo(getClass().cast(other));
    }

    private boolean equalsTo(LatticePayloadScoreQuery other) {
        return wrappedQuery.equals(other.wrappedQuery) &&
                function.equals(other.function) && (includeSpanScore == other.includeSpanScore) &&
                Objects.equals(decoder, other.decoder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrappedQuery, function, decoder, includeSpanScore);
    }

    private class PayloadSpanWeight extends SpanWeight {

        private final SpanWeight innerWeight;

        public PayloadSpanWeight(IndexSearcher searcher, SpanWeight innerWeight, float boost) throws IOException {
            super(LatticePayloadScoreQuery.this, searcher, null, boost);
            this.innerWeight = innerWeight;
        }

        @Override
        public void extractTermStates(Map<Term, TermStates> contexts) {
            innerWeight.extractTermStates(contexts);
        }

        @Override
        public Spans getSpans(LeafReaderContext ctx, Postings requiredPostings) throws IOException {
            return innerWeight.getSpans(ctx, requiredPostings.atLeast(Postings.PAYLOADS));
        }

        @Override
        public SpanScorer scorer(LeafReaderContext context) throws IOException {
            Spans spans = getSpans(context, Postings.PAYLOADS);
            if (spans == null)
                return null;
            LeafSimScorer docScorer = innerWeight.getSimScorer(context);
            PayloadSpans payloadSpans = new PayloadSpans(spans, decoder);
            System.out.println("spans=" + payloadSpans.toString());
            return new PayloadSpanScorer(this, payloadSpans, docScorer);
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return innerWeight.isCacheable(ctx);
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            innerWeight.extractTerms(terms);
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            PayloadSpanScorer scorer = (PayloadSpanScorer)scorer(context);
            if (scorer == null || scorer.iterator().advance(doc) != doc)
                return Explanation.noMatch("No match");

            scorer.score();  // force freq calculation
            Explanation payloadExpl = scorer.getPayloadExplanation();

            if (includeSpanScore) {
                SpanWeight innerWeight = ((PayloadSpanWeight) scorer.getWeight()).innerWeight;
                Explanation innerExpl = innerWeight.explain(context, doc);
                return Explanation.match(scorer.scoreCurrentDoc(), "PayloadSpanQuery, product of:", innerExpl, payloadExpl);
            }

            return scorer.getPayloadExplanation();
        }
    }

    private class PayloadSpans extends FilterSpans implements SpanCollector {

        private final PayloadDecoder decoder;
        public int payloadsSeen;
        public float payloadScore;

        public List<Float> spanScores = new ArrayList<>();
        public float currentSpanScore;
        public int numSpansSeen = 0;

        private PayloadSpans(Spans in, PayloadDecoder decoder) {
            super(in);
            this.decoder = decoder;
        }

        @Override
        protected AcceptStatus accept(Spans candidate) throws IOException {
            return AcceptStatus.YES;
        }

        @Override
        protected void doStartCurrentDoc() {
            payloadScore = 0;
            payloadsSeen = 0;
            spanScores = new ArrayList<>();
            currentSpanScore = 0.0f;
            numSpansSeen = 0;
        }

        @Override
        public void collectLeaf(PostingsEnum postings, int position, Term term) throws IOException {
            BytesRef payload = postings.getPayload();
            float payloadFactor = decoder.computePayloadFactor(payload);
            currentSpanScore = function.currentLeafScore(docID(), getField(), in.startPosition(), in.endPosition(),
                    payloadsSeen, currentSpanScore, payloadFactor);

            System.out.println("term=" + term + "; payload=" + payloadFactor);
            payloadsSeen++;
        }

        @Override
        public void reset() {}

        @Override
        protected void doCurrentSpans() throws IOException {
            currentSpanScore = 0.0f;
            payloadsSeen = 0;
            in.collect(this);
            System.out.println("span=" + in + "; width=" + in.width());
            if (in.startPosition() != Spans.NO_MORE_POSITIONS) {
                payloadScore = function.spanScore(docID(), getField(), in.startPosition(), in.endPosition(),
                        in.width(), payloadsSeen, payloadScore, currentSpanScore);
                numSpansSeen++;
            }
        }
    }

    private class PayloadSpanScorer extends SpanScorer {

        private final PayloadSpans spans;
        private final float MIN_LOG_SCORE = (float)Math.log(10e-7f);
        private final float SCORE_MULT = (float)Math.log(10e4f);

        private PayloadSpanScorer(SpanWeight weight, PayloadSpans spans, LeafSimScorer docScorer) throws IOException {
            super(weight, spans, docScorer);
            this.spans = spans;
        }

        protected float getPayloadScore_() {
            float score = function.docScore(docID(), getField(), spans.payloadsSeen, spans.payloadScore);
            if (score >= 0 == false) {
                return 0;
            } else {
                return score;
            }
        }

        protected float getPayloadScore() {
            return function.docScore(docID(), getField(), spans.numSpansSeen, spans.payloadScore);
        }

        /*
        protected float getPayloadScore__() {
            float maxScore = -Float.MAX_VALUE;
            float logPayload;
            float newScore;
            float s;
            for (List<Float> payloadSet : spans.payloadSets) {
                s = 0.0f;
                for (float payload : payloadSet) {
                    logPayload = (float)Math.log(payload);
                    newScore = s + logPayload;
                    s = newScore < MIN_LOG_SCORE ? MIN_LOG_SCORE : newScore;
                }
                System.out.println("s=" + s);
                System.out.println("maxScore=" + maxScore);
                if (s > maxScore) {
                    maxScore = s;
                }
            }
            System.out.println("=>maxScore=" + maxScore);
            maxScore += SCORE_MULT;
            float score = (float)Math.exp(maxScore);
            System.out.println("=>score=" + score);
            return score;
        }
        */

        protected Explanation getPayloadExplanation() {
            Explanation expl = function.explain(docID(), getField(), spans.payloadsSeen, spans.payloadScore);
            if (expl.getValue().floatValue() < 0) {
                expl = Explanation.match(0, "truncated score, max of:", Explanation.match(0f, "minimum score"), expl);
            } else if (Float.isNaN(expl.getValue().floatValue())) {
                expl = Explanation.match(0, "payload score, computed as (score == NaN ? 0 : score) since NaN is an illegal score from:", expl);
            }
            return expl;
        }

        protected float getSpanScore() throws IOException {
            return super.scoreCurrentDoc();
        }

        @Override
        protected float scoreCurrentDoc() throws IOException {
            float score = getPayloadScore();
            System.out.println("score=" + score);
            if (includeSpanScore)
                return getSpanScore() * score;
            return score;
        }

    }

}

