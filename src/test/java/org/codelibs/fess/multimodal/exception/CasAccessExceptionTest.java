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
package org.codelibs.fess.multimodal.exception;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.codelibs.fess.crawler.exception.CrawlerSystemException;
import org.codelibs.fess.multimodal.UnitWebappTestCase;

public class CasAccessExceptionTest extends UnitWebappTestCase {

    public void test_constructorWithMessageAndCause_storesValuesCorrectly() {
        final String message = "Test error message";
        final Exception cause = new IOException("IO error");

        final CasAccessException exception = new CasAccessException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(exception instanceof CrawlerSystemException);
    }

    public void test_constructorWithMessageOnly_storesMessageCorrectly() {
        final String message = "Test error message";

        final CasAccessException exception = new CasAccessException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof CrawlerSystemException);
    }

    public void test_constructorWithNullMessage_acceptsNull() {
        final Exception cause = new IOException("IO error");

        final CasAccessException exception = new CasAccessException(null, cause);

        assertNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    public void test_constructorWithEmptyMessage_acceptsEmpty() {
        final String message = "";
        final Exception cause = new IOException("IO error");

        final CasAccessException exception = new CasAccessException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    public void test_constructorWithNullCause_acceptsNull() {
        final String message = "Test error message";

        final CasAccessException exception = new CasAccessException(message, null);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    public void test_constructorWithNullMessageAndCause_acceptsBoth() {
        final CasAccessException exception = new CasAccessException(null, null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    public void test_exceptionChaining_preservesCauseCorrectly() {
        final IOException rootCause = new IOException("Root cause");
        final RuntimeException intermediateCause = new RuntimeException("Intermediate", rootCause);
        final String message = "CAS error";

        final CasAccessException exception = new CasAccessException(message, intermediateCause);

        assertEquals(message, exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    public void test_stackTrace_includesToStringOfCause() {
        final IOException cause = new IOException("IO error details");
        final String message = "CAS connection failed";

        final CasAccessException exception = new CasAccessException(message, cause);

        final String stackTrace = getStackTraceAsString(exception);
        assertTrue(stackTrace.contains("CAS connection failed"));
        assertTrue(stackTrace.contains("Caused by"));
        assertTrue(stackTrace.contains("IO error details"));
    }

    public void test_serialization_roundTripPreservesData() throws Exception {
        final String message = "Serialization test message";
        final IOException cause = new IOException("Serialization cause");
        final CasAccessException original = new CasAccessException(message, cause);

        // Serialize
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        // Deserialize
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        final CasAccessException deserialized = (CasAccessException) ois.readObject();
        ois.close();

        // Verify
        assertEquals(original.getMessage(), deserialized.getMessage());
        assertEquals(original.getCause().getClass(), deserialized.getCause().getClass());
        assertEquals(original.getCause().getMessage(), deserialized.getCause().getMessage());
    }

    public void test_inheritanceHierarchy_extendsCorrectClass() {
        final CasAccessException exception = new CasAccessException("test");

        assertTrue(exception instanceof CrawlerSystemException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    private String getStackTraceAsString(Throwable throwable) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        throwable.printStackTrace(new java.io.PrintStream(baos));
        return baos.toString();
    }
}