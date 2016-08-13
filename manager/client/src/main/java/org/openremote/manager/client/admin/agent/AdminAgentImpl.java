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
package org.openremote.manager.client.admin.agent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueListBox;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.FormGroup;
import org.openremote.manager.client.widget.FormView;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.connector.Connector;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminAgentImpl extends FormView implements AdminAgent {

    interface UI extends UiBinder<HTMLPanel, AdminAgentImpl> {
    }

    @UiField
    FormGroup nameGroup;
    @UiField
    TextBox nameInput;

    @UiField
    FormGroup descriptionGroup;
    @UiField
    TextBox descriptionInput;

    @UiField
    FormGroup enabledGroup;
    @UiField
    SimpleCheckBox enabledCheckBox;

    @UiField(provided = true)
    ValueListBox<Connector> connectorListBox;

    @UiField
    PushButton createButton;

    @UiField
    PushButton updateButton;

    @UiField
    PushButton deleteButton;

    @UiField
    PushButton cancelButton;

    protected Presenter presenter;

    @Inject
    public AdminAgentImpl(ManagerMessages managerMessages) {

        connectorListBox = new ValueListBox<>(new AbstractRenderer<Connector>() {
            @Override
            public String render(Connector connector) {
                if (connector == null)
                    return managerMessages.noConnectorAssigned();
                return connector.getName();
            }
        });

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        connectorListBox.addValueChangeHandler(event -> {
            Connector connector= event.getValue();
            if (presenter != null) {
                presenter.onConnectorSelected(connector);
            }
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            nameInput.setValue(null);
            descriptionInput.setValue(null);
            enabledCheckBox.setValue(false);
            connectorListBox.setValue(null);
            connectorListBox.setAcceptableValues(new ArrayList<>());
        }
    }

    @Override
    public void setConnectors(Connector[] connectors) {
        connectorListBox.setAcceptableValues(Arrays.asList(connectors));
    }

    @Override
    public void setAssignedConnector(Connector connector) {
        connectorListBox.setValue(connector);

        if (connector != null) {
        }
    }

    @Override
    public void setName(String name) {
        nameInput.setValue(name);
    }

    @Override
    public String getName() {
        return nameInput.getValue().length() > 0 ? nameInput.getValue() : null;
    }

    @Override
    public void setNameError(boolean error) {
        nameGroup.setError(error);
    }

    @Override
    public void setDescription(String description) {
        descriptionInput.setValue(description);
    }

    @Override
    public String getDescription() {
        return descriptionInput.getValue().length() > 0 ? descriptionInput.getValue() : null;
    }

    @Override
    public void setDescriptionError(boolean error) {
        descriptionGroup.setError(error);
    }

    @Override
    public void setAgentEnabled(Boolean enabled) {
        enabledCheckBox.setValue(enabled != null ? enabled : false);
    }

    @Override
    public boolean getAgentEnabled() {
        return enabledCheckBox.getValue();
    }

    @Override
    public void setAgentEnabledError(boolean error) {
        enabledGroup.setError(error);
    }

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
    }

    @Override
    public void enableDelete(boolean enable) {
        deleteButton.setVisible(enable);
    }

    @UiHandler("updateButton")
    void updateClicked(ClickEvent e) {
        if (presenter != null)
            presenter.update();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.create();
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        if (presenter != null)
            presenter.delete();
    }

    @UiHandler("cancelButton")
    void cancelClicked(ClickEvent e) {
        if (presenter != null)
            presenter.cancel();
    }
}
