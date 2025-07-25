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

import org.codelibs.fess.crawler.exception.CrawlerSystemException;

/**
 * Exception thrown when there are issues accessing or communicating with the CAS (CLIP as Service) server.
 * This exception extends CrawlerSystemException and is used to indicate problems during image processing
 * or embedding generation operations.
 */
public class CasAccessException extends CrawlerSystemException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new CasAccessException with the specified error message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the underlying cause of this exception
     */
    public CasAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CasAccessException with the specified error message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public CasAccessException(final String message) {
        super(message);
    }
}
