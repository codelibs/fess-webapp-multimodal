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
package org.codelibs.fess.multimodal.embedding;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.embedding.AbstractEmbeddingClient;
import org.codelibs.fess.embedding.EmbeddingException;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.codelibs.fess.multimodal.client.CasProtocol;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class ClipEmbeddingClientTest extends UnitWebappTestCase {

    @Test
    public void test_getName() {
        assertEquals("clip", new ClipEmbeddingClient().getName());
    }

    @Test
    public void test_getConfigPrefix() {
        assertEquals("content_chunker.embedding.clip", new ClipEmbeddingClient().getConfigPrefix());
    }

    /** Every text is embedded independently; the CLIP server has no batch endpoint. */
    @Test
    public void test_embedDocuments_callsPerText() {
        final List<String> seen = new ArrayList<>();
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                seen.add(text);
                return new float[] { 1.0f, 2.0f };
            }
        };

        final List<String> texts = new ArrayList<>();
        texts.add("a");
        texts.add("b");
        final List<float[]> vectors = client.embedDocuments(texts);

        assertEquals(2, vectors.size());
        assertEquals(2, seen.size());
        assertEquals("a", seen.get(0));
        assertEquals("b", seen.get(1));
        assertEquals(1.0f, vectors.get(0)[0]);
    }

    @Test
    public void test_embedDocuments_emptyInput() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                fail("embedText must not be called for an empty input.");
                return null;
            }
        };
        assertEquals(0, client.embedDocuments(new ArrayList<>()).size());
        assertEquals(0, client.embedDocuments(null).size());
    }

    @Test
    public void test_embedQuery_delegatesToEmbedText() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected float[] embedText(final String text) {
                return new float[] { text.length() };
            }
        };
        final List<String> texts = new ArrayList<>();
        texts.add("abc");
        final List<float[]> vectors = client.embedQuery(texts);
        assertEquals(1, vectors.size());
        assertEquals(3.0f, vectors.get(0)[0]);
    }

    /** checkAvailabilityNow() runs inside init(); a throw there would stop container startup. */
    @Test
    public void test_checkAvailabilityNow_failsClosed() {
        final ClipEmbeddingClient client = new ClipEmbeddingClient() {
            @Override
            protected String getApiUrl() {
                throw new RuntimeException("boom");
            }
        };
        assertFalse(client.checkAvailabilityNow());
    }

    // ========== Real HTTP path coverage (HttpServer loopback stub, mirroring CasClientTest) ==========
    //
    // The tests above bypass embedText()/getApiUrl() via anonymous subclasses and never exercise
    // the actual HTTP call in checkAvailabilityNow() (the /health probe) or in embedText() itself
    // (status-code handling, JSON parsing, the getDimension() mismatch check). The tests below
    // cover that real path with a JDK HttpServer loopback stub, the same technique CasClientTest
    // uses for CasClient.sendImage().

    /**
     * Stubs {@link ComponentUtil#getFessConfig()} so the client resolves to a real
     * {@link java.net.http}-backed HTTP call against the given loopback base URL, with the given
     * vector dimension. Must be paired with {@link #tearDownMockConfig()} in a {@code finally}
     * block so the stub does not leak into later test classes.
     *
     * @param apiUrl the CLIP server base URL (e.g. {@code http://127.0.0.1:<port>})
     * @param dimension the value {@code content_chunker.embedding.dimension} should report
     */
    private void setUpMockConfig(final String apiUrl, final int dimension) {
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getSystemProperty(final String key, final String defaultValue) {
                if (AbstractEmbeddingClient.EMBEDDING_NAME_PROPERTY.equals(key)) {
                    // AbstractEmbeddingClient#init() no-ops unless this equals getName(), which
                    // would leave httpClient null and NPE on the first getHttpClient() call.
                    return ClipEmbeddingClient.NAME;
                } else if (AbstractEmbeddingClient.EMBEDDING_DIMENSION_PROPERTY.equals(key)) {
                    return String.valueOf(dimension);
                } else if (MultiModalConstants.CLIP_API_URL.equals(key)) {
                    return apiUrl;
                }
                return defaultValue;
            }

            @Override
            public String getHttpProxyHost() {
                // The real FessConfig.SimpleImpl delegates to an ObjectiveConfig that is never
                // initialized outside a real container and NPEs; AbstractEmbeddingClient.
                // configureProxy() calls this unconditionally from buildHttpClient(), which
                // getHttpClient() reaches from both checkAvailabilityNow() and embedText().
                return null;
            }

            @Override
            public Integer getHttpProxyPortAsInteger() {
                // Same NPE-avoidance as getHttpProxyHost() above; both are called unconditionally
                // by configureProxy() before it checks whether a proxy is actually configured.
                return null;
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
    }

    private void tearDownMockConfig() {
        ComponentUtil.setFessConfig(null);
    }

    // ---- checkAvailabilityNow(): real /health call ----

    @Test
    public void test_checkAvailabilityNow_health2xx_returnsTrue() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/health", exchange -> {
                final byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpMockConfig("http://127.0.0.1:" + server.getAddress().getPort(), 3);
            try {
                final ClipEmbeddingClient client = new ClipEmbeddingClient();
                assertTrue(client.checkAvailabilityNow());
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_checkAvailabilityNow_healthNon2xx_returnsFalse() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/health", exchange -> {
                final byte[] body = "{\"status\":\"unavailable\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpMockConfig("http://127.0.0.1:" + server.getAddress().getPort(), 3);
            try {
                final ClipEmbeddingClient client = new ClipEmbeddingClient();
                assertFalse(client.checkAvailabilityNow());
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_checkAvailabilityNow_connectionRefused_returnsFalse() throws Exception {
        // fail-closed: nothing listens on this port (bound then immediately released).
        final int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        setUpMockConfig("http://127.0.0.1:" + closedPort, 3);
        try {
            final ClipEmbeddingClient client = new ClipEmbeddingClient();
            assertFalse(client.checkAvailabilityNow());
        } finally {
            tearDownMockConfig();
        }
    }

    // ---- embedText() (invoked through embedQuery()): real /post call ----

    @Test
    public void test_embedText_success_returnsVectorMatchingDimension() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext(CasProtocol.POST_PATH, exchange -> {
                final byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpMockConfig("http://127.0.0.1:" + server.getAddress().getPort(), 3);
            try {
                final ClipEmbeddingClient client = new ClipEmbeddingClient();
                final List<String> texts = new ArrayList<>();
                texts.add("running dogs");
                final List<float[]> vectors = client.embedQuery(texts);

                assertEquals(1, vectors.size());
                assertEquals(3, vectors.get(0).length);
                assertEquals(0.1f, vectors.get(0)[0]);
                assertEquals(0.2f, vectors.get(0)[1]);
                assertEquals(0.3f, vectors.get(0)[2]);
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void test_embedText_non2xxStatus_throwsEmbeddingException() throws Exception {
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

            setUpMockConfig("http://127.0.0.1:" + server.getAddress().getPort(), 3);
            try {
                final ClipEmbeddingClient client = new ClipEmbeddingClient();
                final List<String> texts = new ArrayList<>();
                texts.add("running dogs");
                try {
                    client.embedQuery(texts);
                    fail("EmbeddingException is expected.");
                } catch (final EmbeddingException e) {
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
    public void test_embedText_dimensionMismatch_throwsEmbeddingException() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext(CasProtocol.POST_PATH, exchange -> {
                // Server returns a 2-dimensional vector while config declares dimension=3.
                final byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2]}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();

            setUpMockConfig("http://127.0.0.1:" + server.getAddress().getPort(), 3);
            try {
                final ClipEmbeddingClient client = new ClipEmbeddingClient();
                final List<String> texts = new ArrayList<>();
                texts.add("running dogs");
                try {
                    client.embedQuery(texts);
                    fail("EmbeddingException is expected.");
                } catch (final EmbeddingException e) {
                    assertTrue("message should mention expected dimension", e.getMessage().contains("expected=3"));
                    assertTrue("message should mention actual dimension", e.getMessage().contains("actual=2"));
                }
            } finally {
                tearDownMockConfig();
            }
        } finally {
            server.stop(0);
        }
    }
}
