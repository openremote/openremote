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
import org.openremote.manager.client.event.ServerSendEvent;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.agent.RefreshInventoryEvent;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;
import org.openremote.model.Attribute;
import org.openremote.model.Attributes;
import org.openremote.model.asset.Asset;

import java.util.List;

import static org.openremote.manager.shared.connector.Connector.ASSET_ATTRIBUTE_CONNECTOR;

public class AgentAttributesEditor extends AttributesEditor<AttributesEditor.Style> {

    final protected Agent agent;
    final protected Asset agentAsset;

    final protected FormGroup connectorListBoxGroup = new FormGroup();
    final protected FormValueListBox<Connector> connectorListBox;

    final protected FormGroup actionsGroup = new FormGroup();
    final protected FormButton refreshInventoryButton = new FormButton();

    // This is a dummy we use when the Agent's assigned connector is not installed
    protected final String notFoundConnectorType = "NOT_FOUND_CONNECTOR";
    protected final Connector notFoundConnector = new Connector(notFoundConnectorType);

    protected List<Connector> availableConnectors;

    public AgentAttributesEditor(Environment environment,
                                 Container<AttributesEditor.Style> container,
                                 Attributes attributes,
                                 boolean isCreateAsset,
                                 Asset agentAsset) {
        super(environment, container, attributes);
        this.agentAsset = agentAsset;
        this.agent = new Agent(attributes, isCreateAsset);

        connectorListBox = createConnectorListBox();
        FormLabel connectorListBoxLabel = new FormLabel();
        connectorListBoxLabel.setText(environment.getMessages().connector());
        FormField connectorListBoxField = new FormField();
        connectorListBoxField.add(connectorListBox);
        connectorListBoxGroup.addFormLabel(connectorListBoxLabel);
        connectorListBoxGroup.addFormField(connectorListBoxField);

        FormLabel actionsLabel = new FormLabel();
        actionsLabel.setText(environment.getMessages().actions());
        FormField actionsField = new FormField();
        actionsGroup.addFormLabel(actionsLabel);
        actionsGroup.addFormField(actionsField);

        refreshInventoryButton.setText(environment.getMessages().refreshInventory());
        refreshInventoryButton.setDanger(true);
        refreshInventoryButton.addClickHandler(event -> {
            doRefreshInventory();
        });
        refreshInventoryButton.setEnabled(false);
        actionsField.add(refreshInventoryButton);

        container.getFormView().setFormBusy(true);
/*
        loadConnectors(connectors -> {
            availableConnectors = Arrays.asList(connectors);

            connectorListBox.setValue(null);
            connectorListBox.setAcceptableValues(availableConnectors);

            Connector connector = getConnector();
            if (connector != null) {
                connectorListBox.setValue(connector);
                refreshInventoryButton.setEnabled(
                    connector.isSupportsInventoryRefresh() && agentAsset.getId() != null
                );
                agent.writeConnectorSettings(connector);
            } else if (agent.getConnectorType() != null) {
                notFoundConnector.setName(
                    agent.getConnectorType() +
                        " (" + environment.getMessages().connectorNotInstalled() + ")"
                );
                connectorListBox.setValue(notFoundConnector);
            }

            container.getFormView().setFormBusy(false);
            //clearBuild();
        });
*/
    }

    @Override
    protected FormGroup createAttributeGroup(Attribute attribute) {
        if (attribute.getName().equals(ASSET_ATTRIBUTE_CONNECTOR))
            return null;
        return super.createAttributeGroup(attribute);
    }

    @Override
    protected FormLabel createAttributeLabel(Attribute attribute) {
        FormLabel formLabel = super.createAttributeLabel(attribute);
        if (attribute.getName().equals("enabled")) {
            formLabel.setText(environment.getMessages().enabled());
        }
        return formLabel;
    }

    /* TODO
    @Override
    protected String getDescription(Attribute attribute) {
        String description = super.getDescription(attribute);
        if (attribute.getName().equals("enabled")) {
            description = environment.getMessages().enableAgentDescription();
        }
        return description;
    }
    */

    @Override
    public void build() {
        container.getPanel().add(connectorListBoxGroup);
        container.getPanel().add(actionsGroup);
        super.build();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        connectorListBoxGroup.setOpaque(opaque);
        actionsGroup.setOpaque(opaque);
    }

    protected FormValueListBox<Connector> createConnectorListBox() {
        FormValueListBox<Connector> listBox = new FormValueListBox<>(
            new AbstractRenderer<Connector>() {
                @Override
                public String render(Connector connector) {
                    if (connector == null)
                        return environment.getMessages().noConnectorAssigned();
                    return connector.getName();
                }
            }
        );
        listBox.addValueChangeHandler(event -> {
            Connector connector = event.getValue();
            onConnectorSelected(connector);
        });
        return listBox;
    }

    protected void onConnectorSelected(Connector connector) {
        if (connector != null) {
            agent.setConnectorType(connector.getType());
            Connector c = getConnector();
            agent.writeConnectorSettings(c);
            refreshInventoryButton.setEnabled(
                c.isSupportsInventoryRefresh() && agentAsset.getId() != null
            );
        } else {
            Connector oldConnector = getConnector();
            agent.removeConnectorSettings(oldConnector);
            agent.removeConnectorType();
            refreshInventoryButton.setEnabled(false);
        }
        // clearBuild();
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

    protected void doRefreshInventory() {
        if (agentAsset.getId() == null)
            return;
        container.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationInventoryRefresh(agentAsset.getName()),
            () -> {
                environment.getEventBus().dispatch(new ServerSendEvent(
                    new RefreshInventoryEvent(agentAsset.getId())
                ));
                environment.getEventBus().dispatch(
                    new ShowInfoEvent(environment.getMessages().inventoryRefreshed(agentAsset.getName()))
                );
            }
        );
    }
}
