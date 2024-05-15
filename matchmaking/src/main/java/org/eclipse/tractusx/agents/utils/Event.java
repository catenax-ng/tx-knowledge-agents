/*
 *  Copyright (c) 2024 T-Systems International GmbH
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */
package org.eclipse.tractusx.agents.utils;

import java.util.ArrayList;
import java.util.List;

public abstract class Event {

    public List<CallbackAddress> getCallbackAddresses() {
        return new ArrayList<>();
    }


    /**
     * The name of the event in dot notation.
     *
     * @return the event name.
     */
    public abstract String name();
}