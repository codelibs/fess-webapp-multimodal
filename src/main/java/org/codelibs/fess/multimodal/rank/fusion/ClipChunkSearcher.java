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
package org.codelibs.fess.multimodal.rank.fusion;

import org.codelibs.fess.rank.fusion.SemanticChunkSearcher;

/**
 * Multimodal (CLIP) variant of the semantic chunk searcher.
 *
 * <p>The retrieval itself is the same as the core searcher's: the difference is what the
 * query is embedded by, and that comes from {@code fess_embedding++.xml} replacing the
 * embedding client rather than from anything here.</p>
 *
 * <p>What remains is the name. It is not derived from the class, because it is not an
 * implementation detail: it is what the {@code searcher} field of each document reports, and
 * what the mosaic theme labels its results with.</p>
 */
public class ClipChunkSearcher extends SemanticChunkSearcher {

    /** Searcher name exposed in the {@code searcher} provenance field. */
    protected static final String SEARCHER_NAME = "multi_modal";

    /**
     * Constructs a new ClipChunkSearcher instance.
     */
    public ClipChunkSearcher() {
        // Default constructor
    }

    @Override
    public String getName() {
        // Without this the base derives "clip_chunk" from the class name, which would break
        // every consumer of the `searcher` provenance field (the mosaic theme's badges).
        return SEARCHER_NAME;
    }
}
