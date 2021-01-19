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
package org.openremote.manager.asset;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetModelResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

public class AssetModelResourceImpl extends ManagerWebResource implements AssetModelResource {

    protected AssetModelService assetModelService;

    public AssetModelResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetModelService assetModelService) {
        super(timerService, identityService);
        this.assetModelService = assetModelService;
    }

    @Override
    public AssetTypeInfo[] getAssetInfos(RequestParams requestParams, String parentId, String parentType) {
        return assetModelService.getAssetInfos(parentId, parentType);
    }

    @Override
    public AssetTypeInfo getAssetInfo(RequestParams requestParams, String parentId, String assetType) {
        return assetModelService.getAssetInfo(parentId, assetType);
    }

    @Override
    public AssetDescriptor<?>[] getAssetDescriptors(RequestParams requestParams, String parentId, String parentType) {
        return assetModelService.getAssetDescriptors(parentId, parentType);
    }

    @Override
    public ValueDescriptor<?>[] getValueDescriptors(RequestParams requestParams, String parentId) {
        return assetModelService.getValueDescriptors(parentId);
    }

    @Override
    public MetaItemDescriptor<?>[] getMetaItemDescriptors(RequestParams requestParams, String parentId) {
        return assetModelService.getMetaItemDescriptors(parentId);
    }
}
