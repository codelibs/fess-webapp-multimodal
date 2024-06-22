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

import static org.codelibs.core.lang.StringUtil.EMPTY;
import static org.codelibs.fess.Constants.MAPPING_TYPE_ARRAY;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_VECTOR_FIELD;
import static org.codelibs.fess.multimodal.MultiModalConstants.X_FESS_EMBEDDING;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.ingest.Ingester;
import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.codelibs.fess.util.ComponentUtil;

public class EmbeddingIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(EmbeddingIngester.class);

    @PostConstruct
    public void init() {

        ComponentUtil.getFessConfig().addCrawlerMetadataNameMapping(X_FESS_EMBEDDING, CONTENT_VECTOR_FIELD, MAPPING_TYPE_ARRAY, EMPTY);
    }

    @Override
    protected Map<String, Object> process(final Map<String, Object> target) {
        if (target.containsKey(CONTENT_VECTOR_FIELD)) {
            logger.debug("[{}] : {}", CONTENT_VECTOR_FIELD, target);
            if (target.get(CONTENT_VECTOR_FIELD) instanceof final String[] encodedEmbeddings) {
                final float[] embedding = EmbeddingUtil.decodeFloatArray(encodedEmbeddings[0]);
                logger.debug("embedding:{}", embedding);
                target.put(CONTENT_VECTOR_FIELD, embedding);
            } else {
                logger.warn("{} is not an array.", CONTENT_VECTOR_FIELD);
            }
        }
        return target;
    }
}
