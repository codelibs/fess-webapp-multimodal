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
package org.codelibs.fess.multimodal.query;

import static org.codelibs.fess.multimodal.MultiModalConstants.CAS_CLIENT;

import org.codelibs.fess.multimodal.client.CasClient;
import org.codelibs.fess.multimodal.index.query.KNNQueryBuilder;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.index.query.QueryBuilder;

/**
 * Builder for constructing multimodal search queries that combine text and vector search.
 * Converts text queries into vector embeddings and builds KNN queries for semantic search.
 */
public class MultiModalQueryBuilder {

    /** The vector field to search against. */
    protected String field;
    /** The text query to convert to embeddings. */
    protected String query;
    /** The number of nearest neighbors to retrieve. */
    protected int k;
    /** The minimum score threshold for matches. */
    protected Float minScore;

    private MultiModalQueryBuilder() {
        // nothing
    }

    /**
     * Builder class for constructing MultiModalQueryBuilder instances.
     */
    public static class Builder {

        /**
         * Constructs a new Builder instance.
         */
        public Builder() {
            // Default constructor
        }

        private String field;
        private String query;
        private int k = 10;
        private Float minScore;

        /**
         * Sets the vector field to search against.
         *
         * @param field the vector field name
         * @return this builder for chaining
         */
        public Builder field(final String field) {
            this.field = field;
            return this;
        }

        /**
         * Sets the text query to convert to embeddings.
         *
         * @param query the text query
         * @return this builder for chaining
         */
        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        /**
         * Sets the number of nearest neighbors to retrieve.
         *
         * @param k the number of neighbors
         * @return this builder for chaining
         */
        public Builder k(final int k) {
            this.k = k;
            return this;
        }

        /**
         * Sets the minimum score threshold for matches.
         *
         * @param minScore the minimum score
         * @return this builder for chaining
         */
        public Builder minScore(final Float minScore) {
            this.minScore = minScore;
            return this;
        }

        /**
         * Builds the MultiModalQueryBuilder with configured parameters.
         *
         * @return the constructed MultiModalQueryBuilder
         */
        public MultiModalQueryBuilder build() {
            final MultiModalQueryBuilder builder = new MultiModalQueryBuilder();
            builder.field = field;
            builder.query = query;
            builder.k = k;
            builder.minScore = minScore;
            return builder;
        }
    }

    /**
     * Converts this multimodal query to an OpenSearch QueryBuilder.
     * Generates text embeddings using the CAS client and creates a KNN query.
     *
     * @return the QueryBuilder for execution
     */
    public QueryBuilder toQueryBuilder() {
        final CasClient client = ComponentUtil.getComponent(CAS_CLIENT);
        final float[] embedding = client.getTextEmbedding(query);
        return new KNNQueryBuilder.Builder().field(field).vector(embedding).minScore(minScore).k(k).build();
    }

}
