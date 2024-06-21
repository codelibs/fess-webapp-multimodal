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
package org.codelibs.fess.multimodal.crawler.extractor;

import java.io.InputStream;
import java.util.logging.Logger;

import org.codelibs.core.io.CloseableUtil;
import org.codelibs.core.io.ResourceUtil;
import org.codelibs.fess.crawler.container.StandardCrawlerContainer;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.crawler.helper.impl.MimeTypeHelperImpl;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.client.CasClient;
import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author shinsuke
 *
 */
public class CasExtractorTest extends PlainTestCase {
    static final Logger logger = Logger.getLogger(CasExtractorTest.class.getName());

    public CasExtractor casExtractor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final StandardCrawlerContainer container = new StandardCrawlerContainer();
        container//
                .singleton("mimeTypeHelper", MimeTypeHelperImpl.class)//
                .singleton("casExtractor", CasExtractor.class)//
                .singleton("casClient", new CasClient() {
                    @Override
                    public float[] getImageEmbedding(final InputStream in) {
                        return new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
                    }
                })//
        ;

        casExtractor = container.getComponent("casExtractor");
        casExtractor.init();
    }

    public void test_getTika() {
        final InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg");
        final ExtractData extractData = casExtractor.getText(in, null);
        final String content = extractData.getContent();
        CloseableUtil.closeQuietly(in);
        assertEquals(0, content.length());
        final String[] values = extractData.getValues(MultiModalConstants.X_FESS_EMBEDDING);
        assertEquals(1, values.length);
        final float[] embedding = EmbeddingUtil.decodeFloatArray(values[0]);
        assertEquals(5, embedding.length);
    }
}
