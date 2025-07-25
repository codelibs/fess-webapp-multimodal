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
package org.codelibs.fess.multimodal;

/**
 * Constants used throughout the multimodal search plugin.
 * Contains system property keys, field names, and component identifiers
 * for configuring and using multimodal search functionality.
 */
public class MultiModalConstants {

    private static final String PREFIX = "fess.multimodal.";

    /** System property key for vector dimension configuration. */
    public static final String CONTENT_DIMENSION = PREFIX + "content.dimension";

    /** System property key for vector search engine configuration. */
    public static final String CONTENT_ENGINE = PREFIX + "content.engine";

    /** System property key for vector search method configuration. */
    public static final String CONTENT_METHOD = PREFIX + "content.method";

    /** System property key for vector space type configuration. */
    public static final String CONTENT_SPACE_TYPE = PREFIX + "content.space_type";

    /** System property key for vector field name configuration. */
    public static final String CONTENT_FIELD = PREFIX + "content.field";

    /** System property key for minimum score threshold configuration. */
    public static final String MIN_SCORE = PREFIX + "min_score";

    /** Default vector field name. */
    public static final String DEFAULT_CONTENT_FIELD = PREFIX + "content_vector";

    /** Header name for embedding data in document metadata. */
    public static final String X_FESS_EMBEDDING = "X-FESS-Embedding";

    /** Component name for the multimodal searcher. */
    public static final String SEARCHER = "multiModalSearcher";

    /** Component name for the CAS client. */
    public static final String CAS_CLIENT = "casClient";

    /** Component name for the multimodal search helper. */
    public static final String HELPER = "multiModalSearchHelper";

    private MultiModalConstants() {
        // nothing
    }
}
