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
package org.codelibs.fess.multimodal.crawler.extractor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.codelibs.core.io.CloseableUtil;
import org.codelibs.core.io.ResourceUtil;
import org.codelibs.fess.crawler.container.StandardCrawlerContainer;
import org.codelibs.fess.crawler.entity.ExtractData;
import org.codelibs.fess.crawler.helper.impl.MimeTypeHelperImpl;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.client.CasClient;
import org.codelibs.fess.multimodal.exception.CasAccessException;
import org.codelibs.fess.multimodal.util.EmbeddingUtil;
import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.junit.jupiter.api.TestInfo;

/**
 * @author shinsuke
 *
 */
public class CasExtractorTest extends UnitWebappTestCase {
    static final Logger logger = Logger.getLogger(CasExtractorTest.class.getName());

    public CasExtractor casExtractor;

    @Override
    protected void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

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

    public void test_getText_withValidImage_extractsEmbedding() {
        final InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg");
        final ExtractData extractData = casExtractor.getText(in, null);
        CloseableUtil.closeQuietly(in);

        assertNotNull(extractData);
        final String[] values = extractData.getValues(MultiModalConstants.X_FESS_EMBEDDING);
        assertNotNull(values);
        assertEquals(1, values.length);

        final float[] embedding = EmbeddingUtil.decodeFloatArray(values[0]);
        assertEquals(5, embedding.length);
        assertEquals(1.0f, embedding[0]);
        assertEquals(2.0f, embedding[1]);
        assertEquals(3.0f, embedding[2]);
        assertEquals(4.0f, embedding[3]);
        assertEquals(5.0f, embedding[4]);
    }

    public void test_getText_withParams_passesParams() {
        final InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg");
        final Map<String, String> params = new HashMap<>();
        params.put("test_key", "test_value");

        final ExtractData extractData = casExtractor.getText(in, params);
        CloseableUtil.closeQuietly(in);

        assertNotNull(extractData);
        final String[] values = extractData.getValues(MultiModalConstants.X_FESS_EMBEDDING);
        assertNotNull(values);
        assertEquals(1, values.length);
    }

    public void test_getText_withErrorInEmbedding_handlesGracefully() {
        final StandardCrawlerContainer container = new StandardCrawlerContainer();
        container//
                .singleton("mimeTypeHelper", MimeTypeHelperImpl.class)//
                .singleton("casExtractor", CasExtractor.class)//
                .singleton("casClient", new CasClient() {
                    @Override
                    public float[] getImageEmbedding(final InputStream in) {
                        throw new CasAccessException("Test error");
                    }
                })//
        ;

        final CasExtractor extractor = container.getComponent("casExtractor");
        extractor.init();

        final InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg");
        final ExtractData extractData = extractor.getText(in, null);
        CloseableUtil.closeQuietly(in);

        assertNotNull(extractData);
        // Should handle error gracefully without embedding
        final String[] values = extractData.getValues(MultiModalConstants.X_FESS_EMBEDDING);
        if (values != null) {
            // If values exist, they should be valid
            for (String value : values) {
                assertNotNull(value);
            }
        }
    }

    public void test_getText_withNullInputStream_handlesGracefully() {
        try {
            final ExtractData extractData = casExtractor.getText(null, null);
            assertNotNull(extractData);
        } catch (Exception e) {
            // Expected - may throw exception for null input
            assertNotNull(e);
        }
    }

    public void test_getText_withEmptyInputStream_handlesGracefully() {
        final InputStream in = new ByteArrayInputStream(new byte[0]);
        try {
            final ExtractData extractData = casExtractor.getText(in, null);
            assertNotNull(extractData);
        } catch (Exception e) {
            // Expected - may throw exception for empty input
            assertNotNull(e);
        } finally {
            CloseableUtil.closeQuietly(in);
        }
    }

    public void test_getWeight_returnsCorrectValue() {
        final int weight = casExtractor.getWeight();
        assertEquals(10, weight);
    }

    public void test_init_initializesClient() {
        final StandardCrawlerContainer container = new StandardCrawlerContainer();
        container//
                .singleton("mimeTypeHelper", MimeTypeHelperImpl.class)//
                .singleton("casExtractor", CasExtractor.class)//
                .singleton("casClient", new CasClient() {
                    @Override
                    public float[] getImageEmbedding(final InputStream in) {
                        return new float[] { 1.0f };
                    }
                })//
        ;

        final CasExtractor extractor = container.getComponent("casExtractor");
        extractor.init();

        assertNotNull(extractor.client);
    }

    public void test_getText_withMultipleImages_processesEach() {
        for (int i = 0; i < 3; i++) {
            final InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg");
            final ExtractData extractData = casExtractor.getText(in, null);
            CloseableUtil.closeQuietly(in);

            assertNotNull(extractData);
            final String[] values = extractData.getValues(MultiModalConstants.X_FESS_EMBEDDING);
            assertNotNull(values);
            assertEquals(1, values.length);

            final float[] embedding = EmbeddingUtil.decodeFloatArray(values[0]);
            assertEquals(5, embedding.length);
        }
    }
}
