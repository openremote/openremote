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

import elemental.json.JsonValue;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.asset.ProtectedAssetInfo;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class AssetResourceImpl extends WebResource implements AssetResource {

    private static final Logger LOG = Logger.getLogger(AssetResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;

    public AssetResourceImpl(AssetStorageService assetStorageService) {
        this.assetStorageService = assetStorageService;
    }

    @Override
    public ProtectedAssetInfo[] getCurrentUserAssets(@BeanParam RequestParams requestParams) {
        try {
            if (isSuperUser() || !isRestrictedUser()) {
                return new ProtectedAssetInfo[0];
            }
            return assetStorageService.findProtectedOfUser(getAuthenticatedRealm(), getUsername());
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void updateCurrentUserAsset(@BeanParam RequestParams requestParams, String assetId, ProtectedAssetInfo assetInfo) {
        throw new UnsupportedOperationException("Not Implemented"); // TODO
    }

    @Override
    public AssetInfo[] getRoot(@BeanParam RequestParams requestParams, String realm) {
        try {
            if (realm == null || realm.length() == 0) {
                realm = getAuthenticatedRealm();
            }
            if (!isRealmAccessibleByUser(realm) || isRestrictedUser()) {
                return new AssetInfo[0];
            }
            return assetStorageService.findRoot(realm);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public AssetInfo[] getChildren(@BeanParam RequestParams requestParams, String parentId) {
        try {
            if (isRestrictedUser()) {
                return new AssetInfo[0];
            }
            return isSuperUser()
                ? assetStorageService.findChildren(parentId)
                : assetStorageService.findChildrenInRealm(parentId, getAuthenticatedRealm());
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Asset get(@BeanParam RequestParams requestParams, String assetId) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            Asset asset = assetStorageService.find(assetId);
            if (asset == null)
                throw new WebApplicationException(NOT_FOUND);
            if (!isRealmAccessibleByUser(asset.getRealm())) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't retrieve asset '"
                        + assetId + " + ' of realm: " + asset.getRealm()
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            return asset;
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void update(@BeanParam RequestParams requestParams, String assetId, Asset asset) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            ServerAsset serverAsset = assetStorageService.find(assetId);
            if (serverAsset == null)
                throw new WebApplicationException(NOT_FOUND);

            // Check old realm, must be accessible
            if (!isRealmAccessibleByUser(serverAsset.getRealm())) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't update asset '"
                        + assetId + " + ' of realm: " + serverAsset.getRealm()
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // Map into server-side asset, do not allow to change the type
            ServerAsset updatedAsset = ServerAsset.map(asset, serverAsset, null, null, serverAsset.getType(), null);

            // Check new realm
            if (!isRealmAccessibleByUser(updatedAsset.getRealm())) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't update asset '"
                        + assetId + " + ' of realm: " + serverAsset.getRealm()
                );
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            updatedAsset = assetStorageService.merge(updatedAsset);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void updateAttribute(@BeanParam RequestParams requestParams, String assetId, String attributeName, JsonValue value) {
        throw new UnsupportedOperationException("Not Implemented"); // TODO
    }

    @Override
    public void create(@BeanParam RequestParams requestParams, Asset asset) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            if (!isRealmAccessibleByUser(asset.getRealm())) {
                LOG.fine("Forbidden access for user '" + getUsername() + "', can't create asset in realm: " + asset.getRealm());
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
    public void delete(@BeanParam RequestParams requestParams, String assetId) {
        try {
            if (isRestrictedUser()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            ServerAsset serverAsset = assetStorageService.find(assetId);
            if (serverAsset == null)
                return;
            if (!isRealmAccessibleByUser(serverAsset.getRealm())) {
                LOG.fine(
                    "Forbidden access for user '" + getUsername() + "', can't delete asset '"
                        + assetId + " + ' of realm: " + serverAsset.getRealm()
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
