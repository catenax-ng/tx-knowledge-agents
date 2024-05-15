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
package org.eclipse.tractusx.agents.remoting.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.tractusx.agents.remoting.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A remoting SAIL config captures anything that a remoting
 * inference service needs to know to expose its capabilities.
 */
public class RemotingSailConfig extends AbstractSailImplConfig {
    /**
     * logger
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * constants
     */
    public static final String CONFIG_NAMESPACE = "https://w3id.org/catenax/ontology/function#";
    public static final String COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    public static final String CALLBACK_NAME = "callbackAddress";
    public static final String FUNCTION_NAME = "Function";
    public static final String AUTHENTICATION_CODE = "authenticationCode";
    public static final String AUTHENTICATION_KEY = "authenticationKey";
    public static final String RESULT_NAME = "Result";
    public static final String ARGUMENT_NAME = "Argument";
    public static final String INPUT_ATTRIBUTE = "input";
    public static final String RESULT_ATTRIBUTE = "result";
    public static final String BATCH_ATTRIBUTE = "batch";
    public static final String CALLBACK_ATTRIBUTE = "callbackProperty";
    public static final String INVOCATION_ID_ATTRIBUTE = "invocationIdProperty";
    public static final String INPUT_PROPERTY_ATTRIBUTE = "inputProperty";
    public static final String RESULT_ID_ATTRIBUTE = "resultIdProperty";
    public static final String CORRELATION_INPUT_ATTRIBUTE = "correlationInput";
    public static final String OUTPUT_PROPERTY_ATTRIBUTE = "outputProperty";
    public static final String ARGUMENT_ATTRIBUTE = "argumentName";
    public static final String STRIP_ATTRIBUTE = "strip";
    public static final String MANDATORY_ATTRIBUTE = "mandatory";
    public static final String DEFAULT_ATTRIBUTE = "default";
    public static final String GROUP_ATTRIBUTE = "formsBatchGroup";
    public static final String PRIORITY_ATTRIBUTE = "priority";
    public static final String PATH_ATTRIBUTE = "valuePath";
    public static final String TYPE_RELATION = "dataType";
    public static final String OUTPUT_ATTRIBUTE = "output";
    public static final String INVOCATION_PROPERTY = "supportsInvocation";
    public static final String URL_ATTRIBUTE = "targetUri";
    public static final String METHOD_ATTRIBUTE = "invocationMethod";

    /**
     * when interacting with parser/exporter
     */
    protected ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * iri for predicate
     */
    protected IRI supportsInvocationPredicate = vf.createIRI(CONFIG_NAMESPACE, INVOCATION_PROPERTY);
    protected IRI callbackAddressPredicate = vf.createIRI(CONFIG_NAMESPACE, CALLBACK_NAME);
    protected IRI targetUriPredicate = vf.createIRI(CONFIG_NAMESPACE, URL_ATTRIBUTE);
    protected IRI invocationMethodPredicate = vf.createIRI(CONFIG_NAMESPACE, METHOD_ATTRIBUTE);
    protected IRI inputPredicate = vf.createIRI(CONFIG_NAMESPACE, INPUT_ATTRIBUTE);
    protected IRI outputPredicate = vf.createIRI(CONFIG_NAMESPACE, OUTPUT_ATTRIBUTE);
    protected IRI resultPredicate = vf.createIRI(CONFIG_NAMESPACE, RESULT_ATTRIBUTE);
    protected IRI argumentNamePredicate = vf.createIRI(CONFIG_NAMESPACE, ARGUMENT_ATTRIBUTE);
    protected IRI stripPredicate = vf.createIRI(CONFIG_NAMESPACE, STRIP_ATTRIBUTE);
    protected IRI mandatoryPredicate = vf.createIRI(CONFIG_NAMESPACE, MANDATORY_ATTRIBUTE);
    protected IRI defaultPredicate = vf.createIRI(CONFIG_NAMESPACE, DEFAULT_ATTRIBUTE);

    protected IRI groupPredicate = vf.createIRI(CONFIG_NAMESPACE, GROUP_ATTRIBUTE);
    protected IRI priorityPredicate = vf.createIRI(CONFIG_NAMESPACE, PRIORITY_ATTRIBUTE);
    protected IRI returnPathPredicate = vf.createIRI(CONFIG_NAMESPACE, PATH_ATTRIBUTE);
    protected IRI batchPredicate = vf.createIRI(CONFIG_NAMESPACE, BATCH_ATTRIBUTE);
    protected IRI callbackPredicate = vf.createIRI(CONFIG_NAMESPACE, CALLBACK_ATTRIBUTE);
    protected IRI invocationIdPredicate = vf.createIRI(CONFIG_NAMESPACE, INVOCATION_ID_ATTRIBUTE);
    protected IRI resultIdPredicate = vf.createIRI(CONFIG_NAMESPACE, RESULT_ID_ATTRIBUTE);
    protected IRI correlationInputPredicate = vf.createIRI(CONFIG_NAMESPACE, CORRELATION_INPUT_ATTRIBUTE);
    protected IRI inputPropertyPredicate = vf.createIRI(CONFIG_NAMESPACE, INPUT_PROPERTY_ATTRIBUTE);
    protected IRI outputPropertyPredicate = vf.createIRI(CONFIG_NAMESPACE, OUTPUT_PROPERTY_ATTRIBUTE);
    protected IRI dataTypePredicate = vf.createIRI(CONFIG_NAMESPACE, TYPE_RELATION);
    protected IRI apredicate = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
    protected IRI functionClass = vf.createIRI(CONFIG_NAMESPACE, FUNCTION_NAME);
    protected IRI resultClass = vf.createIRI(CONFIG_NAMESPACE, RESULT_NAME);
    protected IRI argumentClass = vf.createIRI(CONFIG_NAMESPACE, ARGUMENT_NAME);
    protected IRI authenticationCodePredicate = vf.createIRI(COMMON_NAMESPACE, AUTHENTICATION_CODE);
    protected IRI authenticationKeyPredicate = vf.createIRI(COMMON_NAMESPACE, AUTHENTICATION_KEY);


    /**
     * keeps a list of invocation configs
     */
    protected Map<String, ServiceConfig> services = new HashMap<>();

    String callbackAddress;

    public ValueFactory getValueFactory() {
        return vf;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    /**
     * create a new config
     */
    public RemotingSailConfig() {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating new remoting SAIL config.");
        }
    }

    /**
     * gets a service config
     *
     * @param iri of the service
     * @return service config
     */
    public ServiceConfig getService(String iri) {
        return services.get(iri);
    }

    /**
     * registers a service config
     *
     * @param iri     service name
     * @param service config
     */
    public void putService(String iri, ServiceConfig service) {
        services.put(iri, service);
    }

    /**
     * lists services
     *
     * @return set of irirs
     */
    public Set<String> listServices() {
        return services.keySet();
    }

    /**
     * create a new config
     */
    public RemotingSailConfig(String type) {
        super(type);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Creating new remoting SAIL config for type %s.", type));
        }
    }

    @Override
    public String toString() {
        return super.toString() + "/config";
    }

    /**
     * validates the config
     */
    @Override
    public void validate() throws SailConfigException {
        if (logger.isDebugEnabled()) {
            logger.debug("About to validate.");
        }
        super.validate();
        for (Map.Entry<String, ServiceConfig> configs : services.entrySet()) {
            if (configs.getValue().callbackProperty != null) {
                if (callbackAddress == null) {
                    throw new SailConfigException(String.format("There should be a repository-wide callbackAddress configured when service %s has a callbackProperty.", configs.getKey()));
                }
            }
            configs.getValue().validate(configs.getKey());
        }
    }

    /**
     * Save the config
     */
    @Override
    public Resource export(Model model) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("About to export to model %s.", model));
        }
        Resource repoNode = super.export(model);
        if (callbackAddress != null) {
            model.add(repoNode, callbackAddressPredicate, vf.createIRI(callbackAddress));
        }
        for (Map.Entry<String, ServiceConfig> func : services.entrySet()) {
            IRI functionNode = vf.createIRI(func.getKey());
            model.add(repoNode, supportsInvocationPredicate, functionNode);
            model.add(functionNode, apredicate, functionClass);
            model.add(functionNode, targetUriPredicate, vf.createLiteral(func.getValue().targetUri));
            model.add(functionNode, invocationMethodPredicate, vf.createLiteral(func.getValue().method));
            model.add(functionNode, batchPredicate, vf.createLiteral(func.getValue().batch));
            if (func.getValue().callbackProperty != null) {
                model.add(functionNode, callbackPredicate, vf.createLiteral(func.getValue().callbackProperty));
            }
            if (func.getValue().inputProperty != null) {
                model.add(functionNode, inputPropertyPredicate, vf.createLiteral(func.getValue().inputProperty));
            }
            if (func.getValue().invocationIdProperty != null) {
                model.add(functionNode, invocationIdPredicate, vf.createLiteral(func.getValue().invocationIdProperty));
            }
            if (func.getValue().authentication != null) {
                model.add(functionNode, authenticationCodePredicate, vf.createLiteral(func.getValue().authentication.authCode));
                model.add(functionNode, authenticationKeyPredicate, vf.createLiteral(func.getValue().authentication.authKey));
            }
            for (Map.Entry<String, ArgumentConfig> arg : func.getValue().arguments.entrySet()) {
                IRI argumentNode = vf.createIRI(arg.getKey());
                model.add(functionNode, inputPredicate, argumentNode);
                model.add(argumentNode, apredicate, argumentClass);
                model.add(argumentNode, argumentNamePredicate, vf.createLiteral(arg.getValue().argumentName));
                model.add(argumentNode, mandatoryPredicate, vf.createLiteral(arg.getValue().mandatory));
                model.add(argumentNode, priorityPredicate, vf.createLiteral(arg.getValue().priority));
                model.add(argumentNode, groupPredicate, vf.createLiteral(arg.getValue().formsBatchGroup));
                if (arg.getValue().getStrip() != null) {
                    model.add(argumentNode, stripPredicate, vf.createLiteral(arg.getValue().getStrip()));
                }
                model.add(argumentNode, defaultPredicate, Invocation.convertOutputToValue(arg.getValue().defaultValue, vf, "", "https://json-schema.org/draft/2020-12/schema#Object"));
            }
            IRI resultNode = vf.createIRI(func.getValue().resultName);
            model.add(functionNode, resultPredicate, resultNode);
            model.add(resultNode, apredicate, resultClass);
            ResultConfig result = func.getValue().result;
            if (result.callbackProperty != null) {
                model.add(resultNode, callbackPredicate, vf.createLiteral(result.callbackProperty));
            }
            if (result.outputProperty != null) {
                model.add(resultNode, outputPropertyPredicate, vf.createLiteral(result.outputProperty));
            }
            if (result.resultIdProperty != null) {
                model.add(functionNode, invocationIdPredicate, vf.createLiteral(result.resultIdProperty));
            }
            if (result.correlationInput != null) {
                model.add(functionNode, correlationInputPredicate, vf.createLiteral(result.correlationInput));
            }
            for (Map.Entry<String, ReturnValueConfig> arg : result.outputs.entrySet()) {
                IRI argumentNode = vf.createIRI(arg.getKey());
                model.add(functionNode, outputPredicate, argumentNode);
                model.add(argumentNode, apredicate, argumentClass);
                model.add(argumentNode, returnPathPredicate, vf.createLiteral(arg.getValue().path));
                model.add(argumentNode, dataTypePredicate, vf.createIRI(arg.getValue().dataType));
            }
        }
        return repoNode;
    }

    /**
     * parse the config
     */
    @Override
    public void parse(Model model, Resource implNode) throws SailConfigException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("About to parse from model %s and resource %s.", model, implNode));
        }
        super.parse(model, implNode);
        model.getStatements(implNode, callbackAddressPredicate, null).forEach(statement -> callbackAddress = statement.getObject().stringValue());
        model.getStatements(implNode, supportsInvocationPredicate, null).forEach(statement -> {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("About to process function from statement %s.", statement));
            }
            if (!statement.getObject().isIRI()) {
                throw new SailConfigException(String.format("Object of the %s predicate must be IRI but was %s", supportsInvocationPredicate, statement.getObject()));
            }
            IRI functionNode = (IRI) statement.getObject();
            ServiceConfig ic = new ServiceConfig();
            services.put(functionNode.stringValue(), ic);
            Models.objectLiteral(model.filter(functionNode, targetUriPredicate, null))
                    .ifPresent(targetUri -> ic.targetUri = targetUri.stringValue());
            Models.objectLiteral(model.filter(functionNode, invocationMethodPredicate, null))
                    .ifPresent(invocationMethod -> ic.method = invocationMethod.stringValue());
            Models.objectLiteral(model.filter(functionNode, batchPredicate, null))
                    .ifPresent(batch -> ic.batch = batch.longValue());
            Models.objectLiteral(model.filter(functionNode, callbackPredicate, null))
                    .ifPresent(async -> ic.callbackProperty = async.stringValue());
            Models.objectLiteral(model.filter(functionNode, inputPropertyPredicate, null))
                    .ifPresent(ip -> ic.inputProperty = ip.stringValue());
            Models.objectLiteral(model.filter(functionNode, invocationIdPredicate, null))
                    .ifPresent(iid -> ic.invocationIdProperty = iid.stringValue());
            Models.objectLiteral(model.filter(functionNode, authenticationKeyPredicate, null))
                    .ifPresent(authKey -> {
                        if (ic.authentication == null) {
                            ic.authentication = new AuthenticationConfig();
                        }
                        ic.authentication.authKey = authKey.stringValue();
                    });
            Models.objectLiteral(model.filter(functionNode, authenticationCodePredicate, null))
                    .ifPresent(authCode -> {
                        if (ic.authentication == null) {
                            ic.authentication = new AuthenticationConfig();
                        }
                        ic.authentication.authCode = authCode.stringValue();
                    });
            model.getStatements(functionNode, inputPredicate, null).forEach(
                    argumentStatement -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("About to process argument from statement %s.", argumentStatement));
                        }
                        if (!argumentStatement.getObject().isIRI()) {
                            throw new SailConfigException(String.format("Object of the %s predicate must be IRI but was %s", inputPredicate, argumentStatement.getObject()));
                        }
                        IRI argumentNode = (IRI) argumentStatement.getObject();
                        ArgumentConfig ac = new ArgumentConfig();
                        ic.arguments.put(argumentNode.stringValue(), ac);
                        Models.objectLiteral(model.filter(argumentNode, argumentNamePredicate, null))
                                .ifPresent(argumentName -> ac.argumentName = argumentName.stringValue());
                        Models.objectLiteral(model.filter(argumentNode, mandatoryPredicate, null))
                                .ifPresent(mandatory -> ac.mandatory = Boolean.parseBoolean(mandatory.stringValue()));
                        Models.objectLiteral(model.filter(argumentNode, priorityPredicate, null))
                                .ifPresent(priority -> ac.priority = Integer.parseInt(priority.stringValue()));
                        Models.objectLiteral(model.filter(argumentNode, groupPredicate, null))
                                .ifPresent(group -> ac.formsBatchGroup = Boolean.parseBoolean(group.stringValue()));
                        Models.objectLiteral(model.filter(argumentNode, defaultPredicate, null))
                                .ifPresent(def -> ac.defaultValue = Invocation.convertToObject(def, JsonNode.class, null));
                        Models.objectLiteral(model.filter(argumentNode, stripPredicate, null))
                                .ifPresent(strip -> ac.setStrip(strip.stringValue()));
                        Models.objectIRI(model.filter(argumentNode, stripPredicate, null))
                                .ifPresent(strip -> ac.setStrip(strip.stringValue()));
                    }
            );
            Models.objectIRI(model.filter(functionNode, resultPredicate, null))
                    .ifPresent(result -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("About to process result %s.", result));
                        }
                        ic.resultName = result.stringValue();
                        ResultConfig rc = new ResultConfig();
                        ic.result = rc;
                        Models.objectLiteral(model.filter(result, outputPropertyPredicate, null))
                                .ifPresent(op -> rc.outputProperty = op.stringValue());
                        Models.objectLiteral(model.filter(result, resultIdPredicate, null))
                                .ifPresent(rid -> rc.resultIdProperty = rid.stringValue());
                        Models.objectIRI(model.filter(result, correlationInputPredicate, null))
                                .ifPresent(rid -> rc.correlationInput = rid.stringValue());
                        Models.objectLiteral(model.filter(result, callbackPredicate, null))
                                .ifPresent(async -> rc.callbackProperty = async.stringValue());
                        model.getStatements(result, outputPredicate, null).forEach(
                                outputStatement -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(String.format("About to process output from statement %s.", outputStatement));
                                    }
                                    if (!outputStatement.getObject().isIRI()) {
                                        throw new SailConfigException(String.format("Object of the %s predicate must be IRI but was %s", inputPredicate, outputStatement.getObject()));
                                    }
                                    IRI outputNode = (IRI) outputStatement.getObject();
                                    ReturnValueConfig rvc = new ReturnValueConfig();
                                    rc.outputs.put(outputNode.stringValue(), rvc);
                                    Models.objectLiteral(model.filter(outputNode, returnPathPredicate, null))
                                            .ifPresent(path -> rvc.path = path.stringValue());
                                    Models.objectIRI(model.filter(outputNode, dataTypePredicate, null))
                                            .ifPresent(dataType -> rvc.dataType = dataType.stringValue());
                                }
                        );

                    });


        });
    }

}