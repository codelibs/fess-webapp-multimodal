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

import org.dbflute.utflute.core.PlainTestCase;

public class MultiModalSearcherTest extends PlainTestCase {

    private MultiModalSearcher searcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        searcher = new MultiModalSearcher();
    }

    @Override
    protected void tearDown() throws Exception {
        // Clean up any remaining context
        searcher.closeContext();
        super.tearDown();
    }

    public void test_searcherInstantiation() {
        // Test that the searcher can be instantiated without issues
        assertNotNull(searcher);
        assertTrue(searcher instanceof MultiModalSearcher);
    }

    public void test_getContext_withoutSetup_returnsNull() {
        // Without any context setup, should return null gracefully
        assertNull(searcher.getContext());
    }

    public void test_closeContext_withoutContext_handlesGracefully() {
        // Should not throw exception when closing non-existent context
        searcher.closeContext();
        assertNull(searcher.getContext());
    }
}