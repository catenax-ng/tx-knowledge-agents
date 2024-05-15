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

# Tractus-X Matchmaking Agent

The Matchmaking Agent is a module of the [Tractus-X Knowledge Agents Reference Implementations](../README.md).

## About this Module

Up to now the Matchmaking Agent's functionality was integrated into the Agent Plane Container.
This container separates the Matchmaking Agent's functionality from the Agent Plane Container.
As a result, the RDF Store is disjoined from the public-facing Agent Plane container.
It caters for the need of small and medium sized companies who want to directly upload/provide use case data as well as for
security-aware companies who want to separate data storage from public API even for meta-data.
This container also has the ability to be used by the Simple Graph Exchanger Feature, adding the ability to host business data from the RDF Store over the Matchmaking Agent
Communication between the Agent Plane Container and the Matchmaking Agent by a REST API on the docker (internal) network.

## Architecture

The FOSS Matchmaking Agent is built using [Jersey](https://github.com/eclipse-ee4j/jersey) (for the REST endpoints) and [RDF4J](https://rdf4j.org/) (for SPARQL parsing). It exposes three endpoints which are to be certified/tested in the KA architecture:
* With the '/api/agent' endpoint the EDC tenant can issue queries and execute skills in interaction with local resources and the complete Dataspace (the so-called Matchmaking Agent that is also hit by incoming Agent transfers).
* With the '/api/graph' endpoint the EDC tenant can manage graph content which can be published as assets.
* With the '/agentsource' endpoint communication with the EDC Dataplane is carried out. However, this is a docker internal endpoint and can not be accessed by a user from the internet

The Matchmaking Agent does not perform any logic but just returns pre-rendered results depending on the input parameters.

![Source Code](docs/MatchmakingAgentDiagram.drawio.svg)


Architecture is similar to the one shown in Tractus-X Knowledge Agents EDC Extensions (KA-EDC). However the AgentSource Object within the Dataplane accesses the AgentSourceController of the MatchmakingAgent by a REST API.

### Security

The conforming agent will be configurable to support the following http/s based security mechanisms:
- Basic Authentication
- Oauth2

## Deployment

### Compile, Test, Package and Containerization

```console
mvn -Pwith-docker-image
```

This will generate

- a jar file in the respective target folder
- a docker image within the local docker repository which can be started with 

### Run Locally

The [standalone jar](target/matchmaking-agent-1.12.19-SNAPSHOT.jar) may be started as follows

```console
java -Dproperty.file.location="dataplane.properties" -cp  ../matchmaking-agent-1.12.19-SNAPSHOT.jar org.eclipse.tractusx.agents.conforming.Bootstrap 
```
Make sure that jar file, properties file and dataspace.ttl are in the same directory
Then you should be able to reach the /graph endpoint

Afterwards, you should be able to access the [local endpoint](http://localhost:8281/api/agent) by directly invoking a query

```console
curl --location --globoff 'localhost:8280/api/agent?query=SELECT%20%3Fsubject%20%3Fpredicate%20%3Fobject%20WHERE%20{%20%3Fsubject%20%3Fpredicate%20%3Fobject.}'
```

### Run in Docker

```console
 docker run -i -p 8280:8280 -t <imageID>
```

Afterwards, you should be able to access the [local endpoint](http://localhost:8281/api/agent) by directly invoking a query

```console
curl --location --globoff 'localhost:8280/api/agent?query=SELECT%20%3Fsubject%20%3Fpredicate%20%3Fobject%20WHERE%20{%20%3Fsubject%20%3Fpredicate%20%3Fobject.}'
```

Nevertheless it is intended that the Matchmaking Agent does not run on its own but rather in a combination of a EDC Data Plane and a Control plane as depicted in the diagram above

### Notice for Docker Image

DockerHub: https://hub.docker.com/r/tractusx/matchmaking-agent

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/knowledge-agents/tree/main/matchmaking
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile: https://github.com/eclipse-tractusx/knowledge-agents/blob/main/matchmaking/src/main/docker/Dockerfile
- Project license: Apache License, Version 2.0

**Used base image**

- [eclipse-temurin:17-jre-alpine](https://github.com/adoptium/containers)
- Official Eclipse Temurin DockerHub page: https://hub.docker.com/_/eclipse-temurin
- Eclipse Temurin Project: https://projects.eclipse.org/projects/adoptium.temurin
- Additional information about the Eclipse Temurin images: https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.

### Helm Chart

A helm chart for deploying the matchmaking agent can be found under [this folder](../charts/matchmaking-agent).

It can be added to your umbrella chart.yaml by the following snippet

```console
dependencies:
  - name: matchmaking-agent
    repository: https://eclipse-tractusx.github.io/charts/dev
    version: 1.12.19-SNAPSHOT
    alias: my-matchmmaking-agent
```

and then installed using

```console
helm dependency update
```

In your values.yml, you configure your specific instance of the matchmaking agent like this

```console
##############################################################################################
# Matchmaking Agent
##############################################################################################

my-matchmaking-agent:
  securityContext: *securityContext
  nameOverride: my-matchmaking-agent
  fullnameOverride: my-matchmaking-agent
  ingresses:
    - enabled: true
      # -- The hostname to be used to precisely map incoming traffic onto the underlying network service
      hostname: "my-matchmaking-agent.public-ip"
      annotations:
        nginx.ingress.kubernetes.io/rewrite-target: /$1
        nginx.ingress.kubernetes.io/use-regex: "true"
      # -- Agent endpoints exposed by this ingress resource
      endpoints:
        - default
      tls:
        enabled: true
        secretName: my-conforming-tls
```
