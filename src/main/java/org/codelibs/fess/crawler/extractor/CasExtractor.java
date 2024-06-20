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
package org.codelibs.fess.crawler.extractor;

import java.io.InputStream;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.MultiModalConstants;
import org.codelibs.fess.client.CasClient;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.crawler.extractor.impl.TikaExtractor;
import org.codelibs.fess.ingest.EmbeddingIngester;
import org.codelibs.fess.util.EmbeddingUtil;

public class CasExtractor extends TikaExtractor {

    private static final Logger logger = LogManager.getLogger(EmbeddingIngester.class);

    protected CasClient client;

    @Override
    public int getWeight() {
        return 10;
    }

    @Override
    @PostConstruct
    public void init() {
        super.init();

        client = crawlerContainer.getComponent("casClient");
    }

    @Override
    public ExtractData getText(final InputStream inputStream, final Map<String, String> params) {
        return getText(inputStream, params, (data, in) -> {
            try {
                data.putValue(MultiModalConstants.X_FESS_EMBEDDING, EmbeddingUtil.encodeFloatArray(client.getImageEmbedding(in)));
            } catch (final Exception e) {
                logger.warn("Failed to convert an image to a vector.", e);
            }
        });
    }

}
