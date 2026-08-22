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
package org.codelibs.fess.multimodal.rank.fusion;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.multimodal.query.StructuredQuerySplitter;
import org.codelibs.fess.multimodal.query.StructuredQuerySplitter.Split;
import org.codelibs.fess.mylasta.action.FessUserBean;
import org.codelibs.fess.rank.fusion.SearchResult;
import org.codelibs.fess.rank.fusion.SemanticChunkSearcher;
import org.dbflute.optional.OptionalThing;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Multimodal (CLIP) variant of Fess core's semantic chunk searcher.
 *
 * <p>Core refuses any query containing search syntax, and a facet or label selection is
 * folded into the query string as {@code label:"x"} -- so on a faceted UI the vector branch
 * silently disappears the moment a filter is applied. Core is right to refuse rather than
 * return unfiltered hits: its semantic branch builds only a permission filter and a kNN
 * query, so it cannot honour those conditions.</p>
 *
 * <p>This subclass recovers both halves instead: the free text is embedded on its own, and
 * the conditions become real filters, applied in the same two places core applies the
 * permission filter -- the outer bool (which is what enforces them, and what the exact-kNN
 * mode relies on) and the kNN query's own filter (efficient filtering, so ANN does not spend
 * its k on documents that are about to be discarded).</p>
 */
public class ClipChunkSearcher extends SemanticChunkSearcher {

    /** Searcher name exposed in the `searcher` provenance field. */
    protected static final String SEARCHER_NAME = "multi_modal";

    /** Conditions recovered from the current request's query string. */
    protected final ThreadLocal<QueryBuilder> conditionFilterHolder = new ThreadLocal<>();

    /**
     * Constructs a new ClipChunkSearcher instance.
     */
    public ClipChunkSearcher() {
        // Default constructor
    }

    @Override
    public String getName() {
        // Without this the base derives "clip_chunk" from the class name, which would break
        // every consumer of the `searcher` provenance field (the mosaic theme's badges).
        return SEARCHER_NAME;
    }

    @Override
    protected boolean isPlainQuery(final String query) {
        return StructuredQuerySplitter.split(query) != null;
    }

    @Override
    protected SearchResult search(final String query, final SearchRequestParams params, final OptionalThing<FessUserBean> userBean) {
        final Split split = StructuredQuerySplitter.split(query);
        if (split == null) {
            // Not splittable: let the base apply its own gate, which will skip the branch.
            return super.search(query, params, userBean);
        }
        if (split.conditions.isEmpty()) {
            return super.search(split.text, params, userBean);
        }
        final QueryBuilder conditionFilter = buildConditionFilter(split.conditions);
        if (conditionFilter == null) {
            // Non-empty conditions that failed to become a filter must never be silently dropped:
            // searching split.text without them would let documents the user meant to exclude
            // leak through the vector branch. Falling back to the original (unsplit) query
            // instead routes it through core's own isPlainQuery gate, which then skips the
            // vector branch entirely -- the safe side of this failure. Unreachable today
            // (StructuredQuerySplitter never returns a non-empty conditions map that
            // buildConditionFilter can't turn into a filter -- see the note there), but the two
            // are independently testable classes, so this guards the seam between them.
            return super.search(query, params, userBean);
        }
        conditionFilterHolder.set(conditionFilter);
        try {
            // Only the free text is passed on: the base embeds whatever string it receives,
            // and `label:"x"` in the embedded text is noise to the CLIP text encoder.
            return super.search(split.text, params, userBean);
        } finally {
            conditionFilterHolder.remove();
        }
    }

    @Override
    protected QueryBuilder buildKnnChunkQuery(final float[] queryVector, final SearchRequestParams params, final QueryBuilder filter) {
        final QueryBuilder conditionFilter = conditionFilterHolder.get();
        if (conditionFilter == null) {
            return super.buildKnnChunkQuery(queryVector, params, filter);
        }
        // Merged into the kNN query's own filter too, so ANN does not spend its k on documents
        // that are about to be discarded (efficient filtering) -- but per core's own comment on
        // the identical tradeoff for its permission filter (SemanticChunkSearcher#createSearchCondition:
        // "the copy handed to the knn query below is a recall aid, not the security boundary"),
        // that copy alone is not the enforcement boundary. The outer bool filter below is what
        // actually enforces the condition, matching the exact-mode path in buildExactChunkQuery
        // and the two places core itself applies its permission filter.
        final BoolQueryBuilder merged = QueryBuilders.boolQuery();
        if (filter != null) {
            merged.filter(filter);
        }
        merged.filter(conditionFilter);
        return QueryBuilders.boolQuery().must(super.buildKnnChunkQuery(queryVector, params, merged)).filter(conditionFilter);
    }

    @Override
    protected QueryBuilder buildExactChunkQuery(final float[] queryVector) {
        final QueryBuilder conditionFilter = conditionFilterHolder.get();
        final QueryBuilder chunkQuery = super.buildExactChunkQuery(queryVector);
        if (conditionFilter == null) {
            return chunkQuery;
        }
        // The exact (full-scan) path takes no per-query filter, so the conditions have to
        // wrap it here. Both modes therefore constrain on the same clause set.
        return QueryBuilders.boolQuery().must(chunkQuery).filter(conditionFilter);
    }

    /**
     * Turns the recovered conditions into a filter clause.
     *
     * @param conditions field name to values
     * @return the filter, or {@code null} when there is nothing to filter on
     */
    protected QueryBuilder buildConditionFilter(final Map<String, List<String>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for (final Map.Entry<String, List<String>> entry : conditions.entrySet()) {
            final List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            // One terms clause per field: values within a field are OR, fields are AND --
            // the same semantics QueryStringBuilder gives them on the keyword branch.
            boolQuery.filter(QueryBuilders.termsQuery(entry.getKey(), values));
        }
        return boolQuery.hasClauses() ? boolQuery : null;
    }
}
