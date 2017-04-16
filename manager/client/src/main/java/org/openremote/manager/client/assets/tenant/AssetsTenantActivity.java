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
package org.openremote.manager.client.assets.tenant;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.TenantMapper;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.asset.AssetViewPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetsTenantActivity extends AssetBrowsingActivity<AssetsTenantPlace> implements AssetsTenant.Presenter {

    final AssetsTenant view;
    final TenantResource tenantResource;
    final TenantMapper tenantMapper;

    protected String realmId;
    protected Tenant tenant;

    @Inject
    public AssetsTenantActivity(Environment environment,
                                Tenant currentTenant,
                                AssetBrowser.Presenter assetBrowserPresenter,
                                AssetsTenant view,
                                TenantResource tenantResource,
                                TenantMapper tenantMapper) {
        super(environment, currentTenant, assetBrowserPresenter);
        this.view = view;
        this.tenantResource = tenantResource;
        this.tenantMapper = tenantMapper;
    }

    @Override
    protected AppActivity<AssetsTenantPlace> init(AssetsTenantPlace place) {
        this.realmId = place.getRealmId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
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


        if (realmId != null) {
            environment.getRequestService().execute(
                tenantMapper,
                requestParams -> tenantResource.getForRealmId(requestParams, realmId),
                200,
                tenant -> {
                    this.tenant = tenant;
                    writeTenantToView();
                },
                ex -> handleRequestException(ex, environment)
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    protected void writeTenantToView() {
        view.setTenantName(tenant.getDisplayName());
    }

}
