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
package org.openremote.manager.client.assets.asset;

import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.widget.AttributeRefEditor;
import org.openremote.manager.client.widget.FormView;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.interop.Consumer;
import org.openremote.model.util.Pair;

import java.util.List;
import java.util.Map;

public interface AssetEdit extends FormView, AssetBaseView<AssetEdit.Presenter> {

    interface Presenter extends AssetBaseView.Presenter {

        void onParentSelection(BrowserTreeNode treeNode);

        void onMapClicked(double lng, double lat);

        void onAccessPublicRead(boolean enabled);

        void onAssetTypeSelected(AssetType value);

        boolean addAttribute(String name, String type);

        void removeAttribute(AssetAttribute attribute);

        void update();

        void create();

        void delete();

        void getLinkableAssetsAndAttributes(ValueHolder valueHolder, Consumer<Map<AttributeRefEditor.AssetInfo, List<AttributeRefEditor.AttributeInfo>>> assetAttributeConsumer);
    }

    void setPresenter(Presenter presenter);

    String getName();

    void setNameError(boolean error);

    void showMapPopup(double lng, double lat, String text);

    void hideMapPopup();

    void selectWellKnownType(AssetType assetType);

    void setAvailableWellKnownTypes(AssetType[] assetTypes);

    void setType(String type);

    void setTypeEditable(boolean editable);

    String getType();

    void setTypeError(boolean error);

    void setAvailableAttributeTypes(List<Pair<String, String>> displayNamesAndTypes);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);

}
