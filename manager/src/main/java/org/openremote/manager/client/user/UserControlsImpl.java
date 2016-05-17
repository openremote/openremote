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
package org.openremote.manager.client.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.AbstractAppPanel;

import javax.inject.Inject;

public class UserControlsImpl extends AbstractAppPanel implements UserControls {

    interface UI extends UiBinder<PopupPanel, UserControlsImpl> {
    }

    final ManagerMessages managerMessages;

    @UiField
    Label usernameLabel;

    @UiField
    Label userMessage;

    Presenter presenter;

    @Inject
    public UserControlsImpl(ManagerMessages managerMessages) {
        super(GWT.create(UI.class), true);
        this.managerMessages = managerMessages;
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setUsername(String username) {
        usernameLabel.setText(username);
        userMessage.setText(managerMessages.signedInAs(username));
    }

    @UiHandler("logoutButton")
    void logoutClicked(ClickEvent e) {
        presenter.doLogout();
    }
}