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
package org.openremote.manager.client.rules.global;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.inject.Provider;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.rules.AbstractRulesEditor;
import org.openremote.manager.client.widget.FlexSplitPanel;

import javax.inject.Inject;

public class GlobalRulesEditorImpl
    extends AbstractRulesEditor<GlobalRulesEditor.Presenter>
    implements GlobalRulesEditor {

    interface UI extends UiBinder<FlexSplitPanel, GlobalRulesEditorImpl> {
    }

    @Inject
    public GlobalRulesEditorImpl(Provider<ConfirmationDialog> confirmationDialogProvider, AssetBrowser assetBrowser) {
        super(confirmationDialogProvider, assetBrowser);

        GlobalRulesEditorImpl.UI ui = GWT.create(GlobalRulesEditorImpl.UI.class);
        initWidget(ui.createAndBindUi(this));
    }

}
