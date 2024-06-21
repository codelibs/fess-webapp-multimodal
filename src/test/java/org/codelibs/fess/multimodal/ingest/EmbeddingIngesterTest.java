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
package org.codelibs.fess.multimodal.ingest;

import java.util.HashMap;
import java.util.Map;

import org.dbflute.utflute.core.PlainTestCase;

public class EmbeddingIngesterTest extends PlainTestCase {
    private static final String VECTOR_FIELD = "vector_field";

    public void test_process() {
        final EmbeddingIngester ingester = new EmbeddingIngester();
        ingester.embeddingField = VECTOR_FIELD;

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
}
