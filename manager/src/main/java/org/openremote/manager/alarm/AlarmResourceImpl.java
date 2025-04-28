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

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.AlarmResource;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.http.RequestParams;

import org.openremote.model.util.TextUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams, String realm, Alarm.Status status, String assetId, String assigneeId) {
        try {
            String filterRealm = TextUtil.isNullOrEmpty(realm) ? getAuthenticatedRealm().getName() : realm;

            if (!isRealmActiveAndAccessible(filterRealm) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + filterRealm + "' is not active or inaccessible");
            }
           return alarmService.getAlarms(filterRealm, status, assetId, assigneeId).toArray(new SentAlarm[0]);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public void removeAlarms(RequestParams requestParams, List<Long> alarmIds) {
        try {
            List<SentAlarm> alarms = alarmService.getAlarms(alarmIds);
            alarms.forEach(alarm -> {
                if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
                    throw new ForbiddenException("Realm '" + alarm.getRealm() + "' is not active or inaccessible");
                }
            });
            alarmService.removeAlarms(alarms, alarmIds);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public SentAlarm getAlarm(RequestParams requestParams, Long alarmId) {
        try {
            SentAlarm alarm = alarmService.getAlarm(alarmId);
            if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + alarm.getRealm() + "' is not active or inaccessible");
            }
            return alarmService.getAlarm(alarmId);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public void removeAlarm(RequestParams requestParams, Long alarmId) {
        try {
            SentAlarm alarm = alarmService.getAlarm(alarmId);
            if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + alarm.getRealm() + "' is not active or inaccessible");
            }
            alarmService.removeAlarm(alarm);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public SentAlarm createAlarm(RequestParams requestParams, Alarm alarm, List<String> assetIds) {
        try {
            if (getUserId() != null) {
                alarm.setSource(MANUAL);
                alarm.setSourceId(getUserId());
            } else if (getClientId() != null) {
                alarm.setSource(CLIENT);
                alarm.setSourceId(getClientId());
            }

            if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + alarm.getRealm() + "' is not active or inaccessible");
            }
            return alarmService.sendAlarm(alarm, assetIds);
        } catch (NullPointerException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public void updateAlarm(RequestParams requestParams, Long alarmId, SentAlarm newAlarm) {
        try {
            SentAlarm oldAlarm = alarmService.getAlarm(alarmId);
            if (!isRealmAccessibleByUser(oldAlarm.getRealm()) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + oldAlarm.getRealm() + "' is not active or inaccessible");
            }
            alarmService.updateAlarm(oldAlarm, newAlarm);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public List<AlarmAssetLink> getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        try {
            SentAlarm alarm = alarmService.getAlarm(alarmId);
            if (!isRealmAccessibleByUser(realm) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + realm + "' is not active or inaccessible");
            }
            return alarmService.getAssetLinks(alarm, realm);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public void setAssetLinks(RequestParams requestParams, List<AlarmAssetLink> links) {
        try {
            if (links == null || links.isEmpty()) {
                throw new IllegalArgumentException("Missing links");
            }
            Set<String> realms = links.stream().map(link -> link.getId().getRealm()).collect(Collectors.toSet());

            if (!isRealmAccessibleByUser(realms.stream().findFirst().orElse(null)) && !isSuperUser()) {
                throw new ForbiddenException("Realm '" + realms.stream().findFirst() + "' is not active or inaccessible");
            }
            alarmService.linkAssets(links);
        } catch (EntityNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

}
