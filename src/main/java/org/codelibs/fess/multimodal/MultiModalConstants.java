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
package org.codelibs.fess.multimodal;

public class MultiModalConstants {

    private static final String PREFIX = "fess.multimodal.";

    public static final String MIN_SCORE = PREFIX + "min_score";

    public static final String CONTENT_VECTOR_FIELD = System.getProperty(PREFIX + "content.field", "content_vector");

    public static final String X_FESS_EMBEDDING = "X-FESS-Embedding";

    public static final String SEARCHER = "multiModalSearcher";

    public static final String CAS_CLIENT = "casClient";

    private MultiModalConstants() {
        // nothing
    }
}
