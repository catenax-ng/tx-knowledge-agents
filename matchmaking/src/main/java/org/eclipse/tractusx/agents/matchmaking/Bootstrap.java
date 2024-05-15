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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.tractusx.agents.AgentConfig;
import org.eclipse.tractusx.agents.http.transfer.AgentSourceServlet;
import org.eclipse.tractusx.agents.utils.Monitor;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import sun.misc.Signal;

import java.io.IOException;

@OpenAPIDefinition(
        info = @Info(
                title = "Swagger Server",
                version = "0.8.7",
                description = "# Motivation  This API is designed along the [W3C SPARQL 1.1](https://www.w3.org/TR/sparql11-query/) specification. It represents the core interface of the Catena-X Knowledge Agents (CX KA) Architecture to enable federated (i.e. distributed, but independent) and sovereign  (i.e. collaborative, but controlled and secured) data processing over GAIA-X/IDS dataspaces.  For that purpose, this API is used in three different functions/building  blocks of CX KA:   1. As a Consumer-facing entrypoint into the dataspace (the so-called       Matchmaking Agent).    2. As a Provider-facing callback from the dataspace into the backend       (the so-called Binding Agent).   3. As an intermediate Transfer protocol between \"Sinks\" representing SPARQL Remote        Service Contexts (=sub queries/routines) and the       corresponding \"Sources\" representing backend-bound SPARQL Graph Contexts.       These Sinks and Sources are to be implemented using the       EDC (Eclipse Dataspace Components) framework.       For each of these three functions, a particular \"profile\" of this API  (here: a fragment or variant of the full-blown SPARQL 1.1 specification)  is employed:   1. The KA-MATCH profile allows to call federated SPARQL logic as stored procedures       (so-called Skills) based on a rich set of meta-data (ontology)   2. The KA-BIND profile allows to delegate non-federated and data-focussed SPARQL sub-queries by compiling them into native backend API calls (e.g. in SQL or REST).    3. The KA-TRANSFER profile allows to wrap (and unwrap) well-defined header and       protocol information from SPARQL into the generic payload of the EDC Http transfer.  This API is already designed with alternative query protocols (such as GRAPHQL  or Federated SQL) in mind.  # Examples    ## Invoke a Locally-Stored Parameterized Skill (Simple)      ```   curl --location '${KA-MATCH}/agent?asset=urn%3Acx%3ASkill%3Aconsumer%3ALifetime&(vin=WBAAL31029PZ00001&troubleCode=P0746&troubleCode=P0745)&(vin=WBAAL31029PZ00002&troubleCode=P0744)&(vin=WBAAL31029PZ00003&troubleCode=P0743)' \\    --header 'Authorization: Basic ${UuencodedUsernameColonPassword}'   ```      ## Invoke a Dataspace-Stored Parameterized Skill (Flexible)      ```   curl --location '${KA-MATCH}/agent?asset=${EDC-BUSINESSPARTNER}%23urn%3Acx%3ASkill%3Aconsumer%3ALifetime' \\   --header 'Content-Type: application/sparql-results+json' \\   --header 'Authorization: Basic ${UuencodedUsernameColonPassword}' \\   --data '{       \"head\": {           \"vars\": [               \"vin\",               \"troubleCode\"           ]       },       \"results\": {           \"bindings\": [               {                   \"vin\": {                       \"type\": \"literal\",                       \"value\": \"WBAAL31029PZ00001\"                   },                   \"troubleCode\": {                       \"type\": \"literal\",                       \"value\": \"P0746\"                   }               },               {                   \"vin\": {                       \"type\": \"literal\",                       \"value\": \"WBAAL31029PZ00001\"                   },                   \"troubleCode\": {                       \"type\": \"literal\",                       \"value\": \"P0745\"                   }               },               {                   \"vin\": {                       \"type\": \"literal\",                       \"value\": \"WBAAL31029PZ00002\"                   },                   \"troubleCode\": {                       \"type\": \"literal\",                       \"value\": \"P0744\"                   }               }           ]       }   }'   ```      ## Register a Parameterized Skill      ```   curl --location '${KA-MATCH}/agent/skill?asset=urn%3Acx%3ASkill%3Aconsumer%3ALifetime' \\   --header 'Content-Type: application/sparql-query' \\   --header 'Authorization: Basic ${UuencodedUsernameColonPassword}' \\   --data-raw 'PREFIX xsd:           <http://www.w3.org/2001/XMLSchema#>    PREFIX rdf:           <http://www.w3.org/1999/02/22-rdf-syntax-ns#>   PREFIX rdfs:          <http://www.w3.org/2000/01/rdf-schema#>   PREFIX cx:            <https://w3id.org/catenax/ontology#>    ############################################################################################   #                  Catena-X Knowledge Agents Sample Federated Skill              #                         Realizes a 5-Step Business Process                             #            \"Remaining Useful Life Prognosis based on Diagnosis TroubleCodes\"         ############################################################################################   # Preconditions:                                                                       # - A Contract Offering from OEM (e.g. BMW) to CONSUMER (e.g. ADAC)                    #   - VIN-VAN Conversion                                                               #   - DTC Analysis/Resolution (including the READING of PartType and Description)      #   - Serial Part & SUPPLIER Lookup                                                      # - A Contract Offering from SUPPLIER (e.g. ZF) to OEM                                 #   - Telematics data (including the PROPAGATION of LoadSpectrum)                        #   - RUL Prognosis Invocation (including the DISTRIBUTION of RUL results)           ############################################################################################      ####   # 5. Project the actual output of the Skill on CONSUMER side   ####   SELECT ?van ?troubleCode ?description ?affectedPart ?distanceKm ?timeDays ?vin WHERE {        ####     # 1. The CONSUMER detects a trouble code on a car in his fleet     ####     VALUES (?vin ?troubleCode) { (\"@vin\"^^xsd:string \"@troubleCode\"^^xsd:string) }.          ####     # 2. The CONSUMER looks up the OEM (connector) associated to the VIN      #    using the Federated Data Catalogue  (Catalogue=Default Graph)     ####     ?oem cx:isIssuerOfVehicleIdentificationNumber ?vin;          cx:hasConnector ?oemConnector.      ?oemConnector cx:offersAsset ?diagnoseAsset.     ?diagnoseAsset rdf:type <https://w3id.org/catenax/ontology#GraphAsset>;                    rdfs:isDefinedBy <https://w3id.org/catenax/ontology/diagnosis>.      ####     # 3. The CONSUMER delegates the following logic to the OEM (connector)     ####     SERVICE ?oemConnector {         ####       # 3.1 The OEM (e.g. BMW) anomyzes the VIN into an anomymous (VAN) node       #.    and gets some master data with it        ####       ?van cx:isAnonymousVehicle ?vin;            cx:hasRegistration ?registration.        ####       # 3.2 The OEM analyzes the DTC-affected part type (Diagnosis Graph)       ####       GRAPH ?diagnoseAsset {         ?Dtc rdf:type cx:DTC;              cx:Code ?troubleCode;             cx:affects [ cx:EnDenomination ?partType ];              cx:Description ?description.              } # OEM#Diagnosis context        ####       # 3.3 The OEM obtains fresh telematics/load-spectrum data for the vehicle       #     focussed to the problematic partType (Telematics Graph)        ####       ?van cx:latestMileageReceived ?mileage;            cx:latestDetailReceived ?telematicsDetail.       ?telematicsDetail cx:hasPartType ?partType;                         cx:hasLoadSpectrum ?loadSpectrum.        ####       # 3.4 The OEM looks up the serialized part of the VAN (Traceability Graph)       #     and the supplier address in the dataspace       ####       ?serializedPart cx:isComponentOf+ ?van;                       cx:hasPartType ?partType;                       cx:hasName ?affectedPart;                       cx:hasSupplier [                           cx:hasConnector ?tieraConnector                       ].                   ?tieraConnector cx:offersAsset ?prognosisAsset.       ?prognosisAsset rdfs:isDefinedBy </prognosis>.        ####       # 4. The OEM (and not the CONSUMER) delegates to the SUPPLIER (connector)       #    which means that load spectrum data etc is only exchanged using their       #    contract and between their connectors.       ####       SERVICE ?tieraConnector {           ####         # 4.1 The SUPPLIER adds additional measurement information         ####         ?telematicsDetail cx:hasFile ?loadSpectrumFile;                           cx:hasHeader ?loadSpectrumHeader.          ####         # 4.2 The SUPPLIER invokes a prognosis model associated the part type using the load-spectrum data         ####         GRAPH ?prognosisAsset {            ?invocation rdf:type cx:LifetimePrognosis;                              # <--General vehicle info               cx:loadCollectiveMileage ?mileage;               cx:loadCollectiveRegistrationDate ?registration;                # <--Part Info from the OEM               cx:loadCollectiveComponent ?affectedPart;               cx:loadCollectiveBody ?loadSpectrum;                              # <--Additional info from the SUPPLIER               cx:loadCollectiveFile ?loadSpectrumFile;               cx:loadCollectiveHeader ?loadSpectrumHeader;                               # -->the actual prognosis output               cx:remainingDistance ?distanceKm;                cx:remainingTime ?timeDays.                  } # SUPPLIER#Prognosis context            } # SUPPLIER context      } # OEM context         # now we do reporting/operationalising on the CONSUMER side   } ORDER BY ?remainingDistance LIMIT 5'   ```    ## Invoke an Ad-hoc Query       ```   curl --location '${KA-BIND}/agent' \\   --header 'Content-Type: application/sparql-query' \\   --header 'Authorization: Basic ${UuencodedUsernameColonPassword}' \\   --data-raw 'PREFIX xsd:           <http://www.w3.org/2001/XMLSchema#>    PREFIX rdf:           <http://www.w3.org/1999/02/22-rdf-syntax-ns#>   PREFIX rdfs:          <http://www.w3.org/2000/01/rdf-schema#>   PREFIX cx:            <https://w3id.org/catenax/ontology#>    # Sample Graph Context that is Delegated/Instantiated to a Binding Agent Call   SELECT ?partType ?description WHERE {        VALUES (?troubleCode) { (\"P0745\"^^xsd:string) (\"P0746\"^^xsd:string) }.           ?Dtc rdf:type cx:DTC;              cx:Code ?troubleCode;             cx:affects [ cx:EnDenomination ?partType ];              cx:Description ?description.              }    ```    ",
                termsOfService = "http://catenax.io",
                contact = @Contact(email = "info@catenax.io"),
                license = @License(
                        name = "Eclipse Public License",
                        url = "https://www.eclipse.org/legal/epl-2.0/"
                )
        )
)

public class Bootstrap {

    private Monitor monitor;
    Server server;
    SharedObjectManager sharedObjectManager;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     */
    public Bootstrap() {
        handleSignal("TERM");
        this.sharedObjectManager = SharedObjectManager.getInstance();
        this.monitor = sharedObjectManager.getMonitor();

        // Create Jetty server
        this.server = new Server();
        HandlerCollection handlerList = new HandlerCollection();
        server.setHandler(handlerList);

        // For AgentSourceController endpoint (internal endpoints) 
        AgentConfig conf = sharedObjectManager.getAgentConfig();
        int port1 = conf.getMatchmakingPort();
        monitor.debug(String.format("Configuring server on port %s", port1));
        ServletContextHandler handler1 = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler1.setContextPath("/");
        ResourceConfig resourceConfig1 = new ResourceConfig();
        monitor.debug(String.format("Registering %s", "AgentSourceController Constructor"));
        resourceConfig1.register(AgentSourceServlet.class);
        handler1.addServlet(AgentSourceServlet.class, conf.getMatchmakingPath());
        ServerConnector connector1 = new ServerConnector(server);
        connector1.setPort(port1);
        connector1.setName("conn1");
        handler1.setVirtualHosts(new String[]{"@conn1"});
        handlerList.addHandler(handler1);
        server.addConnector(connector1); 

        // For AgentController and GraphController endpoints (public endpoints) 
        int port2 = conf.getDefaultPort();
        monitor.debug(String.format("Configuring server on port %s", port2));
        ServletContextHandler handler2 = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler2.setContextPath(conf.getDefaultPath());
        ResourceConfig resourceConfig2 = new ResourceConfig();
        monitor.debug("registering AgentController");
        resourceConfig2.register(sharedObjectManager.getAgentController());
        monitor.debug("registering GraphController");
        resourceConfig2.register(sharedObjectManager.getGraphController());
        handler2.addServlet(new ServletHolder(new ServletContainer(resourceConfig2)), "/*");
        ServerConnector connector2 = new ServerConnector(server);
        connector2.setPort(port2);
        connector2.setName("conn2");
        handler2.setVirtualHosts(new String[]{"@conn2"});
        handlerList.addHandler(handler2);
        server.addConnector(connector2);
        
        // For AgreementController endpoint (callback endpoint between control plane and matchmaking agent) 
        int port3 = conf.getCallbackPort();
        monitor.debug(String.format("Configuring server on port %s", port3));
        ServletContextHandler handler3 = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler3.setContextPath(conf.getCallbackPath());
        ResourceConfig resourceConfig3 = new ResourceConfig();
        monitor.debug(String.format("Registering agreement controller %s", sharedObjectManager.getAgreementController()));
        resourceConfig3.register(sharedObjectManager.getAgreementController());
        handler3.addServlet(new ServletHolder(new ServletContainer(resourceConfig3)), "/*");
        ServerConnector connector3 = new ServerConnector(server);
        connector3.setPort(port3);
        connector3.setName("conn3");
        handler3.setVirtualHosts(new String[]{"@conn3"});
        handlerList.addHandler(handler3);
        server.addConnector(connector3);

        // Start the server
        monitor.debug("Trying to start the server");
        try {
            server.start();
            sharedObjectManager.start();
            server.join();
        } catch (Exception e) {
            sharedObjectManager.shutdown();
            e.printStackTrace();
        }

    }

    /**
     * Main method.
     *
     * @param args arguments
     * @throws IOException in case some strange things happen
     */
    public static void main(String[] args) throws IOException {
        new Bootstrap();
        System.out.println("Started Matchmaking Agent.");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void shutdown() {
        System.out.println("Shutdown initiated");
        server.destroy();
        SharedObjectManager.getInstance().shutdown();
        System.exit(0);
    }

    private void handleSignal(String name) {
        Signal signal = new Signal(name);
        Signal.handle(signal, signalEvent -> {
            System.out.println("Signal received: " + signal.getName());
            if ("TERM".equals(signal.getName())) {
                shutdown();
            }
        });
    }
}
