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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codelibs.core.lang.StringUtil;

/**
 * Splits an assembled Fess query string into the plain free text and the structured
 * conditions Fess core folded into it.
 *
 * <p>Core's {@code QueryStringBuilder} appends facet, label and sort selections to the query
 * string, and core's semantic branch then refuses the whole query because it contains search
 * syntax. This splitter recovers the two halves so the free text can be embedded on its own
 * and the conditions can be applied as real filters.</p>
 *
 * <p>Only the exact shapes {@code QueryStringBuilder} produces are recognised. Anything else
 * -- ranges, wildcards, user-authored phrases, boolean operators, sort -- yields {@code null},
 * which means "leave this query to core's own gate".</p>
 */
public final class StructuredQuerySplitter {

    /** Fields whose conditions can be translated into term filters. */
    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList("label", "host", "site", "filetype", "mimetype", "lang"));

    /** ` name:"value"` */
    private static final Pattern QUOTED_TERM = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*):\"([^\"\\\\]*)\"");

    /** ` (name:"v1" OR name:"v2" ...)` */
    private static final Pattern QUOTED_GROUP =
            Pattern.compile("\\(\\s*([a-zA-Z_][a-zA-Z0-9_.]*):\"[^\"\\\\]*\"(?:\\s+OR\\s+\\1:\"[^\"\\\\]*\")*\\s*\\)");

    /** ` name:value` with no quoting */
    private static final Pattern BARE_TERM = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*):([^\\s\"()\\[\\]{}^~*?\\\\]+)");

    /** Anything left over that means the user wrote real query syntax. */
    private static final Pattern RESIDUAL_SYNTAX =
            Pattern.compile("[\"():\\[\\]{}^~*?\\\\]|&&|\\|\\||(?:^|\\s)[+\\-]\\S|\\b(?:AND|OR|NOT|TO)\\b");

    private StructuredQuerySplitter() {
        // nothing
    }

    /** The plain text and the structured conditions recovered from a query string. */
    public static final class Split {
        /** The free text to embed. Never blank. */
        public final String text;

        /** Field name to values. Never null; may be empty. */
        public final Map<String, List<String>> conditions;

        Split(final String text, final Map<String, List<String>> conditions) {
            this.text = text;
            this.conditions = conditions;
        }
    }

    /**
     * Splits an assembled query string.
     *
     * @param query the assembled query string
     * @return the split, or {@code null} when the query is not safe to split
     */
    public static Split split(final String query) {
        if (StringUtil.isBlank(query)) {
            return null;
        }
        final Map<String, List<String>> conditions = new HashMap<>();
        String remaining = query;

        remaining = extractGroups(remaining, conditions);
        if (remaining == null) {
            return null;
        }
        remaining = extractTerms(remaining, QUOTED_TERM, conditions);
        if (remaining == null) {
            return null;
        }
        remaining = extractTerms(remaining, BARE_TERM, conditions);
        if (remaining == null) {
            return null;
        }

        final String text = remaining.replaceAll("\\s+", " ").trim();
        if (StringUtil.isBlank(text) || RESIDUAL_SYNTAX.matcher(text).find()) {
            return null;
        }
        return new Split(text, conditions);
    }

    private static String extractGroups(final String query, final Map<String, List<String>> conditions) {
        final Matcher matcher = QUOTED_GROUP.matcher(query);
        final StringBuilder buf = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            final String field = matcher.group(1);
            if (!ALLOWED_FIELDS.contains(field)) {
                return null;
            }
            final Matcher inner = QUOTED_TERM.matcher(matcher.group());
            while (inner.find()) {
                conditions.computeIfAbsent(field, k -> new ArrayList<>()).add(inner.group(2));
            }
            buf.append(query, last, matcher.start()).append(' ');
            last = matcher.end();
        }
        buf.append(query.substring(last));
        return buf.toString();
    }

    private static String extractTerms(final String query, final Pattern pattern, final Map<String, List<String>> conditions) {
        final Matcher matcher = pattern.matcher(query);
        final StringBuilder buf = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            final String field = matcher.group(1);
            if (!ALLOWED_FIELDS.contains(field)) {
                return null;
            }
            conditions.computeIfAbsent(field, k -> new ArrayList<>()).add(matcher.group(2));
            buf.append(query, last, matcher.start()).append(' ');
            last = matcher.end();
        }
        buf.append(query.substring(last));
        return buf.toString();
    }
}
