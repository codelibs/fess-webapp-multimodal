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
package org.codelibs.fess.util;

import org.codelibs.fess.util.EmbeddingUtil;
import org.dbflute.utflute.core.PlainTestCase;

public class EmbeddingUtilTest extends PlainTestCase {

    public void test_encodeFloatArray() {
        float[] array = new float[] { 1.0f, 2.0f, 3.0f };
        assertEquals("P4AAAEAAAABAQAAA", EmbeddingUtil.encodeFloatArray(array));
    }

    public void test_decodeFloatArray() {
        float[] array = EmbeddingUtil.decodeFloatArray("P4AAAEAAAABAQAAA");
        assertEquals(3, array.length);
        assertEquals(1.0f, array[0]);
        assertEquals(2.0f, array[1]);
        assertEquals(3.0f, array[2]);
    }
}
