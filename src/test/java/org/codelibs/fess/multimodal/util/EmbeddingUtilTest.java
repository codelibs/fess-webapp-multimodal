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
package org.codelibs.fess.multimodal.util;

import org.codelibs.fess.multimodal.UnitWebappTestCase;

public class EmbeddingUtilTest extends UnitWebappTestCase {

    public void test_encodeFloatArray() {
        final float[] array = { 1.0f, 2.0f, 3.0f };
        assertEquals("P4AAAEAAAABAQAAA", EmbeddingUtil.encodeFloatArray(array));
    }

    public void test_decodeFloatArray() {
        final float[] array = EmbeddingUtil.decodeFloatArray("P4AAAEAAAABAQAAA");
        assertEquals(3, array.length);
        assertEquals(1.0f, array[0]);
        assertEquals(2.0f, array[1]);
        assertEquals(3.0f, array[2]);
    }

    public void test_roundTripEncoding_preservesAccuracy() {
        final float[] original = { 1.5f, -2.7f, 0.0f, 999.9f, -0.001f };
        final String encoded = EmbeddingUtil.encodeFloatArray(original);
        final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);

        assertEquals(original.length, decoded.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals("Element at index " + i, original[i], decoded[i], 0.0001f);
        }
    }

    public void test_encodeFloatArray_emptyArray_handlesCorrectly() {
        final float[] emptyArray = new float[0];
        final String encoded = EmbeddingUtil.encodeFloatArray(emptyArray);
        assertNotNull(encoded);

        final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);
        assertEquals(0, decoded.length);
    }

    public void test_encodeFloatArray_singleElement_handlesCorrectly() {
        final float[] singleElement = { 42.0f };
        final String encoded = EmbeddingUtil.encodeFloatArray(singleElement);
        final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);

        assertEquals(1, decoded.length);
        assertEquals(42.0f, decoded[0], 0.0001f);
    }

    public void test_encodeFloatArray_largeArray_handlesCorrectly() {
        final float[] largeArray = new float[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (float) (Math.sin(i) * 1000);
        }

        final String encoded = EmbeddingUtil.encodeFloatArray(largeArray);
        final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);

        assertEquals(largeArray.length, decoded.length);
        for (int i = 0; i < largeArray.length; i++) {
            assertEquals("Element at index " + i, largeArray[i], decoded[i], 0.0001f);
        }
    }

    public void test_encodeFloatArray_specialValues_handlesCorrectly() {
        final float[] specialValues =
                { Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, Float.MAX_VALUE, Float.MIN_VALUE, -0.0f, 0.0f };

        final String encoded = EmbeddingUtil.encodeFloatArray(specialValues);
        final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);

        assertEquals(specialValues.length, decoded.length);
        assertEquals(Float.POSITIVE_INFINITY, decoded[0]);
        assertEquals(Float.NEGATIVE_INFINITY, decoded[1]);
        assertTrue(Float.isNaN(decoded[2]));
        assertEquals(Float.MAX_VALUE, decoded[3]);
        assertEquals(Float.MIN_VALUE, decoded[4]);
        assertEquals(-0.0f, decoded[5]);
        assertEquals(0.0f, decoded[6]);
    }

    public void test_encodeFloatArray_nullArray_throwsException() {
        try {
            EmbeddingUtil.encodeFloatArray(null);
            fail("Expected NullPointerException for null array");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void test_decodeFloatArray_nullString_throwsException() {
        try {
            EmbeddingUtil.decodeFloatArray(null);
            fail("Expected exception for null string");
        } catch (Exception e) {
            // Expected - could be NullPointerException or IllegalArgumentException
        }
    }

    public void test_decodeFloatArray_emptyString_returnsEmptyArray() {
        final float[] result = EmbeddingUtil.decodeFloatArray("");

        // Empty string decodes to empty array
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    public void test_decodeFloatArray_invalidBase64_throwsException() {
        try {
            EmbeddingUtil.decodeFloatArray("invalid_base64!");
            fail("Expected exception for invalid base64");
        } catch (Exception e) {
            // Expected - illegal base64 characters
        }
    }

    public void test_decodeFloatArray_malformedData_handlesGracefully() {
        // Valid base64 but not divisible by 4 bytes (float size)
        // "Hello" in base64 decodes to 5 bytes
        final float[] result = EmbeddingUtil.decodeFloatArray("SGVsbG8=");

        // Should create array with truncated length (5/4 = 1 float)
        assertNotNull(result);
        assertEquals(1, result.length);
        // The actual float value depends on how "Hell" bytes are interpreted as float
    }

    public void test_encoding_consistency_multipleRuns() {
        final float[] testArray = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };

        // Encode multiple times and verify consistency
        final String encoded1 = EmbeddingUtil.encodeFloatArray(testArray);
        final String encoded2 = EmbeddingUtil.encodeFloatArray(testArray);
        final String encoded3 = EmbeddingUtil.encodeFloatArray(testArray);

        assertEquals(encoded1, encoded2);
        assertEquals(encoded2, encoded3);

        // Decode and verify all are identical
        final float[] decoded1 = EmbeddingUtil.decodeFloatArray(encoded1);
        final float[] decoded2 = EmbeddingUtil.decodeFloatArray(encoded2);
        final float[] decoded3 = EmbeddingUtil.decodeFloatArray(encoded3);

        assertArrayEquals(testArray, decoded1);
        assertArrayEquals(testArray, decoded2);
        assertArrayEquals(testArray, decoded3);
    }

    public void test_performance_largeArrays() {
        // Test with progressively larger arrays to ensure reasonable performance
        final int[] sizes = { 100, 512, 1024, 2048 };

        for (int size : sizes) {
            final float[] testArray = new float[size];
            for (int i = 0; i < size; i++) {
                testArray[i] = (float) Math.random();
            }

            final long startTime = System.currentTimeMillis();
            final String encoded = EmbeddingUtil.encodeFloatArray(testArray);
            final float[] decoded = EmbeddingUtil.decodeFloatArray(encoded);
            final long endTime = System.currentTimeMillis();

            assertEquals(size, decoded.length);
            assertArrayEquals(testArray, decoded);

            // Ensure reasonable performance (should complete within 1 second even for large arrays)
            assertTrue("Performance test failed for size " + size + ", took " + (endTime - startTime) + "ms", (endTime - startTime) < 1000);
        }
    }

    private void assertArrayEquals(float[] expected, float[] actual) {
        assertEquals("Array lengths should be equal", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Elements at index " + i + " should be equal", expected[i], actual[i], 0.0001f);
        }
    }
}
