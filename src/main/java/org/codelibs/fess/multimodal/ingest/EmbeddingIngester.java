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

import static org.codelibs.core.lang.StringUtil.EMPTY;
import static org.codelibs.fess.Constants.MAPPING_TYPE_ARRAY;
import static org.codelibs.fess.multimodal.MultiModalConstants.X_FESS_EMBEDDING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.Constants;
import org.codelibs.fess.helper.ChunkVectorHelper;
import org.codelibs.fess.ingest.Ingester;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.annotation.PostConstruct;

/**
 * Writes the crawler-computed CLIP image embedding into Fess core's content_chunk_vector
 * field at index time.
 *
 * <p>Core's Content Chunk Vector Indexer job only ever processes documents whose
 * content_chunk_status is absent, so stamping {@code done} here keeps image documents out
 * of its reach permanently -- which matters because an image has no text content and the
 * job would otherwise mark it {@code skipped} and, if it ever did run over a document whose
 * EXIF metadata made content non-blank, overwrite the CLIP vector with a text embedding.</p>
 */
public class EmbeddingIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(EmbeddingIngester.class);

    /**
     * Constructs a new EmbeddingIngester instance.
     */
    public EmbeddingIngester() {
        // Default constructor
    }

    /**
     * Registers the crawler metadata mapping that routes the extractor's X-FESS-Embedding
     * header into the staging document field.
     */
    @PostConstruct
    public void init() {
        ComponentUtil.getFessConfig()
                .addCrawlerMetadataNameMapping(X_FESS_EMBEDDING, MultiModalConstants.EMBEDDING_STAGING_FIELD, MAPPING_TYPE_ARRAY, EMPTY);
        if (logger.isDebugEnabled()) {
            logger.debug("staging field: {}", MultiModalConstants.EMBEDDING_STAGING_FIELD);
        }
    }

    @Override
    protected Map<String, Object> process(final Map<String, Object> target) {
        final Object staged = target.remove(MultiModalConstants.EMBEDDING_STAGING_FIELD);
        if (staged == null) {
            return target;
        }
        if (!(staged instanceof final String[] encodedEmbeddings) || encodedEmbeddings.length == 0) {
            logger.warn("{} is not a non-empty array.", MultiModalConstants.EMBEDDING_STAGING_FIELD);
            return target;
        }

        final float[] embedding = EmbeddingUtil.decodeFloatArray(encodedEmbeddings[0]);
        if (embedding == null || embedding.length == 0) {
            logger.warn("Failed to decode the image embedding.");
            return target;
        }

        // ArrayList/HashMap and a raw float[] only. List.of/Map.of/Float[] are not
        // registered with the crawler's Kryo serializer and would drop the whole document
        // on any code path that serializes this map.
        final Map<String, Object> vectorEntry = new HashMap<>();
        vectorEntry.put(ChunkVectorHelper.VECTOR_SUBFIELD, embedding);
        final List<Map<String, Object>> vectorList = new ArrayList<>(1);
        vectorList.add(vectorEntry);

        target.put(Constants.CONTENT_CHUNK_VECTOR_FIELD, vectorList);
        target.put(Constants.CONTENT_CHUNK_STATUS_FIELD, Constants.DONE);
        if (logger.isDebugEnabled()) {
            logger.debug("Wrote an image vector. dimension={}", embedding.length);
        }
        return target;
    }
}
