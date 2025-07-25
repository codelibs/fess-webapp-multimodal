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
package org.codelibs.fess.multimodal.util;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Utility class for encoding and decoding embedding vectors.
 * Provides methods to convert between float arrays and base64-encoded strings
 * for storage and transmission of embedding data.
 */
public class EmbeddingUtil {

    private EmbeddingUtil() {
        // nothing
    }

    /**
     * Encodes a float array into a base64-encoded string.
     *
     * @param floatArray the float array to encode
     * @return base64-encoded string representation of the float array
     */
    public static String encodeFloatArray(final float[] floatArray) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(floatArray.length * 4);
        for (final float value : floatArray) {
            byteBuffer.putFloat(value);
        }
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decodes a base64-encoded string back into a float array.
     *
     * @param encodedString the base64-encoded string to decode
     * @return float array decoded from the string
     */
    public static float[] decodeFloatArray(final String encodedString) {
        final byte[] bytes = Base64.getDecoder().decode(encodedString);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        final float[] floatArray = new float[bytes.length / 4];
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = byteBuffer.getFloat();
        }
        return floatArray;
    }

}
