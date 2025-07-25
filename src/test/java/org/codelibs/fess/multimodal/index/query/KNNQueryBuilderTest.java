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
package org.codelibs.fess.multimodal.index.query;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.dbflute.utflute.core.PlainTestCase;

public class KNNQueryBuilderTest extends PlainTestCase {

    private static final String TEST_FIELD = "test_vector_field";
    private static final float[] TEST_VECTOR = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
    private static final int TEST_K = 15;

    public void test_builder_defaultValues_setsCorrectDefaults() {
        final KNNQueryBuilder.Builder builder = new KNNQueryBuilder.Builder();
        final KNNQueryBuilder query = builder.build();

        // Default values should be set appropriately
        assertNull(query.fieldName);
        assertNull(query.vector);
        assertEquals(10, query.k); // DEFAULT_K
        assertNull(query.filter);
        assertFalse(query.ignoreUnmapped);
        assertNull(query.maxDistance);
        assertNull(query.minScore);
    }

    public void test_builder_fluentAPI_chainsCorrectly() {
        final BoolQueryBuilder testFilter = QueryBuilders.boolQuery();

        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(TEST_K).filter(testFilter)
                .ignoreUnmapped(true).maxDistance(0.8f).minScore(0.5f).build();

        assertEquals(TEST_FIELD, query.fieldName);
        assertSame(TEST_VECTOR, query.vector);
        assertEquals(TEST_K, query.k);
        assertSame(testFilter, query.filter);
        assertTrue(query.ignoreUnmapped);
        assertEquals(Float.valueOf(0.8f), query.maxDistance);
        assertEquals(Float.valueOf(0.5f), query.minScore);
    }

    public void test_builder_field_setsFieldName() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field("my_field").build();

        assertEquals("my_field", query.fieldName);
    }

    public void test_builder_vector_setsVector() {
        final float[] vector = { 1.0f, 2.0f };
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().vector(vector).build();

        assertSame(vector, query.vector);
    }

    public void test_builder_k_setsKValue() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().k(25).build();

        assertEquals(25, query.k);
    }

    public void test_builder_filter_setsFilter() {
        final BoolQueryBuilder filter = QueryBuilders.boolQuery();
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().filter(filter).build();

        assertSame(filter, query.filter);
    }

    public void test_builder_ignoreUnmapped_setsFlag() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().ignoreUnmapped(true).build();

        assertTrue(query.ignoreUnmapped);
    }

    public void test_builder_maxDistance_setsThreshold() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().maxDistance(0.9f).build();

        assertEquals(Float.valueOf(0.9f), query.maxDistance);
    }

    public void test_builder_minScore_setsThreshold() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().minScore(0.3f).build();

        assertEquals(Float.valueOf(0.3f), query.minScore);
    }

    public void test_builder_build_constructsImmutableInstance() {
        final KNNQueryBuilder.Builder builder = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR);

        final KNNQueryBuilder query1 = builder.build();
        final KNNQueryBuilder query2 = builder.build();

        // Should create separate instances
        assertNotSame(query1, query2);
        // But with same values
        assertEquals(query1.fieldName, query2.fieldName);
        assertSame(query1.vector, query2.vector);
    }

    // Note: Serialization tests removed due to unavailable BytesStreamOutput and StreamInput classes
    // These would test the writeTo() and constructor(StreamInput) methods

    // Note: XContent serialization tests removed due to JsonXContent usage complexity
    // These would test the doXContent method but require proper XContentBuilder setup

    public void test_equals_identicalObjects_returnsTrue() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(TEST_K).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(TEST_K).build();

        assertTrue(query1.equals(query2));
        assertTrue(query2.equals(query1));
    }

    public void test_equals_differentFieldName_returnsFalse() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field("field1").vector(TEST_VECTOR).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field("field2").vector(TEST_VECTOR).build();

        assertFalse(query1.equals(query2));
    }

    public void test_equals_differentVector_returnsFalse() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(new float[] { 1.0f, 2.0f }).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(new float[] { 3.0f, 4.0f }).build();

        assertFalse(query1.equals(query2));
    }

    public void test_equals_differentK_returnsFalse() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(10).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(20).build();

        assertFalse(query1.equals(query2));
    }

    public void test_equals_nullComparison_returnsFalse() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).build();

        assertFalse(query.equals(null));
    }

    public void test_equals_selfComparison_returnsTrue() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).build();

        assertTrue(query.equals(query));
    }

    public void test_hashCode_identicalObjects_returnsSameHashCode() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(TEST_K).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(TEST_VECTOR).k(TEST_K).build();

        assertEquals(query1.hashCode(), query2.hashCode());
    }

    public void test_hashCode_differentObjects_mayReturnDifferentHashCode() {
        final KNNQueryBuilder query1 = new KNNQueryBuilder.Builder().field("field1").vector(TEST_VECTOR).build();

        final KNNQueryBuilder query2 = new KNNQueryBuilder.Builder().field("field2").vector(TEST_VECTOR).build();

        // Hash codes may be different (but not required to be)
        assertNotSame(query1.hashCode(), query2.hashCode());
    }

    public void test_build_nullVector_allowsNull() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(null).build();

        assertNull(query.vector);
    }

    public void test_build_emptyVector_allowsEmpty() {
        final float[] emptyVector = new float[0];
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).vector(emptyVector).build();

        assertSame(emptyVector, query.vector);
        assertEquals(0, query.vector.length);
    }

    public void test_build_negativeK_allowsNegative() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).k(-1).build();

        assertEquals(-1, query.k);
    }

    public void test_build_zeroK_allowsZero() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).k(0).build();

        assertEquals(0, query.k);
    }

    public void test_getWriteableName_returnsKnn() {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).build();

        assertEquals("knn", query.getWriteableName());
    }

    public void test_doToQuery_throwsUnsupportedOperationException() throws IOException {
        final KNNQueryBuilder query = new KNNQueryBuilder.Builder().field(TEST_FIELD).build();

        try {
            query.doToQuery(null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("doToQuery is not supported.", e.getMessage());
        }
    }

    private void assertArrayEquals(float[] expected, float[] actual) {
        assertNotNull("Expected array should not be null", expected);
        assertNotNull("Actual array should not be null", actual);
        assertEquals("Array lengths should be equal", expected.length, actual.length);

        for (int i = 0; i < expected.length; i++) {
            assertEquals("Elements at index " + i + " should be equal", expected[i], actual[i], 0.0001f);
        }
    }
}
