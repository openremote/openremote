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
package org.openremote.manager.client.map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Provider;
import elemental.client.Browser;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.client.widget.Hyperlink;
import org.openremote.manager.client.widget.MapWidget;
import org.openremote.model.util.Pair;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.value.ObjectValue;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<FlexSplitPanel, MapViewImpl> {
    }

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    Hyperlink viewAssetLink;

    @UiField
    MapWidget mapWidget;

    final AssetBrowser assetBrowser;
    final Provider<MapInfoPanel> infoPanelProvider;
    MapInfoPanel infoPanel;

    Presenter presenter;

    @Inject
    public MapViewImpl(AssetBrowser assetBrowser, Provider<MapInfoPanel> infoPanelProvider) {
        this.assetBrowser = assetBrowser;
        this.infoPanelProvider = infoPanelProvider;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(() -> {
            mapWidget.resize();
            if (infoPanel != null)
                infoPanel.resize();
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Reset state
        sidebarContainer.clear();
/*
        viewAssetLink.setVisible(false);
        viewAssetLink.setTargetHistoryToken("");
*/
        showDroppedPin(GeoJSON.EMPTY_FEATURE_COLLECTION);
        if (infoPanel != null) {
            infoPanel.hide();
            infoPanel = null;
        }

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setAssetViewHistoryToken(String token) {
        if (token != null) {
            viewAssetLink.setTargetHistoryToken(token);
            viewAssetLink.setVisible(true);
        }
    }

    @Override
    public void initialiseMap(ObjectValue mapOptions) {
        mapWidget.initialise(mapOptions, () -> {
            mapWidget.addNavigationControl();
            if (presenter != null)
                presenter.onMapReady();
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void showDroppedPin(GeoJSON geoFeature) {
        if (mapWidget.isMapReady()) {
            mapWidget.showFeature(MapWidget.FEATURE_SOURCE_DROPPED_PIN, geoFeature);
        }
    }

    @Override
    public void flyTo(double[] coordinates) {
        if (mapWidget.isMapReady()) {
            mapWidget.flyTo(coordinates);
        }
    }

    @Override
    public void showInfoItems(List<MapInfoItem> infoItems) {
        if (!mapWidget.isMapReady())
            return;

        if (infoPanel == null) {
            infoPanel = infoPanelProvider.get();
        }

        infoPanel.setItems(infoItems);

        mapWidget.resize();
        Browser.getWindow().setTimeout(
            () -> infoPanel.showBottomRightOf(mapWidget, 10, 30), 100
        );
    }
}
