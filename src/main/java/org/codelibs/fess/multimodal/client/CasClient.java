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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.multimodal.exception.CasAccessException;
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
        imageWidth = Integer.getInteger("clip.image.width", 224);
        imageHeight = Integer.getInteger("clip.image.height", 224);
        maxImageWidth = Integer.getInteger("clip.image.max_width", 3000);
        maxImageHeight = Integer.getInteger("clip.image.max_height", 2000);
        imageFormat = System.getProperty("clip.image.format", "png");
        clipEndpoint = System.getProperty("clip.server.endpoint", "http://localhost:51000");

        logger.debug("image: {}x{}, max: {}x{}, format: {}, endpoint: {}", imageWidth, imageHeight, maxImageWidth, maxImageHeight,
                imageFormat, clipEndpoint);
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
        final String body = "{\"data\":[{\"blob\":\"" + StringEscapeUtils.escapeJson(encodedImage) + "\"}],\"execEndpoint\":\"/\"}";
        logger.debug("request body: {}", body);
        try (CurlResponse response = Curl.post(clipEndpoint + "/post").header("Content-Type", "application/json").body(body).execute()) {
            final Map<String, Object> contentMap = response.getContent(PARSER);
            if (((contentMap.get("data") instanceof final List dataList)
                    && (!dataList.isEmpty() && dataList.get(0) instanceof final Map data))
                    && (data.get("embedding") instanceof final List embeddingList)) {
                logger.debug("embedding: {}", embeddingList);
                final float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = ((Number) embeddingList.get(i)).floatValue();
                }
                return embedding;
            }
        } catch (final IOException e) {
            throw new CasAccessException("Clip server failed to generate an embedding.", e);
        }
        throw new CasAccessException("Clip server cannot generate an embedding");
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

    /**
     * Generates an embedding vector for the given text query.
     *
     * @param query text string to generate embedding for
     * @return float array representing the text embedding
     * @throws CasAccessException if the embedding generation fails
     */
    public float[] getTextEmbedding(final String query) {
        final String body = "{\"data\":[{\"text\":\"" + StringEscapeUtils.escapeJson(query) + "\"}],\"execEndpoint\":\"/\"}";
        logger.debug("request body: {}", body);
        try (CurlResponse response = Curl.post(clipEndpoint + "/post").header("Content-Type", "application/json").body(body).execute()) {
            final Map<String, Object> contentMap = response.getContent(PARSER);
            if (((contentMap.get("data") instanceof final List dataList)
                    && (!dataList.isEmpty() && dataList.get(0) instanceof final Map data))
                    && (data.get("embedding") instanceof final List embeddingList)) {
                logger.debug("embedding: {}", embeddingList);
                final float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = ((Number) embeddingList.get(i)).floatValue();
                }
                return embedding;
            }
        } catch (final IOException e) {
            throw new CasAccessException("Clip server failed to generate an embedding.", e);
        }
        throw new CasAccessException("Clip server cannot generate an embedding");
    }
}
