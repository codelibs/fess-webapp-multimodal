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
package org.codelibs.fess.multimodal.helper;

import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_DIMENSION;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_ENGINE;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_FIELD;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_METHOD;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_SPACE_TYPE;
import static org.codelibs.fess.multimodal.MultiModalConstants.DEFAULT_CONTENT_FIELD;
import static org.codelibs.fess.multimodal.MultiModalConstants.MIN_SCORE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.opensearch.client.SearchEngineClient;
import org.codelibs.fess.query.parser.QueryParser;
import org.codelibs.fess.util.ComponentUtil;

import com.google.common.base.CharMatcher;

import jakarta.annotation.PostConstruct;

/**
 * Helper class for configuring and managing multimodal search functionality.
 * Handles vector field configuration, query rewriting, and OpenSearch mapping setup.
 */
public class MultiModalSearchHelper {
    private static final Logger logger = LogManager.getLogger(MultiModalSearchHelper.class);

    /**
     * Constructs a new MultiModalSearchHelper instance.
     */
    public MultiModalSearchHelper() {
        // Default constructor
    }

    /** Minimum score threshold for search results. */
    protected Float minScore;

    private String vectorField;

    /**
     * Initializes the multimodal search helper by configuring OpenSearch mappings,
     * setting up query filters, and loading configuration parameters.
     */
    @PostConstruct
    public void init() {
        final SearchEngineClient client = ComponentUtil.getSearchEngineClient();
        client.addDocumentSettingRewriteRule(s -> s.replace("\"codec\":", "\"knn\": true,\"codec\":"));
        client.addDocumentMappingRewriteRule(s -> {
            final String dimension = System.getProperty(CONTENT_DIMENSION); // ex. 512
            final String method = System.getProperty(CONTENT_METHOD); // ex. hnsw
            final String engine = System.getProperty(CONTENT_ENGINE); // ex. lucene
            final String spaceType = System.getProperty(CONTENT_SPACE_TYPE, "l2"); // ex. l2
            if (logger.isDebugEnabled()) {
                logger.debug("field: {}, dimension: {}, method: {}, engine: {}, spaceType: {}", vectorField, dimension, method, engine,
                        spaceType);
            }
            if (StringUtil.isBlank(dimension) || StringUtil.isBlank(vectorField) || StringUtil.isBlank(method)
                    || StringUtil.isBlank(engine)) {
                return s;
            }
            return s.replace("\"content\":", "\"" + vectorField + "\": {\n" //
                    + "  \"type\": \"knn_vector\",\n" //
                    + "  \"dimension\": " + dimension + ",\n" //
                    + "  \"method\": {\n" //
                    + "    \"name\": \"" + method + "\",\n" //
                    + "    \"engine\": \"" + engine + "\",\n" //
                    + "    \"space_type\": \"" + spaceType + "\"\n" //
                    + "  }\n" //
                    + "},\n" //
                    + "\"content\":");
        });

        if (ComponentUtil.hasQueryParser()) {
            final QueryParser queryParser = ComponentUtil.getQueryParser();
            queryParser.addFilter((query, chain) -> chain.parse(rewriteQuery(query)));
        }

        load();
        ComponentUtil.getSystemHelper().addUpdateConfigListener("MultiModalSearch", this::load);
    }

    /**
     * Loads configuration parameters from system properties.
     *
     * @return summary string of loaded configuration
     */
    protected String load() {
        final StringBuilder buf = new StringBuilder();

        buf.append("vector_field=");
        vectorField = System.getProperty(CONTENT_FIELD, DEFAULT_CONTENT_FIELD).trim(); // ex. content_vector
        buf.append(vectorField);

        buf.append(", min_score=");
        final String minScoreValue = System.getProperty(MIN_SCORE);
        if (StringUtil.isNotBlank(minScoreValue)) {
            try {
                minScore = Float.valueOf(minScoreValue);
                buf.append(minScore);
            } catch (final NumberFormatException e) {
                logger.debug("Failed to parse {}.", minScoreValue, e);
                minScore = null;
            }
        } else {
            minScore = null;
        }

        return buf.toString();
    }

    /**
     * Rewrites queries to handle phrase queries for multimodal search.
     * Wraps simple queries in quotes if they don't contain field specifications.
     *
     * @param query the original query string
     * @return the rewritten query string
     */
    protected String rewriteQuery(final String query) {
        if (StringUtil.isBlank(query) || (query.indexOf('"') != -1) || !CharMatcher.whitespace().matchesAnyOf(query)) {
            return query;
        }

        for (final String field : ComponentUtil.getQueryFieldConfig().getSearchFields()) {
            if (query.indexOf(field + ":") != -1) {
                return query;
            }
        }

        return "\"" + query + "\"";
    }

    /**
     * Gets the configured minimum score threshold.
     *
     * @return the minimum score, or null if not configured
     */
    public Float getMinScore() {
        return minScore;
    }

    /**
     * Gets the configured vector field name.
     *
     * @return the vector field name
     */
    public String getVectorField() {
        return vectorField;
    }
}
