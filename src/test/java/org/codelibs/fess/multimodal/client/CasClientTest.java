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

import java.io.InputStream;
import java.util.logging.Logger;

import org.codelibs.core.io.ResourceUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.fess.multimodal.crawler.extractor.CasExtractorTest;
import org.dbflute.utflute.core.PlainTestCase;

public class CasClientTest extends PlainTestCase {
    static final Logger logger = Logger.getLogger(CasExtractorTest.class.getName());

    public void test_encodeImage() throws Exception {
        final CasClient client = new CasClient();
        client.init();
        try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
            final String data = client.encodeImage(in);
            assertEquals(70804, data.length());
            // FileUtil.writeBytes("test.png", Base64.getDecoder().decode(data));
        }
    }

    public void test_getImageEmbedding() throws Exception {
        final CasClient client = new CasClient();
        client.init();
        try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
            final float[] embedding = client.getImageEmbedding(in);
            assertEquals(512, embedding.length);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        }
    }

    public void test_getTextEmbedding() throws Exception {
        final CasClient client = new CasClient();
        client.init();
        try {
            final float[] embedding = client.getTextEmbedding("running dogs");
            assertEquals(512, embedding.length);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        }
    }
}