# Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0

FROM eclipse-temurin:22_36-jre-alpine
ARG JAR
ARG LIB

ARG APP_USER=agent
ARG APP_UID=10001
ARG APP_GID=30000

RUN addgroup --gid "$APP_GID" --system "$APP_USER"

RUN adduser \
     --shell /sbin/nologin \
     --disabled-password \
     --gecos "" \
     --ingroup "$APP_USER" \
     --no-create-home \
     --uid "$APP_UID" \
     "$APP_USER"

USER "$APP_USER"
WORKDIR /app
VOLUME /tmp

COPY target/matchmaking-agent-*.jar /app/lib/
COPY resources/matchmaking.properties /app/configuration.properties
COPY resources/dataspace.ttl /app/dataspace.ttl

# TODO implement wget or curl-based health check
HEALTHCHECK NONE

EXPOSE 8080

ENTRYPOINT ["java", \
            "-cp", \
            "/app/lib/*", \
            "org.eclipse.tractusx.agents.matchmaking.Bootstrap"]