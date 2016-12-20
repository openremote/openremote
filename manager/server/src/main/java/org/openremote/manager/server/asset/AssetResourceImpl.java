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

import org.openremote.container.web.WebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.RequestParams;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class AssetResourceImpl extends WebResource implements AssetResource {

    protected final AssetService assetService;

    public AssetResourceImpl(AssetService assetService) {
        this.assetService = assetService;
    }

    // TODO Restrict access to realms!
    @Override
    public AssetInfo[] getRoot(@BeanParam RequestParams requestParams, String realm) {
        if (realm == null || realm.length() == 0) {
            realm = getAuthenticatedRealm();
        }
        return assetService.getRoot(realm);
    }

    // TODO Restrict access to realms!
    @Override
    public AssetInfo[] getByType(@BeanParam RequestParams requestParams, String assetType) {
        ServerAsset[] assets = assetService.findByTypeInAllRealms(assetType);
        List<AssetInfo> assetInfos = new ArrayList<>();
        for (ServerAsset asset : assets) {
            assetInfos.add(new AssetInfo(asset));
        }
        return assetInfos.toArray(new AssetInfo[assetInfos.size()]);
    }

    // TODO Restrict access to realms!
    @Override
    public AssetInfo[] getChildren(@BeanParam RequestParams requestParams, String parentId) {
        return assetService.getChildren(parentId);
    }

    // TODO Restrict access to realms!
    @Override
    public Asset get(@BeanParam RequestParams requestParams, String assetId) {
        Asset asset = assetService.get(assetId);
        if (asset == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        return asset;
    }

    // TODO Restrict access to realms!
    @Override
    public void update(@BeanParam RequestParams requestParams, String assetId, Asset asset) {
        try {
            // TODO We must limit updates to the same realm, also perform "new parent" checks!
            assetService.merge(
                mapToServerAsset(asset, assetService.get(assetId))
            );
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    // TODO Restrict access to realms!
    @Override
    public void create(@BeanParam RequestParams requestParams, Asset asset) {
        try {
            assetService.merge(
                mapToServerAsset(asset, new ServerAsset())
            );
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    // TODO Restrict access to realms!
    @Override
    public void delete(@BeanParam RequestParams requestParams, String assetId) {
        assetService.delete(assetId);
    }

    protected ServerAsset mapToServerAsset(Asset asset, ServerAsset serverAsset) {
        if (serverAsset == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        return ServerAsset.map(asset, serverAsset);
    }

}
