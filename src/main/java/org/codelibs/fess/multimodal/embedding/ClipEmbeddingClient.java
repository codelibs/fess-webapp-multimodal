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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.embedding.AbstractEmbeddingClient;
import org.codelibs.fess.embedding.EmbeddingException;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.client.CasProtocol;
import org.codelibs.fess.util.ComponentUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Embedding client backed by the CLIP server's text tower.
 *
 * <p>This is the search-side half of multimodal search: the query text is embedded into
 * the same vector space as the image embeddings the crawler wrote, so Fess core's
 * semantic chunk searcher can retrieve images with a plain text query. The crawler-side
 * image half lives in {@code CasClient}, which cannot go through this class because the
 * crawler process has no {@code embeddingClientManager} component.</p>
 */
public class ClipEmbeddingClient extends AbstractEmbeddingClient {

    private static final Logger logger = LogManager.getLogger(ClipEmbeddingClient.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** The name identifier for the CLIP embedding client. */
    public static final String NAME = "clip";

    /**
     * Constructs a new ClipEmbeddingClient instance.
     */
    public ClipEmbeddingClient() {
        // Default constructor
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getConfigPrefix() {
        return MultiModalConstants.CLIP_CONFIG_PREFIX;
    }

    @Override
    protected int getTimeout() {
        return getConfigInt("timeout", 60000);
    }

    /**
     * Returns the CLIP server base URL.
     *
     * @return the base URL
     */
    protected String getApiUrl() {
        return ComponentUtil.getFessConfig().getSystemProperty(MultiModalConstants.CLIP_API_URL, MultiModalConstants.DEFAULT_CLIP_API_URL);
    }

    @Override
    protected boolean checkAvailabilityNow() {
        try {
            final String apiUrl = getApiUrl();
            if (StringUtil.isBlank(apiUrl)) {
                return false;
            }
            final HttpGet request = new HttpGet(apiUrl + "/health");
            try (var response = getHttpClient().execute(request)) {
                final int statusCode = response.getCode();
                if (statusCode < 200 || statusCode >= 300) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[Embedding:CLIP] Health check failed. url={}, statusCode={}", apiUrl, statusCode);
                    }
                    return false;
                }
                return true;
            }
        } catch (final Exception e) {
            // Fail closed: this method is reached synchronously from init(), so a throw here
            // would escape the container's eager init-method assembler.
            if (logger.isDebugEnabled()) {
                logger.debug("[Embedding:CLIP] CLIP server is not available. error={}", e.getMessage());
            }
            return false;
        }
    }

    @Override
    public List<float[]> embedDocuments(final List<String> texts) {
        return embedAll("embedDocuments", texts);
    }

    @Override
    public List<float[]> embedQuery(final List<String> texts) {
        return embedAll("embedQuery", texts);
    }

    /**
     * Embeds every text one at a time. The CLIP server's Jina-style {@code /post} endpoint
     * accepts a data array but processes it item by item with no batching benefit, so a
     * per-text call keeps error attribution precise.
     *
     * @param operation the operation name, for logging
     * @param texts the texts to embed
     * @return one vector per input text, in input order
     */
    protected List<float[]> embedAll(final String operation, final List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        final long startTime = System.currentTimeMillis();
        final List<float[]> vectors = new ArrayList<>(texts.size());
        for (final String text : texts) {
            vectors.add(embedText(text));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[Embedding:CLIP] {} completed. count={}, elapsedTime={}ms", operation, vectors.size(),
                    System.currentTimeMillis() - startTime);
        }
        return vectors;
    }

    /**
     * Embeds a single text through the CLIP server.
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws EmbeddingException if the call fails or the vector dimension is unexpected
     */
    protected float[] embedText(final String text) {
        final String url = getApiUrl() + CasProtocol.POST_PATH;
        try {
            final HttpPost httpRequest = new HttpPost(url);
            httpRequest.setEntity(new StringEntity(CasProtocol.buildTextRequest(text), ContentType.APPLICATION_JSON));
            try (var response = getHttpClient().execute(httpRequest)) {
                final int statusCode = response.getCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new EmbeddingException("CLIP server error: " + statusCode + " " + response.getReasonPhrase());
                }
                final String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                @SuppressWarnings("unchecked")
                final Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                final float[] vector = CasProtocol.parseEmbedding(responseMap);
                final int dimension = getDimension();
                if (vector.length != dimension) {
                    throw new EmbeddingException("CLIP embedding dimension mismatch: expected=" + dimension + ", actual=" + vector.length);
                }
                return vector;
            }
        } catch (final EmbeddingException e) {
            throw e;
        } catch (final Exception e) {
            throw new EmbeddingException("Failed to call the CLIP server. url=" + url, e);
        }
    }
}
