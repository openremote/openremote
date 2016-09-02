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

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.connector.ConnectorUtil;
import org.openremote.manager.shared.connector.ChildAssetSupport;
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

    protected static final Attributes settings;

    static {
        settings = new Attributes();
        settings.add(ConnectorUtil.buildConnectorSetting(
                "host",
                AttributeType.STRING,
                "Host/IP Address",
                "The OR Controller network hostname or IP address",
                true
        ));
        settings.add(ConnectorUtil.buildConnectorSetting(
                "port",
                AttributeType.INTEGER,
                "Port",
                "The OR Controller network port number",
                true,
                "8868",
                null
        ));
        settings.add(ConnectorUtil.buildConnectorSetting(
                "username",
                AttributeType.STRING,
                "Username",
                "The OR Controller Username",
                false
        ));
        settings.add(ConnectorUtil.buildConnectorSetting(
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
    public ChildAssetSupport getChildSupport(Asset parentAsset) {
        return null;
    }

    @Override
    public boolean supportsChildDiscovery(Asset parentAsset) {
        return false;
    }

    @Override
    public Attributes getChildDiscoverySettings(Asset parentAsset) {
        return null;
    }

    @Override
    public Attributes getConnectorSettings() {
        return settings;
    }

    @Override
    public String getChildDiscoveryUri(Asset parentAsset, Attributes discoverySettings) {
        return null;
    }

    @Override
    public boolean supportsMonitoring(Asset asset) {
        return false;
    }

    @Override
    public String getChildInventoryUri(Asset asset) {
        return null;
    }

    @Override
    public String getAssetUri(Asset asset) {
        return null;
    }

    @Override
    public String getAssetMonitorUri(Asset asset) {
        return null;
    }

    @Override
    public Asset createAsset(Asset parent, Attributes assetSettings) {
        return null;
    }
}
