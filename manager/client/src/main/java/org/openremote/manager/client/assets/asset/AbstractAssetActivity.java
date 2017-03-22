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
package org.openremote.manager.client.assets.asset;

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.model.asset.Asset;

public abstract class AbstractAssetActivity<PLACE extends AssetPlace> extends AssetBrowsingActivity<PLACE> {

    protected String assetId;
    protected Asset asset;
    protected Asset parentAsset;

    public AbstractAssetActivity(Environment environment, AssetBrowser.Presenter assetBrowserPresenter) {
        super(environment, assetBrowserPresenter);
    }

    @Override
    protected AppActivity<PLACE> init(PLACE place) {
        this.assetId = place.getAssetId();
        return this;
    }
}
