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
package org.openremote.manager.client.assets.agent;

import com.google.gwt.text.shared.AbstractRenderer;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.connector.ConnectorResource;

import java.util.Arrays;
import java.util.List;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;
import static org.openremote.manager.shared.connector.Connector.ASSET_ATTRIBUTE_CONNECTOR;

public class AgentAttributesEditor extends AttributesEditor<AttributesEditor.Style> {

    final protected Agent agent;

    final protected ConnectorResource connectorResource;
    final protected ConnectorArrayMapper connectorArrayMapper;

    final protected FormGroup connectorDropDownGroup = new FormGroup();
    final protected FormDropDown<Connector> connectorDropDown;

    // This is a dummy we use when the Agent's assigned connector is not installed
    protected final String notFoundConnectorType = "NOT_FOUND_CONNECTOR";
    protected final Connector notFoundConnector = new Connector(notFoundConnectorType);

    protected List<Connector> availableConnectors;

    public AgentAttributesEditor(Environment environment, Container<AttributesEditor.Style> container, Attributes attributes,
                                 ConnectorResource connectorResource, ConnectorArrayMapper connectorArrayMapper) {
        super(environment, container, attributes);
        this.agent = new Agent(attributes);
        this.connectorResource = connectorResource;
        this.connectorArrayMapper = connectorArrayMapper;

        connectorDropDown = createConnectorDropDown();
        FormLabel connectorDropDownLabel = new FormLabel();
        connectorDropDownLabel.setText(environment.getMessages().connector());
        FormField connectorDropDownField = new FormField();
        connectorDropDownField.add(connectorDropDown);
        connectorDropDownGroup.addFormLabel(connectorDropDownLabel);
        connectorDropDownGroup.addFormField(connectorDropDownField);

        container.getFormView().setFormBusy(true);
        loadConnectors(connectors -> {
            availableConnectors = Arrays.asList(connectors);

            connectorDropDown.setValue(null);
            connectorDropDown.setAcceptableValues(availableConnectors);

            Connector connector = getConnector();
            if (connector != null) {
                connectorDropDown.setValue(connector);
                agent.writeConnectorSettings(connector);
            } else if (agent.getConnectorType() != null) {
                notFoundConnector.setName(
                    agent.getConnectorType() +
                        " (" + environment.getMessages().connectorNotInstalled() + ")"
                );
                connectorDropDown.setValue(notFoundConnector);
            }

            container.getFormView().setFormBusy(false);
            refresh();
        });
    }

    @Override
    protected FormGroup createFormGroup(Attribute attribute) {
        if (attribute.getName().equals(ASSET_ATTRIBUTE_CONNECTOR))
            return null;
        return super.createFormGroup(attribute);
    }

    @Override
    public void buildAttributeFormGroups() {
        if (availableConnectors != null) {
            super.buildAttributeFormGroups();
        }
    }

    @Override
    public void render() {
        container.getPanel().add(connectorDropDownGroup);
        super.render();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        connectorDropDownGroup.setOpaque(opaque);
    }

    protected FormDropDown<Connector> createConnectorDropDown() {
        FormDropDown<Connector> connectorDropDown = new FormDropDown<>(
            new AbstractRenderer<Connector>() {
                @Override
                public String render(Connector connector) {
                    if (connector == null)
                        return environment.getMessages().noConnectorAssigned();
                    return connector.getName();
                }
            }
        );
        connectorDropDown.addValueChangeHandler(event -> {
            Connector connector = event.getValue();
            onConnectorSelected(connector);
        });
        return connectorDropDown;
    }

    protected void onConnectorSelected(Connector connector) {
        if (connector != null) {
            agent.setConnectorType(connector.getType());
            agent.writeConnectorSettings(getConnector());
        } else {
            Connector oldConnector = getConnector();
            agent.removeConnectorSettings(oldConnector);
            agent.removeConnectorType();
        }
        refresh();
    }

    protected void loadConnectors(Consumer<Connector[]> onSuccess) {
        environment.getRequestService().execute(
            connectorArrayMapper,
            connectorResource::getConnectors,
            200,
            onSuccess::accept,
            ex -> handleRequestException(ex, environment)
        );
    }

    protected Connector getConnector() {
        Connector assignedConnector = null;
        for (Connector connector : availableConnectors) {
            if (connector.getType().equals(agent.getConnectorType())) {
                assignedConnector = connector;
                break;
            }
        }
        return assignedConnector;
    }
}
