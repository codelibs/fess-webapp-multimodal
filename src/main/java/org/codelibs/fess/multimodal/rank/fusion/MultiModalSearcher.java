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

import static org.codelibs.fess.multimodal.MultiModalConstants.HELPER;

import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.entity.FacetInfo;
import org.codelibs.fess.entity.GeoInfo;
import org.codelibs.fess.entity.HighlightInfo;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.multimodal.helper.MultiModalSearchHelper;
import org.codelibs.fess.mylasta.action.FessUserBean;
import org.codelibs.fess.rank.fusion.DefaultSearcher;
import org.codelibs.fess.rank.fusion.SearchResult;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalThing;

import jakarta.annotation.PostConstruct;

public class MultiModalSearcher extends DefaultSearcher {
    private static final Logger logger = LogManager.getLogger(MultiModalSearcher.class);

    protected ThreadLocal<SearchContext> contextLocal = new ThreadLocal<>();

    @PostConstruct
    public void register() {
        if (logger.isInfoEnabled()) {
            logger.info("Load {}", this.getClass().getSimpleName());
        }

        ComponentUtil.getRankFusionProcessor().register(this);
    }

    @Override
    protected SearchResult search(final String query, final SearchRequestParams params, final OptionalThing<FessUserBean> userBean) {
        try {
            final SearchContext searchContext = createContext(query, params, userBean);
            return super.search(query, searchContext.getParams(), userBean);
        } finally {
            closeContext();
        }
    }

    public SearchContext createContext(final String query, final SearchRequestParams params, final OptionalThing<FessUserBean> userBean) {
        if (contextLocal.get() != null) {
            logger.warn("The context exists: {}", contextLocal.get());
            contextLocal.remove();
        }
        final MultiModalSearchHelper multiModalSearchHelper = ComponentUtil.getComponent(HELPER);
        final SearchRequestParams reqParams = new SearchRequestParamsWrapper(params, multiModalSearchHelper.getMinScore());
        final SearchContext context = new SearchContext(multiModalSearchHelper.getVectorField(), query, reqParams, userBean);
        contextLocal.set(context);
        return context;
    }

    public void closeContext() {
        if (contextLocal.get() == null) {
            logger.warn("The context does not exist.");
        } else {
            contextLocal.remove();
        }
    }

    public SearchContext getContext() {
        return contextLocal.get();
    }

    public static class SearchContext {

        private final String vectorField;
        private final String query;
        private final SearchRequestParams params;
        private final OptionalThing<FessUserBean> userBean;

        public SearchContext(final String vectorField, final String query, final SearchRequestParams params,
                final OptionalThing<FessUserBean> userBean) {
            this.vectorField = vectorField;
            this.query = query;
            this.params = params;
            this.userBean = userBean;
        }

        public String getVectorField() {
            return vectorField;
        }

        public String getQuery() {
            return query;
        }

        public SearchRequestParams getParams() {
            return params;
        }

        public OptionalThing<FessUserBean> getUserBean() {
            return userBean;
        }

        @Override
        public String toString() {
            return "SemanticSearchContext [query=" + query + ", params=" + params + ", userBean=" + userBean.orElse(null) + "]";
        }

    }

    protected static class SearchRequestParamsWrapper extends SearchRequestParams {
        private final SearchRequestParams parent;
        private final Float minScore;

        protected SearchRequestParamsWrapper(final SearchRequestParams params, final Float minScore) {
            this.parent = params;
            this.minScore = minScore;
        }

        @Override
        public String getQuery() {
            return parent.getQuery();
        }

        @Override
        public Map<String, String[]> getFields() {
            return parent.getFields();
        }

        @Override
        public Map<String, String[]> getConditions() {
            return parent.getConditions();
        }

        @Override
        public String[] getLanguages() {
            return parent.getLanguages();
        }

        @Override
        public GeoInfo getGeoInfo() {
            return null;
        }

        @Override
        public FacetInfo getFacetInfo() {
            return null;
        }

        @Override
        public HighlightInfo getHighlightInfo() {
            return null;
        }

        @Override
        public String getSort() {
            return parent.getSort();
        }

        @Override
        public int getStartPosition() {
            return parent.getStartPosition();
        }

        @Override
        public int getPageSize() {
            return parent.getPageSize();
        }

        @Override
        public int getOffset() {
            return parent.getOffset();
        }

        @Override
        public String[] getExtraQueries() {
            return parent.getExtraQueries();
        }

        @Override
        public Object getAttribute(final String name) {
            return parent.getAttribute(name);
        }

        @Override
        public Locale getLocale() {
            return parent.getLocale();
        }

        @Override
        public SearchRequestType getType() {
            return parent.getType();
        }

        @Override
        public String getSimilarDocHash() {
            return parent.getSimilarDocHash();
        }

        @Override
        public Float getMinScore() {
            return minScore;
        }
    }
}
