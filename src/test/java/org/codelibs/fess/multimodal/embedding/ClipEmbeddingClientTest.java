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
package org.codelibs.fess.multimodal.embedding;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.junit.jupiter.api.Test;

public class ClipEmbeddingClientTest extends UnitWebappTestCase {

    @Test
    public void test_getName() {
        assertEquals("clip", new ClipEmbeddingClient().getName());
    }

    @Test
    public void test_getConfigPrefix() {
        assertEquals("content_chunker.embedding.clip", new ClipEmbeddingClient().getConfigPrefix());
    }

    /** Every text is embedded independently; the CLIP server has no batch endpoint. */
    @Test
    public void test_embedDocuments_callsPerText() {
        final List<String> seen = new ArrayList<>();
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                seen.add(text);
                return new float[] { 1.0f, 2.0f };
            }
        };

        final List<String> texts = new ArrayList<>();
        texts.add("a");
        texts.add("b");
        final List<float[]> vectors = client.embedDocuments(texts);

        assertEquals(2, vectors.size());
        assertEquals(2, seen.size());
        assertEquals("a", seen.get(0));
        assertEquals("b", seen.get(1));
        assertEquals(1.0f, vectors.get(0)[0]);
    }

    @Test
    public void test_embedDocuments_emptyInput() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                fail("embedText must not be called for an empty input.");
                return null;
            }
        };
        assertEquals(0, client.embedDocuments(new ArrayList<>()).size());
        assertEquals(0, client.embedDocuments(null).size());
    }

    @Test
    public void test_embedQuery_delegatesToEmbedText() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                return new float[] { text.length() };
            }
        };
        final List<String> texts = new ArrayList<>();
        texts.add("abc");
        final List<float[]> vectors = client.embedQuery(texts);
        assertEquals(1, vectors.size());
        assertEquals(3.0f, vectors.get(0)[0]);
    }

    /** checkAvailabilityNow() runs inside init(); a throw there would stop container startup. */
    @Test
    public void test_checkAvailabilityNow_failsClosed() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected String getApiUrl() {
                throw new RuntimeException("boom");
            }
        };
        assertFalse(client.checkAvailabilityNow());
    }
}
