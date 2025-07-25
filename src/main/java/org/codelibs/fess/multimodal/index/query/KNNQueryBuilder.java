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
package org.codelibs.fess.multimodal.index.query;

import java.io.IOException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.lucene.search.Query;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryShardContext;

/**
 * Query builder for k-nearest neighbor (KNN) vector similarity searches.
 * This builder constructs OpenSearch KNN queries for semantic search using vector embeddings.
 */
public class KNNQueryBuilder extends AbstractQueryBuilder<KNNQueryBuilder> {

    private static final String NAME = "knn";

    private static final ParseField VECTOR_FIELD = new ParseField("vector");
    private static final ParseField K_FIELD = new ParseField("k");
    private static final ParseField FILTER_FIELD = new ParseField("filter");
    private static final ParseField IGNORE_UNMAPPED_FIELD = new ParseField("ignore_unmapped");
    private static final ParseField MAX_DISTANCE_FIELD = new ParseField("max_distance");
    private static final ParseField MIN_SCORE_FIELD = new ParseField("min_score");

    private static final int DEFAULT_K = 10;

    /** The name of the vector field to search against. */
    protected String fieldName;

    /** The query vector for similarity search. */
    protected float[] vector;
    /** The number of nearest neighbors to retrieve. */
    protected int k;
    /** Optional filter to apply to the search results. */
    protected QueryBuilder filter;
    /** Whether to ignore unmapped fields in the query. */
    protected boolean ignoreUnmapped;
    /** Maximum distance threshold for similarity matching. */
    protected Float maxDistance;
    /** Minimum score threshold for similarity matching. */
    protected Float minScore;

    /**
     * Constructs a KNNQueryBuilder from a StreamInput for serialization.
     *
     * @param in the stream input to read from
     * @throws IOException if reading from the stream fails
     */
    public KNNQueryBuilder(final StreamInput in) throws IOException {
        super(in);
        this.fieldName = in.readString();
        this.vector = in.readFloatArray();
        this.k = in.readInt();
        this.filter = in.readOptionalNamedWriteable(QueryBuilder.class);
        this.ignoreUnmapped = in.readBoolean();
        this.maxDistance = in.readOptionalFloat();
        this.minScore = in.readOptionalFloat();
    }

    private KNNQueryBuilder() {
    }

    /**
     * Builder class for constructing KNNQueryBuilder instances with fluent API.
     */
    public static class Builder {

        /**
         * Constructs a new Builder instance.
         */
        public Builder() {
            // Default constructor
        }

        private String fieldName;
        private float[] vector;
        private int k = DEFAULT_K;
        private QueryBuilder filter;
        private boolean ignoreUnmapped = false;
        private Float maxDistance = null;
        private Float minScore = null;

        /**
         * Sets the vector field name to search against.
         *
         * @param fieldName the name of the vector field
         * @return this builder for chaining
         */
        public Builder field(final String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Sets the query vector for similarity search.
         *
         * @param vector the query vector
         * @return this builder for chaining
         */
        public Builder vector(final float[] vector) {
            this.vector = vector;
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
         * Sets an optional filter to apply to search results.
         *
         * @param filter the filter query
         * @return this builder for chaining
         */
        public Builder filter(final QueryBuilder filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets whether to ignore unmapped fields.
         *
         * @param ignoreUnmapped true to ignore unmapped fields
         * @return this builder for chaining
         */
        public Builder ignoreUnmapped(final boolean ignoreUnmapped) {
            this.ignoreUnmapped = ignoreUnmapped;
            return this;
        }

        /**
         * Sets the maximum distance threshold for matches.
         *
         * @param maxDistance the maximum distance
         * @return this builder for chaining
         */
        public Builder maxDistance(final Float maxDistance) {
            this.maxDistance = maxDistance;
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
         * Builds the KNNQueryBuilder with the configured parameters.
         *
         * @return the constructed KNNQueryBuilder
         */
        public KNNQueryBuilder build() {
            final KNNQueryBuilder query = new KNNQueryBuilder();
            query.fieldName = fieldName;
            query.vector = vector;
            query.k = k;
            query.filter = filter;
            query.ignoreUnmapped = ignoreUnmapped;
            query.maxDistance = maxDistance;
            query.minScore = minScore;
            return query;
        }
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected void doWriteTo(final StreamOutput out) throws IOException {
        out.writeString(this.fieldName);
        out.writeFloatArray(this.vector);
        out.writeInt(this.k);
        out.writeOptionalNamedWriteable(this.filter);
        out.writeBoolean(this.ignoreUnmapped);
        out.writeOptionalFloat(this.maxDistance);
        out.writeOptionalFloat(this.minScore);
    }

    @Override
    protected void doXContent(final XContentBuilder xContentBuilder, final Params params) throws IOException {
        xContentBuilder.startObject(NAME);
        xContentBuilder.startObject(fieldName);
        xContentBuilder.field(VECTOR_FIELD.getPreferredName(), vector);
        xContentBuilder.field(K_FIELD.getPreferredName(), k);
        if (filter != null) {
            xContentBuilder.field(FILTER_FIELD.getPreferredName(), filter);
        }
        xContentBuilder.field(IGNORE_UNMAPPED_FIELD.getPreferredName(), ignoreUnmapped);
        if (maxDistance != null) {
            xContentBuilder.field(MAX_DISTANCE_FIELD.getPreferredName(), maxDistance);
        }
        if (minScore != null) {
            xContentBuilder.field(MIN_SCORE_FIELD.getPreferredName(), minScore);
        }
        printBoostAndQueryName(xContentBuilder);
        xContentBuilder.endObject();
        xContentBuilder.endObject();
    }

    @Override
    protected Query doToQuery(final QueryShardContext context) throws IOException {
        throw new UnsupportedOperationException("doToQuery is not supported.");
    }

    @Override
    protected boolean doEquals(final KNNQueryBuilder obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(fieldName, obj.fieldName);
        equalsBuilder.append(vector, obj.vector);
        equalsBuilder.append(k, obj.k);
        equalsBuilder.append(filter, obj.filter);
        equalsBuilder.append(ignoreUnmapped, obj.ignoreUnmapped);
        equalsBuilder.append(maxDistance, obj.maxDistance);
        equalsBuilder.append(minScore, obj.minScore);
        return equalsBuilder.isEquals();
    }

    @Override
    protected int doHashCode() {
        return new HashCodeBuilder().append(fieldName).append(vector).append(k).append(filter).append(ignoreUnmapped).append(maxDistance)
                .append(minScore).toHashCode();
    }
}
