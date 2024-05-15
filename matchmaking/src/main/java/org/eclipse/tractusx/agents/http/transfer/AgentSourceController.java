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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import okhttp3.OkHttpClient;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.tractusx.agents.AgentConfig;
import org.eclipse.tractusx.agents.SkillStore;
import org.eclipse.tractusx.agents.http.HttpUtils;
import org.eclipse.tractusx.agents.rdf.RdfStore;
import org.eclipse.tractusx.agents.service.DataManagement;
import org.eclipse.tractusx.agents.service.EdcSkillStore;
import org.eclipse.tractusx.agents.sparql.SparqlQueryProcessor;
import org.eclipse.tractusx.agents.sparql.SparqlQuerySerializerFactory;
import org.eclipse.tractusx.agents.utils.Config;
import org.eclipse.tractusx.agents.utils.ConfigFactory;
import org.eclipse.tractusx.agents.utils.ConsoleMonitor;
import org.eclipse.tractusx.agents.utils.Monitor;
import org.eclipse.tractusx.agents.utils.TypeManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/agentsource")
public class AgentSourceController extends HttpServlet  {
    
    private static final long serialVersionUID = 1L;
    // EDC services
    protected Monitor monitor = null;
    protected AgentConfig config = null;
    protected SkillStore skillStore = null;

    // the actual Matchmaking Agent is a Fuseki engine
    protected SparqlQueryProcessor processor = null;
    
    
    public AgentSourceController() {
        
        this.monitor = new ConsoleMonitor();
        monitor.debug(String.format("Initializing %s", "AgentSourceController Constructor"));
        Config emptyConfig = ConfigFactory.empty();
        TypeManager typeManager = new TypeManager();
        this.config = new AgentConfig(monitor, emptyConfig);
        RdfStore rdfStore = new RdfStore(config, monitor);
        ServiceExecutorRegistry reg = new ServiceExecutorRegistry();
        SparqlQuerySerializerFactory arqQuerySerializerFactory = new SparqlQuerySerializerFactory();
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxARQ, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_10, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_11, arqQuerySerializerFactory);
        this.processor = new SparqlQueryProcessor(reg, monitor, config, rdfStore, typeManager);
        OkHttpClient httpClient = new OkHttpClient();
        DataManagement catalogService = new DataManagement(monitor, typeManager, httpClient, config);
        skillStore = new EdcSkillStore(catalogService, typeManager, config);
    }
    
    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString() + "/agentsource";
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        resp.setContentType("text/plain");
        resp.getWriter().write("Hello, " + name);
    }
    
    
    /**
     * endpoint for posting a sparql query (maybe as a stored skill with a bindingset)
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    @Consumes({"application/sparql-query", "application/sparql-results+json"})
    public Response postSparqlQuery(@QueryParam("asset") String asset,
                                    @Context HttpHeaders headers,
                                    @Context HttpServletRequest request,
                                    @Context HttpServletResponse response,
                                    @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a SparQL POST request %s for asset %s", request, asset));
        return executeQuery(asset, headers, request, response, uri);
    }
    
    /**
     * the actual execution is done by delegating to the Fuseki engine
     *
     * @param asset target graph
     * @return a response
     */
    public Response executeQuery(String asset, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri) {
        // extract url-encoded parameters
        MultivaluedMap<String, String> allQueryParams = uri.getQueryParameters();
        Map<String, Object> resultMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allQueryParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (!values.isEmpty()) {
                resultMap.put(key, values.get(0));
            }
        }
        // invoke SparqlQueryProcessor
        try {
            processor.execute(request, response, null, asset, resultMap); 
            return Response.status(response.getStatus()).build();
        } catch (WebApplicationException e) {
            return HttpUtils.respond(monitor, headers, e.getResponse().getStatus(), e.getMessage(), e.getCause());
        }
    }


}
