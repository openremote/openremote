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
package org.openremote.manager.client.assets.browser;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetInfo;

public interface AssetBrowser extends IsWidget {

    interface Presenter {

        AssetBrowser getView();

        void onViewAttached();

        void onViewDetached();

        void loadAssetChildren(AssetInfo parent, HasData<AssetInfo> display);

        void onAssetSelected(AssetInfo assetInfo);

        void selectAsset(Asset asset);

        void deselectAsset();

        EventRegistration<AssetSelectedEvent> onSelection(EventListener<AssetSelectedEvent> listener);

        void removeRegistration(EventRegistration registration);
    }

    void setPresenter(Presenter presenter);

    void showAndSelectAsset(String[] path, String selectedAssetId, boolean scrollIntoView);

    void deselectAssets();

    void refreshAssets(boolean isRootRefresh);

}
