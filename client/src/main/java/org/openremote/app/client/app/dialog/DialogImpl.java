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
import org.openremote.app.client.widget.PopupPanel;

import javax.inject.Inject;

public class DialogImpl extends AbstractAppPanel implements Dialog {

    interface UI extends UiBinder<PopupPanel, DialogImpl> {
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

    @Inject
    public DialogImpl() {
        super(GWT.create(UI.class));
    }

    @Override
    public void addStyleName(String name) {
        mainPanel.addStyleName(name);
    }

    @Override
    public void setHeaderLabel(String label) {
        headerLabel.setText(label);
    }

    @Override
    public HasWidgets getContentPanel() {
        return contentPanel;
    }

    @Override
    public HasWidgets getFooterPanel() {
        return footerPanel;
    }
}