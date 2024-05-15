#!/bin/sh

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

#
# Entry script for provisioning agent/ontop with the ability to start multiple endpoints in lazy mode and disable CORS
#

CORS=""
LAZY="--lazy"
ENDPOINT_LENGTH=0
ONTOP_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS
JAVA_TOOL_OPTIONS=""

for ENDPOINT in $ONTOP_PORT ; do  # NOTE: do not double-quote $services here.
  ENDPOINT_LENGTH=$((ENDPOINT_LENGTH+1))
done

if [ "$ONTOP_CORS_ALLOWED_ORIGINS" != "" ]; then
  CORS="--cors-allowed-origins=${ONTOP_CORS_ALLOWED_ORIGINS}"
fi

echo "Found $ENDPOINT_LENGTH endpoints to provide under $CORS $LAZY."

ENDPOINT_INDEX=0
for ENDPOINT in $ONTOP_PORT ; do  # NOTE: do not double-quote $services here.
  ENDPOINT_INDEX=$((ENDPOINT_INDEX+1))
  
  ONTOLOGY="/opt/ontop/ontology.ttl"
  ONTOLOGY_INDEX=0
  for ONTOLOGY_FILE in $ONTOP_ONTOLOGY_FILE ; do
    ONTOLOGY_INDEX=$((ONTOLOGY_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $ONTOLOGY_INDEX ]; then
      ONTOLOGY=$ONTOLOGY_FILE
    fi
  done

  MAPPING="/opt/ontop/input/mapping.obda"
  MAPPING_INDEX=0
  for MAPPING_FILE in $ONTOP_MAPPING_FILE ; do
    MAPPING_INDEX=$((MAPPING_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $MAPPING_INDEX ]; then
      MAPPING=$MAPPING_FILE
    fi
  done

  PROPERTIES="/opt/ontop/input/settings.properties"
  PROPERTIES_INDEX=0
  for PROPERTIES_FILE in $ONTOP_PROPERTIES_FILE ; do
    PROPERTIES_INDEX=$((PROPERTIES_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $PROPERTIES_INDEX ]; then
      PROPERTIES=$PROPERTIES_FILE
    fi
  done

  DEV="false"
  DEV_INDEX=0
  for DEV_MODE in $ONTOP_DEV_MODE ; do
    DEV_INDEX=$((DEV_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $DEV_INDEX ]; then
      DEV=$DEV_MODE
    fi
  done

  TOOL=""
  TOOL_INDEX=0
  for TOOL_OPTIONS in $ONTOP_TOOL_OPTIONS ; do
    TOOL_INDEX=$((TOOL_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $TOOL_INDEX ]; then
      TOOL=$TOOL_OPTIONS
    fi
  done

  PORTAL="/opt/ontop/portal.toml"
  PORTAL_INDEX=0
  for PORTAL_FILE in $ONTOP_PORTAL_FILES ; do
    PORTAL_INDEX=$((PORTAL_INDEX+1))

    if [ $ENDPOINT_INDEX -ge $PORTAL_INDEX ]; then
      PORTAL=$PORTAL_FILE
    fi
  done

  echo "Providing endpoint $ENDPOINT_INDEX on port $ENDPOINT with ontology $ONTOLOGY mapping $MAPPING properties $PROPERTIES portal $PORTAL dev mode $DEV and tool options $TOOL"

  ENDPOINT="--port=$ENDPOINT"
  ONTOLOGY="--ontology=$ONTOLOGY"
  MAPPING="--mapping=$MAPPING"
  PROPERTIES="--properties=$PROPERTIES"
  if [ "$DEV" == "true" ]; then
    DEV="--dev"
  else 
    DEV=""
  fi
  PORTAL="--portal=$PORTAL"

  if [ $ENDPOINT_INDEX -eq $ENDPOINT_LENGTH ]; then
    echo "Invoking last process";
    java $TOOL -cp ./lib/*:./jdbc/* -Dlogback.configurationFile="/opt/ontop/log/logback.xml" -Dlogging.config="/opt/ontop/log/logback.xml" \
      it.unibz.inf.ontop.cli.Ontop endpoint ${ONTOLOGY} ${MAPPING} \
      ${PROPERTIES} ${PORTAL} ${DEV} ${ENDPOINT} ${CORS} ${LAZY};
  else 
    echo "Invoking intermediate process";
    java $TOOL -cp ./lib/*:./jdbc/* -Dlogback.configurationFile="/opt/ontop/log/logback.xml" -Dlogging.config="/opt/ontop/log/logback.xml" \
      it.unibz.inf.ontop.cli.Ontop endpoint ${ONTOLOGY} ${MAPPING} \
      ${PROPERTIES} ${PORTAL} ${DEV} ${ENDPOINT} ${CORS} ${LAZY}&
  fi
done
