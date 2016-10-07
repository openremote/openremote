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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Provider;
import elemental.json.JsonObject;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.client.widget.MapWidget;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.map.GeoJSON;

import javax.inject.Inject;
import java.util.logging.Logger;

public class MapViewImpl extends Composite implements MapView {

    private static final Logger LOG = Logger.getLogger(MapViewImpl.class.getName());

    interface UI extends UiBinder<FlexSplitPanel, MapViewImpl> {
    }

    public interface Style extends CssResource {

        String mapLoadingLabel();

        String navItem();

        String mapControls();

        String mapWidget();

        String infoPanel1();

        String infoPanel2();
    }

    @UiField
    Style style;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    Label mapLoadingLabel;

    @UiField
    MapWidget mapWidget;

    @UiField
    PushButton fullscreenButton;

    final AssetBrowser assetBrowser;
    final Provider<MapInfoPanel> infoPanelProvider;
    final MapInfoPanel infoPanel1;
    final MapInfoPanel infoPanel2;

    Presenter presenter;

    @Inject
    public MapViewImpl(AssetBrowser assetBrowser, Provider<MapInfoPanel> infoPanelProvider) {
        this.assetBrowser = assetBrowser;
        this.infoPanelProvider = infoPanelProvider;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(this::refresh);

        mapLoadingLabel.setVisible(true);
        mapWidget.setVisible(false);

        infoPanel1 = infoPanelProvider.get();
        infoPanel1.addStyleName(style.infoPanel1());
        infoPanel2 = infoPanelProvider.get();
        infoPanel2.addStyleName(style.infoPanel2());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
            if (isMapInitialised()) {
                showInfoPanels();
            }
        } else {
            sidebarContainer.clear();
            hideFeaturesAll();
            hideFeaturesSelection();
            hideInfoPanels();
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
            showInfoPanels();
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void refresh() {
        mapWidget.resize();
        showInfoPanels();
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

    @UiHandler("fullscreenButton")
    public void onFullscreenClicked(ClickEvent e) {
        toggleFullscreen();
    }

    protected void showInfoPanels() {
        infoPanel1.showTopLeftOf(mapWidget, 10, 10);
        infoPanel2.showBottomRightOf(mapWidget, 10, 30);
    }

    protected void hideInfoPanels() {
        infoPanel1.hide();
        infoPanel2.hide();
    }

    protected native void toggleFullscreen() /*-{
        var doc = $wnd.document;
        if (!doc.fullscreenElement && !doc.mozFullScreenElement && !doc.webkitFullscreenElement && !doc.msFullscreenElement) {
            if (doc.documentElement.requestFullscreen) {
                doc.documentElement.requestFullscreen();
            } else if (doc.documentElement.msRequestFullscreen) {
                doc.documentElement.msRequestFullscreen();
            } else if (doc.documentElement.mozRequestFullScreen) {
                doc.documentElement.mozRequestFullScreen();
            } else if (doc.documentElement.webkitRequestFullscreen) {
                doc.documentElement.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT);
            }
        } else {
            if (doc.exitFullscreen) {
                doc.exitFullscreen();
            } else if (doc.msExitFullscreen) {
                doc.msExitFullscreen();
            } else if (doc.mozCancelFullScreen) {
                doc.mozCancelFullScreen();
            } else if (doc.webkitExitFullscreen) {
                doc.webkitExitFullscreen();
            }
        }

    }-*/;
}
