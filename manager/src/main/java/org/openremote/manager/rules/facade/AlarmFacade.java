package org.openremote.manager.rules.facade;

import java.util.List;

import org.openremote.manager.alarm.AlarmService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.Alarm.Source;
import org.openremote.model.rules.Alarms;

public class AlarmFacade<T extends Ruleset> extends Alarms {
    protected final RulesEngineId<T> rulesEngineId;
    protected final AlarmService alarmService;

    public AlarmFacade(RulesEngineId<T> rulesEngineId, AlarmService alarmService) {
        this.rulesEngineId = rulesEngineId;
        this.alarmService = alarmService;
    }

    public Long create(Alarm alarm, String userId) {
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
        alarm.setRealm(rulesEngineId.getRealm().orElseThrow());
        SentAlarm result = alarmService.sendAlarm(alarm, source, sourceId);
        Long id = result.getId();
        if(id != null){
            if(userId != null && !userId.isEmpty()){
                alarmService.assignUser(id, userId);
            }
            return id;
        }
        return null;
    }

    public void linkAssets(List<String> assetIds, Long alarmId){
        if(assetIds != null && !assetIds.isEmpty()){
            String realmId = rulesEngineId.getRealm().orElseThrow(() -> new IllegalStateException("Realm ruleset must have a realm ID"));
            alarmService.linkAssets(assetIds, realmId, alarmId);
        }
    }

}
