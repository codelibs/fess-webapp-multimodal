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
package org.codelibs.fess.multimodal.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.codelibs.fess.multimodal.exception.CasAccessException;
import org.junit.jupiter.api.Test;

public class CasProtocolTest extends UnitWebappTestCase {

    @Test
    public void test_buildTextRequest() {
        assertEquals("{\"data\":[{\"text\":\"hello\"}],\"execEndpoint\":\"/\"}", CasProtocol.buildTextRequest("hello"));
    }

    @Test
    public void test_buildTextRequest_escapesJson() {
        assertEquals("{\"data\":[{\"text\":\"a\\\"b\"}],\"execEndpoint\":\"/\"}", CasProtocol.buildTextRequest("a\"b"));
    }

    @Test
    public void test_buildBlobRequest() {
        assertEquals("{\"data\":[{\"blob\":\"QUJD\"}],\"execEndpoint\":\"/\"}", CasProtocol.buildBlobRequest("QUJD"));
    }

    @Test
    public void test_parseEmbedding() {
        final List<Object> embedding = new ArrayList<>();
        embedding.add(Double.valueOf(1.0d));
        embedding.add(Double.valueOf(-2.5d));
        final Map<String, Object> item = new HashMap<>();
        item.put("embedding", embedding);
        final List<Object> data = new ArrayList<>();
        data.add(item);
        final Map<String, Object> response = new HashMap<>();
        response.put("data", data);

        final float[] result = CasProtocol.parseEmbedding(response);
        assertEquals(2, result.length);
        assertEquals(1.0f, result[0]);
        assertEquals(-2.5f, result[1]);
    }

    @Test
    public void test_parseEmbedding_missingData() {
        try {
            CasProtocol.parseEmbedding(new HashMap<>());
            fail("CasAccessException is expected.");
        } catch (final CasAccessException e) {
            // expected
        }
    }

    @Test
    public void test_parseEmbedding_emptyDataList() {
        final Map<String, Object> response = new HashMap<>();
        response.put("data", new ArrayList<>());
        try {
            CasProtocol.parseEmbedding(response);
            fail("CasAccessException is expected.");
        } catch (final CasAccessException e) {
            // expected
        }
    }

    @Test
    public void test_parseEmbedding_dataElementNotMap() {
        final List<Object> data = new ArrayList<>();
        data.add("not-a-map");
        final Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        try {
            CasProtocol.parseEmbedding(response);
            fail("CasAccessException is expected.");
        } catch (final CasAccessException e) {
            // expected
        }
    }

    @Test
    public void test_parseEmbedding_embeddingNotList() {
        final Map<String, Object> item = new HashMap<>();
        item.put("embedding", "not-a-list");
        final List<Object> data = new ArrayList<>();
        data.add(item);
        final Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        try {
            CasProtocol.parseEmbedding(response);
            fail("CasAccessException is expected.");
        } catch (final CasAccessException e) {
            // expected
        }
    }

    @Test
    public void test_parseEmbedding_nonNumericComponent() {
        final List<Object> embedding = new ArrayList<>();
        embedding.add(Double.valueOf(1.0d));
        embedding.add("not-a-number");
        final Map<String, Object> item = new HashMap<>();
        item.put("embedding", embedding);
        final List<Object> data = new ArrayList<>();
        data.add(item);
        final Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        try {
            CasProtocol.parseEmbedding(response);
            fail("CasAccessException is expected.");
        } catch (final CasAccessException e) {
            // expected
        }
    }
}
