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
package org.codelibs.fess.multimodal.ingest;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.dbflute.utflute.core.PlainTestCase;

public class EmbeddingIngesterTest extends PlainTestCase {

    private static final String VECTOR_FIELD = "vector_field";

    public void test_process() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        Map<String, Object> result = ingester.process(target);
        assertEquals(0, result.size());

        target.clear();
        target.put(VECTOR_FIELD, new String[] { "P4AAAEAAAABAQAAA" });
        result = ingester.process(target);
        assertEquals(1, result.size());
        final float[] array = (float[]) result.get(VECTOR_FIELD);
        assertEquals(3, array.length);
        assertEquals(1.0f, array[0]);
        assertEquals(2.0f, array[1]);
        assertEquals(3.0f, array[2]);

        target.clear();
        target.put(VECTOR_FIELD, "P4AAAEAAAABAQAAA");
        result = ingester.process(target);
        assertEquals(1, result.size());
        assertEquals("P4AAAEAAAABAQAAA", result.get(VECTOR_FIELD));
    }

    public void test_process_emptyMap_returnsEmptyMap() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        final Map<String, Object> result = ingester.process(target);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    public void test_process_withoutVectorField_returnsUnchanged() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        target.put("other_field", "some_value");
        target.put("another_field", 123);

        final Map<String, Object> result = ingester.process(target);

        assertEquals(2, result.size());
        assertEquals("some_value", result.get("other_field"));
        assertEquals(123, result.get("another_field"));
    }

    public void test_process_withValidEncodedEmbedding_decodesCorrectly() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final float[] originalEmbedding = { 1.5f, 2.7f, 3.9f, 4.1f, 5.3f };
        final String encoded = EmbeddingUtil.encodeFloatArray(originalEmbedding);

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, new String[] { encoded });

        final Map<String, Object> result = ingester.process(target);

        assertEquals(1, result.size());
        final float[] decodedArray = (float[]) result.get(VECTOR_FIELD);
        assertEquals(originalEmbedding.length, decodedArray.length);
        for (int i = 0; i < originalEmbedding.length; i++) {
            assertEquals(originalEmbedding[i], decodedArray[i], 0.0001f);
        }
    }

    public void test_process_withMultipleEncodedValues_usesFirstValue() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final String encoded1 = EmbeddingUtil.encodeFloatArray(new float[] { 1.0f, 2.0f });
        final String encoded2 = EmbeddingUtil.encodeFloatArray(new float[] { 3.0f, 4.0f });

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, new String[] { encoded1, encoded2 });

        final Map<String, Object> result = ingester.process(target);

        assertEquals(1, result.size());
        final float[] decodedArray = (float[]) result.get(VECTOR_FIELD);
        assertEquals(2, decodedArray.length);
        assertEquals(1.0f, decodedArray[0], 0.0001f);
        assertEquals(2.0f, decodedArray[1], 0.0001f);
    }

    public void test_process_withNonArrayValue_keepsOriginal() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, "not_an_array");

        final Map<String, Object> result = ingester.process(target);

        assertEquals(1, result.size());
        assertEquals("not_an_array", result.get(VECTOR_FIELD));
    }

    public void test_process_withNullVectorField_handlesGracefully() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, null);

        final Map<String, Object> result = ingester.process(target);

        assertNotNull(result);
        assertTrue(result.containsKey(VECTOR_FIELD));
        assertNull(result.get(VECTOR_FIELD));
    }

    public void test_process_withEmptyStringArray_throwsException() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, new String[] {});

        try {
            ingester.process(target);
            fail("Expected ArrayIndexOutOfBoundsException for empty array");
        } catch (final ArrayIndexOutOfBoundsException e) {
            // Expected - implementation tries to access encodedEmbeddings[0] without checking length
            assertTrue(e.getMessage().contains("Index 0 out of bounds"));
        }
    }

    public void test_process_withLargeEmbedding_handlesCorrectly() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final float[] largeEmbedding = new float[512];
        for (int i = 0; i < largeEmbedding.length; i++) {
            largeEmbedding[i] = (float) Math.sin(i);
        }
        final String encoded = EmbeddingUtil.encodeFloatArray(largeEmbedding);

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, new String[] { encoded });

        final Map<String, Object> result = ingester.process(target);

        assertEquals(1, result.size());
        final float[] decodedArray = (float[]) result.get(VECTOR_FIELD);
        assertEquals(512, decodedArray.length);
        for (int i = 0; i < largeEmbedding.length; i++) {
            assertEquals(largeEmbedding[i], decodedArray[i], 0.0001f);
        }
    }

    public void test_process_withAdditionalFields_preservesOtherFields() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        final Map<String, Object> target = new HashMap<>();
        target.put(VECTOR_FIELD, new String[] { "P4AAAEAAAABAQAAA" });
        target.put("title", "Test Document");
        target.put("content", "Test content");
        target.put("id", 123);

        final Map<String, Object> result = ingester.process(target);

        assertEquals(4, result.size());
        assertTrue(result.get(VECTOR_FIELD) instanceof float[]);
        assertEquals("Test Document", result.get("title"));
        assertEquals("Test content", result.get("content"));
        assertEquals(123, result.get("id"));
    }

    public void test_process_multipleInvocations_worksConsistently() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = VECTOR_FIELD;

        for (int i = 0; i < 5; i++) {
            final Map<String, Object> target = new HashMap<>();
            target.put(VECTOR_FIELD, new String[] { "P4AAAEAAAABAQAAA" });

            final Map<String, Object> result = ingester.process(target);

            assertEquals(1, result.size());
            final float[] array = (float[]) result.get(VECTOR_FIELD);
            assertEquals(3, array.length);
            assertEquals(1.0f, array[0], 0.0001f);
            assertEquals(2.0f, array[1], 0.0001f);
            assertEquals(3.0f, array[2], 0.0001f);
        }
    }

    public void test_process_withDifferentVectorField_usesCorrectField() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.vectorField = "custom_vector";

        final Map<String, Object> target = new HashMap<>();
        target.put("custom_vector", new String[] { "P4AAAEAAAABAQAAA" });
        target.put(VECTOR_FIELD, "should_not_be_processed");

        final Map<String, Object> result = ingester.process(target);

        assertEquals(2, result.size());
        assertTrue(result.get("custom_vector") instanceof float[]);
        assertEquals("should_not_be_processed", result.get(VECTOR_FIELD));
    }
}
