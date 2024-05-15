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
package org.eclipse.tractusx.agents.remoting;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.*;

import org.eclipse.rdf4j.query.*;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.common.iteration.Iterations;

import org.eclipse.rdf4j.rio.*;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.tractusx.agents.remoting.config.*;
import org.junit.jupiter.api.Test;

/**
 * The main test for a remoting-enabled SparQL endpoint
 */
public class RemotingSailTest {

    public static String REPO_NAMESPACE = "http://www.openrdf.org/config/repository#";

    /**
     * tests parsing a config
     */
    @Test
    public void testConfig() throws Exception {
        Model graph = Rio.parse(RemotingSailTest.class.getResourceAsStream("/config.ttl"), REPO_NAMESPACE,
                RDFFormat.TURTLE);
        RemotingSailConfig rsc = new RemotingSailConfig(RemotingSailFactory.SAIL_TYPE);
        rsc.parse(graph, Models.subjectBNode(graph.filter(null, rsc.getValueFactory().createIRI("http://www.openrdf.org/config/sail#", "sailType"), rsc.getValueFactory().createLiteral("org.eclipse.tractusx.agents:Remoting"))).get());
        rsc.validate();
        assertEquals(5, rsc.listServices().size(), "correct number of invocation configs");
        ServiceConfig health = rsc.getService("https://w3id.org/catenax/ontology/health#HealthIndication");
        assertEquals(100, health.getBatch(), "Correct batch size");
        assertEquals("https://w3id.org/catenax/ontology/health#requestComponentId", health.getResult().getCorrelationInput(), "Correct correlation input");
        ServiceConfig rul = rsc.getService("https://w3id.org/catenax/ontology/rul#RemainingUsefulLife");
        assertNotNull(rul.getCallbackProperty(), "Correct asynchronous mode");
        ArgumentConfig notificationTemplate = rul.getArguments().get("https://w3id.org/catenax/ontology/rul#notification");
        assertNotNull(notificationTemplate, "Found the notification template argument");
        assertEquals(-1, notificationTemplate.getPriority(), "Notification template has default value");
        assertNotNull(notificationTemplate.getDefaultValue(), "Notification template has default value");
        ArgumentConfig component = rul.getArguments().get("https://w3id.org/catenax/ontology/rul#component");
        assertNotNull(component, "Found the component argument");
        assertTrue(component.isFormsBatchGroup(), "Component is marked as batch group");
        ReturnValueConfig responseResult = rul.getResult().getOutputs().get("https://w3id.org/catenax/ontology/rul#content");
        assertNotNull(responseResult, "Notification content found");
    }

    /**
     * tests basic invocation features
     */
    @Test
    public void testInvocation() {

        RemotingSailConfig rsc = new RemotingSailConfig(RemotingSailFactory.SAIL_TYPE);
        ServiceConfig ic = new ServiceConfig();
        rsc.putService("https://w3id.org/catenax/ontology/prognosis#Prognosis", ic);
        ic.setTargetUri("class:org.eclipse.tractusx.agents.remoting.test.TestFunction#test");
        ArgumentConfig ac = new ArgumentConfig();
        ac.setArgumentName("arg0");
        ic.getArguments().put("https://w3id.org/catenax/ontology/prognosis#input-1", ac);
        ac = new ArgumentConfig();
        ac.setArgumentName("arg1");
        ic.getArguments().put("https://w3id.org/catenax/ontology/prognosis#input-2", ac);
        ResultConfig rc = new ResultConfig();
        ic.setResult(rc);
        ic.setResultName("https://w3id.org/catenax/ontology/prognosis#Result");

        ReturnValueConfig rvc = new ReturnValueConfig();

        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#output", rvc);
        rsc.validate();

        Repository rep = new SailRepository(new RemotingSail(rsc));

        try (RepositoryConnection conn = rep.getConnection()) {
            TupleQuery query = (TupleQuery) conn.prepareQuery(QueryLanguage.SPARQL,
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX prognosis: <https://w3id.org/catenax/ontology/prognosis#> " +
                            "SELECT ?invocation ?output " +
                            "WHERE { " +
                            "?invocation a prognosis:Prognosis; " +
                            "            prognosis:input-1 \"1\"^^xsd:string; " +
                            "            prognosis:input-2 \"2\"^^xsd:string; " +
                            "            prognosis:output ?output. " +
                            //"            prognosis:hasResult [ prognosis:output ?output ]. "+
                            "}");
            final TupleQueryResult result = query.evaluate();
            final String[] names = result.getBindingNames().toArray(new String[0]);
            assertEquals(2, names.length, "Correct number of binding names");
            assertEquals("invocation", names[0], "Correct binding name 1");
            assertEquals("output", names[1], "Correct binding name 2");
            java.util.List<BindingSet> bindings = Iterations.asList(result);
            assertEquals(1, bindings.size(), "Correct number of bindings");
            BindingSet firstBindingSet = bindings.get(0);
            assertTrue(firstBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(firstBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertEquals(2, firstBindingSet.size(), "Correct number of variables in binding 0");
            assertTrue(firstBindingSet.getBindingNames().contains("output"), "Found output binding");
            assertEquals("3", firstBindingSet.getValue("output").stringValue());
        }
    }

    /**
     * tests basic invocation features
     */
    @Test
    public void testRemoting() {

        RemotingSailConfig rsc = new RemotingSailConfig(RemotingSailFactory.SAIL_TYPE);
        ServiceConfig ic = new ServiceConfig();
        rsc.putService("https://w3id.org/catenax/ontology/prognosis#Prognosis", ic);
        ic.setTargetUri("https://api.agify.io");
        ArgumentConfig ac = new ArgumentConfig();
        ac.setArgumentName("name");
        ic.getArguments().put("https://w3id.org/catenax/ontology/prognosis#name", ac);
        ReturnValueConfig rvc = new ReturnValueConfig();
        rvc.setPath("age");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        ResultConfig rc = new ResultConfig();
        ic.setResult(rc);
        ic.setResultName("https://w3id.org/catenax/ontology/prognosis#Result");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#prediction", rvc);
        rvc = new ReturnValueConfig();
        rvc.setPath("count");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#count", rvc);
        rsc.validate();

        Repository rep = new SailRepository(new RemotingSail(rsc));

        try (RepositoryConnection conn = rep.getConnection()) {
            TupleQuery query = (TupleQuery) conn.prepareQuery(QueryLanguage.SPARQL,
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX prognosis: <https://w3id.org/catenax/ontology/prognosis#> " +
                            "SELECT ?invocation ?prediction ?count " +
                            "WHERE { " +
                            "?invocation a prognosis:Prognosis; " +
                            "            prognosis:name \"Schorsch\"^^xsd:string; " +
                            "            prognosis:prediction ?prediction; " +
                            "            prognosis:count ?count. " +
                            "}");
            final TupleQueryResult result = query.evaluate();
            final String[] names = result.getBindingNames().toArray(new String[0]);
            assertEquals(3, names.length, "Correct number of binding names");
            assertEquals("invocation", names[0], "Correct binding name 1");
            assertEquals("prediction", names[1], "Correct binding name 2");
            assertEquals("count", names[2], "Correct binding name 3");
            java.util.List<BindingSet> bindings = Iterations.asList(result);
            assertEquals(1, bindings.size(), "Correct number of bindings");
            BindingSet firstBindingSet = bindings.get(0);
            assertTrue(firstBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(firstBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertEquals(3, firstBindingSet.size(), "Correct number of variables in binding 0");
            assertTrue(firstBindingSet.getBindingNames().contains("prediction"), "Found prediction binding");
            assertTrue(61 <= ((Literal) firstBindingSet.getValue("prediction")).intValue(), "Correct prediction value");
            assertTrue(firstBindingSet.getBindingNames().contains("count"), "Found count binding");
            assertTrue(((Literal) firstBindingSet.getValue("count")).intValue() > 50, "Correct cound value");
        }
    }

    /**
     * tests basic invocation features
     */
    @Test
    public void testRemotingBatch() {

        RemotingSailConfig rsc = new RemotingSailConfig(RemotingSailFactory.SAIL_TYPE);
        ServiceConfig ic = new ServiceConfig();
        rsc.putService("https://w3id.org/catenax/ontology/prognosis#Prognosis", ic);
        ic.setTargetUri("https://api.agify.io");
        ArgumentConfig ac = new ArgumentConfig();
        ac.setArgumentName("name");
        ic.getArguments().put("https://w3id.org/catenax/ontology/prognosis#name", ac);
        ReturnValueConfig rvc = new ReturnValueConfig();
        rvc.setPath("age");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        ResultConfig rc = new ResultConfig();
        ic.setResult(rc);
        ic.setResultName("https://w3id.org/catenax/ontology/prognosis#Result");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#prediction", rvc);
        rvc = new ReturnValueConfig();
        rvc.setPath("count");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#count", rvc);
        rsc.validate();

        Repository rep = new SailRepository(new RemotingSail(rsc));

        try (RepositoryConnection conn = rep.getConnection()) {
            TupleQuery query = (TupleQuery) conn.prepareQuery(QueryLanguage.SPARQL,
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX prognosis: <https://w3id.org/catenax/ontology/prognosis#> " +
                            "SELECT ?invocation ?input ?prediction ?count " +
                            "WHERE { " +
                            " VALUES(?input) { (\"Schorsch\"^^xsd:string) (\"Christoph\"^^xsd:string)}" +
                            " ?invocation a prognosis:Prognosis; " +
                            "             prognosis:name ?input; " +
                            "             prognosis:prediction ?prediction; " +
                            "             prognosis:count ?count. " +
                            "}");
            final TupleQueryResult result = query.evaluate();
            final String[] names = result.getBindingNames().toArray(new String[0]);
            assertEquals(4, names.length, "Correct number of binding names");
            assertEquals("invocation", names[0], "Correct binding name 1");
            assertEquals("input", names[1], "Correct binding name 2");
            assertEquals("prediction", names[2], "Correct binding name 3");
            assertEquals("count", names[3], "Correct binding name 4");
            java.util.List<BindingSet> bindings = Iterations.asList(result);
            assertEquals(2, bindings.size(), "Correct number of bindings");
            BindingSet firstBindingSet = bindings.get(0);
            assertEquals(4, firstBindingSet.size(), "Correct number of variables in binding 0");
            assertTrue(firstBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(firstBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertTrue(firstBindingSet.getBindingNames().contains("input"), "Found input binding");
            assertEquals("Schorsch", firstBindingSet.getValue("input").stringValue());
            assertTrue(firstBindingSet.getBindingNames().contains("prediction"), "Found prediction binding");
            assertTrue(61 <= ((Literal) firstBindingSet.getValue("prediction")).intValue(), "Correct prediction value");
            assertTrue(firstBindingSet.getBindingNames().contains("count"), "Found count binding");
            assertTrue(((Literal) firstBindingSet.getValue("count")).intValue() > 50, "Correct count value");
            BindingSet secondBindingSet = bindings.get(1);
            assertEquals(4, secondBindingSet.size(), "Correct number of variables in binding 1");
            assertTrue(secondBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(secondBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertTrue(secondBindingSet.getBindingNames().contains("input"), "Found input binding");
            assertEquals("Christoph", secondBindingSet.getValue("input").stringValue());
            assertTrue(secondBindingSet.getBindingNames().contains("prediction"), "Found prediction binding");
            assertTrue(41 <= ((Literal) secondBindingSet.getValue("prediction")).intValue(), "Correct prediction value");
            assertTrue(secondBindingSet.getBindingNames().contains("count"), "Found count binding");
            assertTrue(((Literal) secondBindingSet.getValue("count")).intValue() > 15000, "Correct count value");
        }
    }

    /**
     * tests basic invocation features
     */
    @Test
    public void testRemotingResult() throws JsonProcessingException {

        RemotingSailConfig rsc = new RemotingSailConfig(RemotingSailFactory.SAIL_TYPE);
        ServiceConfig ic = new ServiceConfig();
        rsc.putService("https://w3id.org/catenax/ontology/prognosis#Prognosis", ic);
        ic.setTargetUri("https://api.agify.io");
        ArgumentConfig ac = new ArgumentConfig();
        ac.setArgumentName("name");
        ic.getArguments().put("https://w3id.org/catenax/ontology/prognosis#name", ac);
        ReturnValueConfig rvc = new ReturnValueConfig();
        rvc.setPath("age");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        ResultConfig rc = new ResultConfig();
        ic.setResult(rc);
        ic.setResultName("https://w3id.org/catenax/ontology/prognosis#Result");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#prediction", rvc);
        rvc = new ReturnValueConfig();
        rvc.setPath("count");
        rvc.setDataType("http://www.w3.org/2001/XMLSchema#int");
        rc.getOutputs().put("https://w3id.org/catenax/ontology/prognosis#count", rvc);
        rsc.validate();

        Repository rep = new SailRepository(new RemotingSail(rsc));

        try (RepositoryConnection conn = rep.getConnection()) {
            TupleQuery query = (TupleQuery) conn.prepareQuery(QueryLanguage.SPARQL,
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "PREFIX prognosis: <https://w3id.org/catenax/ontology/prognosis#> " +
                            "SELECT ?invocation ?input ?response " +
                            "WHERE { " +
                            " VALUES(?input) { (\"Schorsch\"^^xsd:string) (\"Christoph\"^^xsd:string)}" +
                            " ?invocation a prognosis:Prognosis; " +
                            "             prognosis:name ?input; " +
                            "             prognosis:Result ?response. " +
                            "}");
            final TupleQueryResult result = query.evaluate();
            final String[] names = result.getBindingNames().toArray(new String[0]);
            assertEquals(3, names.length, "Correct number of binding names");
            assertEquals("invocation", names[0], "Correct binding name 1");
            assertEquals("input", names[1], "Correct binding name 2");
            assertEquals("response", names[2], "Correct binding name 3");
            java.util.List<BindingSet> bindings = Iterations.asList(result);
            assertEquals(2, bindings.size(), "Correct number of bindings");
            BindingSet firstBindingSet = bindings.get(1);
            assertEquals(3, firstBindingSet.size(), "Correct number of variables in binding 0");
            assertTrue(firstBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(firstBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertTrue(firstBindingSet.getBindingNames().contains("input"), "Found input binding");
            assertEquals("Schorsch", firstBindingSet.getValue("input").stringValue());
            assertTrue(firstBindingSet.getBindingNames().contains("response"), "Found response binding");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode response = mapper.readTree(((Literal) firstBindingSet.getValue("response")).stringValue());
            assertTrue(61 <= response.get("age").intValue(), "Correct age response property");
            assertTrue(response.get("count").intValue() > 50, "Correct count response property");
            BindingSet secondBindingSet = bindings.get(0);
            assertEquals(3, secondBindingSet.size(), "Correct number of variables in binding 1");
            assertTrue(secondBindingSet.getBindingNames().contains("invocation"), "Found invocation binding");
            assertTrue(secondBindingSet.getValue("invocation").stringValue().startsWith("https://w3id.org/catenax/ontology/prognosis#"), "Invocation binding has the right prefix");
            assertTrue(secondBindingSet.getBindingNames().contains("input"), "Found input binding");
            assertEquals("Christoph", secondBindingSet.getValue("input").stringValue());
            assertTrue(secondBindingSet.getBindingNames().contains("response"), "Found response binding");
            response = mapper.readTree(((Literal) secondBindingSet.getValue("response")).stringValue());
            assertTrue(41 <= response.get("age").intValue(), "Correct age response property");
            assertTrue(response.get("count").intValue() > 15000, "Correct count response property");
        }
    }


}