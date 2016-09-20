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
import org.apache.camel.impl.DefaultComponent;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.connector.ConnectorUtil;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class Controller2Component extends DefaultComponent implements ConnectorComponent {

    public static final String TYPE = "urn:openremote:connector:controller2";
    public static final String DISPLAY_NAME = "OpenRemote Controller";
    public static final String URI_SYNTAX = "'controller2://<IP or host name>:<port>/[discovery|inventory|read|write|listen/<deviceKey>][?username=username&password=secret]";
    protected final Controller2AdapterManager adapterManager;

    public static final Attributes SETTINGS;

    static {
        SETTINGS = new Attributes();
        SETTINGS.put(ConnectorUtil.buildConnectorSetting(
            "host",
            AttributeType.STRING,
            "Host/IP Address",
            "The OR Controller network hostname or IP address",
            true
        ));
        SETTINGS.put(ConnectorUtil.buildConnectorSetting(
            "port",
            AttributeType.INTEGER,
            "Port",
            "The OR Controller network port number",
            true,
            "8868",
            null
        ));
        SETTINGS.put(ConnectorUtil.buildConnectorSetting(
            "username",
            AttributeType.STRING,
            "Username",
            "The OR Controller Username",
            false
        ));
        SETTINGS.put(ConnectorUtil.buildConnectorSetting(
            "password",
            AttributeType.STRING,
            "Password",
            "The OR Controller Password",
            false
        ));
    }

    public Controller2Component(Controller2AdapterManager adapterManager) {
        this.adapterManager = adapterManager;
    }

    public Controller2Component() {
        this(Controller2AdapterManager.DEFAULT_MANAGER);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        try {
            URI parsedUri = URI.create(uri);

            // TODO java.net.URI violates latest RFCs http://bugs.java.com/view_bug.do?bug_id=6587184
            if (parsedUri.getHost() == null)
                throw new IllegalArgumentException("Parsed host was empty, note that host names can not contain underscores");

            if (parsedUri.getPort() == -1)
                throw new IllegalArgumentException("Parsed port was empty");

            parameters.put("host", parsedUri.getHost());
            parameters.put("port", parsedUri.getPort());

            Path path = Paths.get(parsedUri.getPath());

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
    public Attributes getConnectorSettings() {
        return SETTINGS;
    }

    @Override
    public Collection<Capability> getConsumerCapabilities() {
        return Arrays.asList(
            Capability.inventory,
            Capability.listen
        );
    }

    @Override
    public Collection<Capability> getProducerCapabilities() {
        return Arrays.asList(
            Capability.discovery,
            Capability.inventory,
            Capability.read,
            Capability.write
        );
    }

    @Override
    public String buildConsumerEndpoint(Capability capability, String agentAssetId, Agent agent, String deviceKey) {
        String host = getAgentAttributeHost(agent.getAttributes());
        Integer port = getAgentAttributePort(agent.getAttributes());
        if (host == null || port == null) {
            throw new IllegalArgumentException("Host and port must be available in agent: " + agentAssetId);
        }
        switch (capability) {
            case inventory:
                return "controller2://" + host + ":" + port + "/inventory";
            case listen:
                return "controller2://" + host + ":" + port + "/listen/" + deviceKey;
        }
        throw new UnsupportedOperationException("Can't build endpoint for capability: " + capability);
    }

    @Override
    public String buildProducerEndpoint(Capability capability, String agentAssetId, Agent agent) {
        String host = getAgentAttributeHost(agent.getAttributes());
        Integer port = getAgentAttributePort(agent.getAttributes());
        if (host == null || port == null) {
            throw new IllegalArgumentException("Host and port must be available in agent: " + agentAssetId);
        }
        switch (capability) {
            case discovery:
                return "controller2://" + host + ":" + port + "/discovery";
            case inventory:
                return "controller2://" + host + ":" + port + "/inventory";
            case read:
                return "controller2://" + host + ":" + port + "/read";
            case write:
                return "controller2://" + host + ":" + port + "/write";
        }
        throw new UnsupportedOperationException("Can't build endpoint for capability: " + capability);
    }

    protected String getAgentAttributeHost(Attributes agentAttributes) {
        return agentAttributes.hasAttribute("host") ? agentAttributes.get("host").getValueAsString() : null;
    }

    protected Integer getAgentAttributePort(Attributes agentAttributes) {
        return agentAttributes.hasAttribute("port") ? agentAttributes.get("port").getValueAsDouble().intValue() : null;
    }
}
