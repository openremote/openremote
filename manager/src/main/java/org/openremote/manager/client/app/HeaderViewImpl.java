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
package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import org.openremote.manager.client.ThemeStyle;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.flows.FlowsPlace;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.map.MapPlace;

public class HeaderViewImpl extends Composite implements HeaderView {

    interface Style extends CssResource {
        String navItem();

        String active();

        String controls();

        String iconButton();

        String header();
    }

    interface UI extends UiBinder<HTMLPanel, HeaderViewImpl> {
    }

    private static UI ui = GWT.create(UI.class);
    private Presenter presenter;
    private ManagerMessages messages;

    @UiField
    Style style;

    @UiField
    ThemeStyle themeStyle;

    @UiField
    MaterialButton itemInventory;

    @UiField
    MaterialButton itemMap;

    @UiField
    MaterialButton itemFlows;

    @UiField
    MaterialButton itemAssets;

    @UiField
    MaterialButton userButton;

    @UiField
    MaterialButton notificationButton;

    @Inject
    public HeaderViewImpl(ManagerMessages messages) {
        this.messages = messages;
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void onPlaceChange(Place place) {
        itemMap.removeStyleName(style.active());
        itemMap.removeStyleName(themeStyle.NavItemActive());
        itemAssets.removeStyleName(style.active());
        itemAssets.removeStyleName(themeStyle.NavItemActive());
        itemFlows.removeStyleName(style.active());
        itemFlows.removeStyleName(themeStyle.NavItemActive());

        if (place instanceof MapPlace) {
            itemMap.addStyleName(style.active());
            itemMap.addStyleName(themeStyle.NavItemActive());
        }
        if (place instanceof AssetsPlace) {
            itemAssets.addStyleName(style.active());
            itemAssets.addStyleName(themeStyle.NavItemActive());
        }
        if (place instanceof FlowsPlace) {
            itemFlows.addStyleName(style.active());
            itemFlows.addStyleName(themeStyle.NavItemActive());
        }
    }

    @Override
    public void setUsername(String username) {
        if (username != null && username.length() > 0) {
            userButton.setText(messages.signedInAs(username));
            userButton.setVisible(true);
        } else {
            userButton.setVisible(false);
        }
    }

    @UiHandler("userButton")
    void logoutClicked(ClickEvent e) {
        // TODO: Open user self-edit activity
        presenter.doLogout();
    }

    @UiHandler("itemMap")
    void itemMapClicked(ClickEvent e) {
        presenter.goTo(new MapPlace());
    }

    @UiHandler("itemAssets")
    void itemAssetsClicked(ClickEvent e) {
        presenter.goTo(new AssetsPlace());
    }

    @UiHandler("itemFlows")
    void itemFlowsClicked(ClickEvent e) {
        presenter.goTo(new FlowsPlace());
    }

}