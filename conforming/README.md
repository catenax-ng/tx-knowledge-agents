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

# Tractus-X Conforming Agent (KA-CONF)

KA-CONF is a module of the [Tractus-X Knowledge Agents Reference Implementations](../README.md).

## About this Module

The Conforming Agent is a component that may be used in place of Matchmaking and Binding Agents to prove the conformity of other components of the KA architecture.
It implements (depending on its configuration) and validates the KA-MATCH and KA-BIND SPARQL profiles behind the Knowledge Agents standard API.
It may also be used to provoque error behaviour in the KA dataflow in order to check robustness and conformity in the case of problems.
It may also be used in unit test environments as a mock server.

## Architecture

The FOSS Conforming Agent is built using [Jersey](https://github.com/eclipse-ee4j/jersey) (for the REST endpoints) and [RDF4J](https://rdf4j.org/) (for SPARQL parsing). It exposes three endpoints for the three different SPARQL profiles which are to be certified/tested in the KA architecture:
* The '/match' endpoint for the KA-MATCH profile
* The '/bind' endpoint for the KA-BIND profile
* The '/transfer' endpoint for the KA-TRANSFER profile

The Conforming Agent does not perform any logic but just returns prerendered results depending on the input parameters.

### Security

The conforming agent will be configurable to support the following http/s based security mechanisms:
- Basic Authentication
- Oauth2

## Deployment

### Compile, Test & Package

```console
mvn package
```

This will generate

- a [plugin jar](target/original-conforming-agent-1.12.19-SNAPSHOT.jar) containing all necessary components to be dropped into a Jakarta-Compatible Web Server.
- a [standalone jar](target/conforming-agent-1.12.19-SNAPSHOT.jar) including the Jakarta-Reference Implementation (Glassfish).

### Run Locally

The [standalone jar](target/conforming-agent-1.12.19-SNAPSHOT.jar) may be started as follows

```console
java -cp target/conforming-agent-1.12.19-SNAPSHOT.jar org.eclipse.tractusx.agents.conforming.Bootstrap"
```

### Containerizing

You could either call

```console
mvn install -Pwith-docker-image
```

or invoke the following docker command after a successful package run

```console
docker build -t tractusx/conforming-agent:1.12.19-SNAPSHOT -f src/main/docker/Dockerfile .
```

This will create a docker image based on a minimal java environment for running the Glassfish-based standalone jar.

To run the docker image, you could invoke this command

```console
docker run -p 8080:8080 \
  tractusx/conforming-agent:1.12.19-SNAPSHOT
````

Afterwards, you should be able to access the [local SparQL endpoint](http://localhost:8080/) via
the browser or by directly invoking a query

```console
curl --location --request POST 'http://localhost:8080/bind' \
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

DockerHub: https://hub.docker.com/r/tractusx/conforming-agent

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/knowledge-agents/tree/main/conforming
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile: https://github.com/eclipse-tractusx/knowledge-agents/blob/main/conforming/src/main/docker/Dockerfile
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
  - name: conforming-agent
    repository: https://eclipse-tractusx.github.io/charts/dev
    version: 1.12.19-SNAPSHOT
    alias: my-conforming-agent
```

and then installed using

```console
helm dependency update
```

In your values.yml, you configure your specific instance of the conforming agent like this

```console
##############################################################################################
# Conforming Agent
##############################################################################################

my-conforming-agent:
  securityContext: *securityContext
  nameOverride: my-conforming-agent
  fullnameOverride: my-conforming-agent
  ingresses:
    - enabled: true
      # -- The hostname to be used to precisely map incoming traffic onto the underlying network service
      hostname: "my-conforming-agent.public-ip"
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
