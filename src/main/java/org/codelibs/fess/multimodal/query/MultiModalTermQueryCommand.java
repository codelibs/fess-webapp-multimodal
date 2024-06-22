/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
import org.apache.lucene.search.TermQuery;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.multimodal.rank.fusion.MultiModalSearcher;
import org.codelibs.fess.multimodal.rank.fusion.MultiModalSearcher.SearchContext;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.query.TermQueryCommand;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.index.query.QueryBuilder;

public class MultiModalTermQueryCommand extends TermQueryCommand {

    private static final Logger logger = LogManager.getLogger(MultiModalTermQueryCommand.class);

    @Override
    protected QueryBuilder convertDefaultTermQuery(final FessConfig fessConfig, final QueryContext context, final TermQuery termQuery,
            final float boost, final String field, final String text) {
        final SearchContext searchContext = getSearchContext();

        if (!DEFAULT_FIELD.equals(field) || searchContext == null) {
            return super.convertDefaultTermQuery(fessConfig, context, termQuery, boost, field, text);
        }

        final QueryBuilder queryBuilder =
                new MultiModalQueryBuilder.Builder().query(text).minScore(searchContext.getParams().getMinScore()).build().toQueryBuilder();
        context.addFieldLog(field, text);
        context.addHighlightedQuery(text);
        if (logger.isDebugEnabled()) {
            logger.debug("KNNQueryBuilder: {}", queryBuilder);
        }
        return queryBuilder;
    }

    protected SearchContext getSearchContext() {
        final MultiModalSearcher searcher = ComponentUtil.getComponent(SEARCHER);
        return searcher.getContext();
    }
}
