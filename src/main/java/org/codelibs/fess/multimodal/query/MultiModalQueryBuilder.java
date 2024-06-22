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
package org.codelibs.fess.multimodal.query;

import static org.codelibs.fess.multimodal.MultiModalConstants.CAS_CLIENT;
import static org.codelibs.fess.multimodal.MultiModalConstants.CONTENT_VECTOR_FIELD;

import org.codelibs.fess.multimodal.client.CasClient;
import org.codelibs.fess.multimodal.index.query.KNNQueryBuilder;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.index.query.QueryBuilder;

public class MultiModalQueryBuilder {

    protected String query;
    protected Float minScore;

    private MultiModalQueryBuilder() {
        // nothing
    }

    public static class Builder {

        private String query;
        private Float minScore;

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder minScore(final Float minScore) {
            this.minScore = minScore;
            return this;
        }

        public MultiModalQueryBuilder build() {
            final MultiModalQueryBuilder builder = new MultiModalQueryBuilder();
            builder.query = query;
            builder.minScore = minScore;
            return builder;
        }
    }

    public QueryBuilder toQueryBuilder() {
        final CasClient client = ComponentUtil.getComponent(CAS_CLIENT);
        final float[] embedding = client.getTextEmbedding(query);
        return new KNNQueryBuilder.Builder().field(CONTENT_VECTOR_FIELD).vector(embedding).minScore(minScore).build();
    }

}
