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

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.manager.client.rules.asset.AssetRulesList;
import org.openremote.manager.client.rules.asset.AssetRulesListActivity;
import org.openremote.manager.client.rules.asset.AssetRulesListImpl;
import org.openremote.manager.client.rules.global.*;
import org.openremote.manager.client.rules.tenant.TenantRulesList;
import org.openremote.manager.client.rules.tenant.TenantRulesListActivity;
import org.openremote.manager.client.rules.tenant.TenantRulesListImpl;
import org.openremote.manager.shared.rules.RulesResource;

public class RulesModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(GlobalRulesList.class).to(GlobalRulesListImpl.class).in(Singleton.class);
        bind(GlobalRulesListActivity.class);

        bind(GlobalRulesEditor.class).to(GlobalRulesEditorImpl.class).in(Singleton.class);
        bind(GlobalRulesEditorActivity.class);

        bind(TenantRulesList.class).to(TenantRulesListImpl.class).in(Singleton.class);
        bind(TenantRulesListActivity.class);

        bind(AssetRulesList.class).to(AssetRulesListImpl.class).in(Singleton.class);
        bind(AssetRulesListActivity.class);
    }

    @Provides
    @Singleton
    public native RulesResource getRulesResource() /*-{
        return $wnd.RulesResource;
    }-*/;
}
