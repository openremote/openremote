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
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.rules.AbstractRulesList;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.shared.rules.GlobalRuleset;

import javax.inject.Inject;

public class GlobalRulesListImpl
    extends AbstractRulesList<GlobalRulesList.Presenter, GlobalRuleset>
    implements GlobalRulesList {

    interface UI extends UiBinder<FlexSplitPanel, GlobalRulesListImpl> {
    }

    @Inject
    public GlobalRulesListImpl(AssetBrowser assetBrowser, FormTableStyle formTableStyle) {
        super(assetBrowser, formTableStyle);
    }

    @Override
    protected void initComposite() {
        GlobalRulesListImpl.UI ui = GWT.create(GlobalRulesListImpl.UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    protected void onPresenterReset() {
        super.onPresenterReset();
        headline.setText(managerMessages.manageGlobalRulesets());
    }
}
