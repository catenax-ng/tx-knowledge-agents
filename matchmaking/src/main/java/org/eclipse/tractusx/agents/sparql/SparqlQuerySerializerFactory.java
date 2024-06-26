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
package org.eclipse.tractusx.agents.sparql;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FmtTemplate;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;

/**
 * A serializer factory for sparql queries which
 * stratifies the resulting groups (joins) because not
 * all SparQL endpoints (such as ONTOP) can deal with the
 * level of nesting that fuseki syntax graphs represent with
 * their max-2 child operators.
 */
public class SparqlQuerySerializerFactory implements QuerySerializerFactory {
    @Override
    public QueryVisitor create(Syntax syntax, Prologue prologue, IndentedWriter writer) {
        SerializationContext cxt1 = new SerializationContext(prologue, new NodeToLabelMapBNode("b", false));
        SerializationContext cxt2 = new SerializationContext(prologue, new NodeToLabelMapBNode("c", false));
        return new SparqlQuerySerializer(writer, new StratifiedFormatterElement(writer, cxt1), new FmtExprSPARQL(writer, cxt1), new FmtTemplate(writer, cxt2));
    }

    @Override
    public QueryVisitor create(Syntax syntax, SerializationContext context, IndentedWriter writer) {
        return new SparqlQuerySerializer(writer, new StratifiedFormatterElement(writer, context), new FmtExprSPARQL(writer, context), new FmtTemplate(writer, context));
    }

    @Override
    public boolean accept(Syntax syntax) {
        return Syntax.syntaxARQ.equals(syntax) || Syntax.syntaxSPARQL_10.equals(syntax) || Syntax.syntaxSPARQL_11.equals(syntax);
    }
}
