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
package org.openremote.manager.server.asset;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebResource;
import org.openremote.manager.shared.asset.AssetProcessingException;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;
import org.openremote.model.value.Values;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.openremote.model.attribute.AttributeEvent.Source.CLIENT;

public class AssetResourceImpl extends ManagerWebResource implements AssetResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;
    protected final MessageBrokerService messageBrokerService;

    public AssetResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             AssetStorageService assetStorageService,
                             MessageBrokerService messageBrokerService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.messageBrokerService = messageBrokerService;
    }

    @Override
    public Asset[] getCurrentUserAssets(RequestParams requestParams) {
        try {
            if (isSuperUser()) {
                return new Asset[0];
            }

            if (!isRestrictedUser()) {
                List<ServerAsset> result = assetStorageService.findAll(
                    new AssetQuery()
                        .parent(new AssetQuery.ParentPredicate(true))
                        .tenant(new AssetQuery.TenantPredicate().realm(getAuthenticatedRealm()))
                );
                return result.toArray(new Asset[result.size()]);
            }

            List<ServerAsset> assets = assetStorageService.findAll(
                new AssetQuery().select(new AssetQuery.Select(false, true)).userId(getUserId())
            );

            // Filter assets that might have been moved into a different realm and can no longer be accessed by user
            // TODO: Should we forbid moving assets between realms?
            Tenant authenticatedTenant = getAuthenticatedTenant();
            Iterator<ServerAsset> it = assets.iterator();
            while (it.hasNext()) {
                ServerAsset asset = it.next();
                if (!asset.getRealmId().equals(authenticatedTenant.getId())) {
                    LOG.warning("User '" + getUsername() + "' linked to asset in other realm, skipping: " + asset);
                    it.remove();
                }
            }
            return assets.toArray(new ServerAsset[assets.size()]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Asset[] getRoot(RequestParams requestParams, String realmId) {
        try {
            Tenant tenant = realmId == null || realmId.length() == 0
                ? getAuthenticatedTenant()
                : identityService.getIdentityProvider().getTenantForRealmId(realmId);

            if (tenant == null) {
                throw new WebApplicationException(NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(tenant) || isRestrictedUser()) {
                return new Asset[0];
            }

            List<ServerAsset> result = assetStorageService.findAll(
                new AssetQuery()
                    .parent(new AssetQuery.ParentPredicate(true))
                    .tenant(new AssetQuery.TenantPredicate(tenant.getId()))
            );
            return result.toArray(new Asset[result.size()]);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Asset[] getChildren(RequestParams requestParams, String parentId) {
        try {
            if (isRestrictedUser()) {
                return new Asset[0];
            }

            AssetQuery query = new AssetQuery()
                .parent(new AssetQuery.ParentPredicate(parentId));

            if (requestParams.loadComplete) {
                query = query.select(new AssetQuery.Select(true));
            }

            if (isSuperUser()) {
                List<ServerAsset> result = assetStorageService.findAll(
                    query
                );
                return result.toArray(new Asset[result.size()]);
            } else {
                Tenant tenant = getAuthenticatedTenant();
                if (tenant == null || !tenant.isActive(timerService.getCurrentTimeMillis())) {
                    throw new WebApplicationException(NOT_FOUND);
                }
                List<ServerAsset> result = assetStorageService.findAll(
                    query
                        .tenant(new AssetQuery.TenantPredicate(tenant.getId()))
                );
                return result.toArray(new Asset[result.size()]);
            }
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Asset get(RequestParams requestParams, String assetId) {
        try {
            ServerAsset asset;

            // Check restricted
            if (isRestrictedUser()) {
                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
                asset = assetStorageService.find(assetId, true, true);
            } else {
                asset = assetStorageService.find(assetId, true);
            }

            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "': " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            return asset;

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void update(RequestParams requestParams, String assetId, Asset asset) {
        try {
            ServerAsset serverAsset;
            // Check restricted
            if (isRestrictedUser()) {
                if (!assetStorageService.isUserAsset(getUserId(), assetId)) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
                // TODO Implement restricted user asset updates, what do we want to allow to update?
                throw new UnsupportedOperationException("TODO Implement asset updates for restricted users");
            } else {
                serverAsset = assetStorageService.find(assetId, true);
            }

            if (serverAsset == null)
                throw new WebApplicationException(NOT_FOUND);

            Tenant tenant = identityService.getIdentityProvider().getTenantForRealmId(asset.getRealmId());
            if (tenant == null)
                throw new WebApplicationException(BAD_REQUEST);

            // Check old realm, must be accessible
            if (!isTenantActiveAndAccessible(tenant)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't update: " + serverAsset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Map into server-side asset, do not allow to change the type
            ServerAsset updatedAsset = ServerAsset.map(asset, serverAsset, null, serverAsset.getType(), null);

            // Check new realm
            if (!isTenantActiveAndAccessible(updatedAsset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't update: " + updatedAsset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            assetStorageService.merge(updatedAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void writeAttributeValue(RequestParams requestParams, String assetId, String attributeName, String rawJson) {
        try {
            try {
                Value value = Values.instance()
                    .parse(rawJson)
                    .orElse(null); // When parsing literal JSON "null"

                AttributeEvent event = new AttributeEvent(
                    new AttributeRef(assetId, attributeName), value, timerService.getCurrentTimeMillis()
                );

                // Process asynchronously but block for a little while waiting for the result
                Map<String, Object> headers = new HashMap<>();
                headers.put(AttributeEvent.HEADER_SOURCE, CLIENT);
                headers.put(Constants.AUTH_CONTEXT, getAuthContext());
                Object result = messageBrokerService.getProducerTemplate().requestBodyAndHeaders(
                    AssetProcessingService.ASSET_QUEUE, event, headers
                );

                if (result instanceof AssetProcessingException) {
                    AssetProcessingException processingException = (AssetProcessingException) result;
                    switch (processingException.getReason()) {
                        case ILLEGAL_SOURCE:
                        case NO_AUTH_CONTEXT:
                        case INSUFFICIENT_ACCESS:
                            throw new WebApplicationException(Response.Status.FORBIDDEN);
                        case ASSET_NOT_FOUND:
                        case ATTRIBUTE_NOT_FOUND:
                            throw new WebApplicationException(NOT_FOUND);
                        case INVALID_AGENT_LINK:
                        case ILLEGAL_AGENT_UPDATE:
                        case INVALID_ATTRIBUTE_EXECUTE_STATUS:
                            throw new IllegalStateException(processingException);
                        default:
                            throw processingException;
                    }
                }

            } catch (ValueException ex) {
                throw new IllegalStateException("Error parsing JSON", ex);
            }

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void create(RequestParams requestParams, Asset asset) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // If there was no realm provided (create was called by regular user in manager UI), use the auth realm
            if (asset.getRealmId() == null || asset.getRealmId().length() == 0) {
                asset.setRealmId(getAuthenticatedTenant().getId());
            }

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't create: " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            ServerAsset serverAsset = ServerAsset.map(asset, new ServerAsset());

            // Allow client to set identifier
            // TODO Instead should return asset identifier instead of NO CONTENT
            if (asset.getId() != null) {
                // At least some sanity check, we must hope that the client has set a unique ID
                if (asset.getId().length() < 22) {
                    LOG.fine("Identifier value is too short, can't persist asset: " + asset);
                    throw new WebApplicationException(BAD_REQUEST);
                }
                serverAsset.setId(asset.getId());
            }

            assetStorageService.merge(serverAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String assetId) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            ServerAsset asset = assetStorageService.find(assetId, true, false);
            if (asset == null)
                return;

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't delete: " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            if (!assetStorageService.delete(assetId)) {
                throw new WebApplicationException(BAD_REQUEST);
            }
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

}
