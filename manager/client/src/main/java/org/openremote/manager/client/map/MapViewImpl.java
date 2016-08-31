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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import elemental.json.JsonObject;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.client.widget.MapWidget;
import org.openremote.manager.shared.map.GeoJSON;

import javax.inject.Inject;
import java.util.logging.Logger;

public class MapViewImpl extends Composite implements MapView {

    private static final Logger LOG = Logger.getLogger(MapViewImpl.class.getName());

    interface UI extends UiBinder<FlexSplitPanel, MapViewImpl> {
    }

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    Label mapLoadingLabel;

    @UiField
    MapWidget mapWidget;

    final AssetBrowser assetBrowser;

    Presenter presenter;

    @Inject
    public MapViewImpl(AssetBrowser assetBrowser) {
        this.assetBrowser = assetBrowser;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(this::refresh);

        mapLoadingLabel.setVisible(true);
        mapWidget.setVisible(false);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        } else {
            sidebarContainer.clear();
            hideFeaturesAll();
            hideFeaturesSelection();
        }
    }

    @Override
    public void initialiseMap(JsonObject mapOptions) {
        mapLoadingLabel.setVisible(false);
        Scheduler.get().scheduleDeferred(() -> {
            mapWidget.initialise(mapOptions);
            mapWidget.addNavigationControl();
            mapWidget.setVisible(true);
            mapWidget.resize();
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void refresh() {
        mapWidget.resize();
    }

    @Override
    public void showFeaturesAll(GeoJSON mapFeatures) {
        mapWidget.showFeatures(MapWidget.FEATURES_SOURCE_ALL, mapFeatures);
    }

    @Override
    public void hideFeaturesAll() {
        showFeaturesAll(GeoJSON.EMPTY_FEATURE_COLLECTION);
    }

    @Override
    public void showFeaturesSelection(GeoJSON mapFeatures) {
        mapWidget.showFeatures(MapWidget.FEATURES_SOURCE_SELECTION, mapFeatures);
    }

    @Override
    public void hideFeaturesSelection() {
        showFeaturesSelection(GeoJSON.EMPTY_FEATURE_COLLECTION);
    }

    @Override
    public void flyTo(double[] coordinates) {
        mapWidget.flyTo(coordinates);
    }
}
