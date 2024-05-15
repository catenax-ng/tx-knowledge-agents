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
package org.eclipse.tractusx.agents.sparql;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * Converter to convert a HttpServletRequest to a OkHttpRequest for
 * a application/json media type
 */

public class HttpServletRequestToOkHttpRequestConverter {

    public Request convert(HttpServletRequest httpServletRequest) throws IOException {
        // Extracting request method, URL, and headers
        String method = httpServletRequest.getMethod();
        String url = buildUrl(httpServletRequest);
        Headers headers = buildHeaders(httpServletRequest);
        RequestBody requestBody = buildRequestBody(httpServletRequest);

        // Building okhttp3.Request
        return new Request.Builder()
                .method(method, requestBody)
                .url(url)
                .headers(headers)
                .build();
    }

    private String buildUrl(HttpServletRequest httpServletRequest) {
        StringBuffer url = httpServletRequest.getRequestURL();
        String queryString = httpServletRequest.getQueryString();
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    // Copying headers from HttpServletRequest to okhttp3.Headers
    private Headers buildHeaders(HttpServletRequest httpServletRequest) {
        Headers.Builder headersBuilder = new Headers.Builder();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpServletRequest.getHeader(headerName);
            headersBuilder.add(headerName, headerValue);
        }

        return headersBuilder.build();
    }

    // Extracting request body of application/json;
    private RequestBody buildRequestBody(HttpServletRequest httpServletRequest) throws IOException {
        if ("POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
            Enumeration<String> parameterNames = httpServletRequest.getParameterNames();
            Map<String, String> jsonPayload = new HashMap<>();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = httpServletRequest.getParameter(paramName);
                jsonPayload.put(paramName, paramValue);
            }

            MediaType mediaType = MediaType.get("application/json; charset=utf-8");
            return RequestBody.create(new Gson().toJson(jsonPayload), mediaType);
        }

        return null;
    }
    
}
