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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.admin.AdminPlace;
import org.openremote.manager.client.apps.AppsPlace;
import org.openremote.manager.client.map.MapPlace;
import org.openremote.manager.client.rules.RulesPlace;
import org.openremote.manager.client.user.UserAccountPlace;
import org.openremote.manager.client.widget.PushButton;

public class HeaderViewImpl extends Composite implements HeaderView {

    interface UI extends UiBinder<HTMLPanel, HeaderViewImpl> {
    }

    private static UI ui = GWT.create(UI.class);
    private Presenter presenter;

    @UiField
    PushButton mapButton;

    @UiField
    PushButton assetsButton;

    @UiField
    PushButton rulesButton;

    @UiField
    PushButton appsButton;

    @UiField
    PushButton adminButton;

    @UiField
    PushButton userButton;

    @Inject
    public HeaderViewImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        mapButton.setEnabled(presenter.isMapEnabled());
        assetsButton.setEnabled(presenter.isAssetsEnabled());
        rulesButton.setEnabled(presenter.isRulesEnabled());
        appsButton.setEnabled(presenter.isAppsEnabled());
        adminButton.setEnabled(presenter.isAdminEnabled());
    }

    @Override
    public void onPlaceChange(Place place) {
        mapButton.removeStyleName("active");
        assetsButton.removeStyleName("active");
        rulesButton.removeStyleName("active");
        appsButton.removeStyleName("active");
        adminButton.removeStyleName("active");
        userButton.removeStyleName("active");

        if (place instanceof MapPlace) {
            mapButton.addStyleName("active");
        }
        if (place instanceof AssetsPlace) {
            assetsButton.addStyleName("active");
        }
        if (place instanceof RulesPlace) {
            rulesButton.addStyleName("active");
        }
        if (place instanceof AppsPlace) {
            appsButton.addStyleName("active");
        }
        if (place instanceof AdminPlace) {
            adminButton.addStyleName("active");
        }
        if (place instanceof UserAccountPlace) {
            userButton.addStyleName("active");
        }
    }

    @Override
    public void setUsername(String username) {
        /* TOOD We need the space
        userButton.setText(username != null ? username : "");
        */
        userButton.setEnabled(username != null && username.length() > 0);
    }

    @UiHandler("mapButton")
    void mapClicked(ClickEvent e) {
        presenter.navigateMap();
    }

    @UiHandler("assetsButton")
    void assetsClicked(ClickEvent e) {
        presenter.navigateAssets();
    }

    @UiHandler("adminButton")
    void adminClicked(ClickEvent e) {
        presenter.navigateAdmin();
    }

    @UiHandler("rulesButton")
    void rulesClicked(ClickEvent e) {
        presenter.navigateRules();
    }

    @UiHandler("appsButton")
    void appsClicked(ClickEvent e) {
        presenter.navigateApps();
    }

    @UiHandler("userButton")
    public void userClicked(final ClickEvent event) {
        presenter.getUserControls().toggleRelativeTo(userButton);
    }
}