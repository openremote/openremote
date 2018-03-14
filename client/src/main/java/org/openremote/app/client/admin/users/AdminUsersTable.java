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
package org.openremote.app.client.admin.users;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.SingleSelectionModel;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.FormTableStyle;
import org.openremote.app.client.widget.FormTable;
import org.openremote.app.client.widget.IconCell;
import org.openremote.model.security.User;

public class AdminUsersTable extends FormTable<User> {

    public interface Style extends CssResource {
        String usernameColumn();

        String firstNameColumn();

        String lastNameColumn();

        String enabledColumn();
    }

    final protected Style style;

    final protected SingleSelectionModel<User> selectionModel = new SingleSelectionModel<>();

    final protected TextColumn<User> usernameColumn = new TextColumn<User>() {
        @Override
        public String getValue(User user) {
            return user.getUsername();
        }
    };

    final protected TextColumn<User> firstNameColumn = new TextColumn<User>() {
        @Override
        public String getValue(User user) {
            return user.getFirstName();
        }
    };

    final protected TextColumn<User> lastNameColumn = new TextColumn<User>() {
        @Override
        public String getValue(User user) {
            return user.getLastName();
        }
    };

    final protected Column<User, String> enabledColumn = new Column<User, String>(new IconCell()) {
        @Override
        public String getValue(User user) {
            return user.getEnabled() ? "check-circle" : "circle-thin";
        }
    };

    public AdminUsersTable(ManagerMessages managerMessages,
                           Style style,
                           FormTableStyle formTableStyle) {
        super(Integer.MAX_VALUE, formTableStyle);

        this.style = style;

        setSelectionModel(selectionModel);

        applyStyleCellText(usernameColumn);
        addColumn(usernameColumn, createHeader(managerMessages.username()));
        addColumnStyleName(0, style.usernameColumn());

        applyStyleCellText(firstNameColumn);
        addColumn(firstNameColumn, createHeader(managerMessages.firstName()));
        addColumnStyleName(1, style.firstNameColumn());

        applyStyleCellText(lastNameColumn);
        addColumn(lastNameColumn, createHeader(managerMessages.lastName()));
        addColumnStyleName(2, style.lastNameColumn());

        addColumn(enabledColumn, createHeader(managerMessages.enabled()));
        addColumnStyleName(3, style.enabledColumn());
        enabledColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    @Override
    public SingleSelectionModel<User> getSelectionModel() {
        return selectionModel;
    }

    public User getSelectedObject() {
        return getSelectionModel().getSelectedObject();
    }

}
