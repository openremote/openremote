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
import elemental.json.JsonObject;
import gwt.material.design.client.ui.MaterialLoader;

import javax.inject.Inject;

public class MapViewImpl extends Composite implements MapView {

    interface UI extends UiBinder<MapWidget, MapViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    MapWidget mapWidget;

    @Inject
    public MapViewImpl() {
        initWidget(ui.createAndBindUi(this));
        MaterialLoader.showLoading(true);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void initialiseMap(JsonObject mapOptions) {
        mapWidget.initialise(mapOptions);
        MaterialLoader.showLoading(false);
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }
}
