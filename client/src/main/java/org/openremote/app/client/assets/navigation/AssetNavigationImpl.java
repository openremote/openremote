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
package org.openremote.app.client.assets.navigation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.app.client.assets.asset.AssetEditPlace;
import org.openremote.app.client.assets.asset.AssetPlace;
import org.openremote.app.client.assets.asset.AssetViewPlace;
import org.openremote.app.client.widget.Hyperlink;

import javax.inject.Inject;

public class AssetNavigationImpl extends Composite implements AssetNavigation {

    interface UI extends UiBinder<HTMLPanel, AssetNavigationImpl> {
    }

    Presenter presenter;

    @UiField
    FlowPanel navItemContainer;

    @UiField
    Hyperlink viewAssetLink;

    @UiField
    Hyperlink editAssetLink;

    @Inject
    public AssetNavigationImpl() {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        viewAssetLink.removeStyleName("active");
        editAssetLink.removeStyleName("active");

        if (presenter == null) {
            navItemContainer.clear();
            viewAssetLink.setTargetHistoryToken("");
            editAssetLink.setTargetHistoryToken("");
            return;
        }

        viewAssetLink.setTargetHistoryToken(presenter.getAssetViewPlaceToken());
        editAssetLink.setTargetHistoryToken(presenter.getAssetEditPlaceToken());

        AssetPlace place = presenter.getActivePlace();
        if (place instanceof AssetViewPlace) {
            viewAssetLink.addStyleName("active");
        } else if (place instanceof AssetEditPlace) {
            editAssetLink.addStyleName("active");
        }
    }

    @Override
    public void addNavItem(IsWidget widget) {
        navItemContainer.add(widget);
    }

    @Override
    public void setEnabled(boolean enabled) {
        viewAssetLink.setVisible(enabled);
        editAssetLink.setVisible(enabled);
    }
}
