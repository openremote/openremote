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
package org.openremote.app.client.map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import elemental2.dom.DomGlobal;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.AppPanel;
import org.openremote.app.client.widget.FlexSplitPanel;
import org.openremote.app.client.widget.Hyperlink;
import org.openremote.app.client.widget.MapWidget;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.geo.GeoJSONFeatureCollection;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.ObjectValue;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

public class MapViewImpl extends Composite implements MapView {

    private static final Logger LOG = Logger.getLogger(MapViewImpl.class.getName());

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
    final MapInfoPanel mapInfoPanel;

    Presenter presenter;

    @Inject
    public MapViewImpl(AssetBrowser assetBrowser, MapInfoPanel mapInfoPanel) {
        this.assetBrowser = assetBrowser;
        this.mapInfoPanel = mapInfoPanel;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        // Resize the map when the splitter is moved
        splitPanel.setOnResize(() -> mapWidget.resize());
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
        showDroppedPin(GeoJSONFeatureCollection.EMPTY);

        mapInfoPanel.close();

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
        mapWidget.initialise(mapOptions, presenter.getSecurity(), () -> {
            mapWidget.addNavigationControl();

            mapInfoPanel.setTarget(mapWidget);
            mapInfoPanel.setPosition(AppPanel.Position.TARGET_BOTTOM_RIGHT);
            mapInfoPanel.setMarginRight(10);
            mapInfoPanel.setMarginBottom(30);

            // The info panel has to move when the map is resized
            mapWidget.addResizeListener(() -> {
                if (mapInfoPanel.isOpen()) {
                    mapInfoPanel.open();
                }
            });

            // Wait until first resize is done to finalize map init
            final MapWidget.ResizeListener initResizeListener = new MapWidget.ResizeListener() {
                @Override
                public void onResize() {
                    mapWidget.removeResizeListener(this);
                    if (presenter != null)
                        presenter.onMapReady();
                }
            };
            mapWidget.addResizeListener(initResizeListener);

            // Resize to fit in viewport
            mapWidget.resize();
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
    public void flyTo(GeoJSONPoint point) {
        if (mapWidget.isMapReady()) {
            mapWidget.flyTo(point);
        }
    }

    @Override
    public void showInfoItems(List<MapInfoItem> infoItems) {
        if (!mapWidget.isMapReady())
            return;
        // TODO There is something wrong with the offset calculation in popup panels, this helps
        DomGlobal.setTimeout(p0 -> {
            mapInfoPanel.setItems(infoItems);
            mapInfoPanel.open();
        }, 100);
    }
}
