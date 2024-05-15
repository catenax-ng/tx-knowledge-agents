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
# Build Container: Fixes diverse vulnerabilities in guava <32, netty, jetty, spring-framework <5.3.28 and spring-web (all 5 versions - need to exclude a deprecated package from the jar)
##

FROM eclipse-temurin:22_36-jdk-alpine AS build

COPY target/original-remoting-agent-*.jar /opt/lib/
#COPY target/lib/guava-*.jar /opt/lib/
COPY target/lib/netty-*.jar /opt/lib/
COPY target/lib/http2-hpack-*.jar /opt/lib/
COPY target/lib/jetty-*.jar /opt/lib/
COPY target/lib/spring-*.jar /opt/lib/
COPY target/lib/logback-*.jar /opt/lib/
COPY target/lib/httpmime-*.jar /opt/lib/
COPY target/lib/commons-logging-*.jar /opt/lib/
COPY target/lib/snappy-java-*.jar /opt/lib/
COPY target/lib/zookeeper-*.jar /opt/lib/

COPY resources/web/logging.properties /opt/conf/
COPY resources/web/server.xml /opt/conf/
COPY resources/web/web.xml /opt/conf/
COPY resources/web/tomcat-users.xml /opt/conf/

WORKDIR /tmp

RUN  mv /opt/lib/spring-web-5.3.31.jar . && \
     jar xvf spring-web-5.3.31.jar && \
     rm -f spring-web-5.3.31.jar && \
     rm -rf org/springframework/remoting/httpinvoker && \
     jar cf /opt/lib/spring-web.jar *

RUN wget -q -O /tmp/rdf4j.zip "https://rhlx01.hs-esslingen.de/pub/Mirrors/eclipse/rdf4j/eclipse-rdf4j-4.3.8-sdk.zip" && \
    unzip -d /tmp /tmp/rdf4j.zip eclipse-rdf4j-4.3.8/war/rdf4j-server.war  eclipse-rdf4j-4.3.8/war/rdf4j-workbench.war && \
    unzip /tmp/eclipse-rdf4j-4.3.8/war/rdf4j-server.war -d /tmp/rdf4j-server && \
#    rm /tmp/rdf4j-server/WEB-INF/lib/guava-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/netty-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/http2-hpack-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/jetty-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/spring-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/logback-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/snappy-*.jar && \
    rm /tmp/rdf4j-server/WEB-INF/lib/zookeeper-*.jar && \
    cp /opt/lib/*.jar /tmp/rdf4j-server/WEB-INF/lib/ 

COPY resources/web/rdf4j.xml /tmp/rdf4j-server/WEB-INF/web.xml
COPY resources/web/callback.xml /tmp/rdf4j-server/WEB-INF/callback.xml

##
# Target Container: Use a valid base image
##

FROM eclipse-temurin:22_36-jre-alpine

ARG APP_USER=tomcat
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

# Environment variables
ENV TOMCAT_MAJOR=9 \
    TOMCAT_VERSION=9.0.83 \
    CATALINA_HOME=/opt/tomcat

RUN mkdir -p /opt

# install tomcat
RUN wget -q -O /tmp/apache-tomcat.tar.gz https://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    gunzip /tmp/apache-tomcat.tar.gz && \
    tar -C /opt -xf /tmp/apache-tomcat.tar && \
    ln -s /opt/apache-tomcat-$TOMCAT_VERSION $CATALINA_HOME && \
    rm -rf /tmp/* /var/cache/apk/* /opt/apache-tomcat-$TOMCAT_VERSION/webapps/* && \
    rm $CATALINA_HOME/conf/logging.properties && \
    rm $CATALINA_HOME/conf/server.xml && \
    rm $CATALINA_HOME/conf/tomcat-users.xml && \
    rm $CATALINA_HOME/conf/web.xml && \
    mkdir -p /var/rdf4j/server/conf && \
    mkdir -p /var/rdf4j/server/repositories && \
    mkdir -p /opt/tomcat/webapps/rdf4j-server && \
    chown -R tomcat:tomcat /var/rdf4j /opt/apache-tomcat-$TOMCAT_VERSION && \
	chmod 775 /opt/apache-tomcat-$TOMCAT_VERSION $CATALINA_HOME/bin $CATALINA_HOME/bin/catalina.sh /var/rdf4j/server 

EXPOSE 8080

WORKDIR $CATALINA_HOME

ENV JAVA_OPTS="-Xmx2g"
ENV CATALINA_OPTS="-Dorg.eclipse.rdf4j.appdata.basedir=/var/rdf4j"
# Additional Java debugging options
ARG JAVA_TOOL_OPTIONS ""

# TODO Should have a healthcheck
HEALTHCHECK NONE

USER tomcat

COPY --from=build /opt/conf/*.xml /opt/conf/*.properties /opt/tomcat/conf/
COPY --from=build /tmp/rdf4j-server/ /opt/tomcat/webapps/rdf4j-server/
COPY resources/web/startup.sh /opt/tomcat/bin/startup.sh

ENTRYPOINT [ "/opt/tomcat/bin/startup.sh" ]


