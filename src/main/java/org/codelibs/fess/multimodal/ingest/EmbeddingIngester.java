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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.ingest.Ingester;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.helper.MultiModalSearchHelper;
import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.annotation.PostConstruct;

/**
 * Ingester that processes embedding data during document indexing.
 * Converts encoded embedding strings to float arrays for vector search operations.
 */
public class EmbeddingIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(EmbeddingIngester.class);

    /**
     * Constructs a new EmbeddingIngester instance.
     */
    public EmbeddingIngester() {
        // Default constructor
    }

    /** The name of the vector field where embeddings are stored. */
    protected String vectorField;

    /**
     * Initializes the ingester by setting up the vector field configuration
     * and registering metadata mappings for embedding data.
     */
    @PostConstruct
    public void init() {
        final MultiModalSearchHelper helper = ComponentUtil.getComponent(MultiModalConstants.HELPER);
        vectorField = helper.getVectorField();
        ComponentUtil.getFessConfig().addCrawlerMetadataNameMapping(X_FESS_EMBEDDING, vectorField, MAPPING_TYPE_ARRAY, EMPTY);
        if (logger.isDebugEnabled()) {
            logger.debug("vector field: {}", vectorField);
        }
    }

    @Override
    protected Map<String, Object> process(final Map<String, Object> target) {
        if (target.containsKey(vectorField)) {
            logger.debug("[{}] : {}", vectorField, target);
            if (target.get(vectorField) instanceof final String[] encodedEmbeddings) {
                final float[] embedding = EmbeddingUtil.decodeFloatArray(encodedEmbeddings[0]);
                logger.debug("embedding:{}", embedding);
                target.put(vectorField, embedding);
            } else {
                logger.warn("{} is not an array.", vectorField);
            }
        }
        return target;
    }
}
