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

import org.codelibs.fess.multimodal.UnitWebappTestCase;
import org.codelibs.fess.multimodal.query.StructuredQuerySplitter.Split;
import org.junit.jupiter.api.Test;

public class StructuredQuerySplitterTest extends UnitWebappTestCase {

    @Test
    public void test_plainQuery() {
        final Split split = StructuredQuerySplitter.split("mountain sunset");
        assertNotNull(split);
        assertEquals("mountain sunset", split.text);
        assertTrue(split.conditions.isEmpty());
    }

    @Test
    public void test_singleLabel() {
        final Split split = StructuredQuerySplitter.split("mountain sunset label:\"photos\"");
        assertNotNull(split);
        assertEquals("mountain sunset", split.text);
        assertEquals(1, split.conditions.size());
        assertEquals(1, split.conditions.get("label").size());
        assertEquals("photos", split.conditions.get("label").get(0));
    }

    @Test
    public void test_labelOrGroup() {
        final Split split = StructuredQuerySplitter.split("cat (label:\"a\" OR label:\"b\")");
        assertNotNull(split);
        assertEquals("cat", split.text);
        assertEquals(2, split.conditions.get("label").size());
        assertEquals("a", split.conditions.get("label").get(0));
        assertEquals("b", split.conditions.get("label").get(1));
    }

    @Test
    public void test_bareExQ() {
        final Split split = StructuredQuerySplitter.split("cat filetype:jpeg");
        assertNotNull(split);
        assertEquals("cat", split.text);
        assertEquals("jpeg", split.conditions.get("filetype").get(0));
    }

    @Test
    public void test_multipleConditions() {
        final Split split = StructuredQuerySplitter.split("red car filetype:jpeg label:\"photos\"");
        assertNotNull(split);
        assertEquals("red car", split.text);
        assertEquals("jpeg", split.conditions.get("filetype").get(0));
        assertEquals("photos", split.conditions.get("label").get(0));
    }

    /** sort is not a filter and the semantic branch cannot honour it: refuse the query. */
    @Test
    public void test_sortIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat sort:filename.asc"));
    }

    /** A field outside the allowlist is refused rather than silently ignored. */
    @Test
    public void test_unknownFieldIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat anything:1"));
    }

    @Test
    public void test_rangeIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat timestamp:[now/d-1d TO *]"));
    }

    @Test
    public void test_userQuotedPhraseIsRejected() {
        assertNull(StructuredQuerySplitter.split("\"mountain sunset\""));
    }

    @Test
    public void test_booleanOperatorIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat AND dog"));
    }

    @Test
    public void test_wildcardIsRejected() {
        assertNull(StructuredQuerySplitter.split("ca* filetype:jpeg"));
    }

    /** Conditions only, no free text: nothing to embed, so refuse. */
    @Test
    public void test_conditionsOnlyIsRejected() {
        assertNull(StructuredQuerySplitter.split("filetype:jpeg"));
    }

    @Test
    public void test_blankIsRejected() {
        assertNull(StructuredQuerySplitter.split("   "));
        assertNull(StructuredQuerySplitter.split(null));
    }

    /**
     * The back-reference in QUOTED_GROUP requires every OR-ed term to share the same field
     * name. A mismatched field name means the group as a whole fails to match, the parentheses
     * survive into the residual text, and RESIDUAL_SYNTAX then rejects the query.
     */
    @Test
    public void test_mismatchedFieldNameOrGroupIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat (label:\"a\" OR host:\"b\")"));
    }

    /** Same as above but with three terms, so only the first two share a field name. */
    @Test
    public void test_mismatchedFieldNameOrGroupWithThreeTermsIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat (label:\"a\" OR host:\"b\" OR label:\"c\")"));
    }

    /** An allowlist violation must reject the whole query regardless of where it appears. */
    @Test
    public void test_allowlistViolationAfterAllowedFieldIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat label:\"a\" anything:1"));
    }

    /** Same as above but with the disallowed field appearing first. */
    @Test
    public void test_allowlistViolationBeforeAllowedFieldIsRejected() {
        assertNull(StructuredQuerySplitter.split("cat anything:1 label:\"a\""));
    }

    /**
     * An empty quoted value is a legitimate condition, not a missing one: the list must still
     * hold exactly one element. {@code List.toString()} renders a single-element list holding an
     * empty string as "[]", the same as an empty list, so this asserts size() explicitly rather
     * than relying on toString() or isEmpty().
     */
    @Test
    public void test_emptyQuotedValueIsPreserved() {
        final Split split = StructuredQuerySplitter.split("cat label:\"\"");
        assertNotNull(split);
        assertEquals("cat", split.text);
        assertEquals(1, split.conditions.get("label").size());
        assertEquals("", split.conditions.get("label").get(0));
    }
}
