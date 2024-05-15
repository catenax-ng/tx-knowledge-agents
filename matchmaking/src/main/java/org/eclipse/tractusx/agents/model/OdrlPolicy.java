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
package org.eclipse.tractusx.agents.model;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.agents.jsonld.JsonLd;
import org.eclipse.tractusx.agents.jsonld.JsonLdObject;

/**
 * represents a policy
 */
public class OdrlPolicy extends JsonLdObject {

    public OdrlPolicy(JsonObject node) {
        super(node);
    }

    public String getPermissionAsString() {
        return JsonLd.asString(object.get("http://www.w3.org/ns/odrl/2/permission"));
    }

    public String getObligationAsString() {
        return JsonLd.asString(object.get("http://www.w3.org/ns/odrl/2/obligation"));
    }

    public String getProhibitionAsString() {
        return JsonLd.asString(object.get("http://www.w3.org/ns/odrl/2/prohibition"));
    }

}
