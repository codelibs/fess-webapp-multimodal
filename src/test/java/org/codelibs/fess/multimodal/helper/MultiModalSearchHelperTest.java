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

import org.dbflute.utflute.core.PlainTestCase;

public class MultiModalSearchHelperTest extends PlainTestCase {

    private MultiModalSearchHelper helper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        helper = new MultiModalSearchHelper();

        // Clear any existing system properties
        clearSystemProperties();
    }

    @Override
    protected void tearDown() throws Exception {
        clearSystemProperties();
        super.tearDown();
    }

    private void clearSystemProperties() {
        System.clearProperty(CONTENT_DIMENSION);
        System.clearProperty(CONTENT_METHOD);
        System.clearProperty(CONTENT_ENGINE);
        System.clearProperty(CONTENT_SPACE_TYPE);
        System.clearProperty(CONTENT_FIELD);
        System.clearProperty(MIN_SCORE);
    }

    public void test_load_defaultConfiguration_setsDefaults() {
        // No system properties set
        final String result = helper.load();

        assertEquals(DEFAULT_CONTENT_FIELD, helper.getVectorField());
        assertNull(helper.getMinScore());
        assertTrue(result.contains("vector_field=" + DEFAULT_CONTENT_FIELD));
        assertTrue(result.contains("min_score="));
    }

    public void test_load_customConfiguration_appliesCustomValues() {
        // Set custom properties
        System.setProperty(CONTENT_FIELD, "custom_vector_field");
        System.setProperty(MIN_SCORE, "0.8");

        final String result = helper.load();

        assertEquals("custom_vector_field", helper.getVectorField());
        assertEquals(Float.valueOf(0.8f), helper.getMinScore());
        assertTrue(result.contains("vector_field=custom_vector_field"));
        assertTrue(result.contains("min_score=0.8"));
    }

    public void test_load_invalidMinScore_handlesGracefully() {
        System.setProperty(MIN_SCORE, "invalid_number");

        final String result = helper.load();

        assertNull(helper.getMinScore());
        assertTrue(result.contains("min_score="));
    }

    public void test_load_emptyMinScore_handlesGracefully() {
        System.setProperty(MIN_SCORE, "");

        final String result = helper.load();

        assertNull(helper.getMinScore());
    }

    public void test_load_whitespaceMinScore_handlesGracefully() {
        System.setProperty(MIN_SCORE, "   ");

        final String result = helper.load();

        assertNull(helper.getMinScore());
    }

    // Note: All rewriteQuery tests removed due to ComponentUtil dependencies
    // These would test query rewriting logic but require container initialization

    // Note: rewriteQuery tests removed due to ComponentUtil dependency
    // These would test query rewriting logic that requires container initialization

    public void test_getMinScore_returnsConfiguredValue() {
        System.setProperty(MIN_SCORE, "0.7");
        helper.load();

        assertEquals(Float.valueOf(0.7f), helper.getMinScore());
    }

    public void test_getVectorField_returnsConfiguredValue() {
        System.setProperty(CONTENT_FIELD, "my_vector_field");
        helper.load();

        assertEquals("my_vector_field", helper.getVectorField());
    }

    public void test_rewriteQuery_nullQuery_returnsNull() {
        helper.load();
        final String result = helper.rewriteQuery(null);
        assertNull(result);
    }

    public void test_rewriteQuery_emptyQuery_returnsEmpty() {
        helper.load();
        final String result = helper.rewriteQuery("");
        assertEquals("", result);
    }

    public void test_rewriteQuery_blankQuery_returnsBlank() {
        helper.load();
        final String result = helper.rewriteQuery("   ");
        assertEquals("   ", result);
    }

    public void test_rewriteQuery_singleWord_returnsUnchanged() {
        helper.load();
        final String result = helper.rewriteQuery("test");
        assertEquals("test", result);
    }

    public void test_rewriteQuery_queryWithQuotes_returnsUnchanged() {
        helper.load();
        final String result = helper.rewriteQuery("\"test query\"");
        assertEquals("\"test query\"", result);
    }

    public void test_rewriteQuery_queryWithoutSpaces_returnsUnchanged() {
        helper.load();
        final String result = helper.rewriteQuery("testquery");
        assertEquals("testquery", result);
    }

    public void test_load_trimsVectorField() {
        System.setProperty(CONTENT_FIELD, "  custom_vector  ");
        helper.load();

        assertEquals("custom_vector", helper.getVectorField());
    }

    public void test_load_multipleInvocations_updatesValues() {
        System.setProperty(CONTENT_FIELD, "field1");
        System.setProperty(MIN_SCORE, "0.5");
        helper.load();

        assertEquals("field1", helper.getVectorField());
        assertEquals(Float.valueOf(0.5f), helper.getMinScore());

        System.setProperty(CONTENT_FIELD, "field2");
        System.setProperty(MIN_SCORE, "0.9");
        helper.load();

        assertEquals("field2", helper.getVectorField());
        assertEquals(Float.valueOf(0.9f), helper.getMinScore());
    }

    public void test_load_zeroMinScore_setsZero() {
        System.setProperty(MIN_SCORE, "0.0");
        helper.load();

        assertEquals(Float.valueOf(0.0f), helper.getMinScore());
    }

    public void test_load_negativeMinScore_setsNegative() {
        System.setProperty(MIN_SCORE, "-0.5");
        helper.load();

        assertEquals(Float.valueOf(-0.5f), helper.getMinScore());
    }

    public void test_load_veryLargeMinScore_setsLarge() {
        System.setProperty(MIN_SCORE, "999999.99");
        helper.load();

        assertEquals(Float.valueOf(999999.99f), helper.getMinScore());
    }

    public void test_getMinScore_withoutLoad_returnsNull() {
        final MultiModalSearchHelper newHelper = new MultiModalSearchHelper();
        assertNull(newHelper.getMinScore());
    }

    public void test_getVectorField_withoutLoad_returnsNull() {
        final MultiModalSearchHelper newHelper = new MultiModalSearchHelper();
        assertNull(newHelper.getVectorField());
    }
}