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

import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.codelibs.fess.multimodal.exception.CasAccessException;

/**
 * Wire contract for the CLIP embedding server's Jina-style {@code POST /post} endpoint.
 *
 * <p>Both the crawler-side image path ({@link CasClient}) and the search-side text path
 * ({@code ClipEmbeddingClient}) speak this protocol, so the request shape and the response
 * parsing live here rather than being duplicated.</p>
 */
public final class CasProtocol {

    /** Path appended to the configured CLIP server base URL. */
    public static final String POST_PATH = "/post";

    private CasProtocol() {
        // nothing
    }

    /**
     * Builds the request body for a text embedding.
     *
     * @param text the text to embed
     * @return the JSON request body
     */
    public static String buildTextRequest(final String text) {
        return "{\"data\":[{\"text\":\"" + StringEscapeUtils.escapeJson(text) + "\"}],\"execEndpoint\":\"/\"}";
    }

    /**
     * Builds the request body for an image embedding.
     *
     * @param encodedImage the base64-encoded image
     * @return the JSON request body
     */
    public static String buildBlobRequest(final String encodedImage) {
        return "{\"data\":[{\"blob\":\"" + StringEscapeUtils.escapeJson(encodedImage) + "\"}],\"execEndpoint\":\"/\"}";
    }

    /**
     * Extracts {@code data[0].embedding} from a parsed response body.
     *
     * @param responseMap the parsed response body
     * @return the embedding vector
     * @throws CasAccessException if the response carries no usable embedding
     */
    public static float[] parseEmbedding(final Map<String, Object> responseMap) {
        if (responseMap != null && responseMap.get("data") instanceof final List<?> dataList && !dataList.isEmpty()
                && dataList.get(0) instanceof final Map<?, ?> data && data.get("embedding") instanceof final List<?> embeddingList) {
            final float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embedding.length; i++) {
                if (!(embeddingList.get(i) instanceof final Number number)) {
                    throw new CasAccessException("Clip server returned a non-numeric embedding component at index " + i);
                }
                embedding[i] = number.floatValue();
            }
            return embedding;
        }
        throw new CasAccessException("Clip server cannot generate an embedding");
    }
}
