/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.app.client.notifications;

import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.CredentialMapper;
import org.openremote.app.client.admin.RoleArrayMapper;
import org.openremote.app.client.admin.TenantArrayMapper;
import org.openremote.app.client.admin.UserArrayMapper;
import org.openremote.app.client.assets.AssetArrayMapper;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.AssetMapper;
import org.openremote.app.client.assets.AssetQueryMapper;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.notification.*;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.TenantResource;
import org.openremote.model.security.User;
import org.openremote.model.security.UserResource;
import org.openremote.model.util.TextUtil;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NotificationsActivity extends AssetBrowsingActivity<NotificationsPlace> implements NotificationsView.Presenter {

    final protected TenantResource tenantResource;
    final protected TenantArrayMapper tenantArrayMapper;
    final protected UserResource userResource;
    final protected UserArrayMapper userArrayMapper;
    final protected AssetResource assetResource;
    final protected AssetMapper assetMapper;
    final protected AssetArrayMapper assetArrayMapper;
    final protected AssetQueryMapper assetQueryMapper;
    final protected CredentialMapper credentialMapper;
    final protected RoleArrayMapper roleArrayMapper;
    final protected NotificationEditor notificationEditor;
    final protected NotificationResource notificationResource;
    final protected NotificationMapper notificationMapper;
    final protected SentNotificationArrayMapper sentNotificationArrayMapper;
    final protected NotificationsView view;

    protected Tenant[] tenants;
    protected NotificationsPlace place;
    protected FilterOptions filterOptions;
    protected SendOptions sendOptions;

    @Inject
    public NotificationsActivity(Environment environment,
                                 AssetBrowser.Presenter assetBrowserPresenter,
                                 NotificationsView view,
                                 TenantResource tenantResource,
                                 TenantArrayMapper tenantArrayMapper,
                                 AssetResource assetResource,
                                 AssetMapper assetMapper,
                                 AssetArrayMapper assetArrayMapper,
                                 AssetQueryMapper assetQueryMapper,
                                 UserResource userResource,
                                 UserArrayMapper userArrayMapper,
                                 CredentialMapper credentialMapper,
                                 RoleArrayMapper roleArrayMapper,
                                 NotificationEditor notificationEditor,
                                 NotificationResource notificationResource,
                                 NotificationMapper notificationMapper,
                                 SentNotificationArrayMapper sentNotificationArrayMapper) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetArrayMapper = assetArrayMapper;
        this.assetQueryMapper = assetQueryMapper;
        this.userResource = userResource;
        this.userArrayMapper = userArrayMapper;
        this.credentialMapper = credentialMapper;
        this.roleArrayMapper = roleArrayMapper;
        this.notificationEditor = notificationEditor;
        this.notificationResource = notificationResource;
        this.notificationMapper = notificationMapper;
        this.sentNotificationArrayMapper = sentNotificationArrayMapper;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            String realm = null;
            String assetId = null;
            if (event.getSelectedNode() instanceof TenantTreeNode) {
                realm = ((TenantTreeNode) event.getSelectedNode()).getTenant().getRealm();
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                realm = ((AssetTreeNode) event.getSelectedNode()).getAsset().getTenantRealm();
                assetId = event.getSelectedNode().getId();
            }

            if (!Objects.equals(this.place.getRealm(), realm) || !Objects.equals(this.place.getAssetId(), assetId)) {
                environment.getPlaceController().goTo(new NotificationsPlace(realm, assetId));
            }
        }));

        view.setBusy(true);

        // Build filter options
        if (filterOptions != null) {
            filterOptions.setChangedCallback(null);
            filterOptions.setTargetsChangedCallback(null);
            filterOptions = null;
        }

        loadTenants(tenants -> {
            Map<String, String> realms = Arrays.stream(tenants)
                .collect(Collectors.toMap(Tenant::getRealm, Tenant::getDisplayName));

            Map<String, String> realmIds = Arrays.stream(tenants)
                .collect(Collectors.toMap(Tenant::getRealm, Tenant::getId));

            filterOptions = new FilterOptions(realms, realmIds, this::loadTargets);

            String selectedRealm = !TextUtil.isNullOrEmpty(place.getRealm()) && realms.containsKey(place.getRealm())
                ? place.getRealm() : null;

            filterOptions.setSelectedRealm(selectedRealm);

            if (selectedRealm != null && !TextUtil.isNullOrEmpty(place.getAssetId())) {
                filterOptions.setSelectedTargetType(Notification.TargetType.ASSET);
                filterOptions.setSelectedTarget(place.getAssetId());
            }

            filterOptions.setChangedCallback(this::onFilterOptionsChanged);
            view.setFilterOptions(filterOptions);
            view.setBusy(false);
            onFilterOptionsChanged();
        });
    }

    @Override
    public void refreshNotifications() {
        view.setBusy(true);
        refreshNotifications(notifications -> {
            view.setNotifications(notifications);
            view.setDeleteAllEnabled(notifications.length > 0);
            view.setBusy(false);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        notificationEditor.reset();
        view.setPresenter(null);
    }

    @Override
    public void showNotificationEditor() {

        notificationEditor.setResult(null);

        loadTenants(tenants -> {
            Map<String, String> realms = Arrays.stream(tenants)
                .collect(Collectors.toMap(Tenant::getRealm, Tenant::getDisplayName));

            Map<String, String> realmIds = Arrays.stream(tenants)
                .collect(Collectors.toMap(Tenant::getRealm, Tenant::getId));

            if (sendOptions == null) {
                sendOptions = new SendOptions(realms, realmIds, this::loadTargets, this::createMessage);
            }

            String selectedRealm = !TextUtil.isNullOrEmpty(place.getRealm()) && realms.containsKey(place.getRealm())
                ? place.getRealm() : null;

            sendOptions.setSelectedTargetType((Notification.TargetType)null);
            sendOptions.setSelectedRealm(selectedRealm);

            if (selectedRealm != null && !TextUtil.isNullOrEmpty(place.getAssetId())) {
                sendOptions.setSelectedTargetType(Notification.TargetType.ASSET);
                sendOptions.setSelectedTarget(place.getAssetId());
            }

            notificationEditor.setSendOptions(sendOptions);
            notificationEditor.setOnSend(() -> sendNotification(notificationEditor::setResult));
            notificationEditor.setOnClose(this::refreshNotifications);
            notificationEditor.show();
        });
    }

    protected AbstractNotificationMessage createMessage() {
        if (sendOptions == null) {
            return null;
        }

        switch (sendOptions.getSelectedNotificationType()) {
            case PushNotificationMessage.TYPE:
                return new PushNotificationMessage();
            default:
                return null;
        }
    }

    @Override
    public void deleteNotifications() {
        String tenantId = filterOptions.getTenantId();
        String userId = filterOptions.getUserId();
        String assetId = filterOptions.getAssetId();
        Long fromTimestamp = filterOptions.getFromTimestamp();

        environment.getApp().getRequests().send(
            requestParams -> notificationResource.removeNotifications(requestParams,
                null,
                filterOptions.getSelectedNotificationType(),
                fromTimestamp,
                null,
                tenantId,
                userId,
                assetId),
            204,
            () -> {
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().notificationsDeleted())
                );
                refreshNotifications();
            }
        );
    }

    @Override
    public void deleteNotification(Long id) {
        environment.getApp().getRequests().send(
            requestParams -> notificationResource.removeNotification(requestParams, id),
            204,
            () -> {
                view.removeNotification(id);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().notificationDeleted(id))
                );
            }
        );
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<NotificationsPlace> init(NotificationsPlace place) {
        this.place = place;
        return this;
    }

    protected void sendNotification(Consumer<NotificationSendResult> sendResultConsumer) {
        notificationEditor.setBusy(true);

        if (sendOptions.getMessage() == null) {
            sendResultConsumer.accept(NotificationSendResult.failure(
                environment.getMessages().notificationMessageMissing()
            ));
            notificationEditor.setBusy(false);
            return;
        }

        if (sendOptions.getSelectedTargetType() == null || TextUtil.isNullOrEmpty(sendOptions.getSelectedTarget())) {
            sendResultConsumer.accept(NotificationSendResult.failure(
                environment.getMessages().notificationTargetMissing()
            ));
            notificationEditor.setBusy(false);
            return;
        }

        Notification notification = buildNotification();

        if (notification == null) {
            sendResultConsumer.accept(NotificationSendResult.failure(environment.getMessages().notificationBuildError()));
            notificationEditor.setBusy(false);
            return;
        }

        environment.getApp().getRequests().sendWith(
            notificationMapper,
            requestParams -> notificationResource.sendNotification(requestParams, buildNotification()),
            204,
            () -> {
                sendResultConsumer.accept(NotificationSendResult.success());
                notificationEditor.setBusy(false);
            }
        );
    }

    protected void onFilterOptionsChanged() {

        boolean refreshEnabled = canLoadNotifications();
        if (!TextUtil.isNullOrEmpty(filterOptions.getSelectedRealm())) {
            if (refreshEnabled && filterOptions.getSelectedTargetType() == Notification.TargetType.ASSET) {
                assetBrowserPresenter.selectAssetById(filterOptions.getSelectedTarget());
            } else {
                assetBrowserPresenter.selectRealm(filterOptions.getSelectedRealm());
            }
        } else {
            assetBrowserPresenter.clearSelection();
        }

        view.setRefreshEnabled(refreshEnabled);
        refreshNotifications();
    }

    protected boolean canLoadNotifications() {
        return !TextUtil.isNullOrEmpty(filterOptions.getSelectedTarget());
    }

    protected void loadTargets(FilterOptions filterOptions, Consumer<Map<String, String>> targetsConsumer) {
        view.setBusy(true);

        if (filterOptions == null || filterOptions.selectedRealm == null || filterOptions.selectedTargetType == null) {
            targetsConsumer.accept(new HashMap<>(0));
            view.setBusy(false);
            return;
        }

        switch (filterOptions.selectedTargetType) {

            case TENANT:
                targetsConsumer.accept(filterOptions.getRealms());
                view.setBusy(false);
                break;
            case USER:
                loadUsers(filterOptions.getSelectedRealm(), users -> {
                    targetsConsumer.accept(Arrays.stream(users)
                        .collect(Collectors.toMap(User::getId, User::getUsername)));
                    view.setBusy(false);
                });
                break;
            case ASSET:
                loadAssets(filterOptions.getSelectedRealm(), assets -> {
                    targetsConsumer.accept(Arrays.stream(assets)
                        .collect(Collectors.toMap(Asset::getId, Asset::getName)));
                    view.setBusy(false);
                });
                break;
        }
    }

    protected void loadTenants(Consumer<Tenant[]> tenantConsumer) {

        if (this.tenants != null) {
            tenantConsumer.accept(this.tenants);
            return;
        }

        environment.getApp().getRequests().sendAndReturn(
            tenantArrayMapper,
            tenantResource::getAll,
            200,
            tenants -> {
                this.tenants = tenants;
                tenantConsumer.accept(tenants);
            });
    }

    protected void loadUsers(String realm, Consumer<User[]> usersConsumer) {
        environment.getApp().getRequests().sendAndReturn(
            userArrayMapper,
            requestParams -> userResource.getAll(requestParams, realm),
            200,
            usersConsumer::accept);
    }

    protected void loadAssets(String realm, Consumer<Asset[]> assetConsumer) {
        environment.getApp().getRequests().sendWithAndReturn(
            assetArrayMapper,
            assetQueryMapper,
            requestParams -> assetResource.queryAssets(requestParams,
                new AssetQuery()
                    .select(new BaseAssetQuery
                        .Select(BaseAssetQuery.Include.ONLY_ID_AND_NAME))
                    .tenant(new TenantPredicate().realm(realm))),
            200,
            assetConsumer::accept);
    }

    protected void refreshNotifications(Consumer<SentNotification[]> notificationConsumer) {

        if (!canLoadNotifications()) {
            notificationConsumer.accept(new SentNotification[0]);
            return;
        }

        String tenantId = filterOptions.getTenantId();
        String userId = filterOptions.getUserId();
        String assetId = filterOptions.getAssetId();
        Long fromTimestamp = filterOptions.getFromTimestamp();

        environment.getApp().getRequests().sendAndReturn(
            sentNotificationArrayMapper,
            requestParams -> notificationResource.getNotifications(requestParams,
                null,
                filterOptions.getSelectedNotificationType(),
                fromTimestamp,
                null,
                tenantId,
                userId,
                assetId),
            200,
            notifications ->
                notificationConsumer.accept(
                    Arrays.stream(notifications)
                        .sorted(Comparator.comparingLong((SentNotification n) -> n.getSentOn().getTime()).reversed())
                        .toArray(SentNotification[]::new))
            );
    }

    protected Notification buildNotification() {
        return new Notification("Manager Message", sendOptions.getMessage(), new Notification.Targets(sendOptions.selectedTargetType, sendOptions.selectedTarget));
    }
}
