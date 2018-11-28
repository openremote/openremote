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
package org.openremote.app.client.app;

import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import org.openremote.app.client.OpenRemoteApp;
import org.openremote.app.client.admin.syslog.AdminSyslogPlace;
import org.openremote.app.client.apps.ConsoleAppSelection;
import org.openremote.app.client.apps.ConsoleAppsPlace;
import org.openremote.app.client.assets.AssetsDashboardPlace;
import org.openremote.app.client.assets.asset.AssetViewPlace;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.assets.tenant.AssetsTenantPlace;
import org.openremote.app.client.event.GoToPlaceEvent;
import org.openremote.app.client.map.MapAssetPlace;
import org.openremote.app.client.map.MapTenantPlace;
import org.openremote.app.client.notifications.NotificationsPlace;
import org.openremote.app.client.rules.asset.AssetRulesListPlace;
import org.openremote.app.client.rules.global.GlobalRulesListPlace;
import org.openremote.app.client.rules.tenant.TenantRulesListPlace;
import org.openremote.app.client.user.UserControls;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;

public class HeaderPresenter implements HeaderView.Presenter {

    final protected HeaderView view;
    final protected UserControls.Presenter userControlsPresenter;
    final protected PlaceController placeController;
    final protected OpenRemoteApp app;

    protected AssetBrowserSelection assetBrowserSelection;
    protected ConsoleAppSelection consoleAppSelection;

    @Inject
    public HeaderPresenter(HeaderView view,
                           UserControls.Presenter userControlsPresenter,
                           OpenRemoteApp app,
                           PlaceController placeController,
                           EventBus eventBus) {
        this.view = view;
        this.userControlsPresenter = userControlsPresenter;
        this.placeController = placeController;
        this.app = app;

        view.setPresenter(this);

        eventBus.register(
            GoToPlaceEvent.class,
            event -> view.onPlaceChange(event.getPlace())
        );

        eventBus.register(AssetBrowserSelection.class,
            event -> assetBrowserSelection = event
        );

        eventBus.register(ConsoleAppSelection.class,
            event -> consoleAppSelection = event
        );

        view.setUsername(app.getSecurity().getUser());
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
        if (assetBrowserSelection.getSelectedNode() instanceof TenantTreeNode) {
            placeController.goTo(new MapTenantPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.getSelectedNode() instanceof AssetTreeNode) {
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
        if (assetBrowserSelection.getSelectedNode() instanceof TenantTreeNode) {
            placeController.goTo(new AssetsTenantPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.getSelectedNode() instanceof AssetTreeNode) {
            placeController.goTo(new AssetViewPlace(assetBrowserSelection.getSelectedNode().getId()));
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
        if (assetBrowserSelection.getSelectedNode() instanceof TenantTreeNode) {
            placeController.goTo(new TenantRulesListPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else if (assetBrowserSelection.getSelectedNode() instanceof AssetTreeNode) {
            placeController.goTo(new AssetRulesListPlace(assetBrowserSelection.getSelectedNode().getId()));
        } else {
            placeController.goTo(new GlobalRulesListPlace());
        }
    }

    @Override
    public void navigateNotifications() {
        if (assetBrowserSelection == null) {
            placeController.goTo(new NotificationsPlace());
            return;
        }
        if (assetBrowserSelection.getSelectedNode() instanceof TenantTreeNode) {
            placeController.goTo(new NotificationsPlace(((TenantTreeNode) assetBrowserSelection.getSelectedNode()).getTenant().getRealm(), null));
        } else if (assetBrowserSelection.getSelectedNode() instanceof AssetTreeNode) {
            placeController.goTo(
                new NotificationsPlace(((AssetTreeNode) assetBrowserSelection.getSelectedNode()).getAsset().getTenantRealm(),
                assetBrowserSelection.getSelectedNode().getId()));
        } else {
            placeController.goTo(new NotificationsPlace());
        }
    }

    @Override
    public void navigateApps() {
        placeController.goTo(new ConsoleAppsPlace(consoleAppSelection != null ? consoleAppSelection.getAppName() : null));
    }

    @Override
    public void navigateAdmin() {
        placeController.goTo(new AdminSyslogPlace());
    }

    @Override
    public UserControls getUserControls() {
        return userControlsPresenter.getView();
    }

    @Override
    public boolean isMapEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:map", Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean isAssetsEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:assets", Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean isRulesEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:rules", Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean isNotificationsEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:admin", Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean isAppsEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:consoles", Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean isAdminEnabled() {
        return app.getSecurity().hasResourceRoleOrIsSuperUser("read:admin", Constants.KEYCLOAK_CLIENT_ID);
    }
}
