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
import org.openremote.manager.client.admin.UserArrayMapper;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.asset.AssetViewPlace;
import org.openremote.manager.client.assets.browser.*;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.manager.shared.security.User;
import org.openremote.manager.shared.security.UserResource;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetsTenantActivity extends AssetBrowsingActivity<AssetsTenantPlace> implements AssetsTenant.Presenter {

    final AssetsTenant view;
    final TenantResource tenantResource;
    final TenantMapper tenantMapper;
    final AssetResource assetResource;
    final UserAssetMapper userAssetMapper;
    final UserAssetArrayMapper userAssetArrayMapper;
    UserResource userResource;
    final protected UserArrayMapper userArrayMapper;

    protected String realmId;
    protected Tenant tenant;
    protected User[] users;

    protected String selectedUserId;
    protected String selectedAssetId;

    @Inject
    public AssetsTenantActivity(Environment environment,
                                Tenant currentTenant,
                                AssetBrowser.Presenter assetBrowserPresenter,
                                AssetsTenant view,
                                TenantResource tenantResource,
                                TenantMapper tenantMapper,
                                AssetResource assetResource,
                                UserAssetMapper userAssetMapper,
                                UserAssetArrayMapper userAssetArrayMapper,
                                UserResource userResource,
                                UserArrayMapper userArrayMapper) {
        super(environment, currentTenant, assetBrowserPresenter);
        this.view = view;
        this.tenantResource = tenantResource;
        this.tenantMapper = tenantMapper;
        this.assetResource = assetResource;
        this.userAssetMapper = userAssetMapper;
        this.userAssetArrayMapper = userAssetArrayMapper;
        this.userResource = userResource;
        this.userArrayMapper = userArrayMapper;
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
                if (this.realmId == null || !this.realmId.equals(event.getSelectedNode().getId())) {
                    environment.getPlaceController().goTo(
                        new AssetsTenantPlace(event.getSelectedNode().getId())
                    );
                }
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                environment.getPlaceController().goTo(
                    new AssetViewPlace(event.getSelectedNode().getId())
                );
            }
        }));

        loadTenant();
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    @Override
    public void onUserSelected(String username) {
        this.selectedUserId = null;
        Arrays.stream(users).filter(user -> user.getUsername().equals(username))
            .findFirst().ifPresent(user -> {
            this.selectedUserId = user.getId();
            if (this.selectedAssetId != null)
                view.setCreateAssetLinkEnabled(true);
        });
        loadUserAssets();
    }

    @Override
    public void onAssetSelected(BrowserTreeNode treeNode) {
        if (treeNode == null || !(treeNode instanceof AssetTreeNode)) {
            this.selectedAssetId = null;
            view.setCreateAssetLinkEnabled(false);
        } else {
            this.selectedAssetId = treeNode.getId();
            if (this.selectedUserId != null)
                view.setCreateAssetLinkEnabled(true);
        }
        loadUserAssets();
    }

    @Override
    public void onCreateAssetLink() {
        if (selectedUserId == null || selectedAssetId == null)
            return;
        UserAsset userAsset = new UserAsset(realmId, selectedUserId, selectedAssetId);
        view.setFormBusy(true);
        environment.getRequestService().execute(
            userAssetMapper,
            requestParams -> assetResource.createUserAsset(requestParams, userAsset),
            204,
            () -> {
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().userAssetLinkCreated()
                ));
                view.setFormBusy(false);
                loadUserAssets();
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void onDeleteAssetLink(UserAsset.Id id) {
        view.setFormBusy(true);
        environment.getRequestService().execute(
            userAssetMapper,
            requestParams -> assetResource.deleteUserAsset(requestParams, id.getRealmId(), id.getUserId(), id.getAssetId()),
            204,
            () -> {
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().userAssetLinkDeleted()
                ));
                view.setFormBusy(false);
                loadUserAssets();
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadTenant() {
        if (this.realmId == null)
            return;
        environment.getRequestService().execute(
            tenantMapper,
            requestParams -> tenantResource.getForRealmId(requestParams, this.realmId),
            200,
            tenant -> {
                this.tenant = tenant;
                loadUsers(() -> {
                    writeTenantToView();
                    loadUserAssets();
                });
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadUsers(Runnable onComplete) {
        environment.getRequestService().execute(
            userArrayMapper,
            requestParams -> userResource.getAll(requestParams, this.tenant.getRealm()),
            200,
            users -> {
                this.users = users;
                writeUsersToView();
                onComplete.run();
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadUserAssets() {
        environment.getRequestService().execute(
            userAssetArrayMapper,
            requestParams -> assetResource.getUserAssetLinks(requestParams, realmId, selectedUserId, selectedAssetId),
            200,
            userAssets -> {

                // Do not allow creating a duplicate of a link we already have
                if ((userAssets.length == 1
                    && userAssets[0].getId().equals(new UserAsset.Id(realmId, selectedUserId, selectedAssetId))))
                view.setCreateAssetLinkEnabled(false);

                view.setUserAssets(userAssets);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void writeTenantToView() {
        view.setTenantName(tenant.getDisplayName());
    }

    protected void writeUsersToView() {
        view.setUsers(this.users);
    }

}
