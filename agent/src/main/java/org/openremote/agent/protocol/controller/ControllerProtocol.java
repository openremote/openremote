/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.controller;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.controller.command.ControllerCommandBasic;
import org.openremote.agent.protocol.controller.command.ControllerCommandMapped;
import org.openremote.agent.protocol.http.HttpClientProtocol;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.container.Container;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.*;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.web.WebTargetBuilder.CONNECTION_POOL_SIZE;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.*;

/**
 * This is a Controller 2.5 protocol to communicate with a running Controller 2.5 instance and getting sensor's status and executing commands.
 * <p>
 * Necessary META for Agent setup is {@link #META_PROTOCOL_BASE_URI} and if a basic authentication is active on Controller instance :
 * {@link #META_PROTOCOL_USERNAME} - {@link #META_PROTOCOL_PASSWORD}
 * <p>
 * The protocol communicate with Controller by using Controller REST API.
 * <p>
 * The protocol manage two kinds of request :
 * <ul>
 * <li>A set of polling requests (one by controller/device name couple) {@link #pollingSensorList}. For each request we just wait for a response 200 (new status) or 403 (timeout after 60 seconds) and relaunch the
 * same request as soon as we have one of those two responses.</li>
 * <li>Executing commands provided by Write Attribute with necessary information. There is different kind of situations explained in
 * {@link org.openremote.agent.protocol.controller.ControllerCommand}</li>
 * </ul>
 * <p>
 * <p>
 * Two cases are considered :
 * <ul>
 * <li>A sensor : if we want to link an attribute to a controller 2.5 sensor to get status we should create an Attribute with proper type
 * considering the situation (temperature, time,...) and two necessary META {@link #META_ATTRIBUTE_DEVICE_NAME} and
 * {@link #META_ATTRIBUTE_SENSOR_NAME}</li>
 * <li>A command : if we want to execute a command on a controller 2.5, we create an attribute with the following available META (see details
 * in {@link org.openremote.agent.protocol.controller.ControllerCommand}) :
 * {@link #META_ATTRIBUTE_DEVICE_NAME} or {@link #META_ATTRIBUTE_COMMAND_DEVICE_NAME}, {@link #META_ATTRIBUTE_COMMAND_NAME} or
 * {@link #META_ATTRIBUTE_COMMANDS_MAP}</li>
 * </ul>
 * <p>
 */
@SuppressWarnings("JavaDoc")
// TODO: Fix or remove this protocol - it does constant polling with no delay for each sensor - should be using long poll mechanism; also futures are not cancelled and NPEs appear etc...nasty
public class ControllerProtocol extends AbstractProtocol {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ControllerProtocol.class);

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":controllerClient";

    /*--------------- META ITEMS TO BE USED ON PROTOCOL CONFIGURATIONS ---------------*/
    /**
     * Base URI for all requests to this server
     */
    public static final String META_PROTOCOL_BASE_URI = PROTOCOL_NAME + ":baseUri";
    
    /**
     * Basic authentication username (string)
     */
    public static final String META_PROTOCOL_USERNAME = PROTOCOL_NAME + ":username";

    /**
     * Basic authentication password (string)
     */
    public static final String META_PROTOCOL_PASSWORD = PROTOCOL_NAME + ":password";

    /*--------------- META ITEMS TO BE USED ON LINKED ATTRIBUTES ---------------*/
    /**
     * Path to query Controller :
     * <p>
     * - status : {@link #META_PROTOCOL_BASE_URI}/devices/{@link #META_ATTRIBUTE_DEVICE_NAME}/status?name={@link #META_ATTRIBUTE_SENSOR_NAME}
     * - execute command : {@link #META_PROTOCOL_BASE_URI}/devices/{@link #META_ATTRIBUTE_DEVICE_NAME}/commands?name={COMMAND_TO_EXECUTE_DEPENDING_CASE}
     * - polling : {@link #META_PROTOCOL_BASE_URI}/devices/{@link #META_ATTRIBUTE_DEVICE_NAME}/polling/<DeviceIDFromController/>?name={@link #META_ATTRIBUTE_SENSOR_NAME}&name={@link #META_ATTRIBUTE_SENSOR_NAME}
     * <p>
     * For Polling, a global set of all sensor_name has to be maintained as Attribute are added. Those sensor_name will be grouped by device_name
     * such that we can group them together.
     * <p>
     * /
     * <p>
     * /**
     * Device name to query on Controller
     * (string)
     */
    public static final String META_ATTRIBUTE_DEVICE_NAME = PROTOCOL_NAME + ":deviceName";

    /**
     * Relative path to endpoint on the server; supports dynamic value insertion, see class javadoc for details
     * (string)
     */
    public static final String META_ATTRIBUTE_SENSOR_NAME = PROTOCOL_NAME + ":sensorName";
    /**
     * A command could be execute on a different device name than the device name used for sensor status
     * (string)
     */
    public static final String META_ATTRIBUTE_COMMAND_DEVICE_NAME = PROTOCOL_NAME + ":commandDeviceName";
    /**
     * A command name
     * (string)
     */
    public static final String META_ATTRIBUTE_COMMAND_NAME = PROTOCOL_NAME + ":commandName";
    /**
     * Relative path to endpoint on the server; supports dynamic value insertion, see class javadoc for details
     * (string)
     */
    public static final String META_ATTRIBUTE_COMMANDS_MAP = PROTOCOL_NAME + ":commandsMap";

    public static final int HEARTBEAT_DELAY_SECONDS = 5;

    protected static final List<MetaItemDescriptorImpl> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays
            .asList(new MetaItemDescriptorImpl(META_PROTOCOL_BASE_URI, ValueType.STRING, true,
                            REGEXP_PATTERN_BASIC_HTTP_URL, MetaItemDescriptor.PatternFailure.HTTP_URL.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_PROTOCOL_USERNAME, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
                            MetaItemDescriptor.PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_PROTOCOL_PASSWORD, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
                            MetaItemDescriptor.PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE.name(), 1, null, false, null, null, null));

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays
            .asList(new MetaItemDescriptorImpl(META_ATTRIBUTE_DEVICE_NAME, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY, MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_ATTRIBUTE_SENSOR_NAME, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY, MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_ATTRIBUTE_COMMAND_DEVICE_NAME, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY, MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_ATTRIBUTE_COMMAND_NAME, ValueType.STRING, false,
                            REGEXP_PATTERN_STRING_NON_EMPTY, MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(), 1, null, false, null, null, null),
                    new MetaItemDescriptorImpl(META_ATTRIBUTE_COMMANDS_MAP, ValueType.OBJECT, false, null, null, 1, null,
                            false, null, null, null));

    /*--------------- Protocol attributes ---------------*/
    public static final String PROTOCOL_DISPLAY_NAME = "Controller Client";
    public static final String PROTOCOL_VERSION = "1.0";

    protected ResteasyClient client;
    private Map<PollingKey, ScheduledFuture> pollingSensorList = new HashMap<>();
    private Map<AttributeRef, Controller> controllersMap = new HashMap<>();
    private Map<AttributeRef, ResteasyWebTarget> controllersTargetMap = new HashMap<>();
    private Map<AttributeRef, ScheduledFuture> controllerHeartbeat = new HashMap<>();
    private Map<AttributeRef, Boolean> initStatusDone = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        client = createClient(executorService, CONNECTION_POOL_SIZE, 70000, null);
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return new ArrayList<>(PROTOCOL_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return new ArrayList<>(ATTRIBUTE_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        LOG.fine("### Adding new protocol " + protocolConfiguration.getReferenceOrThrow() + " (" + protocolConfiguration.getNameOrThrow() + ")");

        String baseURL = protocolConfiguration.getMetaItem(META_PROTOCOL_BASE_URI).flatMap(AbstractValueHolder::getValueAsString)
                .orElseThrow(() -> new IllegalArgumentException("Missing or invalid required meta item: " + META_PROTOCOL_BASE_URI));

        try {
            URI uri = new URIBuilder(baseURL).build();
            WebTargetBuilder webTargetBuilder = new WebTargetBuilder(client, uri);
            String username = protocolConfiguration.getMetaItem(META_PROTOCOL_USERNAME).flatMap(AbstractValueHolder::getValueAsString).orElse(null);
            String password = protocolConfiguration.getMetaItem(META_PROTOCOL_PASSWORD).flatMap(AbstractValueHolder::getValueAsString).orElse(null);

            if (username != null && password != null) {
                webTargetBuilder.setBasicAuthentication(username, password);
            }

            controllersTargetMap.put(protocolConfiguration.getReferenceOrThrow(), webTargetBuilder.build());

            controllersMap.put(protocolConfiguration.getReferenceOrThrow(), new Controller(protocolConfiguration.getReferenceOrThrow()));

            this.updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISCONNECTED);

            controllerHeartbeat.put(protocolConfiguration.getReferenceOrThrow(), this.executorService.scheduleWithFixedDelay(() -> this.executeHeartbeat(protocolConfiguration.getReferenceOrThrow(),
                response -> onHeartbeatResponse(protocolConfiguration.getReferenceOrThrow(), response)), 0, HEARTBEAT_DELAY_SECONDS,
                TimeUnit.SECONDS));
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, "Invalid URI", e);
            updateConnectionStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR);
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        controllersMap.remove(protocolConfiguration.getReferenceOrThrow());
        controllersTargetMap.remove(protocolConfiguration.getReferenceOrThrow());
        controllerHeartbeat.remove(protocolConfiguration.getReferenceOrThrow());

        for (PollingKey key : this.pollingSensorList.keySet()) {
            if (key.getControllerAgentRef().equals(protocolConfiguration.getReferenceOrThrow())) {
                this.pollingSensorList.remove(key);
            }
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        String deviceName = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_DEVICE_NAME, StringValue.class, true, true)
                .map(StringValue::getString).orElse(null);

        String sensorName = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_SENSOR_NAME, StringValue.class, false, true)
                .map(StringValue::getString).orElse(null);

        String commandDeviceName = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_COMMAND_DEVICE_NAME, StringValue.class, false, true)
                .map(StringValue::getString).orElse(null);

        String commandName = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_COMMAND_NAME, StringValue.class, false, true)
                .map(StringValue::getString).orElse(null);

        MultivaluedMap<String, String> commandsMap = Values
                .getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_COMMANDS_MAP, ObjectValue.class, false, true)
                .flatMap(objectValue -> HttpClientProtocol.getMultivaluedMap(objectValue, false)).orElse(null);

        /**
         * Build Sensor Status info for polling request
         */
        if (sensorName != null) {
            LOG.fine("### Adding new sensor [" + deviceName + "," + sensorName + "] linked to " + protocolConfiguration.getReferenceOrThrow() + " (" + protocolConfiguration.getNameOrThrow() + ")");
            controllersMap.get(protocolConfiguration.getReferenceOrThrow()).addSensor(attribute.getReferenceOrThrow(), new ControllerSensor(deviceName, sensorName));

            //Properly stop previously existing polling on device name --> use of false parameter
            PollingKey pollingKey = new PollingKey(deviceName, protocolConfiguration.getReferenceOrThrow());

            if (pollingSensorList.containsKey(pollingKey)) {
                pollingSensorList.get(pollingKey).cancel(true);
            }

            this.initStatusDone.put(attribute.getReferenceOrThrow(), false);

            //Get initial status of sensor
            this.collectInitialStatus(attribute.getReferenceOrThrow(), deviceName, sensorName, protocolConfiguration.getReferenceOrThrow());

            //Put new polling on a new device name or update previous
            this.schedulePollingTask(pollingKey);
        }

        /**
         * If linked Attribute contains command info, we build {@link org.openremote.agent.protocol.controller.ControllerCommand } depending on
         * attribute information.
         */
        if (commandName != null || commandsMap != null) {
            //If no command specific device name is set, then we're using deviceName
            if (commandDeviceName == null && deviceName != null) {
                commandDeviceName = deviceName;
            }

            if (commandName != null) {
                controllersMap.get(protocolConfiguration.getReferenceOrThrow()).addCommand(attribute.getReferenceOrThrow(), new ControllerCommandBasic(commandDeviceName, commandName));
            } else {
                assert commandsMap.size() > 0;
                controllersMap.get(protocolConfiguration.getReferenceOrThrow()).addCommand(attribute.getReferenceOrThrow(), new ControllerCommandMapped(commandDeviceName, computeCommandsMapFromMultiValue(commandsMap)));
            }
        }
    }

    /**
     * Clearing elements if an attribute is unlinked from Controller Agent
     * We don't have to clear {@link #pollingSensorList} as a check is done before scheduling task {@link #schedulePollingTask}
     *
     * @param attribute
     * @param protocolConfiguration
     */
    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        controllersMap.get(protocolConfiguration.getReferenceOrThrow()).removeAttributeRef(attribute.getReferenceOrThrow());
    }

    /**
     * Write action on a linked attribute mean we execute a command on the Controller. It induce a HTTP request and manage it's return code. (No value
     * is returned from the execution of a command)
     *  @param event
     * @param processedValue
     * @param protocolConfiguration
     */
    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        LOG.fine("### Process Linked Attribute Write");

        AttributeRef attributeRef = event.getAttributeRef();
        ControllerCommand controllerCommand = controllersMap.get(protocolConfiguration.getReferenceOrThrow()).getCommand(attributeRef);
        HttpClientProtocol.HttpClientRequest request = RequestBuilder.buildCommandRequest(controllerCommand, event, this.controllersTargetMap.get(protocolConfiguration.getReferenceOrThrow()));

        String body = null;

        if (controllerCommand instanceof ControllerCommandBasic) {
            body = event.getValue().map(v -> {
                ObjectValue objectValue = Values.createObject();
                objectValue.put("parameter", v);
                return objectValue.toString();
            }).orElse(null);
        }
        executeAttributeWriteRequest(request, body, protocolConfiguration.getReferenceOrThrow(), this::onAttributeWriteResponse);
    }

    /**
     * Convert commands map received as {@link MultivaluedMap} into a simple {@link Map}
     *
     * @param multivaluedMap
     * @return
     */
    private Map<String, String> computeCommandsMapFromMultiValue(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, String> commandsMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
            commandsMap.put(entry.getKey(), entry.getValue().get(0));
        }

        return commandsMap;
    }

    private void collectInitialStatus(AttributeRef attributeRef, String deviceName, String sensorName, AttributeRef controllerRef) {
        this.executorService
                .schedule(() -> this.executeInitialStatus(attributeRef, deviceName, sensorName, controllerRef,response -> onInitialStatusResponse(attributeRef, deviceName, sensorName, controllerRef, response)),
                        0);
    }

    private void executeInitialStatus(AttributeRef attributeRef, String deviceName, String sensorName, AttributeRef controllerRef, Consumer<Response> responseConsumer) {
        withLock(getProtocolName() + "::executeInitialStatus::" + attributeRef, () -> {
            LOG.info("### Initial status check for " + attributeRef.getAttributeName() + " [" + deviceName + "," + sensorName + "] ...");

            HttpClientProtocol.HttpClientRequest checkRequest = RequestBuilder.buildStatusRequest(deviceName, Arrays.asList(sensorName), this.controllersTargetMap.get(controllerRef));

            Response response = null;

            try {
                response = checkRequest.invoke(null);
                responseConsumer.accept(response);
            } catch (ProcessingException e) {
                LOG.log(Level.SEVERE, "### Initial status for " + attributeRef.getAttributeName() + " [" + deviceName + "," + sensorName + "] doesn't succeed", e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }

        });
    }

    private void onInitialStatusResponse(AttributeRef attributeRef, String deviceName, String sensorName, AttributeRef controllerRef, Response response) {
        if(response != null) {
            if (response.getStatusInfo().equals(Response.Status.OK)) {
                String responseBodyAsString = response.readEntity(String.class);

                LOG.fine("### New sensor [" + sensorName + "] status received");
                LOG.finer("### Status request body response : " + responseBodyAsString);

                Optional<ArrayValue> arrayValue = Values.parse(responseBodyAsString).flatMap(Values::getArray);
                Optional<List<ObjectValue>> statuses = Values.getArrayElements(arrayValue.orElse(null), ObjectValue.class, false, false);

                if (!statuses.isPresent()) {
                    LOG.warning("### Status response is not a JSON array or empty: " + responseBodyAsString);
                } else {
                    statuses.get().forEach(status -> {
                        String name = status.getString("name").orElse(null);
                        String value = status.getString("value").orElse(null);

                        this.updateAttributeValue(attributeRef, value);

                        this.initStatusDone.put(attributeRef, true);
                    });
                }
            } else {
                LOG.severe("### Status code for initial status received error : " + response.getStatus() + " --> " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            LOG.warning("### Initial status check return a null value for " + attributeRef.getAttributeName() + " [" + deviceName + "," + sensorName + "]");
        }

        if(!this.initStatusDone.get(attributeRef)) {
            collectInitialStatus(attributeRef, deviceName, sensorName, controllerRef);
        }
    }

    /**
     * Compute the polling request for a given deviceName and controller. Method check all registered sensor's (linked to the Protocol) and collect all sensor's name
     * to put them into polling request
     *
     * @param pollingKey Device name and controller agent reference on which we're polling
     * @return {@link ScheduledFuture} task to keep a track on
     */
    private ScheduledFuture computePollingTask(PollingKey pollingKey) {
        return withLockReturning(getProtocolName() + "::computePollingTask::" + pollingKey.getControllerAgentRef() + "::" + pollingKey.getDeviceName(), () -> {
            List<String> sensorNameList = this.controllersMap.get(pollingKey.getControllerAgentRef()).collectSensorNameLinkedToDeviceName(pollingKey.getDeviceName());

            if (sensorNameList.isEmpty()) {
                return null;
            }

            return executorService.schedule(() -> executePollingRequest(pollingKey, sensorNameList,
                    response -> onPollingResponse(pollingKey, sensorNameList, response)), 0);
        });
    }

    /**
     * Polling Request execution if a Connection issue (exception) occurs, we check the nature {@link #checkIfConnectionRefused(Exception, AttributeRef)}
     *
     * @param pollingKey       device name and controller agent ref on which we'll polling
     * @param sensorList       list of sensors to catch status
     * @param responseConsumer Consumer for onResponse treatment
     */
    private void executePollingRequest(PollingKey pollingKey, List<String> sensorList, Consumer<Response> responseConsumer) {
        LOG.info("### Polling Request for device [device=" + pollingKey.getDeviceName() + ", sensors=" + this.formatSensors(sensorList) + "]");

        HttpClientProtocol.HttpClientRequest httpClientRequest = RequestBuilder
                .buildStatusPollingRequest(pollingKey.getDeviceName(), sensorList, this.controllersMap.get(pollingKey.getControllerAgentRef()).getDeviceId(), this.controllersTargetMap.get(pollingKey.getControllerAgentRef()));

        Response response = null;

        try {
            response = httpClientRequest.invoke(null);

            this.updateConnectionStatus(pollingKey.getControllerAgentRef(), ConnectionStatus.CONNECTED);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "### Exception thrown whilst doing polling request [device=" + pollingKey.getDeviceName() + ", sensors=" + this.formatSensors(sensorList) + "]",
                    e);

            this.checkIfConnectionRefused(e, pollingKey.getControllerAgentRef());
        }

        responseConsumer.accept(response);
    }

    /**
     * Polling request should return three different responses :
     * <ul>
     * <li>OK (200) : new values are available for at least one of the sensor provided in queryParam</li>
     * <li>TIMEOUT (408) : during the last 60 seconds following the start of the request, none of the sensors have new values</li>
     * <li>Others : error</li>
     * </ul>
     * <p>
     * In every case, we should start a new polling request directly. Only the 200 response induce an update of every linked attribute having a sensor
     * status updated.
     *
     * @param pollingKey     device name and controller agent ref on which polling has been execute
     * @param sensorNameList sensors requested
     * @param response       Response received from request
     */
    private void onPollingResponse(PollingKey pollingKey, List<String> sensorNameList, Response response) {
        if(response != null) {
            if (response.getStatusInfo() == Response.Status.OK) {
                String responseBodyAsString = response.readEntity(String.class);

                LOG.info("### New sensors status received");
                LOG.finer("### Polling request body response : " + responseBodyAsString);

                Optional<ArrayValue> arrayValue = Values.parse(responseBodyAsString).flatMap(Values::getArray);
                Optional<List<ObjectValue>> statuses = Values.getArrayElements(arrayValue.orElse(null), ObjectValue.class, false, false);

                if (!statuses.isPresent()) {
                    LOG.warning("### Polling response is not a JSON array or empty: " + responseBodyAsString);
                } else {
                    statuses.get().forEach(status -> {
                        String name = status.getString("name").orElse(null);
                        String value = status.getString("value").orElse(null);

                        /**
                         * For every sensors in the request body, find the linked attributeref and update value by calling {@link updateAttributeValue}
                         */
                        this.controllersMap.get(pollingKey.getControllerAgentRef()).getSensorsListForDevice(pollingKey.getDeviceName()).stream()
                                .filter(entry -> entry.getValue().getSensorName().equals(name))
                                .forEach(e -> this.updateAttributeValue(e.getKey(), value));
                    });
                }
            } else if (response.getStatusInfo() == Response.Status.REQUEST_TIMEOUT) {
                LOG.info("### Timeout from polling no changes on Controller side given sensors [device=" + pollingKey.getDeviceName() + ", sensors=" + this.formatSensors(sensorNameList) + "]");
            } else {
                LOG.severe("### Status code received error : " + response.getStatus() + " --> " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            LOG.severe("### Received null response from polling (due to previous exception)");
        }

        //No matter status code, we're continuing to poll
        this.schedulePollingTask(pollingKey);
    }

    /**
     * Update linked attribute with new value. We should take care of attribute type and format
     *
     * @param attributeRef
     * @param value
     */
    private void updateAttributeValue(AttributeRef attributeRef, String value) {
        LOG.fine("### Updating attribute " + attributeRef + " with value " + value);
        AttributeValueDescriptor attributeType = this.linkedAttributes.get(attributeRef).getTypeOrThrow();

        ValueType valueType = attributeType.getValueType();
        try {
            switch (valueType) {
                case BOOLEAN:
                    this.updateLinkedAttribute(new AttributeState(attributeRef, Values.create(Boolean.parseBoolean(value))));
                    break;
                case NUMBER:
                    this.updateLinkedAttribute(new AttributeState(attributeRef, Values.create(Double.parseDouble(value))));
                    break;
                default:
                    this.updateLinkedAttribute(new AttributeState(attributeRef, Values.create(value)));
            }
        } catch (NumberFormatException e) {
            LOG.severe("### Error in parsing NUMBER value [" + value + "]");
            this.updateLinkedAttribute(new AttributeState(attributeRef, Values.create(0.0)));
        }
    }

    private void executeAttributeWriteRequest(HttpClientProtocol.HttpClientRequest request, String body, AttributeRef protocolRef, Consumer<Response> responseConsumer) {
        Response response = null;

        try {
            response = request.invoke(body);

            this.updateConnectionStatus(protocolRef, ConnectionStatus.CONNECTED);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "### Exception thrown whilst doing attribute write request", e);

            this.checkIfConnectionRefused(e, protocolRef);
        }

        responseConsumer.accept(response);
    }

    private void onAttributeWriteResponse(Response response) {
        if (response != null) {
            LOG.fine("### Response from command (204 is a valid and success return) : " + response.getStatus());
            if (response.getStatus() != 204) {
                LOG.severe("### Linked attribute Write request return with an error (different from 204) : " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            LOG.warning("### Response set to null on Write");
        }
    }

    private String formatSensors(List<String> sensorList) {
        return String.join(",", sensorList);
    }

    /**
     * {@link #checkIfConnectionRefused(Exception, AttributeRef)} check the exception received from a request execution to see if it's not a
     * connection issue. If it is the case, we'll start a heartbeat task until we get a new signal. Heartbeat is done evey
     * {@link #HEARTBEAT_DELAY_SECONDS} seconds.
     *
     * @param e           is the exception thrown by invoking the request
     * @param protocolRef is the reference to Protocol configuration (to update status,...)
     */
    private void checkIfConnectionRefused(Exception e, AttributeRef protocolRef) {
        if (e.getCause() instanceof HttpHostConnectException) {
            HttpHostConnectException e2 = (HttpHostConnectException) e.getCause();

            if (e2.getCause() instanceof ConnectException || e2.getCause() instanceof UnknownHostException) {
                ConnectException e3 = (ConnectException) e2.getCause();
                LOG.log(Level.SEVERE, "### Connection refused : " + e3.getMessage());
                this.updateConnectionStatus(protocolRef, ConnectionStatus.DISCONNECTED);

                //Starting a heartbeat Task until connection is OK
                if (!this.controllerHeartbeat.containsKey(protocolRef) || this.controllerHeartbeat.get(protocolRef).isCancelled()) {
                    this.controllerHeartbeat.put(protocolRef, this.executorService
                            .scheduleWithFixedDelay(() -> this.executeHeartbeat(protocolRef, response -> onHeartbeatResponse(protocolRef, response)),
                                    0, HEARTBEAT_DELAY_SECONDS, TimeUnit.SECONDS));
                }
            }
        }
    }

    /**
     * {@link #updateConnectionStatus(AttributeRef, ConnectionStatus)} method will update the Protocol Connection status (disconnected or connected)
     * and will manage to relaunch all the polling requests if it move from disconnected to connected status.
     *
     * @param protocolRef
     * @param status
     */
    private void updateConnectionStatus(AttributeRef protocolRef, ConnectionStatus status) {
        ConnectionStatus previousStatus = this.getStatus(this.getLinkedProtocolConfiguration(protocolRef));

        this.updateStatus(protocolRef, status);

        if (status.equals(ConnectionStatus.CONNECTED) && (previousStatus == null || previousStatus.equals(ConnectionStatus.DISCONNECTED))) {
            //Relaunch polling
            if (this.pollingSensorList.isEmpty()) {
                LOG.info("### no polling to restart for " + protocolRef.getAttributeName() + "...");
            }
            for (PollingKey key : this.pollingSensorList.keySet()) {
                if (key.getControllerAgentRef().equals(protocolRef)) {
                    this.schedulePollingTask(key);
                }
            }
        }

        if (status.equals(ConnectionStatus.DISCONNECTED)) {
            for (ScheduledFuture task : this.pollingSensorList.values()) {
                task.cancel(true);
            }
        }
    }

    /**
     * Heartbeat is used when connection with Controller 2.x is lost and is running until connection is back
     *
     * @param protocolRef
     * @param responseConsumer
     */
    private void executeHeartbeat(AttributeRef protocolRef, Consumer<Response> responseConsumer) {
        withLock(getProtocolName() + "::executeHeartbeat", () -> {
            LOG.info("### Heartbeat check on " + protocolRef.getAttributeName() + "...");

            HttpClientProtocol.HttpClientRequest checkRequest = RequestBuilder.buildCheckRequest(this.controllersTargetMap.get(protocolRef));

            Response response = null;

            try {
                response = checkRequest.invoke(null);
                responseConsumer.accept(response);
            } catch (ProcessingException e) {
                LOG.log(Level.SEVERE, "### Check for " + this.controllersMap.get(protocolRef).getControllerConfigName() + " doesn't succeed", e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        });
    }

    private void onHeartbeatResponse(AttributeRef protocolRef, Response response) {
        //if we don't have any connection issue, we stop the heartbeat check
        if (response != null && (response.getStatusInfo().equals(Response.Status.OK) || response.getStatusInfo().equals(Response.Status.FOUND))) {
            LOG.info("### Heartbeat check on " + protocolRef.getAttributeName() + " succeed !");
            this.updateConnectionStatus(protocolRef, ConnectionStatus.CONNECTED);
            //cancel has to be the last step
            LOG.info("### Stop Heartbeat task for " + protocolRef.getAttributeName());
            this.controllerHeartbeat.get(protocolRef).cancel(true);
        } else {
            if (response != null) {
                LOG.severe("### Heartbeat check response is " + response.getStatus());
            }
            LOG.severe("### Heartbeat check on " + protocolRef.getAttributeName() + " failed !");
        }
    }

    /**
     * Scheduling of a polling request
     * <p>
     * We check if there is sensor to poll for the given device name.
     *
     * @param key
     */
    private void schedulePollingTask(PollingKey key) {
        ScheduledFuture scheduledFuture = computePollingTask(key);

        if (scheduledFuture != null) {
            pollingSensorList.put(key, scheduledFuture);
        }
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }
}
