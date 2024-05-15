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
package org.eclipse.tractusx.agents.http;

import org.eclipse.tractusx.agents.utils.Monitor;

import java.lang.reflect.Proxy;

/**
 * An invocation handler which maps all jakarta objects
 * to a javax.servlet level
 */
public interface JakartaAdapter<TARGET> {

    /**
     * access
     *
     * @return the wrapper object
     */
    TARGET getDelegate();

    /**
     * access
     *
     * @return EDC logging support
     */
    Monitor getMonitor();

    /**
     * unwrap logic
     *
     * @param types array of type annotations
     * @param args  array of objects
     * @return unwrapped array of objects
     * @throws Throwable in case something strange happens
     */
    @SuppressWarnings("rawtypes")
    static Object[] unwrap(Class[] types, Object[] args) throws Throwable {
        if (args == null) args = new Object[0];
        for (int count = 0; count < args.length; count++) {
            if (types[count].getCanonicalName().startsWith("javax.servlet") && args[count] != null) {
                JakartaAdapter<Object> wrapper;
                if (args[count] instanceof JakartaAdapter) {
                    wrapper = (JakartaAdapter<Object>) args[count];
                } else {
                    // we assume its a proxy
                    wrapper = (JakartaAdapter<Object>) Proxy.getInvocationHandler(args[count]);
                }
                args[count] = wrapper.getDelegate();
                Class jakartaClass = JakartaAdapter.class.getClassLoader().loadClass(types[count].getCanonicalName().replace("javax.servlet", "jakarta.servlet"));
                types[count] = jakartaClass;
            }
        }
        return args;
    }

    /**
     * wrap logic
     *
     * @param jakarta    original object
     * @param javaxClass target interfaces
     * @param monitor    EDC loggin subsystem
     * @param <TARGET>   target interfaces as generics
     * @return wrapped object
     */
    static <TARGET> TARGET javaxify(Object jakarta, Class<TARGET> javaxClass, Monitor monitor) {
        if (javax.servlet.ServletInputStream.class.equals(javaxClass)) {
            return (TARGET) new JakartaServletInputStreamAdapter((jakarta.servlet.ServletInputStream) jakarta, monitor);
        }
        if (javax.servlet.ServletOutputStream.class.equals(javaxClass)) {
            return (TARGET) new JakartaServletOutputStreamAdapter((jakarta.servlet.ServletOutputStream) jakarta, monitor);
        }
        return (TARGET) Proxy.newProxyInstance(JakartaAdapterImpl.class.getClassLoader(),
                new Class[]{ javaxClass },
                new JakartaAdapterImpl(jakarta, monitor));
    }

}
