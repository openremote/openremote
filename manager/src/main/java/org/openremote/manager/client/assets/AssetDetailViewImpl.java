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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;

public class AssetDetailViewImpl extends Composite implements AssetDetailView {

    interface UI extends UiBinder<HTMLPanel, AssetDetailViewImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    com.google.gwt.user.client.ui.PushButton sendMessageButton;

    @UiField
    Label messageLabel;

    @UiField
    PushButton showInfo;

    @UiField
    PushButton showTempFailure;

    @UiField
    PushButton showDurableFailure;

    @Inject
    public AssetDetailViewImpl() {
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

    @UiHandler("showInfo")
    public void onShowInfo(final ClickEvent event) {
        presenter.showInfo();
    }

    @UiHandler("showTempFailure")
    public void onShowTempFailure(final ClickEvent event) {
        presenter.showTempFailure();
    }

    @UiHandler("showDurableFailure")
    public void onShowDurableFailure(final ClickEvent event) {
        presenter.showDurableFailure();
    }

    @Override
    public void setMessageText(String text) {
        messageLabel.setText(text);
    }
}
