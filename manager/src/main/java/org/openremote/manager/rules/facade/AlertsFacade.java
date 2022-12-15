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

import org.openremote.manager.alert.AlertService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.alert.Alert;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Alerts;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;

import static org.openremote.model.alert.Alert.Trigger.ASSET_RULESET;
import static org.openremote.model.alert.Alert.Trigger.GLOBAL_RULESET;
import static org.openremote.model.alert.Alert.Trigger.REALM_RULESET;

public class AlertsFacade<T extends Ruleset> extends Alerts {

    protected final RulesEngineId<T> rulesEngineId;
    protected final AlertService alertService;

    public AlertsFacade(RulesEngineId<T> rulesEngineId, AlertService alertService) {
        this.rulesEngineId = rulesEngineId;
        this.alertService = alertService;
    }

    public void send(Alert alert) {
        Alert.Trigger trigger;
        String triggerId = null;

        if (rulesEngineId.getScope() == GlobalRuleset.class){
            trigger = GLOBAL_RULESET;
        } else if (rulesEngineId.getScope() == RealmRuleset.class){
            trigger = REALM_RULESET;
            triggerId = rulesEngineId.getRealm().orElseThrow(() -> new IllegalStateException("Realm ruleset must have a realm ID"));
        } else {
            trigger = ASSET_RULESET;
            triggerId = rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ruleset must have an asset ID"));
        }

        alertService.createAlert(alert, trigger, triggerId);
    }
}
