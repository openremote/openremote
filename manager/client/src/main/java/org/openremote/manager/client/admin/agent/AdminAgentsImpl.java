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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.style.ThemeStyle;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.agent.Agent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminAgentsImpl extends Composite implements AdminAgents {

    interface UI extends UiBinder<HTMLPanel, AdminAgentsImpl> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    ThemeStyle themeStyle;

    @UiField
    AdminAgentsTable.Style agentsTableStyle;

    @UiField
    PushButton createButton;

    @UiField
    SimplePanel tableContainer;

    final AdminAgentsTable table;
    Presenter presenter;

    @Inject
    public AdminAgentsImpl(FormTableStyle formTableStyle) {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        table = new AdminAgentsTable(managerMessages, agentsTableStyle, formTableStyle);
        table.getSelectionModel().addSelectionChangeHandler(event -> {
                Agent selected;
                if ((selected = table.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onAgentSelected(selected);
                }
            }
        );
        tableContainer.add(table);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            table.setRowData(new ArrayList<>());
            table.flush();
        }
    }

    @Override
    public void setAgents(Agent[] agents) {
        tableContainer.setVisible(agents.length > 0);
        table.setRowData(Arrays.asList(agents));
        table.flush();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        presenter.createAgent();
    }
}
