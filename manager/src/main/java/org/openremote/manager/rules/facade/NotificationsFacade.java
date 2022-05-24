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
package org.openremote.manager.rules.facade;

import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.notification.Notification;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Notifications;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RealmRuleset;

import static org.openremote.model.notification.Notification.Source.ASSET_RULESET;
import static org.openremote.model.notification.Notification.Source.GLOBAL_RULESET;
import static org.openremote.model.notification.Notification.Source.REALM_RULESET;

public class NotificationsFacade<T extends Ruleset> extends Notifications {

    protected final RulesEngineId<T> rulesEngineId;
    protected final NotificationService notificationService;

    public NotificationsFacade(RulesEngineId<T> rulesEngineId, NotificationService notificationService) {
        this.rulesEngineId = rulesEngineId;
        this.notificationService = notificationService;
    }

    public void send(Notification notification) {
        Notification.Source source;
        String sourceId = null;

        if (rulesEngineId.getScope() == GlobalRuleset.class) {
            source = GLOBAL_RULESET;
        } else if (rulesEngineId.getScope() == RealmRuleset.class) {
            source = REALM_RULESET;
            sourceId = rulesEngineId.getRealm().orElseThrow(() -> new IllegalStateException("Realm ruleset must have a realm ID"));
        } else {
            source = ASSET_RULESET;
            sourceId = rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ruleset must have an asset ID"));
        }

        notificationService.sendNotification(notification, source, sourceId);
    }
}
