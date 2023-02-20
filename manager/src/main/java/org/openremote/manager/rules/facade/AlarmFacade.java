package org.openremote.manager.rules.facade;

import org.openremote.manager.alarm.AlarmService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.alarm.Alarm;

public class AlarmFacade<T extends Ruleset> extends Alarm {
    protected final RulesEngineId<T> rulesEngineId;
    protected final AlarmService alarmService;

    public AlarmFacade(RulesEngineId<T> rulesEngineId, AlarmService alarmService) {
        this.rulesEngineId = rulesEngineId;
        this.alarmService = alarmService;
    }

    public void create(Alarm alarm) {

    }
}
