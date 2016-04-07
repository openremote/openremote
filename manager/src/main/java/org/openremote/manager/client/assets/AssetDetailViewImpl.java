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
package org.openremote.manager.client.assets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetDetailViewImpl extends Composite implements AssetDetailView {

    private static final Logger LOG = Logger.getLogger(AssetDetailViewImpl.class.getName());

    interface UI extends UiBinder<ScrollPanel, AssetDetailViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    Button sendMessageButton;

    @UiField
    Label messageLabel;

    @UiField
    Button togglePopup;

    final AssetMapPanel assetMapPanel;

    @Inject
    public AssetDetailViewImpl(AssetMapPanel assetMapPanel) {
        this.assetMapPanel = assetMapPanel;
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @UiHandler("sendMessageButton")
    public void onButtonClick(final ClickEvent event) {
        presenter.sendMessage();
    }

    @Override
    public void setMessageText(String text) {
        messageLabel.setText(text);
    }

    @UiHandler("togglePopup")
    public void onToggleButtonClick(final ClickEvent event) {
        if (assetMapPanel.isShowing()) {
            assetMapPanel.hide();
        } else {
            assetMapPanel.show();
        }
    }

}
