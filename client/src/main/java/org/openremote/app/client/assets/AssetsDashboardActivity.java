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
package org.openremote.app.client.assets;

import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.asset.AssetViewPlace;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.assets.tenant.AssetsTenantPlace;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

public class AssetsDashboardActivity
    extends AssetBrowsingActivity<AssetsDashboardPlace>
    implements AssetsDashboard.Presenter {

    final protected AssetsDashboard view;

    @Inject
    public AssetsDashboardActivity(Environment environment,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   AssetsDashboard view) {
        super(environment, assetBrowserPresenter);
        this.view = view;
    }

    @Override
    protected AppActivity<AssetsDashboardPlace> init(AssetsDashboardPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.getSelectedNode() instanceof TenantTreeNode) {
                environment.getPlaceController().goTo(
                    new AssetsTenantPlace(event.getSelectedNode().getId())
                );
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                environment.getPlaceController().goTo(
                    new AssetViewPlace(event.getSelectedNode().getId())
                );
            }
        }));

        assetBrowserPresenter.clearSelection();
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }
}
