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
package org.openremote.agent.controller2;

import elemental.json.Json;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.connector.ConnectorUtil;
import org.openremote.manager.shared.device.InventoryCapabilities;
import org.openremote.manager.shared.attribute.Attributes;

import java.net.URI;
import java.util.Map;

public class Controller2Component extends UriEndpointComponent implements ConnectorComponent {
    public static final String TYPE = "urn:openremote:connector:controller2";
    public static final String DISPLAY_NAME = "OpenRemote Controller";
    public static final String URI_SYNTAX = "'controller2://<IP or host name>:<port>/([<device URI>/<resource URI>]|[discovery|inventory])[?username=username&password=secret]";
    public static final String HEADER_DEVICE_URI = Controller2Component.class.getCanonicalName() + ".HEADER_DEVICE_URI";
    public static final String HEADER_RESOURCE_URI = Controller2Component.class.getCanonicalName() + ".HEADER_RESOURCE_URI";
    public static final String HEADER_COMMAND_VALUE = Controller2Component.class.getCanonicalName() + ".HEADER_COMMAND_VALUE";
    protected final Controller2Adapter.Manager adapterManager;
    protected static final Attributes agentSettings;

    static {
        agentSettings = new Attributes();
        agentSettings.add(ConnectorUtil.buildConnectorSetting(
                "host",
                AttributeType.STRING,
                "Host/IP Address",
                "The OR Controller network hostname or IP address",
                true
        ));
        agentSettings.add(ConnectorUtil.buildConnectorSetting(
                "port",
                AttributeType.INTEGER,
                "Port",
                "The OR Controller network port number",
                true,
                "8868",
                null
        ));
        agentSettings.add(ConnectorUtil.buildConnectorSetting(
                "username",
                AttributeType.STRING,
                "Username",
                "The OR Controller Username",
                false
        ));
        agentSettings.add(ConnectorUtil.buildConnectorSetting(
                "password",
                AttributeType.STRING,
                "Password",
                "The OR Controller Password",
                false
        ));
    }

    public Controller2Component(Controller2Adapter.Manager adapterManager) {
        super(Controller2Endpoint.class);
        this.adapterManager = adapterManager;
    }

    public Controller2Component() {
        this(Controller2Adapter.DEFAULT_MANAGER);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        try {
            URI parsedUri = URI.create(uri);

            parameters.put("host", parsedUri.getHost());
            parameters.put("port", parsedUri.getPort());

            String path = parsedUri.getPath();

            return new Controller2Endpoint(uri, this, adapterManager, path);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Required URL in format of " + URI_SYNTAX, ex);
        }
    }


    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean supportsAgentDiscovery() {
        return true;
    }

    @Override
    public Attributes getAgentSettings() {
        return agentSettings;
    }

    @Override
    public Attributes getAgentDiscoverySettings() {
        return null;
    }

    @Override
    public InventoryCapabilities getCapabilities(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getAgentStatusUri(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getDeviceInventoryUri(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getDeviceDiscoveryUri(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getDevicesUri(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getDeviceMonitorUri(Attributes agentSettings) {
        return null;
    }

    @Override
    public String getAgentDiscoveryUri(Attributes discoverySettings) {
        return null;
    }

    @Override
    public boolean supportsDeviceMonitoring(Attributes agentSettings) {
        return false;
    }
}
