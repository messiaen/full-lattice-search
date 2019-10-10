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

import com.eigendomain.eslatticeindex.plugin.LatticeIndexPlugin;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.index.query.QueryShardException;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.test.AbstractQueryTestCase;
import org.hamcrest.CoreMatchers;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

public class LatticeQueryBuilderTests extends AbstractQueryTestCase<LatticeQueryBuilder> {
    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Collections.singletonList(LatticeIndexPlugin.class);
    }


    @Override
    protected LatticeQueryBuilder doCreateTestQueryBuilder() {
        String fieldName = randomFrom(STRING_FIELD_NAME, STRING_ALIAS_FIELD_NAME, BOOLEAN_FIELD_NAME, INT_FIELD_NAME,
                DOUBLE_FIELD_NAME, DATE_FIELD_NAME);
        Object value;
        if (isTextField(fieldName)) {
            int terms = randomIntBetween(0, 3);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < terms; i++) {
                builder.append(randomAlphaOfLengthBetween(1, 10)).append(" ");
            }
            value = builder.toString().trim();
        } else {
            value = getRandomValueForFieldName(fieldName);
        }

        LatticeQueryBuilder query = new LatticeQueryBuilder(fieldName, value);

        if (randomBoolean() && isTextField(fieldName)) {
            query.analyzerString(randomFrom("simple", "keyword", "whitespace"));
        }

        if (randomBoolean()) {
            query.slop(randomIntBetween(0, 10));
        }

        if (randomBoolean()) {
            query.zeroTermsQuery(randomFrom(MatchQuery.ZeroTermsQuery.ALL, MatchQuery.ZeroTermsQuery.NONE));
        }

        if (randomBoolean()) {
            query.inOrder(randomBoolean());
        }

        if (randomBoolean()) {
            query.includeSpanScore(randomBoolean());
        }

        if (randomBoolean()) {
            query.slopSeconds((float)randomDoubleBetween(0.5, 10, true));
        }

        return query;
    }

    @Override
    protected void doAssertLuceneQuery(LatticeQueryBuilder queryBuilder, Query query, SearchContext context) throws IOException {
        assertThat(query, notNullValue());

        if (query instanceof MatchAllDocsQuery) {
            assertThat(queryBuilder.zeroTermsQuery(), CoreMatchers.equalTo(MatchQuery.ZeroTermsQuery.ALL));
            return;
        }

        assertThat(query, CoreMatchers.either(instanceOf(BooleanQuery.class))
                .or(instanceOf(PhraseQuery.class))
                .or(instanceOf(LatticePayloadScoreQuery.class))
                .or(instanceOf(PointRangeQuery.class))
                .or(instanceOf(IndexOrDocValuesQuery.class))
                .or(instanceOf(TermQuery.class))
                .or(instanceOf(MatchNoDocsQuery.class)));
    }

    public void testIllegalValues() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new LatticeQueryBuilder(null, "value"));
        assertEquals("[match_lattice] requires fieldName", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> new LatticeQueryBuilder("fieldName", null));
        assertEquals("[match_lattice] requires query value", e.getMessage());
    }

    public void testBadAnalyzer() throws IOException {
        LatticeQueryBuilder matchQuery = new LatticeQueryBuilder("fieldName", "text");
        matchQuery.analyzerString("bogusAnalyzer");
        QueryShardException e = expectThrows(QueryShardException.class, () -> matchQuery.toQuery(createShardContext()));
        assertThat(e.getMessage(), CoreMatchers.containsString("analyzer [bogusAnalyzer] not found"));
    }

    public void testFromSimpleJson() throws IOException {
        String json1 = "{\n" +
                "    \"match_lattice\" : {\n" +
                "        \"message\" : \"this is a test\"\n" +
                "    }\n" +
                "}";

        String expected = "{\n" +
                "  \"match_lattice\" : {\n" +
                "    \"message\" : {\n" +
                "      \"query\" : \"this is a test\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"slop_seconds\" : 3.0,\n" +
                "      \"zero_terms_query\" : \"NONE\",\n" +
                "      \"in_order\" : true,\n" +
                "      \"include_span_score\" : true,\n" +
                "      \"payload_function\" : \"default\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}";
        LatticeQueryBuilder qb = (LatticeQueryBuilder) parseQuery(json1);
        checkGeneratedJson(expected, qb);
    }

    public void testFromJson() throws IOException {
        String json = "{\n" +
                "  \"match_lattice\" : {\n" +
                "    \"message\" : {\n" +
                "      \"query\" : \"this is a test\",\n" +
                "      \"slop\" : 2,\n" +
                "      \"slop_seconds\" : 14.8,\n" +
                "      \"zero_terms_query\" : \"ALL\",\n" +
                "      \"in_order\" : false,\n" +
                "      \"include_span_score\" : false,\n" +
                "      \"payload_function\" : \"max\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}";

        LatticeQueryBuilder parsed = (LatticeQueryBuilder) parseQuery(json);
        checkGeneratedJson(json, parsed);

        assertEquals(json, "this is a test", parsed.value());
        assertEquals(json, 2, parsed.slop());
        assertEquals(json, MatchQuery.ZeroTermsQuery.ALL, parsed.zeroTermsQuery());
    }

    public void testParseFailsWithMultipleFields() throws IOException {
        String json = "{\n" +
                "  \"match_phrase\" : {\n" +
                "    \"message1\" : {\n" +
                "      \"query\" : \"this is a test\"\n" +
                "    },\n" +
                "    \"message2\" : {\n" +
                "      \"query\" : \"this is a test\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        ParsingException e = expectThrows(ParsingException.class, () -> parseQuery(json));
        assertEquals("[match_phrase] query doesn't support multiple fields, found [message1] and [message2]", e.getMessage());

        String shortJson = "{\n" +
                "  \"match_phrase\" : {\n" +
                "    \"message1\" : \"this is a test\",\n" +
                "    \"message2\" : \"this is a test\"\n" +
                "  }\n" +
                "}";
        e = expectThrows(ParsingException.class, () -> parseQuery(shortJson));
        assertEquals("[match_phrase] query doesn't support multiple fields, found [message1] and [message2]", e.getMessage());
    }
}
