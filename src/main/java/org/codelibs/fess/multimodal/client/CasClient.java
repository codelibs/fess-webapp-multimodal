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

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.multimodal.exception.CasAccessException;
import org.codelibs.fess.multimodal.MultiModalConstants;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.NamedXContentRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Client for communicating with CAS (CLIP as Service) server to generate embeddings for images and text.
 * This client handles image preprocessing, encoding, and API communication with the CLIP server.
 */
public class CasClient {
    private static final Logger logger = LogManager.getLogger(CasClient.class);

    /**
     * Constructs a new CasClient instance.
     */
    public CasClient() {
        // Default constructor
    }

    /** Response parser function to convert JSON responses from the CLIP server into Map objects. */
    protected static final Function<CurlResponse, Map<String, Object>> PARSER = response -> {
        try (InputStream is = response.getContentAsStream()) {
            return JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, LoggingDeprecationHandler.INSTANCE, is).map();
        } catch (final Exception e) {
            throw new CurlException("Failed to access the content.", e);
        }
    };

    /** Target width for resized images sent to CLIP server. */
    protected int imageWidth;

    /** Target height for resized images sent to CLIP server. */
    protected int imageHeight;

    /** Maximum allowed width for input images before rejection. */
    protected int maxImageWidth;

    /** Maximum allowed height for input images before rejection. */
    protected int maxImageHeight;

    /** Format for encoding images (e.g., png, jpg). */
    protected String imageFormat;

    /** CLIP server endpoint URL. */
    protected String clipEndpoint;

    /**
     * Initializes the CAS client with configuration parameters from system properties.
     * Sets up image dimensions, format, and CLIP server endpoint.
     */
    @PostConstruct
    public void init() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        imageWidth = getIntProperty(fessConfig, MultiModalConstants.CLIP_IMAGE_WIDTH, 224);
        imageHeight = getIntProperty(fessConfig, MultiModalConstants.CLIP_IMAGE_HEIGHT, 224);
        maxImageWidth = getIntProperty(fessConfig, MultiModalConstants.CLIP_IMAGE_MAX_WIDTH, 3000);
        maxImageHeight = getIntProperty(fessConfig, MultiModalConstants.CLIP_IMAGE_MAX_HEIGHT, 2000);
        imageFormat = fessConfig.getSystemProperty(MultiModalConstants.CLIP_IMAGE_FORMAT, "png");
        clipEndpoint = fessConfig.getSystemProperty(MultiModalConstants.CLIP_API_URL, MultiModalConstants.DEFAULT_CLIP_API_URL);

        if (logger.isDebugEnabled()) {
            logger.debug("image: {}x{}, max: {}x{}, format: {}, endpoint: {}", imageWidth, imageHeight, maxImageWidth, maxImageHeight,
                    imageFormat, clipEndpoint);
        }
    }

    /**
     * Reads an int system property, falling back to the default when unset or unparsable.
     *
     * @param fessConfig the config accessor
     * @param key the system property key
     * @param defaultValue the fallback
     * @return the resolved value
     */
    protected int getIntProperty(final FessConfig fessConfig, final String key, final int defaultValue) {
        final String value = fessConfig.getSystemProperty(key, null);
        if (StringUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            logger.warn("Invalid {} value: {}. Using {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns the configured CLIP server base URL.
     *
     * @return the base URL
     */
    public String getClipEndpoint() {
        return clipEndpoint;
    }

    /**
     * Generates an embedding vector for the given image.
     *
     * @param in input stream containing the image data
     * @return float array representing the image embedding
     * @throws CasAccessException if the embedding generation fails
     */
    public float[] getImageEmbedding(final InputStream in) {
        return sendImage(encodeImage(in));
    }

    /**
     * Sends a base64-encoded image to the CLIP server and retrieves the embedding.
     *
     * @param encodedImage base64-encoded image data
     * @return float array representing the image embedding
     * @throws CasAccessException if the server communication fails
     */
    protected float[] sendImage(final String encodedImage) {
        final String body = CasProtocol.buildBlobRequest(encodedImage);
        if (logger.isDebugEnabled()) {
            logger.debug("request body length: {}", body.length());
        }
        try (CurlResponse response =
                Curl.post(clipEndpoint + CasProtocol.POST_PATH).header("Content-Type", "application/json").body(body).execute()) {
            // curl4j does not throw on a non-2xx response, so the status has to be checked
            // explicitly -- otherwise an error page is parsed as an empty embedding and the
            // document is indexed silently vectorless.
            final int httpStatusCode = response.getHttpStatusCode();
            if (httpStatusCode < 200 || httpStatusCode >= 300) {
                throw new CasAccessException("Clip server returned HTTP " + httpStatusCode);
            }
            final Map<String, Object> contentMap;
            try {
                contentMap = response.getContent(PARSER);
            } catch (final CurlException e) {
                // PARSER wraps a malformed response body in a CurlException, which is a
                // RuntimeException, not an IOException -- without this it would escape
                // unwrapped instead of surfacing as a CasAccessException. Scoped to just this
                // call so a CurlException thrown by execute() above (e.g. no CLIP server
                // listening) keeps propagating unwrapped, as callers already expect.
                throw new CasAccessException("Clip server failed to generate an embedding.", e);
            }
            return CasProtocol.parseEmbedding(contentMap);
        } catch (final IOException e) {
            throw new CasAccessException("Clip server failed to generate an embedding.", e);
        }
    }

    /**
     * Encodes an image from input stream to base64 format, with resizing and preprocessing.
     * Images are resized to the target dimensions while maintaining aspect ratio.
     *
     * @param in input stream containing the image data
     * @return base64-encoded image string
     * @throws CasAccessException if image processing fails
     */
    protected String encodeImage(final InputStream in) {
        try (ImageInputStream input = ImageIO.createImageInputStream(in)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                final ImageReader reader = readers.next();
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    reader.setInput(input);
                    final ImageReadParam param = reader.getDefaultReadParam();
                    final int width = reader.getWidth(0);
                    final int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || width > maxImageWidth || height > maxImageHeight) {
                        throw new CasAccessException("Invalid image size: " + width + "x" + height);
                    }

                    final float aspectRatio = (float) width / height;
                    int newWidth = imageWidth;
                    int newHeight = imageHeight;
                    if (aspectRatio > 1) {
                        newHeight = (int) (imageWidth / aspectRatio);
                    } else {
                        newWidth = (int) (imageHeight * aspectRatio);
                    }

                    final int samplingWidth = width / newWidth;
                    final int samplingHeight = height / newHeight;
                    param.setSourceSubsampling(samplingWidth <= 0 ? 1 : samplingWidth, samplingHeight <= 0 ? 1 : samplingHeight, 0, 0);
                    param.setSourceRegion(new Rectangle(width, height));

                    final BufferedImage image = reader.read(0, param);
                    final BufferedImage clipImage = new BufferedImage(imageWidth, imageHeight, image.getType());
                    final int x = (imageWidth - newWidth) / 2;
                    final int y = (imageHeight - newHeight) / 2;
                    clipImage.getGraphics()
                            .drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING), x, y, newWidth, newHeight,
                                    null);
                    ImageIO.write(clipImage, imageFormat, out);
                    image.flush();
                    return Base64.getEncoder().encodeToString(out.toByteArray());
                } finally {
                    reader.dispose();
                }
            }
            throw new CasAccessException("No image.");
        } catch (final CasAccessException e) {
            throw e;
        } catch (final IOException e) {
            throw new CasAccessException("Failed to read an image.", e);
        }
    }
}
