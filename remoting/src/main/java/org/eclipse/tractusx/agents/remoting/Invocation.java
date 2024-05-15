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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.tractusx.agents.remoting.callback.CallbackController;
import org.eclipse.tractusx.agents.remoting.callback.CallbackToken;
import org.eclipse.tractusx.agents.remoting.config.ArgumentComparator;
import org.eclipse.tractusx.agents.remoting.config.ArgumentConfig;
import org.eclipse.tractusx.agents.remoting.config.ReturnValueConfig;
import org.eclipse.tractusx.agents.remoting.config.ServiceConfig;
import org.eclipse.tractusx.agents.remoting.util.BatchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Implements a (batch) invocation and represents an instance of the rdf:type cx-fx:Function
 * One function/invocation binding may result in several invocations as being
 * determined by the batch size of the config.
 * Invocation targets can be local java objects or remote REST services.
 */
@SuppressWarnings("ALL")
public class Invocation {

    protected static Logger logger = LoggerFactory.getLogger(Invocation.class);
    public static final Pattern ARGUMENT_PATTERN = Pattern.compile("\\{(?<arg>[^\\{\\}]*)\\}");

    /**
     * the config of the service invoked
     */
    public ServiceConfig service = null;
    /**
     * unique key for the invocation
     */
    public IRI key = null;
    /**
     * start time
     */
    public long startTime = -1;
    /**
     * end time of the invocation
     */
    public long endTime = -1;
    /**
     * success code
     */
    public int success = 0;
    /**
     * input bindings
     */
    public Map<String, Var> inputs = new HashMap<>();
    /**
     * output bindings
     */
    public Map<Var, IRI> outputs = new HashMap<>();
    /**
     * the connection
     */
    protected final RemotingSailConnection connection;

    public static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssX"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * creates a new invocaiton
     *
     * @param connection the sourrounding graph connection
     */
    public Invocation(RemotingSailConnection connection) {
        this.connection = connection;
    }

    /**
     * converter from the literal to the type system
     *
     * @param binding value to convert
     * @param target  class to convert to
     * @param strip an optional suffix to strip from the result
     * @return converted value
     */
    public static <TARGET> TARGET convertToObject(Value binding, Class<TARGET> target, String strip) throws SailException {
        String renderString = binding.stringValue();
        if (strip != null) {
            int lastIndex = renderString.lastIndexOf(strip);
            if (lastIndex >= 0) {
                renderString = renderString.substring(lastIndex + strip.length());
            }
        }
        if (target.isAssignableFrom(String.class)) {
            return (TARGET) renderString;
        } else if (target.isAssignableFrom(int.class)) {
            try {
                return (TARGET) Integer.valueOf(Integer.parseInt(renderString));
            } catch (NumberFormatException nfe) {
                throw new SailException(String.format("Conversion from %s to %s failed.", binding, target), nfe);
            }
        } else if (target.isAssignableFrom(long.class)) {
            try {
                return (TARGET) Long.valueOf(Long.parseLong(renderString));
            } catch (NumberFormatException nfe) {
                throw new SailException(String.format("Conversion from %s to %s failed.", binding, target), nfe);
            }
        } else if (target.isAssignableFrom(double.class)) {
            try {
                return (TARGET) Double.valueOf(Double.parseDouble(renderString));
            } catch (NumberFormatException nfe) {
                throw new SailException(String.format("Conversion from %s to %s failed.", binding, target), nfe);
            }
        } else if (target.isAssignableFrom(float.class)) {
            try {
                return (TARGET) Float.valueOf(Float.parseFloat(renderString));
            } catch (NumberFormatException nfe) {
                throw new SailException(String.format("Conversion from %s to %s failed.", binding, target), nfe);
            }
        } else if (target.isAssignableFrom(JsonNode.class)) {
            if (binding.isLiteral()) {
                IRI dataType = ((Literal) binding).getDatatype();
                String dataTypeName = dataType.stringValue();
                switch (dataTypeName) {
                    case "http://www.w3.org/2001/XMLSchema#string":
                        return (TARGET) objectMapper.getNodeFactory().textNode(renderString);
                    case "http://www.w3.org/2001/XMLSchema#int":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().numberNode(Integer.valueOf(renderString));
                        } catch (NumberFormatException nfe) {
                            throw new SailException(String.format("Could not convert %s to datatype %s.", renderString, dataTypeName), nfe);
                        }
                    case "http://www.w3.org/2001/XMLSchema#long":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().numberNode(Long.valueOf(renderString));
                        } catch (NumberFormatException nfe) {
                            throw new SailException(String.format("Could not convert %s to datatype %s.", renderString, dataTypeName), nfe);
                        }
                    case "http://www.w3.org/2001/XMLSchema#double":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().numberNode(Double.valueOf(renderString));
                        } catch (NumberFormatException nfe) {
                            throw new SailException(String.format("Could not convert %s to datatype %s.", renderString, dataTypeName), nfe);
                        }
                    case "http://www.w3.org/2001/XMLSchema#float":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().numberNode(Float.valueOf(renderString));
                        } catch (NumberFormatException nfe) {
                            throw new SailException(String.format("Could not convert %s to datatype %s.", renderString, dataTypeName), nfe);
                        }
                    case "http://www.w3.org/2001/XMLSchema#dateTime":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().textNode(objectMapper.getDateFormat().format(objectMapper.getDateFormat().parse(renderString)));
                        } catch (ParseException pe) {
                            throw new SailException(String.format("Could not convert %s to json date.", renderString), pe);
                        }
                    case "http://www.w3.org/2001/XMLSchema#date":
                        try {
                            return (TARGET) objectMapper.getNodeFactory().textNode(dateFormat.format(dateFormat.parse(renderString)));
                        } catch (ParseException pe) {
                            throw new SailException(String.format("Could not convert %s to json date.", renderString), pe);
                        }
                    case "https://json-schema.org/draft/2020-12/schema#Object":
                        try {
                            String representation = renderString;
                            // remove UTF8 linefeeds.
                            representation = representation.replace("\\x0A", "");
                            return (TARGET) objectMapper.readTree(representation);
                        } catch (JsonProcessingException jpe) {
                            throw new SailException(String.format("Could not convert %s to json object.", renderString), jpe);
                        }
                    default:
                        throw new SailException(String.format("Could not convert %s of data type %s.", renderString, dataTypeName));
                }
            } else if (binding.isIRI()) {
                return (TARGET) objectMapper.getNodeFactory().textNode(renderString);
            } else {
                throw new SailException(String.format("Could not convert %s.", renderString));
            }
        }
        throw new SailException(String.format("No conversion from %s to %s possible.", renderString, target));
    }

    @Override
    public String toString() {
        return super.toString() + "/invocation";
    }

    /**
     * traverse path
     *
     * @param source object
     * @param path   under source
     * @return path object, will be source if path is empty
     */
    public static Object traversePath(Object source, String... path) throws SailException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Accessing a path of length %d under %d", path.length, System.identityHashCode(source)));
        }
        for (String elem : path) {
            if (elem != null && elem.length() > 0) {
                if (source instanceof Element) {
                    Element element = (Element) source;
                    if (element.hasAttribute(elem)) {
                        source = element.getAttribute(elem);
                    } else {
                        NodeList nl = element.getElementsByTagName(elem);
                        if (!(nl.getLength() > 0)) {
                            throw new SailException(String.format("No such path %s under object %s", elem, source));
                        }
                        source = nl.item(0);
                    }
                } else if (source instanceof JsonNode) {
                    JsonNode node = (JsonNode) source;
                    if (!hasField(node, elem)) {
                        throw new SailException(String.format("No such path %s under object %s", elem, source));
                    }
                    source = getField(node, elem);
                } else {
                    throw new SailException(String.format("Cannot access path %s under object %s", elem, source));
                }
            }
        }
        return source;
    }

    /**
     * provide a string rep for a given object
     *
     * @param source to convert
     * @return string representation of source
     * @throws SailException in case con version cannot be done
     */
    public static String convertObjectToString(Object source) throws SailException {
        if (source instanceof JsonNode) {
            JsonNode node = (JsonNode) source;
            if (node.isNumber() || node.isTextual()) {
                return node.asText();
            } else {
                try {
                    return objectMapper.writeValueAsString(node);
                } catch (JsonProcessingException jpe) {
                    throw new SailException(jpe);
                }
            }
        } else if (source instanceof Element) {
            try {
                TransformerFactory transFactory = TransformerFactory.newInstance();
                transFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
                transFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
                Transformer transformer = transFactory.newTransformer();
                StringWriter buffer = new StringWriter();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource((Element) source), new StreamResult(buffer));
                return buffer.toString();
            } catch (TransformerException e) {
                throw new SailException(e);
            }
        } else {
            return String.valueOf(source);
        }
    }

    /**
     * converter from the type system to a literal
     *
     * @param target    internal rep
     * @param resultKey eventual batch selector
     * @param output    config name to use for mapping
     * @return mapped value
     * @throws SailException in case the conversion cannot be done
     */
    public Value convertOutputToValue(Object target, String resultKey, IRI output) throws SailException {
        if (service.getResult().getOutputProperty() != null) {
            String[] resultPath = service.getResult().getOutputProperty().split("\\.");
            target = traversePath(target, resultPath);
        }
        if (resultKey != null) {
            if (target.getClass().isArray()) {
                if (service.getResult().getResultIdProperty() != null) {
                    String[] resultPath = service.getResult().getResultIdProperty().split("\\.");
                    target = Arrays.stream(((Object[]) target)).filter(tt -> resultKey.equals(convertObjectToString(traversePath(tt, resultPath))))
                            .findFirst().get();
                } else {
                    try {
                        target = Array.get(target, Integer.parseInt(resultKey));
                    } catch (NumberFormatException nfwe) {
                        throw new SailException(String.format("Could not access index %s of target %s which should be integer.", resultKey, target));
                    }
                }
            } else if (target instanceof ArrayNode) {
                if (service.getResult().getResultIdProperty() != null) {
                    String[] resultPath = service.getResult().getResultIdProperty().split("\\.");
                    ArrayNode array = (ArrayNode) target;
                    boolean found = false;
                    for (int count = 0; count < array.size(); count++) {
                        if (resultKey.equals(convertObjectToString(traversePath(array.get(count), resultPath)))) {
                            target = array.get(count);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new SailException(String.format("Could not find result with key %s under property %s.", resultKey, service.getResult().getResultIdProperty()));
                    }
                } else {
                    try {
                        target = ((ArrayNode) target).get(Integer.parseInt(resultKey));
                    } catch (NumberFormatException nfwe) {
                        throw new SailException(String.format("Could not access index %s of target %s which should be integer.", resultKey, target));
                    }
                }
            } else if (target instanceof Element) {
                if (service.getResult().getResultIdProperty() != null) {
                    String[] resultPath = service.getResult().getResultIdProperty().split("\\.");
                    NodeList nl = ((Element) target).getChildNodes();
                    boolean found = false;
                    for (int count = 0; count < nl.getLength(); count++) {
                        if (resultKey.equals(convertObjectToString(traversePath(nl.item(count), resultPath)))) {
                            target = nl.item(count);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new SailException(String.format("Could not find result with key %s under property %s.", resultKey, service.getResult().getResultIdProperty()));
                    }
                } else {
                    try {
                        target = ((Element) target).getChildNodes().item(Integer.parseInt(resultKey));
                    } catch (NumberFormatException nfwe) {
                        throw new SailException(String.format("Could not access index %s of target %s which should be integer.", resultKey, target));
                    }
                }
            }
        }
        // support nested output as json object for complex result types
        String outputString = output.stringValue();
        String path = null;
        String dataType = "https://json-schema.org/draft/2020-12/schema#Object";
        boolean isCollectiveResult = service.getResultName().equals(output.stringValue());
        if (!isCollectiveResult) {
            ReturnValueConfig cf = null;
            cf = service.getResult().getOutputs().get(output.stringValue());
            if (cf == null) {
                throw new SailException(String.format("No output specification for %s", output));
            }
            path = cf.getPath();
            dataType = cf.getDataType();
        }
        return convertOutputToValue(target, connection.remotingSail.getValueFactory(), path, dataType);
    }

    /**
     * converter from the type system to a literal
     *
     * @param target   source object
     * @param vf       factory for creating literals
     * @param cfPath   path under source object
     * @param dataType name of the target literal type
     * @return a literal
     */
    public static Value convertOutputToValue(Object target, ValueFactory vf, String cfPath, String dataType) throws SailException {
        String[] path = new String[0];
        if (cfPath != null) {
            path = cfPath.split("\\.");
        }
        Object pathObj = traversePath(target, path);
        switch (dataType) {
            case "https://json-schema.org/draft/2020-12/schema#Object":
                return vf.createLiteral(convertObjectToString(pathObj), vf.createIRI("https://json-schema.org/draft/2020-12/schema#Object"));
            case "http://www.w3.org/2001/XMLSchema#dateTime":
                return vf.createLiteral(convertObjectToString(pathObj), vf.createIRI("http://www.w3.org/2001/XMLSchema#dateTime"));
            case "http://www.w3.org/2001/XMLSchema#int":
                try {
                    return vf.createLiteral(Integer.parseInt(convertObjectToString(pathObj)));
                } catch (NumberFormatException nfwe) {
                    throw new SailException(String.format("Could not convert %s to integer.", String.valueOf(pathObj)));
                }
            case "http://www.w3.org/2001/XMLSchema#long":
                try {
                    return vf.createLiteral(Long.parseLong(convertObjectToString(pathObj)));
                } catch (NumberFormatException nfwe) {
                    throw new SailException(String.format("Could not convert %s to integer.", String.valueOf(pathObj)));
                }
            case "http://www.w3.org/2001/XMLSchema#double":
                try {
                    return vf.createLiteral(Double.parseDouble(convertObjectToString(pathObj)));
                } catch (NumberFormatException nfwe) {
                    throw new SailException(String.format("Could not convert %s to integer.", String.valueOf(pathObj)));
                }
            case "http://www.w3.org/2001/XMLSchema#float":
                try {
                    return vf.createLiteral(Float.parseFloat(convertObjectToString(pathObj)));
                } catch (NumberFormatException nfwe) {
                    throw new SailException(String.format("Could not convert %s to float.", String.valueOf(pathObj)));
                }
            case "http://www.w3.org/2001/XMLSchema#string":
                return vf.createLiteral(convertObjectToString(pathObj));
            case "http://www.w3.org/2001/XMLSchema#Element":
                return vf.createLiteral(convertObjectToString(pathObj), vf.createIRI("http://www.w3.org/2001/XMLSchema#Element")); // xml rendering?
            default:
                throw new SailException(String.format("Data Type %s is not supported.", dataType));
        }
    }

    /**
     * perform execution
     *
     * @param connection sail connection in which to perform the invocation
     * @param host       a binding host
     * @return flag indicating whether execution has been attempted (or was already done)
     */
    public boolean execute(RemotingSailConnection connection, BindingHost host) throws SailException {

        startTime = System.currentTimeMillis();

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Starting execution on connection %s with binding host %s at clock %d", connection, host, startTime));
        }

        try {
            if (service.getMatcher().group("classType") != null) {
                executeClass(connection, host);
            } else if (service.getMatcher().group("restType") != null) {
                executeRest(connection, host);
                return true;
            } else {
                throw new SailException("No class or rest binding found.");
            }
            return true;
        } finally {
            endTime = System.currentTimeMillis();
        }
    }

    /**
     * perform REST based executions
     *
     * @param connection sail connection in which to perform the invocation
     * @param host       binding host
     */
    public void executeRest(RemotingSailConnection connection, BindingHost host) throws SailException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("About to invoke REST call to connection %s at host %s", connection, host));
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String ourl = service.getMatcher().group("restType") + "://" + service.getMatcher().group("url");
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("About to invoke REST call to %s ", ourl));
            }
            CloseableHttpResponse response = null;
            CallbackToken asyncToken = null;
            Iterator<Collection<MutableBindingSet>> batches;
            batches = produceBatches(host);
            for (int batchCount = 0; batches.hasNext(); batchCount++) {
                Collection<MutableBindingSet> batch = batches.next();
                final String[] url = { ourl };
                switch (service.getMethod()) {
                    case "GET":
                        boolean isFirst = true;
                        for (MutableBindingSet binding : batch) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("About to process binding set %s", binding));
                            }
                            if (batch.size() > 1) {
                                if (isFirst) {
                                    url[0] = url[0] + "?(";
                                } else {
                                    url[0] = url[0] + "&(";
                                }
                            } else {
                                if (isFirst) {
                                    url[0] = url[0] + "?";
                                } else {
                                    url[0] = url[0] + "&";
                                }
                            }
                            isFirst = false;
                            final boolean[] isFirstArg = { true };
                            service.getArguments().entrySet().stream().sorted(new ArgumentComparator()).forEach(argument -> {
                                if (logger.isTraceEnabled()) {
                                    logger.trace(String.format("About to process argument %s %s", argument.getKey(), argument.getValue()));
                                }
                                Var mapping = inputs.get(argument.getKey());
                                Value value;
                                if (mapping.hasValue()) {
                                    value = mapping.getValue();
                                } else {
                                    value = binding.getValue(mapping.getName());
                                }
                                Object render = convertToObject(value, String.class, argument.getValue().getStrip());
                                if (isFirstArg[0]) {
                                    url[0] = url[0] + argument.getValue().getArgumentName();
                                } else {
                                    url[0] = url[0] + "&" + argument.getValue().getArgumentName();
                                }
                                isFirstArg[0] = false;
                                url[0] = url[0] + "=" + render;
                            });
                            if (batch.size() > 1) {
                                url[0] = url[0] + ")";
                            }
                        }
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Instantiated REST call target with parameters to %s ", url[0]));
                        }
                        final HttpGet httpget = new HttpGet(url[0]);
                        if (service.getAuthentication() != null) {
                            httpget.addHeader(service.getAuthentication().getAuthKey(), service.getAuthentication().getAuthCode());
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("Performing %s ", httpget));
                        }
                        response = httpclient.execute(httpget);
                        break;

                    case "POST-JSON":
                    case "POST-JSON-MF":
                        ObjectMapper objectMapper = new ObjectMapper();

                        ObjectNode body = objectMapper.createObjectNode();
                        ObjectNode message = body;
                        ObjectNode input = body;
                        ArrayNode array = objectMapper.createArrayNode();

                        if (service.getInputProperty() != null) {
                            String[] path = service.getInputProperty().split("\\.");
                            for (int count = 0; count < path.length; count++) {
                                message = input;
                                input = objectMapper.createObjectNode();
                                message.set(path[count], input);
                            }
                            if (service.getBatch() > 1) {
                                message.set(path[path.length - 1], array);
                            }
                        } else {
                            if (service.getBatch() > 1) {
                                throw new SailException(String.format("Cannot use batch mode without inputProperty."));
                            }
                        }

                        final ObjectNode finalinput = input;
                        for (MutableBindingSet binding : batch) {
                            AtomicBoolean isCorrect = new AtomicBoolean(true);
                            service.getArguments().entrySet().stream().sorted(new ArgumentComparator()).forEach(argument -> {
                                if (logger.isTraceEnabled()) {
                                    logger.trace(String.format("About to process argument %s %s", argument.getKey(), argument.getValue()));
                                }
                                processArgument(objectMapper, finalinput, binding, isCorrect, argument.getKey(), argument.getValue());
                            });
                            if (isCorrect.get()) {
                                array.add(input);
                            }
                        }

                        String invocationId = key.stringValue() + String.format("&batch=%d", batchCount);
                        if (service.getInvocationIdProperty() != null) {
                            if (!message.isObject()) {
                                throw new SailException(String.format("Cannot use invocationIdProperty in batch mode without inputProperty."));
                            } else {
                                setNode(objectMapper, ((ObjectNode) message), service.getInvocationIdProperty(), objectMapper.getNodeFactory().textNode(invocationId));
                            }
                        }

                        if (service.getCallbackProperty() != null) {
                            setNode(objectMapper, ((ObjectNode) message), service.getCallbackProperty(), objectMapper.getNodeFactory().textNode(connection.remotingSail.config.getCallbackAddress()));
                            if (service.getResult().getCallbackProperty() != null) {
                                asyncToken = CallbackController.register(service.getResult().getCallbackProperty(), invocationId);
                            }
                        }

                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Derived body %s", body));
                        }

                        final HttpPost httppost = new HttpPost(url[0]);
                        httppost.addHeader("accept", "application/json");
                        if (service.getAuthentication() != null) {
                            httppost.addHeader(service.getAuthentication().getAuthKey(), service.getAuthentication().getAuthCode());
                        }

                        if (service.getMethod().equals("POST-JSON")) {
                            httppost.addHeader("Content-Type", "application/json");
                            httppost.setEntity(new StringEntity(objectMapper.writeValueAsString(body)));
                        } else {
                            MultipartEntityBuilder mpeb = MultipartEntityBuilder.create();
                            mpeb.setBoundary("XXX");
                            Iterator<String> fields = body.fieldNames();
                            while (fields.hasNext()) {
                                String field = fields.next();
                                JsonNode node = body.get(field);
                                String content = objectMapper.writeValueAsString(node);
                                mpeb.addBinaryBody(field, content.getBytes(), ContentType.APPLICATION_JSON, field + ".json");
                            }
                            httppost.setEntity(mpeb.build());
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("Performing %s ", httppost));
                        }
                        response = httpclient.execute(httppost);
                        break;

                    default:
                        throw new SailException(String.format("Cannot invoke method %s", service.getMethod()));
                }

                int lsuccess = response.getStatusLine().getStatusCode();
                if (lsuccess >= 200 && lsuccess < 300) {
                    try {
                        Object result;

                        final HttpEntity entity = response.getEntity();
                        boolean isJson = false;
                        boolean isXml = false;
                        for (Header contentType : response.getHeaders("Content-Type")) {
                            if (contentType.getValue().contains("json")) {
                                isJson = true;
                            } else if (contentType.getValue().contains("xml")) {
                                isXml = true;
                            }
                        }

                        if (isXml) {
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            ByteArrayInputStream in = new ByteArrayInputStream(EntityUtils.toByteArray(entity));
                            result = builder.parse(in).getDocumentElement();
                        } else if (isJson) {
                            ObjectMapper mapper = new ObjectMapper();
                            ByteArrayInputStream in = new ByteArrayInputStream(EntityUtils.toByteArray(entity));
                            result = mapper.readTree(in);
                        } else {
                            result = EntityUtils.toString(entity);
                        }

                        if (asyncToken != null) {
                            result = CallbackController.synchronize(asyncToken);
                        }

                        if (result == null) {
                            logger.warn(String.format("Did not get any response."));
                            success = Math.max(success, 500);
                        } else {
                            for (MutableBindingSet binding : batch) {
                                String key = null;
                                if (service.getResult().getCorrelationInput() != null) {
                                    key = resolve(binding, service.getResult().getCorrelationInput(), null, String.class, null);
                                } else if (service.getBatch() > 1) {
                                    key = "0";
                                }
                                for (Map.Entry<Var, IRI> output : outputs.entrySet()) {
                                    binding.addBinding(output.getKey().getName(), convertOutputToValue(result, key, output.getValue()));
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn(String.format("Got an exception %s when processing invocation results of %s. Ignoring.", e, ourl));
                        success = Math.max(success, 500);
                    }
                } else {
                    logger.warn(String.format("Got an unsuccessful status %d from invoking %s. Ignoring.", lsuccess, ourl));
                    success = Math.max(lsuccess, success);
                }
            }
        } catch (IOException ioe) {
            logger.warn(String.format("Got an exception %s when processing invocation. Ignoring.", ioe));
            success = Math.max(500, success);
        }
    }

    /**
     * processes an argument binding into the output
     *
     * @param objectMapper   json factory
     * @param finalinput     complete output
     * @param binding        current binding
     * @param isCorrect      wrapper around correctness flag
     * @param argumentKey    key to the argument
     * @param argumentConfig config of the argument
     */
    protected void processArgument(ObjectMapper objectMapper, ObjectNode finalinput, MutableBindingSet binding, AtomicBoolean isCorrect, String argumentKey, ArgumentConfig argumentConfig) {
        JsonNode render = resolve(binding, argumentKey, (JsonNode) argumentConfig.getDefaultValue(), JsonNode.class, argumentConfig.getStrip());
        if (render != null) {
            String paths = argumentConfig.getArgumentName();
            Matcher matcher = ARGUMENT_PATTERN.matcher(paths);
            StringBuilder resultPaths = new StringBuilder();
            int end = 0;
            while (matcher.find()) {
                resultPaths.append(paths.substring(end, matcher.start()));
                String toResolve = matcher.group("arg");
                ArgumentConfig targetArg = service.getArguments().get(toResolve);
                String result = resolve(binding, toResolve, (String) targetArg.getDefaultValue(), String.class, targetArg.getStrip());
                resultPaths.append(result);
                end = matcher.end();
            }
            resultPaths.append(paths.substring(end));
            setNode(objectMapper, finalinput, resultPaths.toString(), render);
        } else {
            if (argumentConfig.isMandatory()) {
                // TODO optional arguments
                logger.warn(String.format("Mandatory argument %s has no binding. Leaving the hole tuple.", argumentKey));
                isCorrect.set(false);
            }
        }
    }

    /**
     * resolves a given input predicate against a  binding
     *
     * @param binding      the binding
     * @param input        predicate as uri string
     * @param defaultValue a possible default value
     * @param forClass     target class to convert binding into
     * @param <TARGET>     template type
     * @return found binding of predicate, null if not bound
     */
    private <TARGET> TARGET resolve(MutableBindingSet binding, String input, TARGET defaultValue, Class<TARGET> forClass, String strip) {
        String key;
        Var variable = inputs.get(input);
        Value value = null;
        if (variable != null) {
            if (variable.hasValue()) {
                value = variable.getValue();
            } else {
                value = binding.getValue(variable.getName());
            }
        }

        if (value != null) {
            return convertToObject(value, forClass, strip);
        }

        if (defaultValue != null) {
            if (JsonNode.class.isAssignableFrom(forClass)) {
                try {
                    return (TARGET) objectMapper.readTree(objectMapper.writeValueAsString(defaultValue));
                } catch (JsonProcessingException e) {
                    // jump to default case
                }
            }
            return (TARGET) defaultValue;
        }

        return null;
    }

    /**
     * produces a set of batches to call
     *
     * @param host binding host
     * @return an iterator over the batches as collections of bindingsets
     */
    protected Iterator<Collection<MutableBindingSet>> produceBatches(BindingHost host) {
        var batchGroup = service.getArguments().entrySet().stream().filter(argument -> argument.getValue().isFormsBatchGroup())
                .collect(Collectors.toList());
        final Map<Object, Collection<MutableBindingSet>> batches = new HashMap<>();
        long bindingCount = 0;
        for (MutableBindingSet binding : host.getBindings()) {
            bindingCount++;
            Object key;
            if (batchGroup.isEmpty()) {
                if (service.getBatch() > 1) {
                    key = bindingCount / service.getBatch();
                } else {
                    key = bindingCount;
                }
            } else {
                key = new BatchKey(batchGroup.stream().map(
                        batch -> resolve(binding, batch.getKey(), null, String.class, null)
                ).toArray(size -> new String[size]));
            }
            Collection<MutableBindingSet> targetCollection;
            if (batches.containsKey(key)) {
                targetCollection = batches.get(key);
            } else {
                targetCollection = new ArrayList<>();
                batches.put(key, targetCollection);
            }
            targetCollection.add(binding);
        }
        return batches.values().iterator();
    }

    /**
     * merges two given ObjectNodes
     *
     * @param source the ObjectNodes to be merged
     * @param target the ObjectNodes where source needs to be merged into
     */
    public static ObjectNode mergeObjectNodes(ObjectNode target, ObjectNode source) {
        if (source == null) {
            return target;
        }

        Iterator<Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode sourceValue = entry.getValue();
            JsonNode targetValue = target.get(key);

            if (targetValue != null && targetValue.isObject() && sourceValue.isObject()) {
                // Recursively merge nested objects
                mergeObjectNodes((ObjectNode) targetValue, (ObjectNode) sourceValue);
            } else if (targetValue != null && targetValue.isArray() && sourceValue.isArray()) {
                // Merge arrays
                mergeArrays((ArrayNode) targetValue, (ArrayNode) sourceValue);
            } else {
                // Add the field to target
                target.set(key, sourceValue);
            }
        }

        return target;
    }

    /**
     * merges two given ArrayNodes
     *
     * @param source the ArrayNode to be merged
     * @param target the ArrayNode where source needs to be merged into
     */
    private static void mergeArrays(ArrayNode target, ArrayNode source) {
        int sourceLength = source.size();
        int targetLength = target.size();

        for (int i = 0; i < sourceLength; i++) {
            // target shorter than source?
            if (targetLength < i + 1) {
                target.add(source.get(i));
                return;
            }
            JsonNode targetElement = target.get(i);
            JsonNode sourceElement = source.get(i);
            if (targetElement.isObject() && sourceElement.isObject()) {
                // Recursively merge JSON objects
                mergeObjectNodes((ObjectNode) targetElement, (ObjectNode) sourceElement);
            } else if (targetElement.isArray() && sourceElement.isArray()) {
                // Recursively merge arrays
                mergeArrays((ArrayNode) targetElement, (ArrayNode) sourceElement);
            } else {
                target.set(i, source.get(i));
            }
        }
    }


    /**
     * sets a given node under a possible recursive path
     *
     * @param objectMapper factory
     * @param finalInput   target subject
     * @param pathSpec     a path sepcification
     * @param render       the target object
     */
    public static void setNode(ObjectMapper objectMapper, ObjectNode finalInput, String pathSpec, JsonNode render) {
        String[] pathNames = pathSpec.split(",");
        for (String pathName : pathNames) {
            String[] argPath = pathName.split("\\.");
            JsonNode traverse = finalInput;
            int depth = 0;
            if (argPath.length == depth) {
                finalInput = (ObjectNode) mergeObjectNodes(finalInput, (ObjectNode) render);
                return;
            }
            for (String argField : argPath) {
                if (depth != argPath.length - 1) {
                    if (hasField(traverse, argField)) {
                        JsonNode next = getField(traverse, argField);
                        if (next == null || (!next.isArray() && !next.isObject())) {
                            throw new SailException(String
                                    .format("Field %s was occupied by a non-object object", argField, next));
                        } else {
                            traverse = next;
                        }
                    } else {
                        ObjectNode next = objectMapper.createObjectNode();
                        setObject(objectMapper, traverse, argField, next);
                        traverse = next;
                    }
                } else {
                    setObject(objectMapper, traverse, argField, render);
                }
                depth++;
            } // set argument in input 
        }
    }

    /**
     * accesses a certain field
     *
     * @param traverse object or array
     * @param argField field name or index
     * @return embedded object
     */
    public static JsonNode getField(JsonNode traverse, String argField) {
        return traverse.isArray() ? ((ArrayNode) traverse).get(Integer.parseInt(argField)) : traverse.get(argField);
    }

    /**
     * checks whether this node has a certain field
     *
     * @param traverse object or array
     * @param argField field name or index
     * @return existance of the field
     */
    public static boolean hasField(JsonNode traverse, String argField) {
        return traverse.isArray() ? traverse.size() > Integer.parseInt(argField) : traverse.has(argField);
    }

    /**
     * sets the given object into the given single-level path into a given subject
     *
     * @param objectMapper factory
     * @param render       target object
     * @param traverse     target subject
     * @param argField     target field
     */
    public static void setObject(ObjectMapper objectMapper, JsonNode traverse, String argField, JsonNode render) {
        if (traverse.isObject()) {
            if (traverse.has(argField)) {
                ObjectReader updater = objectMapper.readerForUpdating(traverse.get(argField));
                try {
                    render = updater.readValue(render);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            ((ObjectNode) traverse).set(argField, render);
        } else if (traverse.isArray()) {
            ArrayNode traverseArray = (ArrayNode) traverse;
            int targetIndex = Integer.valueOf(argField);
            while (traverseArray.size() < targetIndex) {
                traverseArray.add(objectMapper.createObjectNode());
            }
            if (traverseArray.size() == targetIndex) {
                traverseArray.add(render);
            } else {
                ObjectReader updater = objectMapper.readerForUpdating(traverseArray.get(targetIndex));
                try {
                    render = updater.readValue(render);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                traverseArray.set(targetIndex, render);
            }
        }
    }

    /**
     * perform class-based reflection execution
     *
     * @param connection sail connection in which to perform the invocation
     * @param host       binding host
     */
    public void executeClass(RemotingSailConnection connection, BindingHost host) throws SailException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("About to invoke Java call to connection %s at host %s", connection, host));
        }
        Class targetClass = null;
        try {
            targetClass = getClass().getClassLoader().loadClass(service.getMatcher().group("class"));
        } catch (ClassNotFoundException e) {
            throw new SailException(e);
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Found class %s ", targetClass));
        }
        Object targetInstance = null;
        try {
            targetInstance = targetClass.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new SailException(e);
        } catch (IllegalAccessException e) {
            throw new SailException(e);
        } catch (InstantiationException e) {
            throw new SailException(e);
        } catch (InvocationTargetException e) {
            throw new SailException(e.getCause());
        }
        for (MutableBindingSet binding : host.getBindings()) {
            Method targetMethod = null;
            Object[] targetParams = null;
            try {
                for (Method meth : targetClass.getMethods()) {
                    if (meth.getName().equals(service.getMatcher().group("method"))) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Found method %s ", meth));
                        }
                        targetParams = new Object[meth.getParameterTypes().length];
                        int argIndex = 0;
                        for (Parameter param : meth.getParameters()) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("Checking parameter %s", param));
                            }
                            ArgumentConfig aconfig = null;
                            for (Map.Entry<String, ArgumentConfig> argument : service.getArguments().entrySet()) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace(String.format("Agains argument %s %s", argument.getKey(),
                                            argument.getValue().getArgumentName()));
                                }
                                if (argument.getValue().getArgumentName().contains(param.getName())) {
                                    aconfig = argument.getValue();
                                    Var arg = inputs.get(argument.getKey());
                                    Value value;
                                    if (!arg.hasValue()) {
                                        value = binding.getValue(arg.getName());
                                    } else {
                                        value = arg.getValue();
                                    }
                                    targetParams[argIndex++] = convertToObject(value, param.getType(), aconfig.getStrip());
                                    break;
                                }
                            }
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("Parameter %s resulted to argument %s ", param, aconfig));
                            }
                            if (aconfig == null) {
                                targetParams = null;
                                break;
                            }
                        }
                        if (targetParams != null) {
                            targetMethod = meth;
                            break;
                        }
                    }
                }
                if (targetMethod == null) {
                    throw new SailException(
                            String.format("Target method %s with suitable arguments could not be found in class %s.",
                                    service.getMatcher().group("method"), targetClass));
                }
            } finally {
                // in case we need to cleanup something
            }
            try {
                Object result = targetMethod.invoke(targetInstance, targetParams);
                for (Map.Entry<Var, IRI> output : outputs.entrySet()) {
                    binding.addBinding(output.getKey().getName(), convertOutputToValue(result, null, output.getValue()));
                }
            } catch (Exception e) {
                logger.warn(String.format("Invocation to %s (method %s) resulted in exception %s", targetInstance, targetMethod, e));
                success = Math.max(500, success);
            }
        }
    }

}