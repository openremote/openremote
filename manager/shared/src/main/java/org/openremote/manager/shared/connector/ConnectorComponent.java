/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.connector;

import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.device.InventoryCapabilities;
import org.openremote.manager.shared.agent.AgentStatus;
import org.openremote.manager.shared.device.Device;

public interface ConnectorComponent extends Connector {
    public static final String HEADER_DISCOVERY_START = ConnectorComponent.class.getCanonicalName() + ".HEADER_DISCOVERY_START";
    public static final String HEADER_DISCOVERY_STOP = ConnectorComponent.class.getCanonicalName() + ".HEADER_DISCOVERY_STOP";
    public static final String HEADER_INVENTORY_ACTION = ConnectorComponent.class.getCanonicalName() + ".HEADER_INVENTORY_ACTION";
    public static final String HEADER_DEVICE_ACTION = ConnectorComponent.class.getCanonicalName() + ".HEADER_DEVICE_ACTION";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_READ = "READ";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_SUBSCRIBE = "SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String ACTION_WRITE = "WRITE";

    /**
     * Optional endpoint URI for performing agent discovery. If implemented
     * when agent discovery is required then a route will be created requiring
     * a consumer from this endpoint which should generate messages for any
     * agents that the connector discovers using the discovery settings provided.
     *
     * {@link Attributes} produced should be valid agent settings. A message should
     * contain either a single {@link Attributes} object (one message per discovered
     * agent) or an array of {@link Attributes} objects (multiple agents per message).
     * Discovery should run as long as this route is running.
     */
    String getAgentDiscoveryUri(Attributes discoverySettings);

    /**
     * Get device inventory capabilities of the supplied agent.
     */
    InventoryCapabilities getCapabilities(Attributes agentSettings);

    /**
     * Indicates whether or not this connector component supports monitoring of devices.
     * If device monitoring is supported then {@link #getDeviceMonitorUri(Attributes)} must
     * return the Endpoint URI for the device monitor.
     */
    boolean supportsDeviceMonitoring(Attributes agentSettings);

    /**
     * Optional endpoint URI for consuming changes in the connector's status;
     * messages should be output with the body set to an appropriate {@link AgentStatus}
     * value.
     *
     * If not supported then just return null.
     */
    String getAgentStatusUri(Attributes agentSettings);

    /**
     * Mandatory endpoint URI for performing device CRUD. The capabilities supported by a particular
     * agent/connector are determined by the response from {@link #getCapabilities(Attributes)}.
     *
     * Support for reading devices from the inventory is mandatory, all other capabilities are
     * optional and are agent/connector dependent.
     *
     * The endpoint should support InOut (Request Reply) producers. The action to perform is set
     * by the {@link #HEADER_INVENTORY_ACTION} message header.
     *
     * {@Link #ACTION_CREATE}, {@Link #ACTION_UPDATE}, {@Link #ACTION_DELETE} operations
     * should consume a {@link Device}[]. In the case of {@link #ACTION_DELETE} then only the
     * device URI(s) should be required. The reply message body should contain an integer indicating
     * the status of the request (conforming to the HTTP Status code specification).
     *
     * {@link #ACTION_READ} operations should consume a {@link Device}[] where only the device
     * URI(s) should be required, if no devices are supplied then all devices should be returned.
     * The reply message body should contain a {@link Device}[] of the requested devices.
     *
     */
    String getDeviceInventoryUri(Attributes agentSettings);

    /**
     * Optional endpoint URI for performing device discovery. If implemented
     * when device discovery is required then a route will be created requiring
     * a consumer from this endpoint which should generate messages for any
     * devices that the connector discovers from the specified agent.
     *
     * {@link Attributes} produced should be valid devices (URI and Type). A message should
     * contain either a single {@link Attributes} object (one message per discovered device)
     * or an array of {@link Attributes} objects (multiple devices per message).
     *
     * Discovery should run as long as this route is running.
     *
     * If not supported then just return null.
     */
    String getDeviceDiscoveryUri(Attributes agentSettings);

    /**
     * Mandatory endpoint URI for: -
     *      Reading from device resources
     *      Writing to device resources
     *      Subscribing/Un-subscribing to device changes
     *
     * A single endpoint is used for all devices that belong to the specified
     * agent. This endpoint is expected to support the InOut MEP with the
     * {@link #HEADER_DEVICE_ACTION} message header indicating the action to
     * perform/data to return on messages to be consumed by this endpoint.
     *
     * {@link #ACTION_SUBSCRIBE}, {@link #ACTION_UNSUBSCRIBE} operations should consume a
     * {@link Device}[] where only the device URI(s) should be required. The reply message body
     * should contain an integer indicating the status of the request (conforming to the HTTP
     * Status code specification). Agents/Connectors that support device subscriptions must return
     * a valid endpoint URI from {@link #getDeviceMonitorUri(Attributes)}.
     */
    String getDevicesUri(Attributes agentSettings);

    /**
     * Optional endpoint URI for consuming device changes from the specified agent;
     * subscribing to device changes is done separately using the devices Endpoint URI.
     *
     * A subscribed device should be monitored by the connector for any resource value
     * changes; these changes should then be output in a message body as {@link Device}[]
     * containing the modified device(s) and resource(s).
     *
     * If not supported (i.e. devices require polling) then just return null.
     */
    String getDeviceMonitorUri(Attributes agentSettings);
}
