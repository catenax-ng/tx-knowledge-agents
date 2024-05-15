<!--
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0

-->
# Tractus-X Remoting Agent (KA-RMT)

KA-RMT is a module of the [Tractus-X Knowledge Agents Reference Implementations](../README.md).

## About this Module

This is a folder providing a FOSS implementations of a Function Binding (aka Remoting) Agent.

Binding Agents are needed by any Agent-Enabled dataspace providers to connect the dataspace protocol/representation (here: a combination of the SPARQL query language/operating on RDF triple graphs) to the underlying business data and logic.

The Remoting Agent in particular is able to interact with typical REST backend services based on XML and/or JSON payloads. The SPARQL profile which is used is called KA-BIND-F (the Knowledge Agent Functional Binding Profile).

## Architecture

Tractus-X Remoting Agent is built on [Eclipse RDF4J](https://rdf4j.org) by introducing a (virtual) storage and inference layer (SAIL) that is indeed is backed by local Java classes and/or remote REST services.

![Remoting Agent Architecture](../docs/RemotingRDF4J.drawio.svg)

The [Remoting SAIL](src/main/java/org/eclipse/tractusx/agents/remoting/RemotingSail.java) provides the implementation of a so-called repository (=a SPARQL endpoint that can be used as the target address of an EDC asset and/or as a service context in federated queries).

Each remoting repository is configured in a TTL (Terse Triple Language) [configuration file](src/test/resources/config.ttl) using the `org.eclipse.tractusx.agents:Remoting` Sailtype and lists
the available functions (=a particular class of nodes). Each remoting repository is parsed, represented and possibly exported from the runtime by means of [Remoting SAIL Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/RemotingSailConfig.java), for each function (class), a corresponding [Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java) is maintained.

Here is an example repository configuration which uses the public service `https://api.agify.io` to make a prediction of the age of a person given its name.

```ttl
# ... PREFIX declarations

[] rdf:type rep:Repository ;
   rep:repositoryID "prognosis" ;
   rdfs:label "Prognosis Functions" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "org.eclipse.tractusx.agents:Remoting" ;
         cx-fx:callbackAddress <http://localhost:8888/callback>;
         cx-fx:supportsInvocation prognosis:Prognosis;
      ]
   ].

# Function declaration

prognosis:Prognosis rdf:type cx-fx:Function;
  dcterms:description "Prognosis is a sample simulation function with input and output bindings."@en ;
  dcterms:title "Prognosis" ;
  cx-fx:targetUri "https://api.agify.io";
  cx-common:authenticationCode "Dummy-Authorization";
  cx-common:authenticationKey "Dummy-Key";
  cx-fx:input prognosis:name;
  cx-fx:result prognosis:hasResult.

prognosis:name rdf:type cx-fx:Argument;
  dcterms:description "Name is an argument to the Prognosis function."@en ;
  dcterms:title "Name";
  cx-fx:argumentName "name".

prognosis:hasResult rdf:type cx-fx:Result;
  cx-fx:output prognosis:prediction;
  cx-fx:output prognosis:support.

prognosis:prediction rdf:type cx-fx:ReturnValue;
   dcterms:description "Prediction (Value) is an integer-based output of the Prognosis function."@en ;
   dcterms:title "Prediction" ;
   cx-fx:valuePath "age";
   cx-fx:dataType xsd:int.

prognosis:support rdf:type cx-fx:ReturnValue;
   dcterms:description "Support (Value) is another integer-based output of the Prognosis function."@en ;
   dcterms:title "Support" ;
   cx-fx:valuePath "count";
   cx-fx:dataType xsd:int.

# Further declarations ...
```

When a concrete query is sent to the RDF4J controller, a [Remoting SAIL Connection](src/main/java/org/eclipse/tractusx/agents/remoting/RemotingSailConnection.java)
is setup which evaluates the query with the help of the stateful [Query Executor](src/main/java/org/eclipse/tractusx/agents/remoting/QueryExecutor.java). With the help of the Remoting Sail Configuraiton, The Query Executor instantiates all function nodes as [Invocations](src/main/java/org/eclipse/tractusx/agents/remoting/Invocation.java). During processing, input and output variables are accessed and stored in a BindingSet Collection for which the Query Executor implements the [IBindingHost](src/main/java/org/eclipse/tractusx/agents/remoting/IBindingHost.java).

```sparql
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX prognosis: <https://w3id.org/catenax/ontology/prognosis#>

SELECT ?name ?invocation ?prediction ?support
WHERE {
  VALUES (?name) { "Schorsch"^^xsd:string }
  ?invocation a prognosis:Prognosis;
                prognosis:name ?name;
                prognosis:prediction ?prediction;
                prognosis:support ?support.
}
```

Currently, there are two principle methods of function bindings available
* Class Binding ([Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java).targetUri follows the pattern "class:<className/>#<methodName/>")
* REST Binding ([Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java)targetUri follows the pattern "https?://<url>")

For REST Binding, we support the following outgoing request formats/content types (being configured via [Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java).method). Note that responses are always interpreted as XML or JSON depending on the response content type.
* GET: Input arguments are mapped to URL query parameters.
* POST-XML: Input Arguments are mapped into an XML document body with content-type "application/xml" T
* POST-JSON: Input Arguments are mapped into an JSON document body with content-type "application/json"
* POST-JSON-MF: Input Arguments are mapped into JSON-based files send in a multi-part  request with content-type multipart/form-data (and XXX as boundary and "application/json" as Content-Disposition)
* POST-XML-MF: Input Arguments are mapped into XML-based files send in a multi-part request with content-type multipart/form-data (and XXX as boundary and application/json as Content-Disposition)

Invocations can be batched. Normally ([Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java).batch is set to 1, no [Argument Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ArgumentConfig.java).formsBatchGroup is set to true) Remoting Agent will produce an outgoing REST call for each incoming tuple/binding. If [Service Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ServiceConfig.java).batch is greater than 1 or
there is some [Argument Config](src/main/java/org/eclipse/tractusx/agents/remoting/config/ArgumentConfig.java).formsBatchGroup set to true, several tuples/bindings can be sent in a single invocation (usually in an array or by using flexible argument paths using '{<iriofinput>}' path elements). In that case, we also expect the responses to contain several individual results which are mapped/joined with the original input bindings using the ResultConfig.correlationInput reference.

Invocation can be asynchronous. That means that the called backend will not return a proper response, just a successful notification code. Instead we send the public URL of the builtin [CallbackController](src/main/java/org/eclipse/tractusx/agents/remoting/callback/CallbackController.java) which is configured in the callbackAddress property of the remoting repository (and is transmitted in the callbackAddressProperty of the ServiceConfig). In order to correlate outgoing (batch) requests with asynchronous responses sent to the CallbackController, we rely on setting a unique request identifier specified in ServiceConfig.invocationIdProperty and comparing it with the content of the ResultConfig.callbackProperty

## Deployment

### Compile, Test & Package

```console
mvn package
```

This will generate
- a [standalone jar](target/remoting-agent-1.12.19-SNAPSHOT.jar) containing all necessary rdf4j components to build your own repository server.
- a [pluging jar](target/original-remoting-agent-1.12.19-SNAPSHOT.jar) which maybe dropped into an rdf4j server for remoting support.

### Run Locally

The standalone jar](target/remoting-agent-1.12.19-SNAPSHOT.jar) contains an example application that runs a sample repository against a sample source

```console
java -jar target/remoting-agent-1.12.19-SNAPSHOT.jar -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```

### Containerizing

You could either call

```console
mvn install -Pwith-docker-image
```

or invoke the following docker command after a successful package run

```console
docker build -t tractusx/remoting-agent:1.12.19-SNAPSHOT -f src/main/docker/Dockerfile .
```

This will create a docker image including an extended rdf4j-server as well as an interactive rdf4j-workbench.

To run the docker image, you could invoke this command

```console
docker run -p 8081:8081 \
  -v $(pwd)/src/test:/var/rdf4j/config \
  tractusx/remoting-agent:1.12.19-SNAPSHOT
````

Afterwards, you should be able to access the [local SparQL endpoint](http://localhost:8081/) via
the browser or by directly invoking a query

```console
curl --location --request POST 'http://localhost:8081/resources' \
--header 'Content-Type: application/sparql-query' \
--header 'Accept: application/json' \
--data-raw 'PREFIX : <http://example.org/voc#>

SELECT ?x
WHERE {
   ?x a :Professor .
}'
```

You may manipulate any of the following environment variables to configure the image behaviour.
Note that there is no builtin security (ssl/auth) for the exposed endpoints.
This must be provided by hiding them in an appropriate service network layer.

| ENVIRONMENT VARIABLE | Required | Example                                                              | Description            | List |
|----------------------|----------|----------------------------------------------------------------------|------------------------|------|
| JAVA_TOOL_OPTIONS    |          | -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8090 | JMV (Debugging option) | X    |

### Notice for Docker Image

DockerHub: https://hub.docker.com/r/tractusx/remoting-agent

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/knowledge-agents/tree/main/remoting
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile: https://github.com/eclipse-tractusx/knowledge-agents/blob/main/remoting/src/main/docker/Dockerfile
- Project license: Apache License, Version 2.0

**Used base image**

- [eclipse-temurin:21-jre-alpine](https://github.com/adoptium/containers)
- Official Eclipse Temurin DockerHub page: https://hub.docker.com/_/eclipse-temurin
- Eclipse Temurin Project: https://projects.eclipse.org/projects/adoptium.temurin
- Additional information about the Eclipse Temurin images: https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.

### Helm Chart

A helm chart for deploying the remoting agent can be found under [this folder](../charts/remoting-agent).

It can be added to your umbrella chart.yaml by the following snippet

```console
dependencies:
  - name: remoting-agent
    repository: https://eclipse-tractusx.github.io/charts/dev
    version: 1.12.19-SNAPSHOT
    alias: my-remoting-agent
```

and then installed using

```console
helm dependency update
```

In your values.yml, you configure your specific instance of the remoting agent like this

```console
##############################################################################################
# API Binding Agent
##############################################################################################

my-remoting-agent:
  securityContext: *securityContext
  nameOverride: my-remoting-agent
  fullnameOverride: my-remoting-agent
  ingresses:
    - enabled: true
      # -- The hostname to be used to precisely map incoming traffic onto the underlying network service
      hostname: "my-remoting-agent.public-ip"
      annotations:
        nginx.ingress.kubernetes.io/rewrite-target: /$1
        nginx.ingress.kubernetes.io/use-regex: "true"
      # -- Agent endpoints exposed by this ingress resource
      endpoints:
        - default
      tls:
        enabled: true
        secretName: my-remoting-tls
  repositories:
    prognosis: |-
      #
      # Rdf4j configuration for prognosis sample
      #
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
      @prefix rep: <http://www.openrdf.org/config/repository#>.
      @prefix sr: <http://www.openrdf.org/config/repository/sail#>.
      @prefix sail: <http://www.openrdf.org/config/sail#>.
      @prefix sp: <http://spinrdf.org/sp#>.
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix json: <https://json-schema.org/draft/2020-12/schema#> .
      @prefix dcterms: <http://purl.org/dc/terms/> .
      @prefix cx-fx: <https://w3id.org/catenax/ontology/function#>.
      @prefix prognosis: <https://w3id.org/prognosis>.

    [] rdf:type rep:Repository ;
       rep:repositoryID "prognosis" ;
       rdfs:label "Prognosis Functions" ;
       rep:repositoryImpl [
          rep:repositoryType "openrdf:SailRepository" ;
          sr:sailImpl [
             sail:sailType "org.eclipse.tractusx.agents:Remoting" ;
             cx-fx:callbackAddress <https://my-remoting-agent.public-ip/callback>;
             cx-fx:supportsInvocation prognosis:Prognosis;
          ]
       ].

    # Function declaration

    prognosis:Prognosis rdf:type cx-fx:Function;
      dcterms:description "Prognosis is a sample simulation function with input and output bindings."@en ;
      dcterms:title "Prognosis" ;
      cx-fx:targetUri "https://api.agify.io";
      cx-common:authenticationCode "Dummy-Authorization";
      cx-common:authenticationKey "Dummy-Key";
      cx-fx:input prognosis:name;
      cx-fx:result prognosis:hasResult.

    prognosis:name rdf:type cx-fx:Argument;
      dcterms:description "Name is an argument to the Prognosis function."@en ;
      dcterms:title "Name";
      cx-fx:argumentName "name".

    prognosis:hasResult rdf:type cx-fx:Result;
      cx-fx:output prognosis:prediction;
      cx-fx:output prognosis:support.

    prognosis:prediction rdf:type cx-fx:ReturnValue;
       dcterms:description "Prediction (Value) is an integer-based output of the Prognosis function."@en ;
       dcterms:title "Prediction" ;
       cx-fx:valuePath "age";
       cx-fx:dataType xsd:int.

    prognosis:support rdf:type cx-fx:ReturnValue;
       dcterms:description "Support (Value) is another integer-based output of the Prognosis function."@en ;
       dcterms:title "Support" ;
       cx-fx:valuePath "count";
       cx-fx:dataType xsd:int.

```
