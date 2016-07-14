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
import org.openremote.manager.client.assets.asset.Asset;

import java.util.List;

public interface AssetBrowser extends IsWidget {

    interface Presenter {

        AssetBrowser getView();

        void loadAssetChildren(Asset parent, HasData<Asset> display);

        void onAssetSelected(Asset asset);

        void selectAsset(String id);
    }

    void setPresenter(Presenter presenter);

    void showAndSelectAsset(List<String> path, String selectedAssetId);

    void deselectAssets();
}
