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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.mylasta.action.FessUserBean;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalThing;
import org.junit.jupiter.api.Test;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class ClipChunkSearcherTest extends UnitWebappTestCase {

    /** The theme reads this name; core would derive "clip_chunk" from the class name. */
    @Test
    public void test_getName() {
        assertEquals("multi_modal", new ClipChunkSearcher().getName());
    }

    /** A query core would reject must be accepted once its conditions are splittable. */
    @Test
    public void test_isPlainQuery_acceptsSplittableConditions() {
        final ClipChunkSearcher searcher = new ClipChunkSearcher();
        assertTrue(searcher.isPlainQuery("mountain sunset"));
        assertTrue(searcher.isPlainQuery("mountain sunset filetype:jpeg"));
        assertTrue(searcher.isPlainQuery("cat (label:\"a\" OR label:\"b\")"));
    }

    @Test
    public void test_isPlainQuery_rejectsRealSyntax() {
        final ClipChunkSearcher searcher = new ClipChunkSearcher();
        assertFalse(searcher.isPlainQuery("\"mountain sunset\""));
        assertFalse(searcher.isPlainQuery("cat AND dog"));
        assertFalse(searcher.isPlainQuery("cat sort:filename.asc"));
        assertFalse(searcher.isPlainQuery("cat timestamp:[now/d-1d TO *]"));
    }

    @Test
    public void test_buildConditionFilter_singleValue() {
        final Map<String, List<String>> conditions = new HashMap<>();
        final List<String> values = new ArrayList<>();
        values.add("jpeg");
        conditions.put("filetype", values);

        final QueryBuilder filter = new ClipChunkSearcher().buildConditionFilter(conditions);
        assertNotNull(filter);
        assertTrue(filter instanceof BoolQueryBuilder);
        assertEquals(1, ((BoolQueryBuilder) filter).filter().size());
    }

    @Test
    public void test_buildConditionFilter_multiValueBecomesOneClause() {
        final Map<String, List<String>> conditions = new HashMap<>();
        final List<String> values = new ArrayList<>();
        values.add("a");
        values.add("b");
        conditions.put("label", values);

        final BoolQueryBuilder filter = (BoolQueryBuilder) new ClipChunkSearcher().buildConditionFilter(conditions);
        // One terms clause for the field, not one clause per value.
        assertEquals(1, filter.filter().size());
    }

    @Test
    public void test_buildConditionFilter_emptyReturnsNull() {
        assertNull(new ClipChunkSearcher().buildConditionFilter(new HashMap<>()));
    }

    // ---- Safety property 1: conditions must reach BOTH the ann (buildKnnChunkQuery) and the
    // exact (buildExactChunkQuery) query-building hooks. Testing this directly (rather than only
    // through isPlainQuery/buildConditionFilter) is what actually pins the "a user's exclusion
    // filter must not leak matching-but-excluded documents in either engine mode" contract. The
    // condition is fed in via the same protected ThreadLocal field the real search() populates
    // (see StructuredQuerySplitter-driven population in search()); reaching it directly here, in
    // the same package, avoids having to stand up the full container that super.search() needs
    // (embedding client manager, role/virtual-host helpers, a live search engine client) just to
    // drive these two query builders.

    @Test
    public void test_buildExactChunkQuery_withoutCondition_delegatesToSuper() {
        final ClipChunkSearcher searcher = new ClipChunkSearcher();
        final String json = searcher.buildExactChunkQuery(new float[] { 0.1f, 0.2f }).toString().replaceAll("\\s", "");
        // Unfiltered: core's own nested/script_score shape, no extra bool/filter wrapper.
        assertTrue(json.contains("script_score"));
        assertFalse(json.contains("\"filter\""));
    }

    @Test
    public void test_buildExactChunkQuery_appliesConditionFilter() {
        final ClipChunkSearcher searcher = new ClipChunkSearcher();
        final QueryBuilder condition = QueryBuilders.termsQuery("filetype", Collections.singletonList("jpeg"));
        searcher.conditionFilterHolder.set(condition);
        try {
            final QueryBuilder query = searcher.buildExactChunkQuery(new float[] { 0.1f, 0.2f });
            assertTrue(query instanceof BoolQueryBuilder);
            final BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
            // The vector scan stays a MUST clause (it drives scoring); the condition is a FILTER
            // clause (excludes non-matching documents without affecting score) -- exactly what a
            // real full-scan mode has to do to keep an excluded document out of the results.
            assertEquals(1, boolQuery.must().size());
            assertEquals(1, boolQuery.filter().size());
            assertTrue(boolQuery.filter().get(0) == condition);
            final String json = query.toString().replaceAll("\\s", "");
            assertTrue(json.contains("script_score"));
            assertTrue(json.contains("\"filetype\":[\"jpeg\"]"));
        } finally {
            searcher.conditionFilterHolder.remove();
        }
    }

    @Test
    public void test_buildKnnChunkQuery_withoutCondition_delegatesToSuper() {
        installMinimalFessConfigStub();
        try {
            final ClipChunkSearcher searcher = new ClipChunkSearcher();
            final QueryBuilder permissionFilter = QueryBuilders.termQuery("role", "guest");
            final String json =
                    searcher.buildKnnChunkQuery(new float[] { 0.1f, 0.2f }, new StubSearchRequestParams(0, 10), permissionFilter)
                            .toString()
                            .replaceAll("\\s", "");
            assertTrue(json.contains("\"knn\""));
            assertTrue(json.contains("\"role\":{\"value\":\"guest\""));
            assertFalse(json.contains("filetype"));
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_buildKnnChunkQuery_appliesConditionFilter() {
        installMinimalFessConfigStub();
        try {
            final ClipChunkSearcher searcher = new ClipChunkSearcher();
            final QueryBuilder permissionFilter = QueryBuilders.termQuery("role", "guest");
            final QueryBuilder condition = QueryBuilders.termsQuery("filetype", Collections.singletonList("jpeg"));
            searcher.conditionFilterHolder.set(condition);
            try {
                final String json =
                        searcher.buildKnnChunkQuery(new float[] { 0.1f, 0.2f }, new StubSearchRequestParams(0, 10), permissionFilter)
                                .toString()
                                .replaceAll("\\s", "");
                assertTrue(json.contains("\"knn\""));
                // Both the caller's permission filter and the recovered condition must survive
                // inside the knn query's own "filter" -- that is the efficient-filtering path, so
                // ANN does not spend its k on documents that are about to be discarded.
                assertTrue(json.contains("\"role\":{\"value\":\"guest\""));
                assertTrue(json.contains("\"filetype\":[\"jpeg\"]"));
            } finally {
                searcher.conditionFilterHolder.remove();
            }
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_buildKnnChunkQuery_appliesConditionFilter_withoutPermissionFilter() {
        installMinimalFessConfigStub();
        try {
            final ClipChunkSearcher searcher = new ClipChunkSearcher();
            final QueryBuilder condition = QueryBuilders.termsQuery("filetype", Collections.singletonList("jpeg"));
            searcher.conditionFilterHolder.set(condition);
            try {
                final String json = searcher.buildKnnChunkQuery(new float[] { 0.1f, 0.2f }, new StubSearchRequestParams(0, 10), null)
                        .toString()
                        .replaceAll("\\s", "");
                assertTrue(json.contains("\"knn\""));
                assertTrue(json.contains("\"filetype\":[\"jpeg\"]"));
            } finally {
                searcher.conditionFilterHolder.remove();
            }
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_buildKnnChunkQuery_appliesConditionFilter_toOuterBoolAsWell() {
        // C1 fix: core places its permission filter in TWO places -- the outer bool `filter`
        // (the actual enforcement boundary) and the knn query's own filter (a recall aid, per
        // SemanticChunkSearcher#createSearchCondition's own comment: "the copy handed to the knn
        // query below is a recall aid, not the security boundary"). Prior to this fix,
        // buildKnnChunkQuery only put the recovered condition inside the knn query's filter,
        // which the earlier substring-only JSON assertions above could not tell apart from the
        // outer-bool placement. This test inspects the returned QueryBuilder's structure
        // directly so it fails if the outer filter clause goes missing again.
        installMinimalFessConfigStub();
        try {
            final ClipChunkSearcher searcher = new ClipChunkSearcher();
            final QueryBuilder condition = QueryBuilders.termsQuery("filetype", Collections.singletonList("jpeg"));
            searcher.conditionFilterHolder.set(condition);
            try {
                final QueryBuilder query =
                        searcher.buildKnnChunkQuery(new float[] { 0.1f, 0.2f }, new StubSearchRequestParams(0, 10), null);
                assertTrue(query instanceof BoolQueryBuilder);
                final BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
                // The knn (nested) query is the sole MUST clause (it drives scoring); the
                // condition is also a FILTER clause on the outer bool, which is what actually
                // enforces it regardless of the knn engine's filter semantics.
                assertEquals(1, boolQuery.must().size());
                assertEquals(1, boolQuery.filter().size());
                assertTrue(boolQuery.filter().get(0) == condition);
            } finally {
                searcher.conditionFilterHolder.remove();
            }
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    // ---- Safety property 3: only the free text must reach the embedding step. `search()` cannot
    // be driven end-to-end without a live Lasta Di container (core's isSearchEnabled() resolves
    // ChunkVectorHelper from ComponentUtil.getComponent, which throws IllegalStateException with
    // no container initialized -- confirmed by running it, not assumed). Instead this drives
    // ClipChunkSearcher.search() through a test-only subclass that overrides isSearchEnabled()
    // (bypassing the ChunkVectorHelper/container lookup) and isPlainQuery(String) (recording
    // exactly what core's own gate receives, then returning false so SemanticChunkSearcher.search
    // short-circuits into emptyResult() before touching the embedding client manager or issuing
    // any OpenSearch call). Both are the same protected extension points core already documents as
    // overridable; no production code changes.

    @Test
    public void test_search_embedsOnlyFreeText_stripsConditions() {
        final RecordingSearcher searcher = new RecordingSearcher();
        searcher.search("cat filetype:jpeg", new StubSearchRequestParams(0, 10), OptionalThing.empty());
        assertEquals("cat", searcher.recordedQuery);
    }

    @Test
    public void test_search_unsplittableQuery_passesOriginalQueryThrough() {
        final RecordingSearcher searcher = new RecordingSearcher();
        searcher.search("cat AND dog", new StubSearchRequestParams(0, 10), OptionalThing.empty());
        assertEquals("cat AND dog", searcher.recordedQuery);
    }

    // C2 fix: non-empty conditions that fail to become a filter must fall back to the
    // *original* query, never split.text (which would search unfiltered and silently drop the
    // conditions). Unreachable via the real StructuredQuerySplitter (see the invariant note on
    // its `conditions` map), so this drives it through a searcher whose buildConditionFilter is
    // overridden to simulate the failure.
    @Test
    public void test_search_nonEmptyConditionsWithNoFilter_fallsBackToOriginalQuery() {
        // Declared as RecordingSearcher (not the subtype) so recordedQuery -- private to
        // RecordingSearcher and not inherited by the subclass per JLS 8.2 -- resolves normally.
        final RecordingSearcher searcher = new NullConditionFilterRecordingSearcher();
        searcher.search("cat filetype:jpeg", new StubSearchRequestParams(0, 10), OptionalThing.empty());
        // The ORIGINAL query, not split.text ("cat"): core's own isPlainQuery then rejects it and
        // skips the vector branch entirely, rather than searching "cat" with the condition lost.
        assertEquals("cat filetype:jpeg", searcher.recordedQuery);
    }

    // C3: conditionFilterHolder must never leak into a later search on the same (pooled) thread.
    // Both branches of search() that can populate it are covered: the conditioned-query path
    // (which sets it, then must clear it in `finally`) and the plain-query path (which must
    // never touch it in the first place, per the C2 short-circuit above).
    @Test
    public void test_search_clearsConditionFilterHolder_afterConditionedQuery() {
        final RecordingSearcher searcher = new RecordingSearcher();
        searcher.search("cat filetype:jpeg", new StubSearchRequestParams(0, 10), OptionalThing.empty());
        assertNull(searcher.conditionFilterHolder.get());
    }

    @Test
    public void test_search_conditionFilterHolder_staysNullForPlainQuery() {
        final RecordingSearcher searcher = new RecordingSearcher();
        searcher.search("cat", new StubSearchRequestParams(0, 10), OptionalThing.empty());
        assertNull(searcher.conditionFilterHolder.get());
    }

    /**
     * Records the exact string {@link ClipChunkSearcher#search} forwards to
     * {@code super.search(...)}, without needing a live container.
     */
    private static class RecordingSearcher extends ClipChunkSearcher {
        private String recordedQuery;

        @Override
        protected boolean isSearchEnabled() {
            // Core's real implementation resolves ChunkVectorHelper from ComponentUtil, which
            // needs a live Lasta Di container this plain unit test does not stand up. Only
            // whether the feature is enabled is being bypassed here -- what gets recorded below is
            // still core's own isPlainQuery gate, fed whatever ClipChunkSearcher.search() decided
            // to forward.
            return true;
        }

        @Override
        protected boolean isPlainQuery(final String query) {
            recordedQuery = query;
            // false short-circuits SemanticChunkSearcher.search() into emptyResult() immediately
            // after this call, before params/userBean are ever dereferenced.
            return false;
        }
    }

    /**
     * Simulates a {@link ClipChunkSearcher#buildConditionFilter} that fails to turn non-empty
     * conditions into a filter -- unreachable via the real {@code StructuredQuerySplitter}, but
     * this pins {@link ClipChunkSearcher#search}'s fail-closed fallback for that seam (C2).
     */
    private static class NullConditionFilterRecordingSearcher extends RecordingSearcher {
        @Override
        protected QueryBuilder buildConditionFilter(final Map<String, List<String>> conditions) {
            return null;
        }
    }

    /**
     * Installs a FessConfig stub that answers every system-property read with the caller's
     * default rather than resolving {@code ComponentUtil.getSystemProperties()} (which needs a
     * live Lasta Di container this plain unit test does not stand up). Also stubs the two proxy
     * getters: {@code FessConfig.SimpleImpl}'s real implementations throw NPE outside a
     * container, and {@link ClipChunkSearcher#buildKnnChunkQuery} reaches this stub transitively
     * through core's {@code getKnnK()}/{@code getKnnEfSearch()}.
     */
    private void installMinimalFessConfigStub() {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                return defaultValue;
            }

            @Override
            public String getHttpProxyHost() {
                return null;
            }

            @Override
            public Integer getHttpProxyPortAsInteger() {
                return null;
            }
        });
    }

    /** Minimal {@link SearchRequestParams} stub -- only start/page size matter to buildKnnChunkQuery. */
    private static class StubSearchRequestParams extends SearchRequestParams {
        private final int start;
        private final int size;

        StubSearchRequestParams(final int start, final int size) {
            this.start = start;
            this.size = size;
        }

        @Override
        public String getQuery() {
            return null;
        }

        @Override
        public Map<String, String[]> getFields() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String[]> getConditions() {
            return Collections.emptyMap();
        }

        @Override
        public String[] getLanguages() {
            return new String[0];
        }

        @Override
        public org.codelibs.fess.entity.GeoInfo getGeoInfo() {
            return null;
        }

        @Override
        public org.codelibs.fess.entity.FacetInfo getFacetInfo() {
            return null;
        }

        @Override
        public org.codelibs.fess.entity.HighlightInfo getHighlightInfo() {
            return null;
        }

        @Override
        public String getSort() {
            return null;
        }

        @Override
        public int getStartPosition() {
            return start;
        }

        @Override
        public int getPageSize() {
            return size;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public String[] getExtraQueries() {
            return new String[0];
        }

        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        @Override
        public Locale getLocale() {
            return Locale.ROOT;
        }

        @Override
        public SearchRequestType getType() {
            return SearchRequestType.SEARCH;
        }

        @Override
        public String getSimilarDocHash() {
            return null;
        }
    }
}
