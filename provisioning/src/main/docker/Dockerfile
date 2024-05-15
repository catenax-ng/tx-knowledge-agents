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

##
# Blueprint Container: Prebuilt distribution of ontop which we will repackage
##

FROM ontop/ontop:5.1.2 as blueprint

##
# Build Container: Fixes diverse vulnerabilities in guava <32, tomcat, spring-boot 2.7<13, spring-framework <5.3.28 and spring-web (all 5 versions - need to exclude a deprecated package from the jar)
##

FROM eclipse-temurin:11-jdk AS build

# run with docker --build-arg jdbcDrivers=path_to_my_driver to establish a different driver
ARG jdbcDrivers="https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar https://download.dremio.com/jdbc-driver/dremio-jdbc-driver-LATEST.jar https://repo1.maven.org/maven2/org/apache/calcite/avatica/avatica/1.22.0/avatica-1.22.0.jar"

# run with docker --build-arg HTTP_PROXY=path_to_my_driver if building behind corporate firewall
ARG HTTP_PROXY=""

COPY target/provisioning-agent-*.jar /opt/ontop/lib/
COPY target/lib/guava-*.jar /opt/ontop/lib/
COPY target/lib/tomcat-embed-*.jar /opt/ontop/lib/
COPY target/lib/spring-*.jar /opt/ontop/lib/
COPY target/lib/postgresql-*.jar /opt/ontop/jdbc/

WORKDIR /tmp

RUN  if [ "${HTTP_PROXY}" != ""  ]; then \
        echo "Acquire::http::Proxy \"${HTTP_PROXY}\"" >> /etc/apt/apt.conf.d/proxy.conf; \
        echo "Acquire::https::Proxy \"${HTTP_PROXY}\"" >> /etc/apt/apt.conf.d/proxy.conf; \
     fi && \
     for jdbcDriver in "$jdbcDrivers"; do wget --no-check-certificate -q -P /opt/ontop/jdbc ${jdbcDriver} ; done && \
     mv /opt/ontop/lib/spring-web-5.3.31.jar . && \
     jar xvf spring-web-5.3.31.jar && \
     rm -f spring-web-5.3.31.jar && \
     rm -rf org/springframework/remoting/httpinvoker && \
     jar cf /opt/ontop/lib/spring-web.jar *

##
# Target Container: Use a valid base image
##

FROM eclipse-temurin:11-jre-alpine

ARG APP_USER=ontop
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

COPY --from=blueprint /opt/ontop /opt/ontop
COPY resources/entrypoint.sh /opt/ontop/.

# unfortunately, ontop does not use explicit group ids and user ids (which are needed by tractusx chart security contraints), so we need to relax executable rights
RUN rm /opt/ontop/lib/guava-*.jar && \
    rm /opt/ontop/lib/tomcat-embed-*.jar && \
    rm /opt/ontop/lib/spring-*.jar && \
    mkdir -p /opt/ontop/input && \
    mkdir -p /opt/ontop/database && \
    mkdir -p /opt/ontop/data && \
    chmod 755 /opt/ontop/entrypoint.sh && \
    chown -R ontop:ontop /opt/ontop

# switch to the user
USER "$APP_USER"

## copy over patched stuff
COPY --from=build /opt/ontop/lib/*.jar /opt/ontop/lib/
COPY --from=build /opt/ontop/jdbc/*.jar /opt/ontop/jdbc/

# run with docker --build-arg sqlFiles=initial_sql to establish a different sql file
ARG sqlFiles="resources/dtc.sql"
COPY ${sqlFiles} /opt/ontop/data/.

COPY resources/cx-ontology.ttl resources/cx-ontology.xml resources/settings.properties /opt/ontop/input/
COPY resources/portal.toml /opt/ontop/

# Additional Java debugging options
ARG JAVA_TOOL_OPTIONS ""

# ontop uses this as the default port, we need multiple ports if exposing multiple endpoints
EXPOSE 8080 8686

# temporary files may be written and should go to that mounted volume
VOLUME /tmp

# Original healthcheck only worked with one endpoint
HEALTHCHECK NONE

# These are the environment variables pointing to the various settings
# To disable a setting simply leave it empty
# To enable different settings for multiple endpoints simply separate the options with a space
ENV PATH="${JAVA_HOME}/bin:/opt/ontop:${PATH}"
ENV ONTOP_PORT=8080
ENV ONTOP_ONTOLOGY_FILE=/opt/ontop/input/ontology.ttl
ENV ONTOP_MAPPING_FILE=/opt/ontop/input/mapping.obda
ENV ONTOP_PROPERTIES_FILE=/opt/ontop/input/settings.properties
ENV ONTOP_PORTAL_FILE=""
ENV ONTOP_CORS_ALLOWED_ORIGINS="*"
ENV ONTOP_DEV_MODE=true

# The user/entry  home
WORKDIR /opt/ontop

# Run Ontop Agents
ENTRYPOINT ["./entrypoint.sh"]
        