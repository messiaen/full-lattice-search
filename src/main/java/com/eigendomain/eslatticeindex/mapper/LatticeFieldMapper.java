/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.eigendomain.eslatticeindex.mapper;

import com.eigendomain.eslatticeindex.LatticeFormat;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.FieldNamesFieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.StringFieldType;
import org.elasticsearch.index.mapper.TextFieldMapper;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.fetch.FetchSubPhase.HitContext;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.index.mapper.TypeParsers.parseTextField;

public class LatticeFieldMapper extends FieldMapper {

    public static final String CONTENT_TYPE = "lattice";
    private static final int POSITION_INCREMENT_GAP_USE_ANALYZER = -1;

    public static class Defaults {
        public static final MappedFieldType FIELD_TYPE = new LatticeFieldType();
        static {
            FIELD_TYPE.freeze();
        }
    }

    public static class Builder extends FieldMapper.Builder<Builder, LatticeFieldMapper> {

        private int positionIncrementGap = POSITION_INCREMENT_GAP_USE_ANALYZER;
        private String latticeFormat = "lattice";
        private float audioPositionIncrementSeconds = 0.01f;

        public Builder(String name) {
            super(name, Defaults.FIELD_TYPE, Defaults.FIELD_TYPE);
            builder = this;
        }

        @Override
        public LatticeFieldType fieldType() {
            return (LatticeFieldType) super.fieldType();
        }

        public Builder positionIncrementGap(int positionIncrementGap) {
            if (positionIncrementGap < 0) {
                throw new MapperParsingException("[positions_increment_gap] must be positive, got " + positionIncrementGap);
            }
            this.positionIncrementGap = positionIncrementGap;
            return this;
        }

        public Builder audioPositionIncrementSeconds(float secs) {
            this.audioPositionIncrementSeconds = secs;
            return this;
        }

        public Builder latticeFormat(String format) {
            LatticeFormat f = LatticeFormat.fromString(format);
            if (null == f) {
                throw new IllegalArgumentException("Invalid lattice format '" + format + "'");
            }
            this.latticeFormat = f.toString();
            return this;
        }

        @Override
        public Builder docValues(boolean docValues) {
            if (docValues) {
                throw new IllegalArgumentException("[" + CONTENT_TYPE + "] fields do not support doc values");
            }
            return super.docValues(docValues);
        }

        @Override
        public LatticeFieldMapper build(BuilderContext context) {
            if (fieldType().indexOptions() == IndexOptions.NONE ) {
                throw new IllegalArgumentException("[" + CONTENT_TYPE + "] fields must be indexed");
            }
            if (positionIncrementGap != POSITION_INCREMENT_GAP_USE_ANALYZER) {
                if (fieldType.indexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) {
                    throw new IllegalArgumentException("Cannot set position_increment_gap on field ["
                            + name + "] without positions enabled");
                }
                fieldType.setIndexAnalyzer(new NamedAnalyzer(fieldType.indexAnalyzer(), positionIncrementGap));
                fieldType.setSearchAnalyzer(new NamedAnalyzer(fieldType.searchAnalyzer(), positionIncrementGap));
                fieldType.setSearchQuoteAnalyzer(new NamedAnalyzer(fieldType.searchQuoteAnalyzer(), positionIncrementGap));
            } else {
                //Using the analyzer's default BUT need to do the same thing AnalysisRegistry.processAnalyzerFactory
                // does to splice in new default of posIncGap=100 by wrapping the analyzer
                if (fieldType.indexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) {
                    int overrideInc = TextFieldMapper.Defaults.POSITION_INCREMENT_GAP;
                    fieldType.setIndexAnalyzer(new NamedAnalyzer(fieldType.indexAnalyzer(), overrideInc));
                    fieldType.setSearchAnalyzer(new NamedAnalyzer(fieldType.searchAnalyzer(), overrideInc));
                    fieldType.setSearchQuoteAnalyzer(new NamedAnalyzer(fieldType.searchQuoteAnalyzer(),overrideInc));
                }
            }
            setupFieldType(context);
            ((LatticeFieldType)fieldType).setLatticeFormat(latticeFormat);
            ((LatticeFieldType)fieldType).setAudioPositionIncrementSeconds(audioPositionIncrementSeconds);
            return new LatticeFieldMapper(
                    name, fieldType(), defaultFieldType,
                    positionIncrementGap, latticeFormat, audioPositionIncrementSeconds,
                    context.indexSettings(), multiFieldsBuilder.build(this, context), copyTo);
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        public Mapper.Builder<LatticeFieldMapper.Builder, LatticeFieldMapper> parse(
                String fieldName, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            LatticeFieldMapper.Builder builder = new LatticeFieldMapper.Builder(fieldName);

            builder.fieldType().setIndexAnalyzer(parserContext.getIndexAnalyzers().getDefaultIndexAnalyzer());
            builder.fieldType().setSearchAnalyzer(parserContext.getIndexAnalyzers().getDefaultSearchAnalyzer());
            builder.fieldType().setSearchQuoteAnalyzer(parserContext.getIndexAnalyzers().getDefaultSearchQuoteAnalyzer());
            parseTextField(builder, fieldName, node, parserContext);
            for (Iterator<Map.Entry<String, Object>> iterator = node.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, Object> entry = iterator.next();
                String propName = entry.getKey();
                Object propNode = entry.getValue();
                if (propName.equals("position_increment_gap")) {
                    int newPositionIncrementGap = XContentMapValues.nodeIntegerValue(propNode, -1);
                    builder.positionIncrementGap(newPositionIncrementGap);
                    iterator.remove();
                } else if (propName.equals("lattice_format")) {
                    String format = XContentMapValues.nodeStringValue(propNode, "lattice");
                    builder.latticeFormat(format);
                } else if (propName.equals("audio_position_increment_seconds")) {
                    float secs = XContentMapValues.nodeFloatValue(propNode, 0.01f);
                    builder.audioPositionIncrementSeconds(secs);
                }
            }
            return builder;
        }
    }

    public static final class LatticeFieldType extends StringFieldType {

        private String latticeFormat = "lattice";
        private float audioPositionIncrementSeconds = 0.01f;

        public LatticeFieldType() {
            setTokenized(true);
        }

        protected LatticeFieldType(LatticeFieldType ref) {
            super(ref);
        }

        public LatticeFieldType clone() {
            return new LatticeFieldType(this);
        }

        public String latticeFormat() {
            return latticeFormat;
        }

        public void setLatticeFormat(String format) {
            this.latticeFormat = format;
        }

        public float audioPositionIncrementSeconds() {
            return audioPositionIncrementSeconds;
        }

        public void setAudioPositionIncrementSeconds(float secs) {
            this.audioPositionIncrementSeconds = secs;
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }

        @Override
        public Query existsQuery(QueryShardContext context) {
            if (omitNorms()) {
                return new TermQuery(new Term(FieldNamesFieldMapper.NAME, name()));
            } else {
                return new NormsFieldExistsQuery(name());
            }
        }

        @Override
        public SpanQuery spanPrefixQuery(String value, SpanMultiTermQueryWrapper.SpanRewriteMethod method, QueryShardContext context) {
            SpanMultiTermQueryWrapper<?> spanMulti =
                    new SpanMultiTermQueryWrapper<>(new PrefixQuery(new Term(name(), indexedValueForSearch(value))));
            spanMulti.setRewriteMethod(method);
            return spanMulti;
        }

        @Override
        public Query phraseQuery(TokenStream stream, int slop, boolean enablePositionIncrements) throws IOException {
            return TextFieldMapper.createPhraseQuery(stream, name(), slop, enablePositionIncrements);
        }

        @Override
        public Query multiPhraseQuery(TokenStream stream, int slop, boolean enablePositionIncrements) throws IOException {
            return TextFieldMapper.createPhraseQuery(stream, name(), slop, enablePositionIncrements);
        }

        @Override
        public Query phrasePrefixQuery(TokenStream stream, int slop, int maxExpansions) throws IOException {
            return TextFieldMapper.createPhrasePrefixQuery(stream, name(), slop, maxExpansions, null, null);
        }
    }

    private int positionIncrementGap;
    private String latticeFormat = "lattice";
    private float audioPositionIncrementSeconds = 0.01f;
    protected LatticeFieldMapper(String simpleName, LatticeFieldType fieldType, MappedFieldType defaultFieldType,
                                       int positionIncrementGap, String latticeFormat,
                                       float audioPositionIncrementSeconds,
                                       Settings indexSettings, MultiFields multiFields, CopyTo copyTo) {
        super(simpleName, fieldType, defaultFieldType, indexSettings, multiFields, copyTo);
        assert fieldType.tokenized();
        assert fieldType.hasDocValues() == false;
        this.positionIncrementGap = positionIncrementGap;
        this.latticeFormat = latticeFormat;
        this.audioPositionIncrementSeconds = audioPositionIncrementSeconds;
    }

    @Override
    protected LatticeFieldMapper clone() {
        return (LatticeFieldMapper) super.clone();
    }

    public int getPositionIncrementGap() {
        return this.positionIncrementGap;
    }

    @Override
    protected void parseCreateField(ParseContext context, List<IndexableField> fields) throws IOException {
        final String value;
        if (context.externalValueSet()) {
            value = context.externalValue().toString();
        } else {
            value = context.parser().textOrNull();
        }

        if (value == null) {
            return;
        }

        if (fieldType().indexOptions() != IndexOptions.NONE || fieldType().stored()) {
            Field field = new Field(fieldType().name(), value, fieldType());
            fields.add(field);
            if (fieldType().omitNorms()) {
                createFieldNamesField(context, fields);
            }
        }
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public LatticeFieldType fieldType() {
        return (LatticeFieldType) super.fieldType();
    }

    @Override
    protected void doXContentBody(XContentBuilder builder, boolean includeDefaults, Params params) throws IOException {
        super.doXContentBody(builder, includeDefaults, params);
        doXContentAnalyzers(builder, includeDefaults);

        if (includeDefaults || positionIncrementGap != POSITION_INCREMENT_GAP_USE_ANALYZER) {
            builder.field("position_increment_gap", positionIncrementGap);
        }
        if (includeDefaults || fieldType().audioPositionIncrementSeconds() != ((LatticeFieldType) defaultFieldType).audioPositionIncrementSeconds()) {
            builder.field("audio_position_increment_seconds", audioPositionIncrementSeconds);
        }
        if (includeDefaults || fieldType().latticeFormat() != ((LatticeFieldType) defaultFieldType).latticeFormat()) {
            builder.field("lattice_format", latticeFormat);
        }
    }
}
