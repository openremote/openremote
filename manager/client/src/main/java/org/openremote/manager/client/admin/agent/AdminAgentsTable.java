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

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.SingleSelectionModel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.FormTable;
import org.openremote.manager.client.widget.IconCell;
import org.openremote.manager.shared.agent.Agent;

public class AdminAgentsTable extends FormTable<Agent> {

    public interface Style extends CssResource {
        String nameColumn();

        String descriptionColumn();

        String connectorColumn();

        String enabledColumn();
    }

    final protected Style style;

    final protected SingleSelectionModel<Agent> selectionModel = new SingleSelectionModel<>();

    final protected TextColumn<Agent> nameColumn = new TextColumn<Agent>() {
        @Override
        public String getValue(Agent agent) {
            return agent.getName();
        }
    };

    final protected TextColumn<Agent> descriptionColumn = new TextColumn<Agent>() {
        @Override
        public String getValue(Agent agent) {
            return agent.getDescription();
        }
    };

    final protected TextColumn<Agent> connectorColumn = new TextColumn<Agent>() {
        @Override
        public String getValue(Agent agent) {
            return agent.getConnectorType();
        }
    };

    final protected Column<Agent, String> enabledColumn = new Column<Agent, String>(new IconCell()) {
        @Override
        public String getValue(Agent agent) {
            return agent.isEnabled() ? "check-circle" : "circle-thin";
        }
    };

    public AdminAgentsTable(ManagerMessages managerMessages,
                            Style style,
                            FormTableStyle formTableStyle) {
        super(Integer.MAX_VALUE, formTableStyle);

        this.style = style;

        setSelectionModel(selectionModel);

        applyStyleCellText(nameColumn);
        addColumn(nameColumn, createHeader(managerMessages.agentName()));
        addColumnStyleName(0, style.nameColumn());

        applyStyleCellText(descriptionColumn);
        addColumn(descriptionColumn, createHeader(managerMessages.description()));
        addColumnStyleName(1, style.descriptionColumn());

        applyStyleCellText(connectorColumn);
        addColumn(connectorColumn, createHeader(managerMessages.connectorType()));
        addColumnStyleName(2, style.connectorColumn());

        addColumn(enabledColumn, createHeader(managerMessages.enabled()));
        addColumnStyleName(3, style.enabledColumn());
        enabledColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    @Override
    public SingleSelectionModel<Agent> getSelectionModel() {
        return selectionModel;
    }

    public Agent getSelectedObject() {
        return getSelectionModel().getSelectedObject();
    }

}
