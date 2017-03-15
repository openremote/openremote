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
package org.openremote.manager.client.rules;

import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.widget.FormGroup;
import org.openremote.manager.client.widget.FormInputText;
import org.openremote.manager.client.widget.FormViewImpl;

public abstract class AbstractRulesEditor<P extends RulesEditor.Presenter>
    extends FormViewImpl
    implements RulesEditor<P> {

    @UiField
    public HTMLPanel sidebarContainer;

    @UiField
    public FormGroup nameGroup;
    @UiField
    public FormInputText nameInput;

    final AssetBrowser assetBrowser;
    protected P presenter;

    public AbstractRulesEditor(Provider<ConfirmationDialog> confirmationDialogProvider, AssetBrowser assetBrowser) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;
    }

    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        } else {
            sidebarContainer.clear();

            // Restore initial state of view
            nameInput.setValue(null);

        }
    }

    @Override
    public void setName(String name) {
        nameInput.setValue(name);
    }

    @Override
    public String getName() {
        return nameInput.getValue().length() > 0 ? nameInput.getValue() : null;
    }

}
