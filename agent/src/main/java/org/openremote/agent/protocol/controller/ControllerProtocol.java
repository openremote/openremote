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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.controller.command.ControllerCommandBasic;
import org.openremote.agent.protocol.controller.command.ControllerCommandMapped;
import org.openremote.agent.protocol.http.HTTPProtocol;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueDescriptor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.container.web.WebTargetBuilder.CONNECTION_POOL_SIZE;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a Controller 2.5 protocol to communicate with a running Controller 2.5 instance and getting sensor's status
 * and executing commands.
 * <p>
 * The protocol communicate with Controller by using Controller REST API.
 * <p>
 * The protocol manage two kinds of request :
 * <ul>
 * <li>A set of polling requests (one by controller/device name couple) {@link #pollingSensorList}. For each request we
 * just wait for a response 200 (new status) or 403 (timeout after 60 seconds) and relaunch the same request as soon as we have one of those two responses.</li>
 * <li>Executing commands provided by Write Attribute with necessary information. There is different kind of situations explained in
 * {@link org.openremote.agent.protocol.controller.ControllerCommand}</li>
 * </ul>
 * <p>
 * <p>
 * Two cases are considered:
 * <ul>
 * <li>A sensor : if we want to link an attribute to a controller 2.5 sensor to get status we should create an Attribute with proper type
 * considering the situation (temperature, time,...) and two necessary META {@link ControllerAgent#META_DEVICE_NAME} and
 * {@link ControllerAgent#META_SENSOR_NAME}</li>
 * <li>A command : if we want to execute a command on a controller 2.5, we create an attribute with the following available META (see details
 * in {@link org.openremote.agent.protocol.controller.ControllerCommand}) :
 * {@link ControllerAgent#META_DEVICE_NAME} or {@link ControllerAgent#META_COMMAND_DEVICE_NAME}, {@link ControllerAgent#META_COMMAND_NAME} or
 * {@link ControllerAgent#META_COMMANDS_MAP}</li>
 * </ul>
 * <p>
 */
// TODO: Fix or remove this protocol - it does polling per device and also futures are not cancelled and NPEs appear etc...nasty
public class ControllerProtocol extends AbstractProtocol<ControllerAgent, ControllerAgentLink> {

    public static final int HEARTBEAT_DELAY_SECONDS = 5;
    public static final String PROTOCOL_DISPLAY_NAME = "Controller Client";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ControllerProtocol.class);
    private final Map<String, Future<?>> pollingSensorList = new HashMap<>();
    protected ResteasyClient client;
    private Controller controller;
    private ResteasyWebTarget controllerWebTarget;
    private ScheduledFuture<?> controllerHeartbeat;
    private final Map<AttributeRef, Boolean> initStatusDone = new HashMap<>();

    public ControllerProtocol(ControllerAgent agent) {
        super(agent);
    }

    @Override
    public void doStart(Container container) throws Exception {

        String baseURL = agent.getControllerURI().orElseThrow(() -> new IllegalArgumentException("Missing or invalid controller URI: " + agent));
        URI uri;

        try {
            uri = new URIBuilder(baseURL).build();
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, "Invalid Controller URI", e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }

        client = createClient(executorService, CONNECTION_POOL_SIZE, 70000, null);

        WebTargetBuilder webTargetBuilder = new WebTargetBuilder(client, uri);

        agent.getUsernamePassword().ifPresent(usernamePassword -> {
            LOG.info("Setting BASIC auth credentials for controller");
            webTargetBuilder.setBasicAuthentication(usernamePassword.getUsername(), usernamePassword.getPassword());
        });

        controllerWebTarget = webTargetBuilder.build();
        controller = new Controller(agent.getId());

        controllerHeartbeat = this.executorService.scheduleWithFixedDelay(
            () -> this.executeHeartbeat(this::onHeartbeatResponse),
            0,
            HEARTBEAT_DELAY_SECONDS,
            TimeUnit.SECONDS);
    }

    @Override
    protected void setConnectionStatus(ConnectionStatus connectionStatus) {
        super.setConnectionStatus(connectionStatus);

        if (connectionStatus.equals(ConnectionStatus.DISCONNECTED)) {
            for (Future<?> task : this.pollingSensorList.values()) {
                task.cancel(true);
            }
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {

        if (controllerHeartbeat != null) {
            controllerHeartbeat.cancel(true);
        }

        pollingSensorList.values().forEach(pollingTask -> pollingTask.cancel(true));
        pollingSensorList.clear();
        initStatusDone.clear();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ControllerAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        String deviceName = agentLink.getDeviceName().orElse(null);
        String sensorName = agentLink.getSensorName().orElse(null);
        String commandDeviceName = agentLink.getCommandDeviceName().orElse(null);
        String commandName = agentLink.getCommandName().orElse(null);

        Map<String, List<String>> commandsMap = agentLink.getCommandsMap().orElse(null);

        /*
         * Build Sensor Status info for polling request
         */
        if (sensorName != null) {
            LOG.finer("### Adding new sensor [" + deviceName + "," + sensorName + "] linked to " + agent.getId() + " (" + agent.getName() + ")");
            controller.addSensor(attributeRef, new ControllerSensor(deviceName, sensorName));

            // Properly stop previously existing polling on device name --> use of false parameter
            if (pollingSensorList.containsKey(deviceName)) {
                pollingSensorList.get(deviceName).cancel(true);
            }

            this.initStatusDone.put(attributeRef, false);

            // Get initial status of sensor
            collectInitialStatus(attributeRef, deviceName, sensorName);

            //Put new polling on a new device name or update previous
            this.schedulePollingTask(deviceName);
        }

        /*
         * If linked Attribute contains command info, we build {@link org.openremote.agent.protocol.controller.ControllerCommand } depending on
         * attribute information.
         */
        if (commandName != null || commandsMap != null) {
            //If no command specific device name is set, then we're using deviceName
            if (commandDeviceName == null && deviceName != null) {
                commandDeviceName = deviceName;
            }

            if (commandName != null) {
                controller.addCommand(attributeRef, new ControllerCommandBasic(commandDeviceName, commandName));
            } else {
                assert commandsMap.size() > 0;
                controller.addCommand(attributeRef, new ControllerCommandMapped(commandDeviceName, computeCommandsMapFromMultiValue(commandsMap)));
            }
        }
    }

    /**
     * Clearing elements if an attribute is unlinked from Controller Agent We don't have to clear {@link
     * #pollingSensorList} as a check is done before scheduling task {@link #schedulePollingTask}
     */
    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ControllerAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        controller.removeAttributeRef(attributeRef);
    }

    /**
     * Write action on a linked attribute mean we execute a command on the Controller. It induce a HTTP request and
     * manage it's return code. (No value is returned from the execution of a command)
     */
    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, ControllerAgentLink agentLink, AttributeEvent event, Object processedValue) {
        LOG.finer("### Process Linked Attribute Write");

        AttributeRef attributeRef = event.getAttributeRef();
        ControllerCommand controllerCommand = controller.getCommand(attributeRef);
        HTTPProtocol.HttpClientRequest request = RequestBuilder.buildCommandRequest(controllerCommand, event, controllerWebTarget);

        String body = null;

        if (controllerCommand instanceof ControllerCommandBasic) {
            body = event.getValue().map(v -> {
                ObjectNode objectValue = ValueUtil.JSON.createObjectNode();
                objectValue.putPOJO("parameter", processedValue);
                return objectValue.toString();
            }).orElse(null);
        }
        executeAttributeWriteRequest(request, body, this::onAttributeWriteResponse);
    }

    /**
     * Convert commands map received as {@link MultivaluedMap} into a simple {@link Map}
     */
    private Map<String, String> computeCommandsMapFromMultiValue(Map<String, List<String>> multivaluedMap) {
        Map<String, String> commandsMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
            commandsMap.put(entry.getKey(), entry.getValue().get(0));
        }

        return commandsMap;
    }

    private void collectInitialStatus(AttributeRef attributeRef, String deviceName, String sensorName) {
        this.executorService
            .submit(() -> this.executeInitialStatus(attributeRef, deviceName, sensorName, response -> onInitialStatusResponse(attributeRef, deviceName, sensorName, response)));
    }

    private void executeInitialStatus(AttributeRef attributeRef, String deviceName, String sensorName, Consumer<Response> responseConsumer) {
        withLock(getProtocolName() + "::executeInitialStatus::" + attributeRef, () -> {
            LOG.info("### Initial status check for " + attributeRef.getName() + " [" + deviceName + "," + sensorName + "] ...");

            HTTPProtocol.HttpClientRequest checkRequest = RequestBuilder.buildStatusRequest(deviceName, Arrays.asList(sensorName), controllerWebTarget);

            Response response = null;

            try {
                response = checkRequest.invoke(null);
                responseConsumer.accept(response);
            } catch (ProcessingException e) {
                LOG.log(Level.SEVERE, "### Initial status for " + attributeRef.getName() + " [" + deviceName + "," + sensorName + "] doesn't succeed", e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }

        });
    }

    private void onInitialStatusResponse(AttributeRef attributeRef, String deviceName, String sensorName, Response response) {
        if (response != null) {
            if (response.getStatusInfo().equals(Response.Status.OK)) {
                LOG.finer("### New sensor [" + sensorName + "] status received");
                ArrayNode arrayValue = response.readEntity(ArrayNode.class);

                if (arrayValue.isEmpty()) {
                    LOG.warning("### Status response is empty");
                } else {
                    arrayValue.forEach(status -> {
                        String name = status.get("name").asText();
                        String value = status.get("value").asText();

                        this.updateAttributeValue(attributeRef, value);
                        this.initStatusDone.put(attributeRef, true);
                    });
                }
            } else {
                LOG.severe("### Status code for initial status received error : " + response.getStatus() + " --> " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            LOG.warning("### Initial status check return a null value for " + attributeRef.getName() + " [" + deviceName + "," + sensorName + "]");
        }

        if (!this.initStatusDone.get(attributeRef)) {
            collectInitialStatus(attributeRef, deviceName, sensorName);
        }
    }

    /**
     * Compute the polling request for a given deviceName and controller. Method check all registered sensor's (linked
     * to the Protocol) and collect all sensor's name to put them into polling request
     */
    private Future<?> computePollingTask(String deviceName) {
        return withLockReturning(getProtocolName() + "::computePollingTask::" + deviceName, () -> {
            List<String> sensorNameList = controller.collectSensorNameLinkedToDeviceName(deviceName);

            if (sensorNameList.isEmpty()) {
                return null;
            }

            return executorService.submit(() -> executePollingRequest(deviceName, sensorNameList,
                response -> onPollingResponse(deviceName, sensorNameList, response)));
        });
    }

    /**
     * Polling Request execution if a Connection issue (exception) occurs, we check the nature {@link
     * #checkIfConnectionRefused}
     */
    private void executePollingRequest(String deviceName, List<String> sensorList, Consumer<Response> responseConsumer) {
        LOG.info("### Polling Request for device [device=" + deviceName + ", sensors=" + this.formatSensors(sensorList) + "]");

        HTTPProtocol.HttpClientRequest httpClientRequest = RequestBuilder
            .buildStatusPollingRequest(deviceName, sensorList, controller.getDeviceId(), controllerWebTarget);

        Response response = null;

        try {
            response = httpClientRequest.invoke(null);
            setConnectionStatus(ConnectionStatus.CONNECTED);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                "### Exception thrown whilst doing polling request [device=" + deviceName + ", sensors=" + this.formatSensors(sensorList) + "]",
                e);

            this.checkIfConnectionRefused(e);
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
     */
    private void onPollingResponse(String deviceName, List<String> sensorNameList, Response response) {
        if (response != null) {
            if (response.getStatusInfo() == Response.Status.OK) {
                String responseBodyAsString = response.readEntity(String.class);

                LOG.info("### New sensors status received");
                LOG.finer("### Polling request body response : " + responseBodyAsString);

                ArrayNode statusArray = ValueUtil.convert(responseBodyAsString, ArrayNode.class);

                if (statusArray == null) {
                    LOG.warning("### Polling response is not a JSON array or empty: " + responseBodyAsString);
                } else {
                    statusArray.forEach(status -> {

                        String name = Optional.ofNullable(status.get("name")).flatMap(ValueUtil::getString).orElse(null);
                        String value = Optional.ofNullable(status.get("value")).flatMap(ValueUtil::getString).orElse(null);

                        /**
                         * For every sensors in the request body, find the linked attributeref and update value by calling {@link #updateAttributeValue}
                         */
                        controller.getSensorsListForDevice(deviceName).stream()
                            .filter(entry -> entry.getValue().getSensorName().equals(name))
                            .forEach(e -> this.updateAttributeValue(e.getKey(), value));
                    });
                }
            } else if (response.getStatusInfo() == Response.Status.REQUEST_TIMEOUT) {
                LOG.info("### Timeout from polling no changes on Controller side given sensors [device=" + deviceName + ", sensors=" + this.formatSensors(sensorNameList) + "]");
            } else {
                LOG.severe("### Status code received error : " + response.getStatus() + " --> " + response.getStatusInfo().getReasonPhrase());
            }
        } else {
            LOG.severe("### Received null response from polling (due to previous exception)");
        }

        //No matter status code, we're continuing to poll
        this.schedulePollingTask(deviceName);
    }

    /**
     * Update linked attribute with new value. We should take care of attribute type and format
     */
    private void updateAttributeValue(AttributeRef attributeRef, String value) {
        LOG.finest("### Updating attribute " + attributeRef + " with value " + value);
        ValueDescriptor<?> valueType = this.linkedAttributes.get(attributeRef).getType();
        Object valueObj = ValueUtil.convert(value, valueType.getType());
        this.updateLinkedAttribute(new AttributeState(attributeRef, valueObj));
    }

    private void executeAttributeWriteRequest(HTTPProtocol.HttpClientRequest request, String body, Consumer<Response> responseConsumer) {
        Response response = null;

        try {
            response = request.invoke(body);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "### Exception thrown whilst doing attribute write request", e);
            this.checkIfConnectionRefused(e);
        }

        responseConsumer.accept(response);
    }

    private void onAttributeWriteResponse(Response response) {
        if (response != null) {
            LOG.finer("### Response from command (204 is a valid and success return) : " + response.getStatus());
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
     * Check the exception received from a request execution to see if it's not a connection issue. If it is the case,
     * we'll start a heartbeat task until we get a new signal. Heartbeat is done every {@link #HEARTBEAT_DELAY_SECONDS}
     * seconds.
     */
    private void checkIfConnectionRefused(Exception e) {
        if (e.getCause() instanceof HttpHostConnectException) {
            HttpHostConnectException e2 = (HttpHostConnectException) e.getCause();

            if (e2.getCause() instanceof ConnectException || e2.getCause() instanceof UnknownHostException) {
                ConnectException e3 = (ConnectException) e2.getCause();
                LOG.log(Level.SEVERE, "Connection refused: " + e3.getMessage());
                setConnectionStatus(ConnectionStatus.DISCONNECTED);

                // Starting a heartbeat Task until connection is OK
                if (controllerHeartbeat == null || this.controllerHeartbeat.isCancelled()) {
                    controllerHeartbeat = this.executorService.scheduleWithFixedDelay(() ->
                        this.executeHeartbeat(this::onHeartbeatResponse), 0, HEARTBEAT_DELAY_SECONDS, TimeUnit.SECONDS);
                }
            }
        }
    }


    /**
     * Heartbeat is used when connection with Controller 2.x is lost and is running until connection is back
     */
    private void executeHeartbeat(Consumer<Response> responseConsumer) {
        withLock(getProtocolName() + "::executeHeartbeat", () -> {
            LOG.info("Doing heartbeat check for controller: " + controllerWebTarget.getUriBuilder().build());

            HTTPProtocol.HttpClientRequest checkRequest = RequestBuilder.buildCheckRequest(controllerWebTarget);

            Response response = null;

            try {
                response = checkRequest.invoke(null);
                responseConsumer.accept(response);
            } catch (ProcessingException e) {
                LOG.log(Level.SEVERE, "Heartbeat check for controller failed: " + controllerWebTarget.getUriBuilder().build(), e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        });
    }

    private void onHeartbeatResponse(Response response) {
        //if we don't have any connection issue, we stop the heartbeat check
        if (response != null && (response.getStatusInfo().equals(Response.Status.OK) || response.getStatusInfo().equals(Response.Status.FOUND))) {
            LOG.info("Heartbeat check for controller success: " + controllerWebTarget.getUriBuilder().build());
            setConnectionStatus(ConnectionStatus.CONNECTED);
            // cancel has to be the last step
            controllerHeartbeat.cancel(true);
            controllerHeartbeat = null;
        } else {
            String responseMsg = "NONE";
            if (response != null) {
                responseMsg = Integer.toString(response.getStatus());
            }
            LOG.severe("Heartbeat check for controller failed (Response = " + responseMsg + "): " + controllerWebTarget.getUriBuilder().build());
        }
    }

    /**
     * Scheduling of a polling request
     * <p>
     * We check if there is sensor to poll for the given device name.
     */
    private void schedulePollingTask(String deviceName) {
        Future<?> scheduledFuture = computePollingTask(deviceName);

        if (scheduledFuture != null) {
            pollingSensorList.put(deviceName, scheduledFuture);
        }
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "or-controller://" + (controllerWebTarget != null ? controllerWebTarget.getUriBuilder().build() : agent.getId());
    }
}
