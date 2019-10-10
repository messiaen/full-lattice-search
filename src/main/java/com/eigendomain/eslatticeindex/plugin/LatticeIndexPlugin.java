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

package com.eigendomain.eslatticeindex.plugin;


import com.eigendomain.eslatticeindex.index.LatticeTokenFilterFactory;
import com.eigendomain.eslatticeindex.index.MyWhiteSpaceTokenizerFactory;
import com.eigendomain.eslatticeindex.index.query.LatticeQueryBuilder;
import com.eigendomain.eslatticeindex.mapper.LatticeFieldMapper;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static java.util.Collections.singletonList;


public class LatticeIndexPlugin extends Plugin implements AnalysisPlugin, SearchPlugin, MapperPlugin {
    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return new HashMap<String, AnalysisProvider<TokenFilterFactory>>(){{
            put("lattice", LatticeTokenFilterFactory::new);
        }};
    }

    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return Collections.singletonMap("my_whitespace", MyWhiteSpaceTokenizerFactory::new);
    }

    @Override
    public List<QuerySpec<?>> getQueries() {
        return singletonList(
                new QuerySpec<>(
                        "match_lattice",
                        LatticeQueryBuilder::new,
                        LatticeQueryBuilder::fromXContent)
        );
    }

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        return Collections.singletonMap(LatticeFieldMapper.CONTENT_TYPE, new LatticeFieldMapper.TypeParser());
    }
}
