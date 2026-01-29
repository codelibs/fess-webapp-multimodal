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

import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.junit.jupiter.api.TestInfo;

public class MultiModalQueryBuilderTest extends UnitWebappTestCase {

    private static final String TEST_FIELD = "test_vector_field";
    private static final String TEST_QUERY = "test search query";
    private static final int TEST_K = 15;
    private static final Float TEST_MIN_SCORE = 0.8f;

    @Override
    protected void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    public void test_builder_defaultValues_setsCorrectDefaults() {
        final MultiModalQueryBuilder.Builder builder = new MultiModalQueryBuilder.Builder();
        final MultiModalQueryBuilder queryBuilder = builder.build();

        assertNull(queryBuilder.field);
        assertNull(queryBuilder.query);
        assertEquals(10, queryBuilder.k); // Default k value
        assertNull(queryBuilder.minScore);
    }

    public void test_builder_fluentAPI_chainsCorrectly() {
        final MultiModalQueryBuilder queryBuilder =
                new MultiModalQueryBuilder.Builder().field(TEST_FIELD).query(TEST_QUERY).k(TEST_K).minScore(TEST_MIN_SCORE).build();

        assertEquals(TEST_FIELD, queryBuilder.field);
        assertEquals(TEST_QUERY, queryBuilder.query);
        assertEquals(TEST_K, queryBuilder.k);
        assertEquals(TEST_MIN_SCORE, queryBuilder.minScore);
    }

    public void test_builder_field_setsFieldName() {
        final MultiModalQueryBuilder queryBuilder = new MultiModalQueryBuilder.Builder().field("my_vector_field").build();

        assertEquals("my_vector_field", queryBuilder.field);
    }

    public void test_builder_query_setsQueryText() {
        final MultiModalQueryBuilder queryBuilder = new MultiModalQueryBuilder.Builder().query("my search query").build();

        assertEquals("my search query", queryBuilder.query);
    }

    public void test_builder_k_setsKValue() {
        final MultiModalQueryBuilder queryBuilder = new MultiModalQueryBuilder.Builder().k(25).build();

        assertEquals(25, queryBuilder.k);
    }

    public void test_builder_minScore_setsMinScore() {
        final MultiModalQueryBuilder queryBuilder = new MultiModalQueryBuilder.Builder().minScore(0.9f).build();

        assertEquals(Float.valueOf(0.9f), queryBuilder.minScore);
    }

    public void test_builder_build_constructsCorrectly() {
        final MultiModalQueryBuilder.Builder builder = new MultiModalQueryBuilder.Builder().field(TEST_FIELD).query(TEST_QUERY);

        final MultiModalQueryBuilder queryBuilder1 = builder.build();
        final MultiModalQueryBuilder queryBuilder2 = builder.build();

        // Should create separate instances
        assertNotSame(queryBuilder1, queryBuilder2);
        // But with same values
        assertEquals(queryBuilder1.field, queryBuilder2.field);
        assertEquals(queryBuilder1.query, queryBuilder2.query);
    }

    // Note: Testing toQueryBuilder() requires CasClient which depends on ComponentUtil
    // These tests focus on the Builder pattern functionality that can be tested in isolation

    public void test_builder_multipleBuilds_createsIndependentInstances() {
        final MultiModalQueryBuilder.Builder builder = new MultiModalQueryBuilder.Builder().field(TEST_FIELD).query(TEST_QUERY);

        final MultiModalQueryBuilder queryBuilder1 = builder.build();
        final MultiModalQueryBuilder queryBuilder2 = builder.build();

        assertNotSame(queryBuilder1, queryBuilder2);

        // Modify one instance's field (if it were mutable)
        // This tests that they are truly independent instances
        assertEquals(queryBuilder1.field, queryBuilder2.field);
        assertEquals(queryBuilder1.query, queryBuilder2.query);
    }

}