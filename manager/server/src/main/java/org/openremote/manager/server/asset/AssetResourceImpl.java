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

import elemental.json.Json;
import elemental.json.JsonValue;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebResource;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;
import org.openremote.model.Attributes;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.ProtectedAsset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class AssetResourceImpl extends ManagerWebResource implements AssetResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;

    public AssetResourceImpl(ManagerIdentityService identityService,
                             AssetStorageService assetStorageService,
                             AssetProcessingService assetProcessingService) {
        super(identityService);
        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
    }

    @Override
    public ProtectedAsset[] getCurrentUserAssets(RequestParams requestParams) {
        try {
            if (isSuperUser() || !isRestrictedUser()) {
                return new ProtectedAsset[0];
            }
            List<ProtectedAsset> assets = Arrays.asList(assetStorageService.findProtectedOfUser(getUserId(), true));
            // Filter assets that might have been moved into a different realm and can no longer be accessed by user
            // TODO: Should we forbid moving assets between realms?
            Iterator<ProtectedAsset> it = assets.iterator();
            while (it.hasNext()) {
                ProtectedAsset asset = it.next();
                String assetRealm = identityService.getActiveTenantRealm(asset.getRealmId());
                if (!assetRealm.equals(getAuthenticatedRealm())) {
                    LOG.warning("User '" + getUsername() + "' has protected asset outside of authenticated realm, skipping: " + asset);
                    it.remove();
                }
            }
            return assets.toArray(new ProtectedAsset[assets.size()]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void updateCurrentUserAsset(RequestParams requestParams, String assetId, ProtectedAsset protectedAsset) {
        throw new UnsupportedOperationException("Not Implemented"); // TODO
    }

    @Override
    public Asset[] getRoot(RequestParams requestParams, String realmId) {
        try {
            if (realmId == null || realmId.length() == 0) {
                realmId = identityService.getActiveTenantRealmId(getAuthenticatedRealm());
            }
            String realm = identityService.getActiveTenantRealm(realmId);
            if (realm == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
                return new Asset[0];
            }
            return assetStorageService.findRoot(realmId, false);
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
            if (isSuperUser()) {
                return assetStorageService.findChildren(parentId, false);
            } else {
                String realmId = identityService.getActiveTenantRealmId(getAuthenticatedRealm());
                if (realmId == null) {
                    throw new WebApplicationException(NOT_FOUND);
                }
                return assetStorageService.findChildrenInRealm(parentId, realmId, false);
            }
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Asset get(RequestParams requestParams, String assetId) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            Asset asset = assetStorageService.find(assetId, true);
            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);
            String realm = identityService.getActiveTenantRealm(asset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't retrieve asset '"
                        + assetId + " + ' of realm: " + realm
                );
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
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            ServerAsset serverAsset = assetStorageService.find(assetId, true);
            if (serverAsset == null)
                throw new WebApplicationException(NOT_FOUND);

            String realm = identityService.getActiveTenantRealm(serverAsset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }

            // Check old realm, must be accessible
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't update asset '"
                        + assetId + " + ' of realm: " + realm
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Map into server-side asset, do not allow to change the type
            ServerAsset updatedAsset = ServerAsset.map(asset, serverAsset, null, serverAsset.getType(), null);

            realm = identityService.getActiveTenantRealm(updatedAsset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }

            // Check new realm
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't update asset '"
                        + assetId + " + ' of realm: " + realm
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            updatedAsset = assetStorageService.merge(updatedAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void writeAttributeValue(RequestParams requestParams, String assetId, String attributeName, String rawJson) {
        try {
            ServerAsset asset = assetStorageService.find(assetId, true);
            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);

            // Check attribute exists
            Attributes attributes = new Attributes(asset.getAttributes());
            if (!attributes.hasAttribute(attributeName))
                throw new WebApplicationException(NOT_FOUND);

            String realm = identityService.getActiveTenantRealm(asset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }

            // Check realm, must be accessible
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't update asset '"
                        + assetId + " + ' of realm: " + realm
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Check restricted
            if (isRestrictedUser() &&
                !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Process update
            try {
                JsonValue value = Json.instance().parse(rawJson);
                assetProcessingService.updateAttributeValue(new AttributeEvent(new AttributeRef(assetId, attributeName), value, this.getClass()));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Error updating attribute: " + attributeName, ex);
            }

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public String readAttributeValue(RequestParams requestParams, String assetId, String attributeName) {
        try {
            ServerAsset asset = assetStorageService.find(assetId, true);
            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);

            String realm = identityService.getActiveTenantRealm(asset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }

            // Check realm, must be accessible
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't read asset '"
                        + assetId + " + ' of realm: " + realm
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Check restricted
            if (isRestrictedUser() &&
                !assetStorageService.findProtectedOfUserContains(getUserId(), asset.getId())) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Attributes attributes = new Attributes(asset.getAttributes());
            Attribute attribute = attributes.get(attributeName);
            if (attribute == null) {
                throw new WebApplicationException(NOT_FOUND);
            }

            // No idea why toJson() would produce a Java null instead of a "null" literal, but it does
            return attribute.hasValue() ? attribute.getValue().toJson() : "null";

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
                asset.setRealmId(identityService.getActiveTenantRealmId(getAuthenticatedRealm()));
            }
            String realm = identityService.getActiveTenantRealm(asset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't create asset in realm: " + realm);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            ServerAsset serverAsset = ServerAsset.map(asset, new ServerAsset());

            // Allow client to set identifier
            if (asset.getId() != null) {
                // At least some sanity check, we must hope that the client has set a unique ID
                if (asset.getId().length() < 22) {
                    LOG.fine("Identifier value is too short, can't persist asset: " + asset);
                    throw new WebApplicationException(BAD_REQUEST);
                }
                serverAsset.setId(asset.getId());
            }

            serverAsset = assetStorageService.merge(serverAsset);

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
            ServerAsset serverAsset = assetStorageService.find(assetId, true);
            if (serverAsset == null)
                return;
            String realm = identityService.getActiveTenantRealm(serverAsset.getRealmId());
            if (realm == null) {
                throw new WebApplicationException(BAD_REQUEST);
            }
            if (!isRealmAccessibleByUser(realm)) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't delete asset '"
                        + assetId + " + ' of realm: " + realm
                );
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
