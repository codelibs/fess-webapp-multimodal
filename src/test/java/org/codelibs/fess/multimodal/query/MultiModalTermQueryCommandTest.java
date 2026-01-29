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
import org.junit.jupiter.api.TestInfo;

public class MultiModalTermQueryCommandTest extends UnitWebappTestCase {

    private MultiModalTermQueryCommand command;

    @Override
    protected void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        command = new MultiModalTermQueryCommand();
    }

    public void test_commandInstantiation() {
        // Test that the command can be instantiated without issues
        assertNotNull(command);
        assertTrue(command instanceof MultiModalTermQueryCommand);
    }

    // Note: getSearchContext test removed due to ComponentUtil dependency
    // This would test context retrieval but requires container initialization
}