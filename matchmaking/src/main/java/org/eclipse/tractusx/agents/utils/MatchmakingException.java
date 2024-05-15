/*
 *  Copyright (c) 2024 T-Systems International GmbH
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.tractusx.agents.utils;


/**
 * Base exception for the system.
 * The system should use unchecked exceptions when appropriate (e.g., non-recoverable errors) and may extend this exception.
 */
public class MatchmakingException extends RuntimeException {

    public MatchmakingException(String message) {
        super(message);
    }

    public MatchmakingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MatchmakingException(Throwable cause) {
        super(cause);
    }

    public MatchmakingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
