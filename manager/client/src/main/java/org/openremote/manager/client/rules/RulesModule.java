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
import com.google.inject.Singleton;
import org.openremote.manager.client.rules.asset.RulesAssetActivity;
import org.openremote.manager.client.rules.asset.RulesAssetView;
import org.openremote.manager.client.rules.asset.RulesAssetViewImpl;
import org.openremote.manager.client.rules.tenant.RulesTenantActivity;
import org.openremote.manager.client.rules.tenant.RulesTenantView;
import org.openremote.manager.client.rules.tenant.RulesTenantViewImpl;

public class RulesModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(RulesGlobalView.class).to(RulesGlobalViewImpl.class).in(Singleton.class);
        bind(RulesGlobalActivity.class);

        bind(RulesTenantView.class).to(RulesTenantViewImpl.class).in(Singleton.class);
        bind(RulesTenantActivity.class);

        bind(RulesAssetView.class).to(RulesAssetViewImpl.class).in(Singleton.class);
        bind(RulesAssetActivity.class);
    }

}
