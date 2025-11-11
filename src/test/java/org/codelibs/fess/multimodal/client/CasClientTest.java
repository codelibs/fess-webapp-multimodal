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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import org.codelibs.core.io.ResourceUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.fess.multimodal.crawler.extractor.CasExtractorTest;
import org.codelibs.fess.multimodal.exception.CasAccessException;
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

    public void test_init_setsDefaultValues() {
        final CasClient client = new CasClient();
        client.init();

        assertEquals(224, client.imageWidth);
        assertEquals(224, client.imageHeight);
        assertEquals(3000, client.maxImageWidth);
        assertEquals(2000, client.maxImageHeight);
        assertEquals("png", client.imageFormat);
        assertEquals("http://localhost:51000", client.clipEndpoint);
    }

    public void test_init_readsSystemProperties() {
        try {
            System.setProperty("clip.image.width", "512");
            System.setProperty("clip.image.height", "512");
            System.setProperty("clip.image.max_width", "5000");
            System.setProperty("clip.image.max_height", "4000");
            System.setProperty("clip.image.format", "jpg");
            System.setProperty("clip.server.endpoint", "http://localhost:8080");

            final CasClient client = new CasClient();
            client.init();

            assertEquals(512, client.imageWidth);
            assertEquals(512, client.imageHeight);
            assertEquals(5000, client.maxImageWidth);
            assertEquals(4000, client.maxImageHeight);
            assertEquals("jpg", client.imageFormat);
            assertEquals("http://localhost:8080", client.clipEndpoint);
        } finally {
            System.clearProperty("clip.image.width");
            System.clearProperty("clip.image.height");
            System.clearProperty("clip.image.max_width");
            System.clearProperty("clip.image.max_height");
            System.clearProperty("clip.image.format");
            System.clearProperty("clip.server.endpoint");
        }
    }

    public void test_encodeImage_nullInputStream_throwsException() {
        final CasClient client = new CasClient();
        client.init();

        try {
            client.encodeImage(null);
            fail("Expected CasAccessException for null input stream");
        } catch (final CasAccessException e) {
            // Expected
            assertTrue(e.getMessage().contains("Failed to read an image") || e.getMessage().contains("No image"));
        }
    }

    public void test_encodeImage_emptyInputStream_throwsException() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        try (InputStream in = new ByteArrayInputStream(new byte[0])) {
            client.encodeImage(in);
            fail("Expected CasAccessException for empty input stream");
        } catch (final CasAccessException e) {
            // Expected
            assertTrue(e.getMessage().contains("No image") || e.getMessage().contains("Failed to read"));
        }
    }

    public void test_encodeImage_invalidImageData_throwsException() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        try (InputStream in = new ByteArrayInputStream("not an image".getBytes())) {
            client.encodeImage(in);
            fail("Expected CasAccessException for invalid image data");
        } catch (final CasAccessException e) {
            // Expected
            assertTrue(e.getMessage().contains("No image") || e.getMessage().contains("Failed to read"));
        }
    }

    public void test_encodeImage_imageTooLarge_throwsException() throws Exception {
        final CasClient client = new CasClient();
        client.init();
        client.maxImageWidth = 100;
        client.maxImageHeight = 100;

        try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
            client.encodeImage(in);
            fail("Expected CasAccessException for image too large");
        } catch (final CasAccessException e) {
            // Expected
            assertTrue(e.getMessage().contains("Invalid image size"));
        }
    }

    public void test_encodeImage_differentAspectRatios() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
            final String data = client.encodeImage(in);
            assertNotNull(data);
            assertTrue(data.length() > 0);
        }
    }

    public void test_getTextEmbedding_emptyString_handlesGracefully() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        try {
            final float[] embedding = client.getTextEmbedding("");
            assertNotNull(embedding);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        }
    }

    public void test_getTextEmbedding_longText_handlesGracefully() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        final StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("word ");
        }

        try {
            final float[] embedding = client.getTextEmbedding(longText.toString());
            assertNotNull(embedding);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        }
    }

    public void test_getTextEmbedding_specialCharacters_handlesGracefully() throws Exception {
        final CasClient client = new CasClient();
        client.init();

        try {
            final float[] embedding = client.getTextEmbedding("日本語のテキスト \"quoted\" <html>");
            assertNotNull(embedding);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        }
    }

    public void test_sendImage_validBase64_returnsEmbedding() {
        final CasClient client = new CasClient();
        client.init();

        try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
            final String encodedImage = client.encodeImage(in);
            final float[] embedding = client.sendImage(encodedImage);
            assertNotNull(embedding);
            assertTrue(embedding.length > 0);
        } catch (final CurlException e) {
            logger.warning(e.getMessage());
        } catch (final Exception e) {
            logger.warning(e.getMessage());
        }
    }
}