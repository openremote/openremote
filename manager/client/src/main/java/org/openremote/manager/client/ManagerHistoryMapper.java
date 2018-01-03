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
package org.openremote.manager.client;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import org.openremote.manager.client.admin.syslog.AdminSyslogPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantsPlace;
import org.openremote.manager.client.admin.users.AdminUsersPlace;
import org.openremote.manager.client.admin.users.edit.AdminUserEditPlace;
import org.openremote.manager.client.admin.users.notifications.AdminUserNotificationsPlace;
import org.openremote.manager.client.apps.ConsoleAppsPlace;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.asset.AssetEditPlace;
import org.openremote.manager.client.assets.asset.AssetViewPlace;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.map.MapAssetPlace;
import org.openremote.manager.client.map.MapTenantPlace;
import org.openremote.manager.client.rules.asset.AssetRulesEditorPlace;
import org.openremote.manager.client.rules.asset.AssetRulesListPlace;
import org.openremote.manager.client.rules.global.GlobalRulesEditorPlace;
import org.openremote.manager.client.rules.global.GlobalRulesListPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesEditorPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.client.user.UserAccountPlace;

@WithTokenizers(
    { // You might have to restart SuperDevMode after changing tokenizers
        MapAssetPlace.Tokenizer.class,
        MapTenantPlace.Tokenizer.class,
        AssetsDashboardPlace.Tokenizer.class,
        AssetsTenantPlace.Tokenizer.class,
        AssetViewPlace.Tokenizer.class,
        AssetEditPlace.Tokenizer.class,
        GlobalRulesListPlace.Tokenizer.class,
        GlobalRulesEditorPlace.Tokenizer.class,
        TenantRulesListPlace.Tokenizer.class,
        TenantRulesEditorPlace.Tokenizer.class,
        AssetRulesListPlace.Tokenizer.class,
        AssetRulesEditorPlace.Tokenizer.class,
        ConsoleAppsPlace.Tokenizer.class,
        AdminSyslogPlace.Tokenizer.class,
        AdminTenantsPlace.Tokenizer.class,
        AdminTenantPlace.Tokenizer.class,
        AdminUsersPlace.Tokenizer.class,
        AdminUserEditPlace.Tokenizer.class,
        AdminUserNotificationsPlace.Tokenizer.class,
        UserAccountPlace.Tokenizer.class
    }
)
public interface ManagerHistoryMapper extends PlaceHistoryMapper {
}
