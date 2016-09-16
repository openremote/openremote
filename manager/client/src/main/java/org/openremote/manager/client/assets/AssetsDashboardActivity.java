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

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.asset.AssetResource;

import javax.inject.Inject;

public class AssetsDashboardActivity
    extends AssetBrowsingActivity<AssetsDashboard, AssetsDashboardPlace>
    implements AssetsDashboard.Presenter {

    @Inject
    public AssetsDashboardActivity(Environment environment,
                                   AssetsDashboard view,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   AssetResource assetResource,
                                   AssetMapper assetMapper) {
        super(environment, view, assetBrowserPresenter, assetResource, assetMapper);
    }

    @Override
    protected void onAssetLoaded() {
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(AssetInfo selectedAssetInfo) {
        environment.getPlaceController().goTo(new AssetPlace(selectedAssetInfo.getId()));
    }

    @Override
    protected void onTenantSelected(String id, String realm) {

    }
}
