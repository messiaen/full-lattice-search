package com.eigendomain.eslatticeindex.index.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.payloads.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.search.MatchQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.elasticsearch.common.lucene.search.Queries.newUnmappedFieldQuery;

public class LatticeQueryBuilder extends AbstractQueryBuilder<LatticeQueryBuilder> implements SpanQueryBuilder {
    public static final String NAME = "match_lattice_phrase";

    private final String fieldName;
    private final Object value;

    private String analyzerString = null;
    private String payloadFuncString = "sum";
    private String payloadDecoderString = "float";
    private float payloadScale = 1.0f;
    private boolean includeSpanScore = true;
    private boolean inOrder = true;
    private int slop = MatchQuery.DEFAULT_PHRASE_SLOP;

    private MatchQuery.ZeroTermsQuery zeroTermsQuery = MatchQuery.DEFAULT_ZERO_TERMS_QUERY;

    private PayloadFunction payloadFunction = new SumPayloadFunction();
    private PayloadDecoder payloadDecoder = PayloadDecoder.FLOAT_DECODER;

    private static final ParseField SLOP_FIELD = new ParseField("slop");
    private static final ParseField IN_ORDER_FIELD = new ParseField("in_order");
    private static final ParseField INCLUDE_SPAN_SCORE_FIELD = new ParseField("include_span_score");
    private static final ParseField PAYLOAD_FUNCTION_FIELD = new ParseField("payload_function");
    private static final ParseField PAYLOAD_DECODER_FIELD = new ParseField("payload_decoder");
    private static final ParseField PAYLOAD_SCALE_FIELD = new ParseField("payload_scale");

    public LatticeQueryBuilder(String fieldName, Object value) {
        super();
        this.fieldName = fieldName;
        this.value = value;
    }

    public LatticeQueryBuilder(StreamInput in) throws IOException {
        super(in);

        this.fieldName = in.readString();
        this.value = in.readGenericValue();
        this.slop = in.readVInt();
        this.inOrder = in.readBoolean();
        this.includeSpanScore = in.readBoolean();
        this.payloadFuncString = in.readString();
        this.payloadDecoderString = in.readString();

        this.analyzerString = in.readOptionalString();
    }

    private static PayloadFunction parsePayloadFuncString(String name) {
        switch(name) {
            case "sum":
                return new SumPayloadFunction();
            case "max":
                return new MaxPayloadFunction();
            case "min":
                return new MinPayloadFunction();
            case "average":
                return new AveragePayloadFunction();
            case "lattice":
                return new LatticePayloadFunction();
            case "denorm_lattice":
                return new LatticePayloadFunction(false, false);
            case "normprob_lattice":
                return new LatticePayloadFunction(false, true);
        }
        throw new IllegalArgumentException("Invalid payload function: " + name);
    }

    private static PayloadDecoder parseDecoder(String name, float payloadScale) {
        if ("float".equals(name)) {
            //return PayloadDecoder.FLOAT_DECODER;
            return new FloatDecoder(payloadScale);
        }
            throw new IllegalArgumentException("Invalid decoder: " + name);
    }

    public String fieldName() {
        return this.fieldName;
    }

    public Object value() {
        return this.value;
    }

    public String analyzerString() {
        return this.analyzerString;
    }

    public LatticeQueryBuilder analyzerString(String analyzerString) {
        this.analyzerString = analyzerString;
        return this;
    }

    public LatticeQueryBuilder payloadFuncString(String payloadFuncString) {
        this.payloadFuncString = payloadFuncString;
        return this;
    }

    public LatticeQueryBuilder payloadFunction(PayloadFunction f) {
        this.payloadFunction = f;
        return this;
    }

    public PayloadFunction payloadFunction() {
        return this.payloadFunction;
    }

    public PayloadDecoder payloadDecoder() {
       return this.payloadDecoder;
    }

    public LatticeQueryBuilder payloadDecoder(PayloadDecoder decoder) {
        this.payloadDecoder = decoder;
        return this;
    }

    public LatticeQueryBuilder payloadScale(float scale) {
        this.payloadScale = scale;
        return this;
    }

    public float payloadScale() {
        return payloadScale;
    }

    public boolean includeSpanScore() {
        return includeSpanScore;
    }

    public boolean inOrder() {
        return inOrder;
    }

    public int slop() {
        return slop;
    }

    public LatticeQueryBuilder slop(int slop) {
        this.slop = slop;
        return this;
    }

    public LatticeQueryBuilder payloadDecoderString(String payloadDecoderString) {
        this.payloadDecoderString = payloadDecoderString;
        return this;
    }

    public LatticeQueryBuilder includeSpanScore(boolean includeSpanScore) {
        this.includeSpanScore = includeSpanScore;
        return this;
    }

    public LatticeQueryBuilder inOrder(boolean inOrder) {
        this.inOrder = inOrder;
        return this;
    }

    public LatticeQueryBuilder zeroTermsQuery(MatchQuery.ZeroTermsQuery zeroTermsQuery) {
        this.zeroTermsQuery = zeroTermsQuery;
        return this;
    }

    public MatchQuery.ZeroTermsQuery zeroTermsQuery() {
        return this.zeroTermsQuery;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(fieldName);
        out.writeGenericValue(value);
        out.writeVInt(slop);
        out.writeBoolean(inOrder);
        out.writeBoolean(includeSpanScore);
        out.writeString(payloadFuncString);
        out.writeString(payloadDecoderString);

        out.writeOptionalString(analyzerString);

    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.startObject(fieldName);
        builder.field(MatchQueryBuilder.QUERY_FIELD.getPreferredName(), value);
        if (analyzerString != null) {
            builder.field(MatchQueryBuilder.ANALYZER_FIELD.getPreferredName(), analyzerString);
        }

        builder.field(SLOP_FIELD.getPreferredName(), slop);
        builder.field(MatchPhraseQueryBuilder.ZERO_TERMS_QUERY_FIELD.getPreferredName(), zeroTermsQuery.toString());
        builder.field(IN_ORDER_FIELD.getPreferredName(), inOrder);
        builder.field(INCLUDE_SPAN_SCORE_FIELD.getPreferredName(), includeSpanScore);
        builder.field(PAYLOAD_FUNCTION_FIELD.getPreferredName(), payloadFuncString);
        builder.field(PAYLOAD_DECODER_FIELD.getPreferredName(), payloadDecoderString);
        builder.field(PAYLOAD_SCALE_FIELD.getPreferredName(), payloadScale);
        printBoostAndQueryName(builder);
        builder.endObject();
        builder.endObject();
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        if (analyzerString != null && context.getIndexAnalyzers().get(analyzerString) == null) {
            throw new QueryShardException(context, "[" + NAME + "] analyzerString [" + analyzerString + "] not found");
        }

        Analyzer analyzer = null;
        if (analyzerString != null) {
            analyzer = context.getMapperService().getIndexAnalyzers().get(analyzerString);
            if (analyzer == null) {
                throw new IllegalArgumentException("No analyzer found for [" + analyzerString + "]");
            }
        }

        final MappedFieldType fieldType = context.fieldMapper(fieldName);
        if (fieldType == null) {
            return newUnmappedFieldQuery(fieldName);
        }

        if (analyzer == null) {
            analyzer = context.getSearchQuoteAnalyzer(fieldType);
        }
        assert analyzer != null;

        if (analyzer == Lucene.KEYWORD_ANALYZER) {
            final Term term = new Term(fieldName, value.toString());
            return fieldType.termQuery(term.bytes(), context);
        }

        SpanNearQuery.Builder builder =  new SpanNearQuery.Builder(fieldName, inOrder);
        builder.setSlop(slop);

        List<SpanTermQuery> termQueries = new ArrayList<>();

        TokenStream source = analyzer.tokenStream(fieldName, value.toString());
        try (CachingTokenFilter stream = new CachingTokenFilter(source)) {
            TermToBytesRefAttribute termAtt = stream.getAttribute(TermToBytesRefAttribute.class);
            PositionIncrementAttribute posIncAtt = stream.addAttribute(PositionIncrementAttribute.class);

            if (termAtt.getBytesRef() == null) {
                throw new IllegalArgumentException("Null term while building query");
            }
            stream.reset();
            while (stream.incrementToken()) {
                int posInc = posIncAtt.getPositionIncrement();
                if (posInc == 0) {
                    throw new IllegalArgumentException("graph queries are not supported");
                }
                termQueries.add(new SpanTermQuery(new Term(fieldName, termAtt.getBytesRef())));
            }
        }

        if (termQueries.size() == 1) {
           return new PayloadScoreQuery(
                   termQueries.get(0),
                   this.payloadFunction,
                   this.payloadDecoder,
                   this.includeSpanScore);
        }

        for (SpanTermQuery tq : termQueries) {
            builder.addClause(tq);
        }
        final SpanQuery spanQuery = builder.build();
        return new PayloadScoreQuery(spanQuery, this.payloadFunction, this.payloadDecoder, this.includeSpanScore);
    }

    @Override
    protected boolean doEquals(LatticeQueryBuilder other) {
        return Objects.equals(fieldName, other.fieldName)
                && Objects.equals(value, other.value)
                && Objects.equals(analyzerString, other.analyzerString)
                && Objects.equals(slop, other.slop)
                && Objects.equals(zeroTermsQuery, other.zeroTermsQuery)
                && Objects.equals(inOrder, other.inOrder)
                && Objects.equals(includeSpanScore, other.includeSpanScore)
                && Objects.equals(payloadDecoder, other.payloadDecoder)
                && Objects.equals(payloadFunction, other.payloadFunction)
                && Objects.equals(payloadScale, other.payloadScale);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(fieldName, analyzerString, value, slop, includeSpanScore, inOrder, payloadDecoder, payloadFunction, payloadScale);
    }

    public static LatticeQueryBuilder fromXContent(XContentParser parser) throws IOException {
        // Largely copied from MatchPhraseQueryBuilder

        MatchQuery.ZeroTermsQuery zeroTermsQuery = MatchQuery.DEFAULT_ZERO_TERMS_QUERY;
        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        int slop = SpanNearQueryBuilder.DEFAULT_SLOP;
        boolean inOrder = SpanNearQueryBuilder.DEFAULT_IN_ORDER;
        boolean includeSpanScore = true;
        String payloadFunc = "sum";
        String payloadDecoder = "float";
        float payloadScale = 1.0f;
        String fieldName = null;
        Object value = null;
        String queryName = null;
        String currentFieldName = null;
        String analyzer = null;

        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, currentFieldName);
                fieldName = currentFieldName;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (MatchQueryBuilder.QUERY_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            value = parser.objectText();
                        } else if (MatchQueryBuilder.ANALYZER_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            analyzer = parser.text();
                        } else if (AbstractQueryBuilder.BOOST_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            boost = parser.floatValue();
                        } else if (SLOP_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            slop = parser.intValue();
                        } else if (AbstractQueryBuilder.NAME_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            queryName = parser.text();
                        } else if (IN_ORDER_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            inOrder = parser.booleanValue();
                        } else if (INCLUDE_SPAN_SCORE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            includeSpanScore = parser.booleanValue();
                        } else if (PAYLOAD_FUNCTION_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            payloadFunc = parser.text();
                        } else if (PAYLOAD_DECODER_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            payloadDecoder = parser.text();
                        } else if (PAYLOAD_SCALE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            payloadScale = parser.floatValue();
                        } else if (MatchPhraseQueryBuilder.ZERO_TERMS_QUERY_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            String zeroTermsValue = parser.text();
                            if ("none".equalsIgnoreCase(zeroTermsValue)) {
                                zeroTermsQuery = MatchQuery.ZeroTermsQuery.NONE;
                            } else if ("all".equalsIgnoreCase(zeroTermsValue)) {
                                zeroTermsQuery = MatchQuery.ZeroTermsQuery.ALL;
                            } else {
                                throw new ParsingException(parser.getTokenLocation(),
                                        "Unsupported zero_terms_query value [" + zeroTermsValue + "]");
                            }
                        } else {
                            throw new ParsingException(parser.getTokenLocation(),
                                    "[" + NAME + "] query does not support [" + currentFieldName + "]");
                        }
                    } else {
                        throw new ParsingException(parser.getTokenLocation(),
                                "[" + NAME + "] unknown token [" + token + "] after [" + currentFieldName + "]");
                    }
                }
            } else {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, parser.currentName());
                fieldName = parser.currentName();
                value = parser.objectText();
            }
        }

        LatticeQueryBuilder builder = new LatticeQueryBuilder(fieldName, value);
        builder.slop(slop);
        builder.inOrder(inOrder);
        builder.zeroTermsQuery(zeroTermsQuery);
        builder.boost(boost);
        builder.analyzerString(analyzer);
        builder.queryName(queryName);
        builder.includeSpanScore(includeSpanScore);
        builder.payloadFunction(parsePayloadFuncString(payloadFunc));
        builder.payloadDecoder(parseDecoder(payloadDecoder, payloadScale));

        return builder;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}

