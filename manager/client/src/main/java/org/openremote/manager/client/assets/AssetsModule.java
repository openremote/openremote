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

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.manager.client.assets.asset.AssetActivity;
import org.openremote.manager.client.assets.asset.AssetView;
import org.openremote.manager.client.assets.asset.AssetViewImpl;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserImpl;
import org.openremote.manager.client.assets.browser.AssetBrowserPresenter;
import org.openremote.manager.shared.ngsi.EntityResource;

public class AssetsModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(AssetBrowser.class).to(AssetBrowserImpl.class).in(Singleton.class);
        bind(AssetBrowser.Presenter.class).to(AssetBrowserPresenter.class).in(Singleton.class);

        bind(AssetsDashboard.class).to(AssetsDashboardImpl.class).in(Singleton.class);
        bind(AssetsDashboardActivity.class);

        bind(AssetView.class).to(AssetViewImpl.class).in(Singleton.class);
        bind(AssetActivity.class);
    }

    @Provides
    @Singleton
    public EntityResource getAssetsResource() {
        return getNativeAssetsResource();
    }

    public static native EntityResource getNativeAssetsResource() /*-{
        return $wnd.AssetsResource;
    }-*/;
}
