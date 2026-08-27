/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.multimodal.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.query.QueryProcessor;
import org.codelibs.fess.query.parser.QueryParser;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * A {@link StructuredQuerySplitter} wired to core's <em>real</em> {@link QueryParser} and a
 * recording stub in place of core's {@link QueryProcessor}.
 *
 * <p>The parser is the real one because the parse tree is exactly what the splitter reasons
 * about: stubbing it would test nothing. The processor is stubbed because standing up core's
 * real {@code QueryCommand}s outside a Lasta Di container means stubbing most of
 * {@code FessConfig} -- {@code FessConfig.SimpleImpl} throws on the decimal boost getters and
 * returns null index field names, both of which core's commands dereference. What the stub does
 * pin is the split itself: which Lucene sub-query is handed to core for conversion, and where
 * each converted clause lands in the filter. How core then converts it is core's own contract,
 * covered by core's tests.</p>
 */
public class StubProcessorSplitter extends StructuredQuerySplitter {

    /** Every Lucene sub-query handed to the processor, in the order it was handed over. */
    public final List<String> converted = new ArrayList<>();

    /** When true the stub returns null, as core does for a clause it cannot express as a query. */
    public boolean convertToNull;

    /** When true the stub throws, as core does for a clause it rejects outright. */
    public boolean convertThrows;

    private final QueryParser queryParser = new QueryParser();

    private final QueryProcessor queryProcessor = new QueryProcessor() {
        @Override
        public QueryBuilder execute(final QueryContext context, final Query query, final float boost) {
            converted.add(query.toString());
            if (convertThrows) {
                throw new IllegalStateException("stubbed conversion failure");
            }
            if (convertToNull) {
                return null;
            }
            return QueryBuilders.termQuery("converted", query.toString());
        }
    };

    /**
     * Constructs a splitter with a real parser and a recording stub processor.
     */
    public StubProcessorSplitter() {
        queryParser.init();
    }

    @Override
    protected QueryParser getQueryParser() {
        return queryParser;
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }
}
