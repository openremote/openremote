/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.client.rules.asset;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.widget.*;

import javax.inject.Inject;
import java.util.logging.Logger;

public class RulesAssetViewImpl extends FormViewImpl implements RulesAssetView {

    private static final Logger LOG = Logger.getLogger(RulesAssetViewImpl.class.getName());

    interface UI extends UiBinder<FlexSplitPanel, RulesAssetViewImpl> {
    }

    interface Style extends CssResource {

        String navItem();

        String formMessages();

    }

    @UiField
    Style style;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    /* ############################################################################ */

    @UiField
    FormGroup nameGroup;
    @UiField
    TextBox nameInput;

    /* ############################################################################ */

    @UiField
    Form submitForm;
    @UiField
    FormGroup submitButtonGroup;
    @UiField
    PushButton createButton;
    @UiField
    PushButton updateButton;
    @UiField
    PushButton deleteButton;

    final AssetBrowser assetBrowser;
    Presenter presenter;

    @Inject
    public RulesAssetViewImpl(AssetBrowser assetBrowser, Provider<ConfirmationDialog> confirmationDialogProvider) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        nameInput.setReadOnly(false);
        nameInput.setValue(null);

        setOpaque(false);

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setFormBusy(boolean busy) {
        super.setFormBusy(busy);
        submitForm.setBusy(busy);
    }

    /* ############################################################################ */

    @Override
    public void setName(String name) {
        nameInput.setValue(name);
    }

    @Override
    public String getName() {
        return nameInput.getValue().length() > 0 ? nameInput.getValue() : null;
    }

    /* ############################################################################ */

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
    }

    @Override
    public void enableDelete(boolean enable) {
        deleteButton.setVisible(enable);
    }

    @UiHandler("updateButton")
    void updateClicked(ClickEvent e) {
        if (presenter != null)
            presenter.update();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.create();
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        if (presenter != null)
            presenter.delete();
    }

    /* ############################################################################ */

    protected void setOpaque(boolean opaque) {
        nameGroup.setOpaque(opaque);
        submitButtonGroup.setOpaque(opaque);
    }
}
