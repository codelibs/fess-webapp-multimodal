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
import java.util.List;
import java.util.Map;

import org.codelibs.fess.Constants;
import org.codelibs.fess.helper.ChunkVectorHelper;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.junit.jupiter.api.Test;

public class EmbeddingIngesterTest extends UnitWebappTestCase {

    /** base64 of the big-endian float[] {1.0f, 2.0f, 3.0f}. */
    private static final String ENCODED = "P4AAAEAAAABAQAAA";

    @Test
    public void test_process_writesNestedVectorAndDoneStatus() {
        final EmbeddingIngester ingester = new EmbeddingIngester();

        final Map<String, Object> target = new HashMap<>();
        target.put(MultiModalConstants.EMBEDDING_STAGING_FIELD, new String[] { ENCODED });

        final Map<String, Object> result = ingester.process(target);

        // The staging field must never reach _source: it is not in the index mapping.
        assertFalse(result.containsKey(MultiModalConstants.EMBEDDING_STAGING_FIELD));
        assertEquals(Constants.DONE, result.get(Constants.CONTENT_CHUNK_STATUS_FIELD));

        final Object vectorField = result.get(Constants.CONTENT_CHUNK_VECTOR_FIELD);
        assertTrue("content_chunk_vector must be a List", vectorField instanceof List);
        final List<?> vectorList = (List<?>) vectorField;
        assertEquals(1, vectorList.size());

        // ArrayList/HashMap/float[] only -- List.of/Map.of/Float[] are unregistered with Kryo.
        assertEquals(java.util.ArrayList.class, vectorField.getClass());
        final Object entry = vectorList.get(0);
        assertEquals(java.util.HashMap.class, entry.getClass());

        final Object vector = ((Map<?, ?>) entry).get(ChunkVectorHelper.VECTOR_SUBFIELD);
        assertTrue("vector must be a raw float[]", vector instanceof float[]);
        final float[] array = (float[]) vector;
        assertEquals(3, array.length);
        assertEquals(1.0f, array[0]);
        assertEquals(2.0f, array[1]);
        assertEquals(3.0f, array[2]);
    }

    @Test
    public void test_process_noEmbedding_leavesDocumentUntouched() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        final Map<String, Object> target = new HashMap<>();
        target.put("url", "http://example.com/a.html");

        final Map<String, Object> result = ingester.process(target);

        assertEquals(1, result.size());
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_VECTOR_FIELD));
        // No status is written: a text document must stay pending so the core chunk job
        // can pick it up if the operator enables it later.
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_STATUS_FIELD));
    }

    @Test
    public void test_process_malformedStagingValue_dropsItWithoutStatus() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        final Map<String, Object> target = new HashMap<>();
        target.put(MultiModalConstants.EMBEDDING_STAGING_FIELD, "not-an-array");

        final Map<String, Object> result = ingester.process(target);

        assertFalse(result.containsKey(MultiModalConstants.EMBEDDING_STAGING_FIELD));
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_VECTOR_FIELD));
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_STATUS_FIELD));
    }

    @Test
    public void test_process_emptyStagingArray_dropsItWithoutStatus() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        final Map<String, Object> target = new HashMap<>();
        target.put(MultiModalConstants.EMBEDDING_STAGING_FIELD, new String[] {});

        final Map<String, Object> result = ingester.process(target);

        assertFalse(result.containsKey(MultiModalConstants.EMBEDDING_STAGING_FIELD));
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_VECTOR_FIELD));
        assertFalse(result.containsKey(Constants.CONTENT_CHUNK_STATUS_FIELD));
    }
}
