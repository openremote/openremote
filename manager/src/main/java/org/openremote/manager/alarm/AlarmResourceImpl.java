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
package org.openremote.manager.alarm;

import jakarta.ws.rs.QueryParam;
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
import org.openremote.model.util.TextUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.openremote.model.alarm.Alarm.Source.CLIENT;
import static org.openremote.model.alarm.Alarm.Source.MANUAL;

public class AlarmResourceImpl extends ManagerWebResource implements AlarmResource {

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

    private String validateExistingAlarmId(Long alarmId) {
        validateAlarmId(alarmId);
        SentAlarm alarm = alarmService.getAlarm(alarmId);
        if (alarm == null) {
            throw new WebApplicationException("Alarm does not exist", BAD_REQUEST);
        }

        if (!isRealmActiveAndAccessible(alarm.getRealm())) {
            throw new WebApplicationException("Alarm is in a nonexistent, inactive or inaccessible realm", FORBIDDEN);
        }

        return alarm.getRealm();
    }

    private Set<String> validateExistingAlarmIds(List<Long> alarmIds) {
        alarmIds.forEach(this::validateAlarmId);

        List<SentAlarm> alarms = alarmService.getAlarms(alarmIds);

        if (alarms == null || alarms.size() != alarmIds.size()) {
            throw new WebApplicationException("One or more alarms do not exist", BAD_REQUEST);
        }

        Set<String> realms = alarms.stream().map(SentAlarm::getRealm).collect(Collectors.toSet());

        if (realms.stream().anyMatch(realm -> !isRealmActiveAndAccessible(realm))) {
            throw new WebApplicationException("One or more alarms are in a nonexistent, inactive or inaccessible realm", FORBIDDEN);
        }

        return realms;
    }

    private void validateRealm(String realm) {
        if (!isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException("Realm '" + realm + "' is not active or inaccessible", Status.FORBIDDEN);
        }
    }

    private void validateRealms(Stream<String> realms) {
        realms.forEach(this::validateRealm);
    }

    private List<SentAlarm> filterByActiveAndAccessibleRealms(List<SentAlarm> sentAlarms) {
        return sentAlarms.stream().filter(sa -> isRealmActiveAndAccessible(sa.getRealm())).toList();
    }

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams, String realm, Alarm.Status status, String assetId, String assigneeId) {
        String filterRealm = TextUtil.isNullOrEmpty(realm) ? getAuthenticatedRealm().getName(): realm;
        validateRealm(filterRealm);
        return alarmService.getAlarms(filterRealm, status, assetId, assigneeId).toArray(new SentAlarm[0]);
    }

    @Override
    public void removeAlarms(RequestParams requestParams, List<Long> alarmIds) {
        Set<String> realms = validateExistingAlarmIds(alarmIds);
        alarmService.removeAlarms(alarmIds, realms);
    }

    @Override
    public SentAlarm getAlarm(RequestParams requestParams, Long alarmId) {
        validateExistingAlarmId(alarmId);
        return alarmService.getAlarm(alarmId);
    }

    @Override
    public void removeAlarm(RequestParams requestParams, Long alarmId) {
        String realm = validateExistingAlarmId(alarmId);
        alarmService.removeAlarm(alarmId, realm);
    }

    @Override
    public SentAlarm createAlarm(RequestParams requestParams, Alarm alarm) {
        validateRealm(alarm.getRealm());
        try {
            if (getUserId() != null) {
                alarm.setSource(MANUAL);
                alarm.setSourceId(getUserId());
            }
            else if (getClientId() != null) {
                alarm.setSource(CLIENT);
                alarm.setSourceId(getClientId());
            }
            return alarmService.sendAlarm(alarm);
        } catch (RuntimeException e) {
            throw new WebApplicationException(e.getMessage(), e, Status.BAD_REQUEST);
        }
    }

    @Override
    public void updateAlarm(RequestParams requestParams, Long alarmId, SentAlarm alarm) {
        validateExistingAlarmId(alarmId);
        if (alarm == null) {
            throw new WebApplicationException("Alarm cannot be null", Status.BAD_REQUEST);
        }

        validateRealm(alarm.getRealm());
        alarmService.updateAlarm(alarmId, getUserId(), alarm);
    }

    @Override
    public List<AlarmAssetLink> getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        validateExistingAlarmId(alarmId);
        validateRealm(realm);
        return alarmService.getAssetLinks(alarmId, realm);
    }

    @Override
    public void setAssetLinks(RequestParams requestParams, List<AlarmAssetLink> links) {
        if (links == null || links.isEmpty()) {
            throw new WebApplicationException("Missing links", Status.BAD_REQUEST);
        }

        validateRealms(links.stream().map(link -> link.getId().getRealm()).distinct());
        alarmService.linkAssets(links);
    }

}
