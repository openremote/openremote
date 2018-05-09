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
package org.openremote.app.client.rules;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.model.event.bus.EventListener;
import org.openremote.app.client.rules.asset.AssetRulesList;
import org.openremote.app.client.rules.asset.AssetRulesListActivity;
import org.openremote.app.client.rules.asset.AssetRulesListImpl;
import org.openremote.app.client.rules.asset.AssetRulesListPlace;
import org.openremote.app.client.rules.global.*;
import org.openremote.app.client.rules.tenant.*;
import org.openremote.model.rules.RulesResource;

public class RulesModule extends AbstractGinModule {

    public static EventListener<AssetBrowserSelection> createDefaultNavigationListener(Environment environment) {
        return event -> {
            if (event.getSelectedNode() instanceof TenantTreeNode) {
                environment.getPlaceController().goTo(
                    new TenantRulesListPlace(event.getSelectedNode().getId())
                );
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                environment.getPlaceController().goTo(
                    new AssetRulesListPlace(event.getSelectedNode().getId())
                );
            }
        };
    }

    @Override
    protected void configure() {
        bind(GlobalRulesList.class).to(GlobalRulesListImpl.class).in(Singleton.class);
        bind(GlobalRulesListActivity.class);

        bind(TenantRulesList.class).to(TenantRulesListImpl.class).in(Singleton.class);
        bind(TenantRulesListActivity.class);

        bind(AssetRulesList.class).to(AssetRulesListImpl.class).in(Singleton.class);
        bind(AssetRulesListActivity.class);

        bind(RulesEditor.class).to(RulesEditorImpl.class).in(Singleton.class);
        bind(GlobalRulesEditorActivity.class);
        bind(TenantRulesEditorActivity.class);
    }

    @Provides
    @Singleton
    public native RulesResource getRulesetResource() /*-{
        return $wnd.openremote.REST.RulesetResource;
    }-*/;
}
