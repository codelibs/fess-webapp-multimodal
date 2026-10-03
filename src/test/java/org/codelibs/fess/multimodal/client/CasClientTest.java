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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.codelibs.core.io.ResourceUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.fess.multimodal.crawler.extractor.CasExtractorTest;
import org.codelibs.fess.multimodal.exception.CasAccessException;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class CasClientTest extends UnitWebappTestCase {
    static final Logger logger = Logger.getLogger(CasExtractorTest.class.getName());

    private void setUpDefaultMockConfig() {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                return defaultValue;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
    }

    private void tearDownMockConfig() {
        ComponentUtil.setFessConfig(null);
    }

    @Test
    public void test_encodeImage() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();
            try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
                final String data = client.encodeImage(in);
                assertEquals(70804, data.length());
                // FileUtil.writeBytes("test.png", Base64.getDecoder().decode(data));
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_getImageEmbedding() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();
            try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
                final float[] embedding = client.getImageEmbedding(in);
                assertEquals(512, embedding.length);
            } catch (final CurlException e) {
                logger.warning(e.getMessage());
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_init_setsDefaultValues() {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                // Return defaultValue to force use of defaults
                return defaultValue;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            final CasClient client = new CasClient();
            client.init();

            assertEquals(224, client.getImageWidth());
            assertEquals(224, client.getImageHeight());
            assertEquals(3000, client.getMaxImageWidth());
            assertEquals(2000, client.getMaxImageHeight());
            assertEquals("png", client.getImageFormat());
            assertEquals("http://localhost:51000", client.getClipEndpoint());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_init_readsSystemProperties() {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                if (MultiModalConstants.CLIP_IMAGE_WIDTH.equals(key)) {
                    return "512";
                } else if (MultiModalConstants.CLIP_IMAGE_HEIGHT.equals(key)) {
                    return "512";
                } else if (MultiModalConstants.CLIP_IMAGE_MAX_WIDTH.equals(key)) {
                    return "5000";
                } else if (MultiModalConstants.CLIP_IMAGE_MAX_HEIGHT.equals(key)) {
                    return "4000";
                } else if (MultiModalConstants.CLIP_IMAGE_FORMAT.equals(key)) {
                    return "jpg";
                } else if (MultiModalConstants.CLIP_API_URL.equals(key)) {
                    return "http://localhost:8080";
                }
                return defaultValue;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            final CasClient client = new CasClient();
            client.init();

            assertEquals(512, client.getImageWidth());
            assertEquals(512, client.getImageHeight());
            assertEquals(5000, client.getMaxImageWidth());
            assertEquals(4000, client.getMaxImageHeight());
            assertEquals("jpg", client.getImageFormat());
            assertEquals("http://localhost:8080", client.getClipEndpoint());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_encodeImage_nullInputStream_throwsException() {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();

            try {
                client.encodeImage(null);
                fail("Expected exception for null input stream");
            } catch (final IllegalArgumentException e) {
                // Expected - ImageIO.createImageInputStream throws IllegalArgumentException for null
                assertTrue(e.getMessage().contains("input == null"));
            } catch (final CasAccessException e) {
                // Also acceptable if wrapped in CasAccessException
                assertTrue(e.getMessage().contains("Failed to read an image") || e.getMessage().contains("No image"));
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_encodeImage_emptyInputStream_throwsException() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();

            try (InputStream in = new ByteArrayInputStream(new byte[0])) {
                client.encodeImage(in);
                fail("Expected CasAccessException for empty input stream");
            } catch (final CasAccessException e) {
                // Expected
                assertTrue(e.getMessage().contains("No image") || e.getMessage().contains("Failed to read"));
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_encodeImage_invalidImageData_throwsException() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();

            try (InputStream in = new ByteArrayInputStream("not an image".getBytes())) {
                client.encodeImage(in);
                fail("Expected CasAccessException for invalid image data");
            } catch (final CasAccessException e) {
                // Expected
                assertTrue(e.getMessage().contains("No image") || e.getMessage().contains("Failed to read"));
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_encodeImage_imageTooLarge_throwsException() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient() {
                @Override
                protected int getMaxImageWidth() {
                    return 100;
                }

                @Override
                protected int getMaxImageHeight() {
                    return 100;
                }
            };
            client.init();

            try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
                client.encodeImage(in);
                fail("Expected CasAccessException for image too large");
            } catch (final CasAccessException e) {
                // Expected
                assertTrue(e.getMessage().contains("Invalid image size"));
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_encodeImage_differentAspectRatios() throws Exception {
        setUpDefaultMockConfig();
        try {
            final CasClient client = new CasClient();
            client.init();

            try (InputStream in = ResourceUtil.getResourceAsStream("images/codelibs_cover.jpeg")) {
                final String data = client.encodeImage(in);
                assertNotNull(data);
                assertTrue(data.length() > 0);
            }
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_sendImage_validBase64_returnsEmbedding() {
        setUpDefaultMockConfig();
        try {
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
        } finally {
            tearDownMockConfig();
        }
    }

    @Test
    public void test_sendImage_non2xxStatus_throwsCasAccessException() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext(CasProtocol.POST_PATH, exchange -> {
                final byte[] body = "{\"message\":\"internal error\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpDefaultMockConfig();
            try {
                final CasClient client = new CasClient() {
                    @Override
                    public String getClipEndpoint() {
                        return "http://127.0.0.1:" + server.getAddress().getPort();
                    }
                };
                client.init();

                try {
                    client.sendImage("QUJD");
                    fail("CasAccessException is expected.");
                } catch (final CasAccessException e) {
                    assertTrue("message should mention the HTTP status", e.getMessage().contains("500"));
                }
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_sendImage_2xxStatus_returnsParsedEmbedding() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext(CasProtocol.POST_PATH, exchange -> {
                final byte[] body = "{\"data\":[{\"embedding\":[0.5,1.5,-2.0]}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpDefaultMockConfig();
            try {
                final CasClient client = new CasClient() {
                    @Override
                    public String getClipEndpoint() {
                        return "http://127.0.0.1:" + server.getAddress().getPort();
                    }
                };
                client.init();

                final float[] embedding = client.sendImage("QUJD");
                assertEquals(3, embedding.length);
                assertEquals(0.5f, embedding[0]);
                assertEquals(1.5f, embedding[1]);
                assertEquals(-2.0f, embedding[2]);
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_sendImage_2xxStatus_malformedJson_throwsCasAccessException() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext(CasProtocol.POST_PATH, exchange -> {
                // A 2xx status with a body that is not valid JSON at all: the PARSER lambda in
                // CasClient wraps the parse failure in a CurlException (a RuntimeException, not
                // an IOException), which previously escaped sendImage() unwrapped instead of
                // surfacing as a CasAccessException.
                final byte[] body = "not json".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpDefaultMockConfig();
            try {
                final CasClient client = new CasClient() {
                    @Override
                    public String getClipEndpoint() {
                        return "http://127.0.0.1:" + server.getAddress().getPort();
                    }
                };
                client.init();

                try {
                    client.sendImage("QUJD");
                    fail("CasAccessException is expected.");
                } catch (final CasAccessException e) {
                    // Expected: the CurlException from the malformed JSON must be wrapped.
                } catch (final CurlException e) {
                    fail("A malformed JSON body must be wrapped in a CasAccessException, not surfaced as a raw CurlException: "
                            + e.getMessage());
                }
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_followsSystemPropertyChangedAfterInit() {
        final String[] endpoint = { "http://clip1.example.com:51000" };
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                if (MultiModalConstants.CLIP_API_URL.equals(key)) {
                    return endpoint[0];
                }
                return defaultValue;
            }
        });
        try {
            final CasClient client = new CasClient();
            client.init();
            assertEquals("http://clip1.example.com:51000", client.getClipEndpoint());

            // A change of system.properties is seen without re-initializing the client.
            endpoint[0] = "http://clip2.example.com:51000";
            assertEquals("http://clip2.example.com:51000", client.getClipEndpoint());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_init_readsSystemPropertyChannel() {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                if (MultiModalConstants.CLIP_API_URL.equals(key)) {
                    return "http://clip.example.com:51000";
                } else if (MultiModalConstants.CLIP_IMAGE_WIDTH.equals(key)) {
                    return "336";
                }
                return defaultValue;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            final CasClient client = new CasClient();
            client.init();
            assertEquals("http://clip.example.com:51000", client.getClipEndpoint());
            assertEquals(336, client.getImageWidth());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_init_defaults() {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                // Return defaultValue to force use of defaults
                return defaultValue;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            final CasClient client = new CasClient();
            client.init();
            assertEquals("http://localhost:51000", client.getClipEndpoint());
            assertEquals(224, client.getImageWidth());
            assertEquals(224, client.getImageHeight());
            assertEquals(3000, client.getMaxImageWidth());
            assertEquals(2000, client.getMaxImageHeight());
            assertEquals("png", client.getImageFormat());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }
}
