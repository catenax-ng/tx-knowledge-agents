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

package org.eclipse.tractusx.agents.matchmaking;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.tractusx.agents.AgentConfig;
import org.eclipse.tractusx.agents.AgreementControllerImpl;
import org.eclipse.tractusx.agents.SkillStore;
import org.eclipse.tractusx.agents.http.AgentController;
import org.eclipse.tractusx.agents.http.DelegationServiceImpl;
import org.eclipse.tractusx.agents.http.GraphController;
import org.eclipse.tractusx.agents.rdf.RdfStore;
import org.eclipse.tractusx.agents.service.DataManagement;
import org.eclipse.tractusx.agents.service.DataspaceSynchronizer;
import org.eclipse.tractusx.agents.service.EdcSkillStore;
import org.eclipse.tractusx.agents.sparql.DataspaceServiceExecutor;
import org.eclipse.tractusx.agents.sparql.SparqlQueryProcessor;
import org.eclipse.tractusx.agents.sparql.SparqlQuerySerializerFactory;
import org.eclipse.tractusx.agents.utils.Config;
import org.eclipse.tractusx.agents.utils.ConfigFactory;
import org.eclipse.tractusx.agents.utils.ConsoleMonitor;
import org.eclipse.tractusx.agents.utils.Monitor;
import org.eclipse.tractusx.agents.utils.TypeManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SharedObjectManager {
    private static final SharedObjectManager INSTANCE = new SharedObjectManager();
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final AgentConfig agentConfig;
    private final RdfStore rdfStore;
    private final ServiceExecutorRegistry reg;
    private final SparqlQueryProcessor processor;
    private final DataManagement catalogService;
    private final SkillStore skillStore;
    private final AgreementControllerImpl agreementController;
    private final AgentController agentController;
    private final GraphController graphController;
    private final DelegationServiceImpl delegationService;
    private final DataspaceSynchronizer synchronizer;
    private final OkHttpClient httpClient;
    

    private SharedObjectManager() {
        // Initialize shared objects
        this.monitor = new ConsoleMonitor();
        this.typeManager = new TypeManager();
        Properties props = new Properties();
        FileInputStream input = null;
        try {
            if (System.getProperty("property.file.location") == null) {
                input = new FileInputStream("/app/configuration.properties");
            } else {
                input = new FileInputStream(System.getProperty("property.file.location"));
            }
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Config emptyConfig = ConfigFactory.fromProperties(props);
        this.agentConfig = new AgentConfig(monitor, emptyConfig);
        this.httpClient = new OkHttpClient();
        this.catalogService = new DataManagement(monitor, typeManager, httpClient, agentConfig);
        agreementController = new AgreementControllerImpl(monitor, agentConfig, catalogService);
        this.rdfStore = new RdfStore(agentConfig, monitor);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(agentConfig.getThreadPoolSize());
        synchronizer = new DataspaceSynchronizer(executorService, agentConfig, catalogService, rdfStore, monitor);
        this.reg = new ServiceExecutorRegistry();   
        reg.addBulkLink(new DataspaceServiceExecutor(monitor, agreementController, agentConfig, httpClient, executorService, typeManager));
        SparqlQuerySerializerFactory arqQuerySerializerFactory = new SparqlQuerySerializerFactory();
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxARQ, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_10, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_11, arqQuerySerializerFactory);
        this.processor = new SparqlQueryProcessor(reg, monitor, agentConfig, rdfStore, typeManager);
        this.skillStore = new EdcSkillStore(catalogService, typeManager, agentConfig);
        this.delegationService = new DelegationServiceImpl(agreementController, monitor, httpClient, typeManager, agentConfig);
        this.agentController = new AgentController(monitor, agreementController, agentConfig, processor, skillStore, delegationService);
        this.graphController = new GraphController(monitor, rdfStore, catalogService, agentConfig);
        
    }

    public void start() {
        synchronizer.start();
    }

    public void shutdown() {
        synchronizer.shutdown();
    }

    public static String convertToCurl(Request request) {
        StringBuilder curlCommand = new StringBuilder("curl");

        // Add method
        curlCommand.append(" -X ").append(request.method());

        // Add headers
        request.headers().toMultimap().forEach((name, values) -> values.forEach(value -> curlCommand.append(" -H '").append(name).append(": ").append(value).append("'")));

        // Add request body if present
        if (request.body() != null) {
            Gson gson = new Gson();
            String bodyString = gson.toJson(request.body().toString());
            curlCommand.append(" -d '").append(bodyString).append("'");
        }

        // Add URL
        curlCommand.append(" '").append(request.url()).append("'");

        return curlCommand.toString();
    }

    public static SharedObjectManager getInstance() {
        return INSTANCE;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public AgentConfig getAgentConfig() {
        return agentConfig;
    }

    public RdfStore getRdfStore() {
        return rdfStore;
    }

    public ServiceExecutorRegistry getReg() {
        return reg;
    }

    public SparqlQueryProcessor getProcessor() {
        return processor;
    }

    public DataManagement getCatalogService() {
        return catalogService;
    }

    public SkillStore getSkillStore() {
        return skillStore;
    }

    public AgreementControllerImpl getAgreementController() {
        return agreementController;
    }

    public DelegationServiceImpl getDelegationService() {
        return delegationService;
    }

    public DataspaceSynchronizer getSynchronizer() {
        return synchronizer;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public AgentController getAgentController() {
        return agentController;
    }

    public GraphController getGraphController() {
        return graphController;
    }
    
}