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

import static org.codelibs.fess.Constants.DEFAULT_FIELD;
import static org.codelibs.fess.multimodal.MultiModalConstants.SEARCHER;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.PhraseQuery;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.multimodal.rank.fusion.MultiModalSearcher;
import org.codelibs.fess.multimodal.rank.fusion.MultiModalSearcher.SearchContext;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.query.PhraseQueryCommand;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.index.query.QueryBuilder;

/**
 * Query command that extends PhraseQueryCommand to handle multimodal phrase queries.
 * Converts phrase queries into vector similarity searches when appropriate.
 */
public class MultiModalPhraseQueryCommand extends PhraseQueryCommand {

    private static final Logger logger = LogManager.getLogger(MultiModalPhraseQueryCommand.class);

    /**
     * Constructs a new MultiModalPhraseQueryCommand instance.
     */
    public MultiModalPhraseQueryCommand() {
        // Default constructor
    }

    @Override
    protected QueryBuilder convertPhraseQuery(final FessConfig fessConfig, final QueryContext context, final PhraseQuery phraseQuery,
            final float boost, final String field, final String[] texts) {
        final SearchContext searchContext = getSearchContext();

        if (!DEFAULT_FIELD.equals(field) || searchContext == null) {
            return super.convertPhraseQuery(fessConfig, context, phraseQuery, boost, field, texts);
        }

        final String text = String.join(" ", texts);
        final SearchRequestParams params = searchContext.getParams();
        final QueryBuilder queryBuilder = new MultiModalQueryBuilder.Builder().field(searchContext.getVectorField()).query(text)
                .k(params.getPageSize()).build().toQueryBuilder();
        context.addFieldLog(field, text);
        context.addHighlightedQuery(text);
        if (logger.isDebugEnabled()) {
            logger.debug("KNNQueryBuilder: {}", queryBuilder);
        }
        return queryBuilder;
    }

    /**
     * Retrieves the current search context from the multimodal searcher.
     *
     * @return the search context, or null if not available
     */
    protected SearchContext getSearchContext() {
        final MultiModalSearcher searcher = ComponentUtil.getComponent(SEARCHER);
        return searcher.getContext();
    }
}
