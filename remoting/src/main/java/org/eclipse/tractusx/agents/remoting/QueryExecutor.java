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
package org.eclipse.tractusx.agents.remoting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.AggregateFunctionCall;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Like;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.tractusx.agents.remoting.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The query processing is done while visiting
 */
@SuppressWarnings("removal")
public class QueryExecutor implements QueryModelVisitor<SailException>, BindingHost {

    /**
     * the state
     */
    protected Map<Value, Invocation> invocations = new HashMap<>();

    /**
     * bindings and results
     */
    protected final Set<String> variables = new HashSet<>();
    protected final List<MutableBindingSet> bindings = new ArrayList<>();
    protected final Map<String, String> outputVariables = new HashMap<>();

    /**
     * the logger
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * the connection
     */
    protected RemotingSailConnection connection;

    /**
     * create a new visitor
     *
     * @param connection the sail connection
     */
    public QueryExecutor(RemotingSailConnection connection) {
        this.connection = connection;
    }


    @Override
    public String toString() {
        return super.toString() + "/visitor";
    }

    @Override
    public void meet(QueryRoot node) throws SailException {
        logger.debug(String.format("Visiting a query root %s", node.getClass()));
        node.getArg().visit(this);
    }

    @Override
    public void meet(Add node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(And node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(ArbitraryLengthPath node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Avg node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    /**
     * convert a binding set to a mutable version
     *
     * @param bs the binding set
     * @return mutable binding set
     */
    protected MutableBindingSet makeMutable(BindingSet bs) {
        if (bs instanceof MutableBindingSet) {
            return (MutableBindingSet) bs;
        } else {
            MutableBindingSet mbs = new MapBindingSet();
            for (String binding : bs.getBindingNames()) {
                mbs.addBinding(bs.getBinding(binding));
            }
            return mbs;
        }
    }

    @Override
    public void meet(BindingSetAssignment node) throws SailException {
        variables.addAll(node.getBindingNames());
        if (bindings.isEmpty()) {
            node.getBindingSets().forEach(binding -> bindings.add(makeMutable(binding)));
        } else {
            bindings.forEach(binding -> node.getBindingSets().forEach(joinBindings -> joinBindings.forEach(binding::addBinding)));
        }
    }

    @Override
    public void meet(BNodeGenerator node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Bound node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Clear node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Coalesce node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Compare node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(CompareAll node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(CompareAny node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(DescribeOperator node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Copy node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Count node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Create node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Datatype node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(DeleteData node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Difference node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Distinct node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(EmptySet node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Exists node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Extension node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(ExtensionElem node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Filter node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(FunctionCall node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(AggregateFunctionCall node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Group node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(GroupConcat node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(GroupElem node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(If node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(In node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(InsertData node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Intersection node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IRIFunction node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IsBNode node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IsLiteral node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IsNumeric node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IsResource node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(IsURI node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Join node) throws SailException {
        logger.debug(String.format("Visiting a join %s", node.getClass()));
        node.getLeftArg().visit(this);
        node.getRightArg().visit(this);
    }

    @Override
    public void meet(Label node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Lang node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(LangMatches node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(LeftJoin node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Like like) throws SailException {

    }

    @Override
    public void meet(Load node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(LocalName node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(MathExpr node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Max node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Min node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Modify node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Move node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(MultiProjection node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Namespace node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Not node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Or node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Order node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(OrderElem node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Projection node) throws SailException {
        logger.debug(String.format("Visiting a projection %s", node.getClass()));
        node.getArg().visit(this);
        for (Invocation invocation : invocations.values()) {
            invocation.execute(connection, this);
        }
        node.getProjectionElemList().visit(this);
    }

    @Override
    public void meet(ProjectionElem node) throws SailException {
        logger.debug(String.format("Visiting a projection element %s", node.getClass()));
        String targetName = node.getTargetName();
        if (targetName == null) {
            targetName = node.getSourceName();
        }
        outputVariables.put(node.getSourceName(), targetName);
    }

    @Override
    public void meet(ProjectionElemList node) throws SailException {
        logger.debug(String.format("Visiting a projection list %s", node.getClass()));
        node.visitChildren(this);
    }

    @Override
    public void meet(Reduced node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Regex node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(SameTerm node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Sample node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Service node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(SingletonSet node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Slice node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    /**
     * implements the actual preparation/binding and invocation/execution of triple based API calls
     */
    @Override
    public void meet(StatementPattern statement) throws SailException {
        if (bindings.isEmpty()) {
            bindings.add(new MapBindingSet());
        }
        Var predicate = statement.getPredicateVar();
        if (!predicate.hasValue()) {
            if (!bindings.get(0).hasBinding(predicate.getName())) {
                throw new SailException(String.format("Predicate %s is not bound", predicate));
            }
            predicate = new Var(predicate.getName(), bindings.get(0).getValue(predicate.getName()));
            checkConsistentBindings(predicate);
        }
        if (!predicate.getValue().isIRI()) {
            throw new SailException(String.format("No support for non-IRI predicate %s", predicate));
        }
        if ("http://www.w3.org/1999/02/22-rdf-syntax-ns#type".equals(predicate.getValue().stringValue())) {
            Var object = statement.getObjectVar();
            if (!object.hasValue()) {
                if (!bindings.get(0).hasBinding(object.getName())) {
                    throw new SailException(String.format("Invocation type %s is not bound", object));
                }
                object = new Var(object.getName(), bindings.get(0).getValue(object.getName()));
                checkConsistentBindings(object);
            }
            if (!object.getValue().isIRI()) {
                throw new SailException(String.format("No support for non-IRI invocation type binding %s", object));
            }
            IRI objectIri = (IRI) object.getValue();
            // TODO lookup configuration 
            Var subject = statement.getSubjectVar();
            if (!subject.hasValue()) {
                String key = String.format("?invocation=%d", connection.remotingSail.getNextId());
                IRI invocationIri = connection.remotingSail.getValueFactory().createIRI(objectIri.getNamespace(), key);
                for (MutableBindingSet binding : bindings) {
                    binding.addBinding(subject.getName(), invocationIri);
                }
                subject = new Var(subject.getName(), invocationIri);
            } else {
                if (!subject.getValue().isIRI()) {
                    throw new SailException(String.format("No support for non-IRI invocation subject binding %s", subject));
                }
                if (!((IRI) subject.getValue()).getNamespace().equals(objectIri.getNamespace())) {
                    throw new SailException(String.format("No support for non-IRI invocation subject binding %s", subject));
                }
            }
            ServiceConfig ic = connection.remotingSail.config.getService(objectIri.stringValue());
            if (ic == null) {
                throw new SailException(String.format("Function %s was not configured. Only got keys %s", objectIri, connection.remotingSail.config.listServices()));
            }
            Invocation invocation = invocations.get(subject.getValue());
            if (invocation != null) {
                if (!invocation.service.equals(ic)) {
                    throw new SailException(String.format("Could not rebind invocation %s with type %s to type %s", subject.getValue(), invocation.service, object.getValue()));
                }
            } else {
                invocation = new Invocation(connection);
                invocation.service = ic;
                invocation.key = (IRI) bindings.get(0).getBinding(subject.getName()).getValue();
                logger.debug(String.format("Registering a new invocation %s for service type %s", subject.getValue(), invocation.service));
                invocations.put(subject.getValue(), invocation);
            }
        } else {
            Var subject = statement.getSubjectVar();
            if (!subject.hasValue()) {
                if (!bindings.get(0).hasBinding(subject.getName())) {
                    throw new SailException(String.format("Subject variable %s not bound to invocation or result.", subject.getName()));
                }
                subject = new Var(subject.getName(), bindings.get(0).getValue(subject.getName()));
            }
            if (!invocations.containsKey(subject.getValue())) {
                throw new SailException(String.format("Trying to bind argument predicate %s to non existent invocation %s. " +
                        "Maybe you need to switch statement order such that rdf:type precedes any other bindings.", predicate.getValue().stringValue(), subject));
            }
            Invocation invocation = invocations.get(subject.getValue());
            IRI argument = (IRI) predicate.getValue();
            if (invocation.service.getResultName().equals(argument.stringValue()) || invocation.service.getResult().getOutputs().containsKey(argument.stringValue())) {
                invocation.outputs.put(statement.getObjectVar(), argument);
            } else if (invocation.service.getArguments().containsKey(argument.stringValue())) {
                invocation.inputs.put(argument.stringValue(), statement.getObjectVar());
            } else {
                throw new SailException(String.format("Predicate %s is neither output nor input predicate for invocation %s", argument, subject));
            }
        }
    }

    /**
     * check that all bindings coincide on the comparison object
     */
    private void checkConsistentBindings(final Var compareObject) {
        bindings.forEach(binding -> {
            Value val = binding.getValue(compareObject.getName());
            if (!val.equals(compareObject.getValue())) {
                throw new SailException(String.format("Having different invocation types %s is currently not supported", compareObject));
            }
        });
    }

    @Override
    public void meet(Str node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Sum node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Union node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(ValueConstant node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(ListMemberOperator node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(Var node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meet(ZeroLengthPath node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    /**
     * visit tripleref
     *
     * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete implementations are expected to override.
     * @since 3.2.0
     */
    @Override
    public void meet(TripleRef node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    /**
     * visit a valueexpr
     *
     * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility. Concrete implementations are expected to override.
     * @since 3.2.0
     */
    @Override
    public void meet(ValueExprTripleRef node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public void meetOther(QueryModelNode node) throws SailException {
        throw new SailException(String.format("No support for %s", node));
    }

    @Override
    public Set<String> getVariables() {
        return variables;
    }

    @Override
    public Collection<MutableBindingSet> getBindings() {
        return bindings;
    }

}