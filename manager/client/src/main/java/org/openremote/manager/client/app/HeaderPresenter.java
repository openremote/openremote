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

import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.apps.AppsPlace;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.map.MapAssetPlace;
import org.openremote.manager.client.map.MapTenantPlace;
import org.openremote.manager.client.rules.asset.AssetRulesListPlace;
import org.openremote.manager.client.rules.global.GlobalRulesListPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.user.UserControls;
import org.openremote.model.Constants;

import java.util.logging.Logger;

public class HeaderPresenter implements HeaderView.Presenter {

    private static final Logger LOG = Logger.getLogger(HeaderPresenter.class.getName());

    final protected HeaderView view;
    final protected UserControls.Presenter userControlsPresenter;
    final protected PlaceController placeController;
    final protected SecurityService securityService;

    protected AssetBrowserSelection assetBrowserSelection;

    @Inject
    public HeaderPresenter(HeaderView view,
                           UserControls.Presenter userControlsPresenter,
                           SecurityService securityService,
                           PlaceController placeController,
                           EventBus eventBus) {
        this.view = view;
        this.userControlsPresenter = userControlsPresenter;
        this.placeController = placeController;
        this.securityService = securityService;

        view.setPresenter(this);

        eventBus.register(
            GoToPlaceEvent.class,
            event -> view.onPlaceChange(event.getPlace())
        );

        eventBus.register(AssetBrowserSelection.class,
            event -> assetBrowserSelection = event
        );

        view.setUsername(securityService.getParsedToken().getPreferredUsername());
        eventBus.register(
            UserChangeEvent.class,
            event -> view.setUsername(event.getUsername())
        );
    }

    @Override
    public HeaderView getView() {
        return view;
    }

    @Override
    public void navigateMap() {
        if (assetBrowserSelection == null) {
            placeController.goTo(new MapAssetPlace());
            return;
        }
        if (assetBrowserSelection.isTenantSelection()) {
            placeController.goTo(new MapTenantPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.isAssetSelection()){
            placeController.goTo(new MapAssetPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else {
            placeController.goTo(new MapAssetPlace());
        }
    }

    @Override
    public void navigateAssets() {
        if (assetBrowserSelection == null) {
            placeController.goTo(new AssetsDashboardPlace());
            return;
        }
        if (assetBrowserSelection.isTenantSelection()) {
            placeController.goTo(new AssetsTenantPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.isAssetSelection()){
            placeController.goTo(new AssetPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else {
            placeController.goTo(new AssetsDashboardPlace());
        }
    }

    @Override
    public void navigateRules() {
        if (assetBrowserSelection == null) {
            placeController.goTo(new GlobalRulesListPlace());
            return;
        }
        if (assetBrowserSelection.isTenantSelection()) {
            placeController.goTo(new TenantRulesListPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.isAssetSelection()){
            placeController.goTo(new AssetRulesListPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else {
            placeController.goTo(new GlobalRulesListPlace());
        }
    }

    @Override
    public void navigateApps() {
        placeController.goTo(new AppsPlace());
    }

    @Override
    public void navigateAdmin() {
        placeController.goTo(new AdminOverviewPlace());

    }

    @Override
    public UserControls getUserControls() {
        return userControlsPresenter.getView();
    }

    @Override
    public boolean isUserInRole(String role) {
        return securityService.hasResourceRoleOrIsSuperUser(role, Constants.KEYCLOAK_CLIENT_ID);
    }
}
