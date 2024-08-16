package org.openremote.manager.alarm;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.AlarmResource;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.http.RequestParams;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import java.util.List;

public class AlarmResourceImpl extends ManagerWebResource implements AlarmResource {

    private static final String INVALID_CRITERIA_SET = "Invalid criteria set";

    private final AlarmService alarmService;

    public AlarmResourceImpl(TimerService timerService,
                             ManagerIdentityService identityService,
                             AlarmService alarmService) {
        super(timerService, identityService);
        this.alarmService = alarmService;
    }

    private void validateAlarmId(Long alarmId) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        if (alarmId < 0) {
            throw new WebApplicationException("Alarm ID cannot be negative", Status.BAD_REQUEST);
        }
    }

    private void validateRealm(String realm) {
        if (realm == null) {
            throw new WebApplicationException("Realm cannot be null", Status.BAD_REQUEST);
        }
        if (!realm.isBlank() && !isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException("Realm '" + realm + "' is not active or inaccessible", Status.FORBIDDEN);
        }
    }

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams) {
        try {
            return alarmService.getAlarms(getAuthenticatedRealmName()).toArray(new SentAlarm[0]);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(INVALID_CRITERIA_SET, Status.BAD_REQUEST);
        }
    }

    @Override
    public void removeAlarms(RequestParams requestParams, List<Long> ids) {
        try {
            alarmService.removeAlarms(ids, getAuthenticatedRealmName());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(INVALID_CRITERIA_SET, Status.BAD_REQUEST);
        }
    }

    @Override
    public void removeAlarm(RequestParams requestParams, Long alarmId) {
        validateAlarmId(alarmId);
        alarmService.removeAlarm(alarmId, getAuthenticatedRealmName());
    }

    @Override
    public SentAlarm createAlarm(RequestParams requestParams, Alarm alarm) {
        SentAlarm success = alarmService.sendAlarm(alarm);
        if (success.getId() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return success;
    }

    @Override
    public SentAlarm createAlarmWithSource(RequestParams requestParams, Alarm alarm, Alarm.Source source, String sourceId) {
        SentAlarm success = alarmService.sendAlarm(alarm, source, sourceId);
        if (success.getId() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return success;
    }

    @Override
    public void updateAlarm(RequestParams requestParams, Long alarmId, SentAlarm alarm) {
        validateAlarmId(alarmId);
        if (alarm == null) {
            throw new WebApplicationException("Alarm cannot be null", Status.BAD_REQUEST);
        }

        alarmService.updateAlarm(alarmId, alarm);
    }

    @Override
    public List<SentAlarm> getAlarmsByAssetId(RequestParams requestParams, String assetId) {
        if (assetId == null) {
            throw new WebApplicationException("Missing asset ID", Status.BAD_REQUEST);
        }
        return alarmService.getAlarmsByAssetId(assetId);
    }

    @Override
    public List<SentAlarm> getOpenAlarms(RequestParams requestParams) {
        try {
            return alarmService.getOpenAlarms();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(INVALID_CRITERIA_SET, Status.BAD_REQUEST);
        }
    }

    @Override
    public List<AlarmAssetLink> getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        validateAlarmId(alarmId);
        validateRealm(realm);
        return alarmService.getAssetLinks(alarmId, realm);
    }

    @Override
    public void setAssetLinks(RequestParams requestParams, List<AlarmAssetLink> links) {
        if (links == null || links.isEmpty()) {
            throw new WebApplicationException("Missing links", Status.BAD_REQUEST);
        }
        alarmService.linkAssets(links);
    }

}
