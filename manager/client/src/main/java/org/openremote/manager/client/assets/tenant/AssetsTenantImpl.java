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
package org.openremote.manager.client.assets.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.client.widget.Headline;

import javax.inject.Inject;

public class AssetsTenantImpl extends Composite implements AssetsTenant {

    interface UI extends UiBinder<FlexSplitPanel, AssetsTenantImpl> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    HTMLPanel sidebarContainer;

    /* ############################################################################ */

    @UiField
    Headline headline;

    final AssetBrowser assetBrowser;
    Presenter presenter;

    @Inject
    public AssetsTenantImpl(AssetBrowser assetBrowser) {
        this.assetBrowser = assetBrowser;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Reset state
        sidebarContainer.clear();
        headline.setText(null);
        headline.setSub(managerMessages.manageTenantAssets());

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setTenantName(String name) {
        headline.setText(name);
    }
}
