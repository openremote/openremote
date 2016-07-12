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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetViewImpl extends Composite implements AssetView {

    private static final Logger LOG = Logger.getLogger(AssetViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, AssetViewImpl> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    SimplePanel assetBrowserContainer;

    @UiField
    SimplePanel assetContentContainer;

    final FormTreeStyle formTreeStyle;

    Presenter presenter;

    @Inject
    public AssetViewImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        assetBrowserContainer.clear();
        assetBrowserContainer.add(presenter.getAssetBrowser());
    }

    @Override
    public void setFormBusy(boolean busy) {
        // TODO
    }

    @Override
    public void setDisplayName(String displayName) {
        assetContentContainer.clear();
        assetContentContainer.add(new Label("SELECTED: " + displayName));
    }
}
