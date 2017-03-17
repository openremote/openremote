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
package org.openremote.manager.client.assets;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

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
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                environment.getPlaceController().goTo(
                    new AssetsTenantPlace(event.getSelectedNode().getId())
                );
            } else if (event.isAssetSelection()) {
                environment.getPlaceController().goTo(
                    new AssetPlace(event.getSelectedNode().getId())
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
