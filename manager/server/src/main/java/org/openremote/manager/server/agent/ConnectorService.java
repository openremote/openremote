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
package org.openremote.manager.server.agent;

import elemental.json.Json;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ComponentConfiguration;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.ParameterConfiguration;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.CamelContextHelper;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.ngsi.Attribute;
import org.openremote.manager.shared.ngsi.AttributeType;
import org.openremote.manager.shared.ngsi.Metadata;
import org.openremote.manager.shared.ngsi.MetadataElement;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ConnectorService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ConnectorService.class.getName());

    final protected Map<String, Connector> connectors = new LinkedHashMap<>();

    @Override
    public void init(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        findConnectors(messageBrokerService.getContext());
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new ConnectorResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Map<String, Connector> getConnectors() {
        return connectors;
    }

    public Connector getConnectorByType(String connectorType) {
        for (Connector connector : connectors.values()) {
            if (connector.getType().equals(connectorType))
                return connector;
        }
        return null;
    }

    public String getConnectorComponent(Connector connector) {
        for (Map.Entry<String, Connector> entry : connectors.entrySet()) {
            if (entry.getValue().getType().equals(connector.getType())) {
                return entry.getKey();
            }
        }
        return null;
    }

    protected void findConnectors(CamelContext context) throws Exception {
        for (Map.Entry<String, Properties> entry : CamelContextHelper.findComponents(context).entrySet()) {
            String connectorComponentName = entry.getKey();

            Properties properties = entry.getValue();
            Object connectorType = properties.get(Connector.PROPERTY_TYPE);

            if (connectorType != null) {
                Component component = context.getComponent(connectorComponentName);
                if (component == null) {
                    throw new IllegalStateException(
                        "Configured component for connector '" + connectorType + "' not available in context: " + connectorComponentName
                    );
                }
                if (component instanceof UriEndpointComponent) {
                    UriEndpointComponent uriEndpointComponent = (UriEndpointComponent) component;

                    Connector connector = createConnector(
                        connectorComponentName,
                        properties,
                        uriEndpointComponent
                    );

                    for (Connector existingConnector : connectors.values()) {
                        if (existingConnector.getType().equals(connector.getType())) {
                            throw new IllegalStateException(
                                "Duplicate connector types are not allowed: " + connector.getType()
                            );
                        }
                    }

                    LOG.info(
                        "Configured connector component '" + connectorComponentName + "' of type: " + connectorType
                    );

                    connectors.put(connectorComponentName, connector);

                } else {
                    LOG.warning(
                        "Component should implement " +
                            UriEndpointComponent.class.getName() + ": " + entry.getKey()
                    );
                }
            }
        }
    }

    protected Connector createConnector(String connectorComponentName,
                                        Properties componentProperties,
                                        UriEndpointComponent component) {

        LOG.fine("Creating connector: " + connectorComponentName);
        Connector connector = new Connector(
            componentProperties.get(Connector.PROPERTY_TYPE).toString()
        );

        UriEndpoint uriEndpoint = component.getEndpointClass().getAnnotation(UriEndpoint.class);
        if (uriEndpoint == null) {
            throw new RuntimeException(
                "The @UriEndpoint annotation is required on the component endpoint class: " + component.getEndpointClass()
            );
        }
        if (uriEndpoint.title() == null) {
            throw new RuntimeException(
                "The @UriEndpoint annotation requires the title attribute on the component endpoint class: " + component.getEndpointClass()
            );
        }
        connector.setName(uriEndpoint.title());

        if (uriEndpoint.syntax() != null) {
            connector.setSyntax(uriEndpoint.syntax());
        }

        Object supportsDiscovery = componentProperties.get(Connector.PROPERTY_SUPPORTS_DISCOVERY);
        if (supportsDiscovery != null) {
            connector.setSupportsDiscovery(Boolean.valueOf(supportsDiscovery.toString()));
        }

        Object supportsInventory = componentProperties.get(Connector.PROPERTY_SUPPORTS_INVENTORY);
        if (supportsInventory != null) {
            connector.setSupportsInventory(Boolean.valueOf(supportsInventory.toString()));
        }

        addConnectorAttributes(
            connector,
            connectorComponentName,
            component.createComponentConfiguration(),
            component.getEndpointClass()
        );

        return connector;
    }

    protected void addConnectorAttributes(Connector connector,
                                          String connectorComponentName,
                                          ComponentConfiguration componentConfiguration,
                                          Class<? extends Endpoint> endpointClass) {

        for (Map.Entry<String, ParameterConfiguration> configEntry : componentConfiguration.getParameterConfigurationMap().entrySet()) {
            try {
                Field field = endpointClass.getDeclaredField(configEntry.getKey());
                if (field.isAnnotationPresent(UriParam.class)) {
                    UriParam uriParam = field.getAnnotation(UriParam.class);

                    String attributeName = uriParam.name().length() != 0 ? uriParam.name() : field.getName();

                    Attribute attribute = new Attribute(attributeName, Json.createObject());
                    attribute.setMetadata(new Metadata(Json.createObject()));

                    if (String.class.isAssignableFrom(field.getType())) {
                        attribute.setType(AttributeType.STRING);
                    } else if (Integer.class.isAssignableFrom(field.getType())) {
                        attribute.setType(AttributeType.INTEGER);
                    } else if (Double.class.isAssignableFrom(field.getType())) {
                        attribute.setType(AttributeType.FLOAT);
                    } else if (Boolean.class.isAssignableFrom(field.getType())) {
                        attribute.setType(AttributeType.BOOLEAN);
                    } else {
                        throw new RuntimeException(
                            "Unsupported endpoint attribute type of connector'" + connectorComponentName + "': " + field.getType()
                        );
                    }

                    if (uriParam.label().length() > 0) {
                        attribute.getMetadata().addElement(
                            new MetadataElement("label", Json.createObject())
                                .setType(AttributeType.STRING.getName())
                                .setValue(Json.create(uriParam.label()))
                        );
                    }

                    if (uriParam.description().length() > 0) {
                        attribute.getMetadata().addElement(
                            new MetadataElement("description", Json.createObject())
                                .setType(AttributeType.STRING.getName())
                                .setValue(Json.create(uriParam.description()))
                        );
                    }

                    if (uriParam.defaultValue().length() > 0) {
                        attribute.getMetadata().addElement(
                            new MetadataElement("defaultValue", Json.createObject())
                                .setType(AttributeType.STRING.getName())
                                .setValue(Json.create(uriParam.defaultValue()))
                        );
                    }

                    if (uriParam.defaultValueNote().length() > 0) {
                        attribute.getMetadata().addElement(
                            new MetadataElement("defaultValueNote", Json.createObject())
                                .setType(AttributeType.STRING.getName())
                                .setValue(Json.create(uriParam.defaultValueNote()))
                        );
                    }

                    if (field.isAnnotationPresent(NotNull.class)) {
                        attribute.getMetadata().addElement(
                            new MetadataElement("required", Json.createObject())
                                .setType(AttributeType.BOOLEAN.getName())
                                .setValue(Json.create(true))
                        );
                    }

                    LOG.fine("Adding connector attribute '" + attributeName + "': " + attribute);
                    connector.addAttribute(attribute);
                }
            } catch (NoSuchFieldException ex) {
                // Ignoring config parameter if there is no annotated field on endpoint class
                // TODO: Inheritance of endpoint classes? Do we care?
            }
        }
    }

}
