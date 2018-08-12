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
package org.openremote.app.client.app.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.widget.AbstractAppPanel;
import org.openremote.app.client.widget.AppPanel;
import org.openremote.app.client.widget.PopupPanel;

import javax.inject.Inject;

public class Dialog extends AbstractAppPanel implements AppPanel {

    interface UI extends UiBinder<PopupPanel, Dialog> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    FlowPanel mainPanel;

    @UiField
    Label headerLabel;

    @UiField
    FlowPanel contentPanel;

    @UiField
    FlowPanel footerPanel;

    public Dialog() {
        super(GWT.create(UI.class));
    }

    public void addStyleName(String name) {
        mainPanel.addStyleName(name);
    }

    public void setHeaderLabel(String label) {
        headerLabel.setText(label);
    }

    public HasWidgets getContentPanel() {
        return contentPanel;
    }

    public HasWidgets getFooterPanel() {
        return footerPanel;
    }
}