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
package org.openremote.manager.client.flows;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.util.Timeout;
import org.openremote.manager.client.widget.Hyperlink;
import org.openremote.manager.shared.asset.AssetInfo;

import javax.inject.Inject;
import java.util.logging.Logger;

public class FlowsViewImpl extends Composite implements FlowsView {

    private static final Logger LOG = Logger.getLogger(FlowsViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, FlowsViewImpl> {
    }

    interface Style extends CssResource {

        String navItem();
    }

    @UiField
    ManagerMessages managerMessages;
    @UiField
    WidgetStyle widgetStyle;

    @UiField
    Style style;

    @UiField
    HTMLPanel agentListPanel;

    @UiField
    IFrameElement frame;

    @UiField
    HTMLPanel placeholder;

    Presenter presenter;

    @Inject
    public FlowsViewImpl() {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            agentListPanel.clear();
            frame.setSrc("blank:");
            placeholder.setVisible(true);
        }
    }

    @Override
    public void setAgents(AssetInfo[] agents) {
        agentListPanel.clear();

        if (agents == null || agents.length == 0) {
            Label noAgentsLabel = new Label(managerMessages.noAgentsFound());
            noAgentsLabel.addStyleName(widgetStyle.SecondaryNavItem());
            agentListPanel.add(noAgentsLabel);
            return;
        }

        for (AssetInfo agent : agents) {
            Hyperlink agentLabel = new Hyperlink();
            agentLabel.setIcon("cubes");
            agentLabel.addStyleName(style.navItem());
            agentLabel.addStyleName(widgetStyle.SecondaryNavItem());
            agentLabel.setText(agent.getName());
            agentLabel.setTargetHistoryToken(presenter.getFlowsHistoryToken());
            agentLabel.addClickHandler(event -> presenter.onAgentSelected(agent));
            agentListPanel.add(agentLabel);
        }
    }

    @Override
    public void setFrameSourceUrl(String frameSourceUrl) {
        placeholder.setVisible(false);
        frame.setSrc(frameSourceUrl);
        Timeout.debounce("setiframefocus", () -> {
            frame.focus();
        }, 500);
    }
}
