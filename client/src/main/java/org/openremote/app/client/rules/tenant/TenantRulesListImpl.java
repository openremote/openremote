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
package org.openremote.app.client.rules.tenant;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.rules.AbstractRulesList;
import org.openremote.app.client.style.FormTableStyle;
import org.openremote.app.client.widget.FlexSplitPanel;
import org.openremote.model.rules.TenantRuleset;

import javax.inject.Inject;

public class TenantRulesListImpl
    extends AbstractRulesList<TenantRulesList.Presenter, TenantRuleset>
    implements TenantRulesList {

    interface UI extends UiBinder<FlexSplitPanel, TenantRulesListImpl> {
    }

    @Inject
    public TenantRulesListImpl(AssetBrowser assetBrowser, FormTableStyle formTableStyle) {
        super(assetBrowser, formTableStyle);
    }

    @Override
    protected void initComposite() {
        TenantRulesListImpl.UI ui = GWT.create(TenantRulesListImpl.UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setRealmLabel(String label) {
        headline.setText(label);
    }
}
