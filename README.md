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

# Tractus-X Knowledge Agents Reference Implementations (KA-RI)

![GitHub contributors](https://img.shields.io/github/contributors/eclipse-tractusx/knowledge-agents)
![GitHub Org's stars](https://img.shields.io/github/stars/eclipse-tractusx)
![GitHub](https://img.shields.io/github/license/eclipse-tractusx/knowledge-agents)
![GitHub all releases](https://img.shields.io/github/downloads/eclipse-tractusx/knowledge-agents/total)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-tractusx_knowledge-agents&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-tractusx_knowledge-agents)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/RoKrish14/knowledge-agents/badge)](https://securityscorecards.dev/viewer/?uri=github.com/RoKrish14/knowledge-agents/)
![build](https://github.com/ossf/scorecard/workflows/build/badge.svg?branch=main)
![CodeQL](https://github.com/ossf/scorecard/workflows/CodeQL/badge.svg?branch=main)


Tractus-X Knowledge Agents Reference Implementations (KA-RI) is a product of the [Catena-X Knowledge Agents Kit (about to move to: Tractus-X Knowledge Agents Kit)](https://bit.ly/tractusx-agents) implementing the "binding" modules of the CX-0084 standard (Federated Queries in Dataspaces).

* See the [Authors](AUTHORS.md)
* See the [Changelog](CHANGELOG.md)
* See the [Code of Conduct](CODE_OF_CONDUCT.md)
* See the [Contribution Guidelines](CONTRIBUTING.md)
* See the [License](LICENSE)
* See the [Notice on 3rd Party Software](NOTICE.md)
* See this [Readme](README.md)
* See the [Security Notice](Security.md)

## About the Project

This repository provides FOSS implementations for so-called Agents. 

An "Agent" is a (re- as well as pro-active) component which understands, partially elaborates and possibly delegates declarative scripts or queries (the so-called "Skills") over individual data and service assets ("Knowledge Graphs") of the dataspace.

Agents speak [Semantic Web Based](https://www.w3.org/2001/sw/wiki/Main_Page) dataspace protocols (such as [SPARQL](https://www.w3.org/2001/sw/wiki/SPARQL)) which are negotiated and 
transferred by the [Tractus-X Knowledge Agent Extensions for the Eclipse Dataspace Components (KA-EDC)](https://github.com/eclipse-tractusx/knowledge-agents-edc).
Binding Agents translate these protocols to your backend data storage or API.

We provide several of agent implementations in this product. 

- [Matchmaking Agent (KA-MATCH upcoming)](matchmaking) An agent implementation is needed by any Agent-Enabled dataspace participant to reason about the Dataspace/EDC (and hence: other Matchmaking Agents). It is also able to delegate work on sub-graphs/asset to tenant-internal agents (Binding Agents) providing actual data and functions. For Agent-Enabled dataspace consumers, deploying the Matchmaking Agent alongside an Agent-Enabled EDC is sufficient.
- Binding Agents are needed by any Agent-Enabled dataspace provider in order to link the Dataspace/Protocol layer to the actual Data and Functional Resources. Binding (sometimes: Bridging) is a technology which rather translates between protocols rather than translating data (Mapping).
  - [Provisioning Agent (KA-PROV)](provisioning) An agent implementation which binds typical SQL-based backend data sources to SPARQL.
  - [Remoting Agent (KA-RMT)](remoting) An agent implementation which binds typical REST services to SPARQL.
- [Conforming Agent (KA-CONF)](conforming) An agent implementation which may play any role (Matchmaking Agent, Binding Agent, EDC Transfer) in order to test the conformity of all other parts of the architecture/standard.

Included in this repository are ready-made [Helm charts](charts). 

They can be installed from the [Tractus-X Helm Repository (Stable Versions)](https://eclipse-tractusx.github.io/charts/stable) or [Tractus-X Helm Repository (Dev Versions)](https://eclipse-tractusx.github.io/charts/dev).

## Getting Started

### Build

To compile, package and containerize the binary artifacts (includes running the unit tests)

```shell
./mvnw package -Pwith-docker-image
```

To publish the binary artifacts (environment variables GITHUB_ACTOR and GITHUB_TOKEN must be set)

```shell
./mvnw -s settings.xml publish
```

To update the [DEPENDENCIES](./DEPENDENCIES) declarations

```shell
./mvnw org.eclipse.dash:license-tool-plugin:license-check 
```

### Deployment

Deployment can be done
* via [JAR libraries](https://github.com/orgs/eclipse-tractusx/packages?repo_name=knowledge-agents&ecosystem=maven) copied into your Java runtime
* via [Docker images](https://hub.docker.com/r/tractusx) 
* via [Helm Charts (Stable Versions)](https://eclipse-tractusx.github.io/charts/stable) or [Helm Charts (Dev Versions)](https://eclipse-tractusx.github.io/charts/stable)

See the individual agent documentations for more detailed deployment information
* [Provisioning Agent](provisioning/README.md)
* [Remoting Agent](remoting/README.md)
* [Conforming Agent](conforming/README.md)

#### Setup using Helm/Kind

In order to run KA-RI applications via helm on your local machine, please make sure the following
preconditions are met.

- Have a local Kubernetes runtime ready. We've tested this setup with [KinD](https://kind.sigs.k8s.io/), but other
  runtimes such
  as [Minikube](https://minikube.sigs.k8s.io/docs/start/) may work as well, we just haven't tested them. All following
  instructions will assume KinD.

For the most bare-bones installation of the dataspace, execute the following commands in a shell:

```shell
kind create cluster -n ka --config kind.config.yaml
# the next step is specific to KinD and will be different for other Kubernetes runtimes!
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
# wait until the ingress controller is ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
# transfer images
kind load docker-image docker.io/tractusx/conforming-agent:1.12.19-SNAPSHOT --name ka
kind load docker-image docker.io/tractusx/matchmaking-agent:1.12.19-SNAPSHOT --name ka
kind load docker-image docker.io/tractusx/provisioning-agent:1.12.19-SNAPSHOT --name ka
kind load docker-image docker.io/tractusx/remoting-agent:1.12.19-SNAPSHOT --name ka
# run container test
ct install --charts charts/conforming-agent 
ct install --charts charts/matchmaking-agent  
ct install --charts charts/provisioning-agent
ct install --charts charts/remoting-agent 
``````

### Notice for Docker Images

* [Notice for Conforming Agent](conforming/README.md#notice-for-docker-images)
* [Notice for Provisioning Agent](provisioning/README.md#notice-for-docker-images)
* [Notice for Remoting Agent](remoting/README.md#notice-for-docker-images)

