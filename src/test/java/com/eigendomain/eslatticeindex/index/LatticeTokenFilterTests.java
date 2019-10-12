/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eigendomain.eslatticeindex.index;

import com.eigendomain.eslatticeindex.plugin.LatticeIndexPlugin;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.AnalysisTestsHelper;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase.TestAnalysis;
import org.elasticsearch.test.ESTokenStreamTestCase;

import java.io.IOException;
import java.io.StringReader;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class LatticeTokenFilterTests extends ESTokenStreamTestCase {
    public void testDefaultLatticeTokenFilter() throws IOException {
        TestAnalysis analysis = createAnalyzer(Settings.EMPTY);

        TokenFilterFactory tokenFilterFactory = analysis.tokenFilter.get("lattice");
        assertThat(tokenFilterFactory, instanceOf(LatticeTokenFilterFactory.class));
    }

    public void testLatticeTokenFilterWithoutFields() throws IOException {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "lattice")
                .build();
        TestAnalysis analysis = createAnalyzer(settings);
        TokenFilterFactory tokenFilter = analysis.tokenFilter.get("lattice");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("the quick brown fox"));
        try (TokenStream in = tokenFilter.create(tokenizer)) {
            assertTokenStreamContents(in, new String[]{"the", "quick", "brown", "fox"});
        }
    }

    public void testLatticeTokenFilterWithBaseFields() throws IOException {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "lattice")
                .build();
        TestAnalysis analysis = createAnalyzer(settings);
        TokenFilterFactory tokenFilter = analysis.tokenFilter.get("lattice");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("the|0|1|0.3 quick|1|0|0.9 brick|1|1|0.01 brown|2|0|1.0 fox|3|0|0.7 box|3|1|0.2"));
        try (TokenStream in = tokenFilter.create(tokenizer)) {
            String[] tokens = new String[]{"the", "quick", "brick", "brown" ,"fox", "box"};
            int[] posIncs = new int[]{1, 1, 0, 1, 1, 0};
            byte[] encode03 = PayloadHelper.encodeFloat(0.3f);
            byte[] encode09 = PayloadHelper.encodeFloat(0.9f);
            byte[] encode001 = PayloadHelper.encodeFloat(0.01f);
            byte[] encode10 = PayloadHelper.encodeFloat(1.0f);
            byte[] encode07 = PayloadHelper.encodeFloat(0.7f);
            byte[] encode02 = PayloadHelper.encodeFloat(0.2f);
            byte[][] payloads = new byte[][]{
                    encode03,
                    encode09, encode001,
                    encode10,
                    encode07, encode02
            };
            assertTokenStreamContents(
                    in,
                    tokens,
                    null,
                    null,
                    null,
                    posIncs,
                    null,
                    null,
                    null,
                    null,
                    true,
                    payloads
            );
        }
    }

    public void testLatticeTokenFilterWithBaseFieldsAndDups() throws IOException {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "lattice")
                .put("index.analysis.filter.my_filter.score_buckets", "0.9, 5, 0.5, 3, 0.2, 2")
                .build();
        TestAnalysis analysis = createAnalyzer(settings);
        TokenFilterFactory tokenFilter = analysis.tokenFilter.get("my_filter");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("the|0|1|0.3 quick|1|0|0.9 brick|1|1|0.01 brown|2|0|1.0 fox|3|0|0.7 box|3|1|0.2"));
        try (TokenStream in = tokenFilter.create(tokenizer)) {
            String[] tokens = new String[]{"the", "the",
                    "quick", "quick", "quick", "quick", "quick", "brick",
                    "brown", "brown", "brown", "brown", "brown",
                    "fox", "fox", "fox", "box", "box"};
            int[] posIncs = new int[]{1, 0,
                    1, 0, 0, 0, 0, 0,
                    1, 0, 0, 0, 0,
                    1, 0, 0, 0, 0};
            byte[] encode03 = PayloadHelper.encodeFloat(0.3f);
            byte[] encode09 = PayloadHelper.encodeFloat(0.9f);
            byte[] encode001 = PayloadHelper.encodeFloat(0.01f);
            byte[] encode10 = PayloadHelper.encodeFloat(1.0f);
            byte[] encode07 = PayloadHelper.encodeFloat(0.7f);
            byte[] encode02 = PayloadHelper.encodeFloat(0.2f);
            byte[][] payloads = new byte[][]{
                    encode03, encode03,
                    encode09, encode09, encode09, encode09, encode09, encode001,
                    encode10, encode10, encode10, encode10, encode10,
                    encode07, encode07, encode07, encode02, encode02
            };
            assertTokenStreamContents(
                    in,
                    tokens,
                    null,
                    null,
                    null,
                    posIncs,
                    null,
                    null,
                    null,
                    null,
                    true,
                    payloads
            );
        }
    }

    public void testLatticeTokenFilterWithAudioFieldsAndDups() throws IOException {
        Settings settings = Settings.builder()
                .put("index.analysis.filter.my_filter.type", "lattice")
                .put("index.analysis.filter.my_filter.score_buckets", "0.9, 5, 0.5, 3, 0.2, 2")
                .put("index.analysis.filter.my_filter.lattice_format", "audio")
                .put("index.analysis.filter.my_filter.audio_position_increment_seconds", "0.1")
                .build();
        TestAnalysis analysis = createAnalyzer(settings);
        TokenFilterFactory tokenFilter = analysis.tokenFilter.get("my_filter");
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(
                "the|0|1|0.3|1.0|0 quick|1|0|0.9|2.0|0.0 brick|1|1|0.01|2.0|0.0 " +
                        "brown|2|0|1.0|3.5|0.0 fox|3|0|0.7|4.0|0.0 box|3|1|0.2|4.0|0.0"));
        try (TokenStream in = tokenFilter.create(tokenizer)) {
            String[] tokens = new String[]{"the", "the",
                    "quick", "quick", "quick", "quick", "quick", "brick",
                    "brown", "brown", "brown", "brown", "brown",
                    "fox", "fox", "fox", "box", "box"};
            int[] posIncs = new int[]{10, 0,
                    10, 0, 0, 0, 0, 0,
                    15, 0, 0, 0, 0,
                    5, 0, 0, 0, 0};
            byte[] encode03 = PayloadHelper.encodeFloat(0.3f);
            byte[] encode09 = PayloadHelper.encodeFloat(0.9f);
            byte[] encode001 = PayloadHelper.encodeFloat(0.01f);
            byte[] encode10 = PayloadHelper.encodeFloat(1.0f);
            byte[] encode07 = PayloadHelper.encodeFloat(0.7f);
            byte[] encode02 = PayloadHelper.encodeFloat(0.2f);
            byte[][] payloads = new byte[][]{
                    encode03, encode03,
                    encode09, encode09, encode09, encode09, encode09, encode001,
                    encode10, encode10, encode10, encode10, encode10,
                    encode07, encode07, encode07, encode02, encode02
            };
            assertTokenStreamContents(
                    in,
                    tokens,
                    null,
                    null,
                    null,
                    posIncs,
                    null,
                    null,
                    null,
                    null,
                    true,
                    payloads
            );
        }
    }

    private TestAnalysis createAnalyzer(Settings filterSettings) throws IOException {
        Settings settings = Settings.builder()
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir())
                .put(filterSettings)
                .build();
        return AnalysisTestsHelper.createTestAnalysisFromSettings(settings, new LatticeIndexPlugin());
    }
}
