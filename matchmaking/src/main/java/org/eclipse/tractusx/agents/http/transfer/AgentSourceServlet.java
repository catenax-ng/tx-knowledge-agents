// Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.agents.http.transfer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.tractusx.agents.matchmaking.SharedObjectManager;
import org.eclipse.tractusx.agents.sparql.SparqlQueryProcessor;
import org.eclipse.tractusx.agents.utils.Monitor;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/agentsource")
public class AgentSourceServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    // EDC services
    private Monitor monitor = null;
    private SparqlQueryProcessor processor = null;
    
    public AgentSourceServlet() {
                
        this.monitor = SharedObjectManager.getInstance().getMonitor();
        this.processor = SharedObjectManager.getInstance().getProcessor();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        OkHttpClient httpClient = SharedObjectManager.getInstance().getHttpClient();
        String catalogRequestBody = "{" +
                "\"@context\": {}," +
                "\"protocol\": \"dataspace-protocol-http\"," +
                "\"providerUrl\": \"%1$s\", " +
                "\"counterPartyAddress\": \"%1$s\", " +
                "\"querySpec\": %2$s }";
        String dspPath = "%s/api/v1/dsp";
        String remoteControlPlaneIdsUrl = "http://oem-control-plane:8282";
        var catalogSpec = String.format(catalogRequestBody, String.format(dspPath, remoteControlPlaneIdsUrl), "{\"offset\":0,\"limit\":50,\"filterExpression\":[{\"operandLeft\":\"https://w3id.org/catenax/ontology/common#isFederated\",\"operator\":\"=\",\"operandRight\":\"true^^xsd:boolean\"}],\"sortOrder\":\"ASC\",\"sortField\":null}");
        var request = new Request.Builder().url("http://oem-control-plane2:8181/management/v2/catalog/request").post(RequestBody.create(catalogSpec, MediaType.parse("application/json")));
        request.addHeader("x-api-key", "foo");
        monitor.debug("sending in doGet" + "http://oem-control-plane2:8181", null);
        monitor.debug(SharedObjectManager.getInstance().convertToCurl(request.build()));
        okhttp3.Response response = httpClient.newCall(request.build()).execute();

        resp.setContentType("text/plain");
        resp.getWriter().println("\n Hello, Agent, I am on status " + response.code());
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        String responseBody = response.body().string();
        System.out.println(responseBody);
        resp.getWriter().println("\n " + responseBody);
        
    }
    
    /**
     * endpoint for posting a sparql query (maybe as a stored skill with a bindingset)
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */

    /*@Consumes({"application/sparql-query", "application/sparql-results+json"})
    public void doPost(@QueryParam("asset") String asset,
                                    @Context HttpHeaders headers,
                                    @Context HttpServletRequest request,
                                    @Context HttpServletResponse response,
                                    @Context UriInfo uri
    ) { */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/sparql-query");
        String asset = req.getParameter("asset");
        monitor.debug(String.format("Received a SparQL POST request %s for asset %s", req, asset));
        executeQuery(asset, req, resp);
        return;
    }
    
    /**
     * the actual execution is done by delegating to the Fuseki engine
     *
     * @param asset target graph
     * @return a response
     */
    public Response executeQuery(String asset, HttpServletRequest request, HttpServletResponse response) {
        // extract url-encoded parameters
       
        Map<String, Object> resultMap = new HashMap<>();
        
        // Get the query string from the request URL
        String queryString = URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8);
        
        if (queryString != null) {
            // Split the query string into individual parameters
            String[] queryParams = queryString.split("&");
            for (String queryParam : queryParams) {
                // Split each parameter into key-value pair
                String[] paramKeyValue = queryParam.split("=");
                if (paramKeyValue.length == 2) {
                    // Add key-value pair to the result map
                    resultMap.put(paramKeyValue[0], paramKeyValue[1]);
                }
            }
        }
        // invoke SparqlQueryProcessor
        try {
            processor.execute(request, response, null, asset, resultMap); 
            return Response.status(response.getStatus()).build();
        } catch (WebApplicationException e) {
            return null;           
        }
    }
}