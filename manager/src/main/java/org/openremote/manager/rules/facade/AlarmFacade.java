package org.openremote.manager.rules.facade;

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

    public void create(Alarm alarm) {
        Alarm.Source source;
        String sourceId = null;

        if (rulesEngineId.getScope() == GlobalRuleset.class) {
            source = Source.GLOBAL_RULESET;
        } else if (rulesEngineId.getScope() == RealmRuleset.class) {
            source = Source.REALM_RULESET;
            sourceId = rulesEngineId.getRealm().orElseThrow(() -> new IllegalStateException("Realm ruleset must have a realm ID"));
        } else {
            source = Source.ASSET_RULESET;
            sourceId = rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ruleset must have an asset ID"));
        }

        alarmService.sendAlarm(alarm, source, sourceId);
    }
}
