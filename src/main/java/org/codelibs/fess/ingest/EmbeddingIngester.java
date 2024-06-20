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
package org.codelibs.fess.ingest;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.MultiModalConstants;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.EmbeddingUtil;

public class EmbeddingIngester extends Ingester {
    private static final Logger logger = LogManager.getLogger(EmbeddingIngester.class);

    protected String embeddingField;

    @PostConstruct
    public void init() {
        embeddingField = System.getProperty("clip.index.embedding_field", "content_vector");

        ComponentUtil.getFessConfig().addCrawlerMetadataNameMapping(MultiModalConstants.X_FESS_EMBEDDING, embeddingField,
                Constants.MAPPING_TYPE_ARRAY, StringUtil.EMPTY);
    }

    @Override
    protected Map<String, Object> process(final Map<String, Object> target) {
        if (target.containsKey(embeddingField)) {
            logger.debug("[{}] : {}", embeddingField, target);
            if (target.get(embeddingField) instanceof final String[] encodedEmbeddings) {
                final float[] embedding = EmbeddingUtil.decodeFloatArray(encodedEmbeddings[0]);
                logger.debug("embedding:{}", embedding);
                target.put(embeddingField, embedding);
            } else {
                logger.warn("{} is not an array.", embeddingField);
            }
        }
        return target;
    }
}
