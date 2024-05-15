// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.tractusx.agents.remoting.config;

import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * represents the config of an argument
 */
public class ArgumentConfig {
    /**
     * name of the argument
     */
    protected String argumentName;
    /**
     * whether it is mandatory
     */
    protected boolean mandatory = true;

    /**
     * strip everything before the last appearance of that string
     */
    protected String strip = null;

    /**
     * an optional default value
     */
    protected Object defaultValue;

    /**
     * whether the argument forms a batch group
     */
    protected boolean formsBatchGroup = false;

    /**
     * the priority with which the argument is processed
     */
    protected int priority = 100;

    /**
     * access
     *
     * @return argument reference
     */
    public String getArgumentName() {
        return argumentName;
    }

    /**
     * sets
     *
     * @param argumentName reference
     */
    public void setArgumentName(String argumentName) {
        this.argumentName = argumentName;
    }

    /**
     * access
     *
     * @return whether this argument is mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * access
     *
     * @return the default value of the argument if not bound
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * access
     *
     * @return whether this argument should form a batch group
     */
    public boolean isFormsBatchGroup() {
        return formsBatchGroup;
    }

    /**
     * access
     *
     * @return the processing priority of the argument
     */
    public int getPriority() {
        return priority;
    }

    /**
     * strip string
     *
     * @return current strip string
     */
    public String getStrip() {
        return strip;
    }

    /**
     * sets the strip string
     *
     * @param strip a string to strip after
     */
    public void setStrip(String strip) {
        this.strip = strip;
    }


    /**
     * validate the argument
     *
     * @param context validation context
     * @throws SailConfigException if validation was unsuccessful
     */
    public void validate(String context) throws SailConfigException {
        if (argumentName == null || argumentName.length() == 0) {
            throw new SailConfigException(String.format("Only support named arguments %s.", context));
        }
    }

    @Override
    public String toString() {
        return super.toString() + "/argument(" + mandatory + ")";
    }

}
