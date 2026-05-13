/*
 * Copyright 2024, OpenRemote Inc.
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

import java.util.List;

import org.openremote.manager.alarm.AlarmService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.Alarm.Source;
import org.openremote.model.rules.Alarms;

public class AlarmFacade<T extends Ruleset> extends Alarms {
    protected final RulesEngineId<T> rulesEngineId;
    protected final AlarmService alarmService;

    public AlarmFacade(RulesEngineId<T> rulesEngineId, AlarmService alarmService) {
        this.rulesEngineId = rulesEngineId;
        this.alarmService = alarmService;
    }

    public Long create(Alarm alarm, List<String> assetIds) {
        Alarm.Source source;

        if (rulesEngineId.getScope() == GlobalRuleset.class) {
            source = Source.GLOBAL_RULESET;
        } else if (rulesEngineId.getScope() == RealmRuleset.class) {
            source = Source.REALM_RULESET;
        } else {
            source = Source.ASSET_RULESET;
        }

        alarm.setRealm(rulesEngineId.getRealm().orElseThrow());
        alarm.setSource(source);

        return alarmService.sendAlarm(alarm, assetIds).getId();
    }

    public void linkAssets(List<String> assetIds, Long alarmId) {
        if (assetIds != null && !assetIds.isEmpty()) {
            String realmId = rulesEngineId.getRealm().orElseThrow(() -> new IllegalStateException("Realm ruleset must have a realm ID"));
            alarmService.linkAssets(assetIds, realmId, alarmId);
        }
    }

}
