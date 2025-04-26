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

    private <T> T mapExceptions(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (SecurityException e) {
            throw new ForbiddenException(e.getMessage(), e);
        }
    }

    private void mapExceptions(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (SecurityException e) {
            throw new ForbiddenException(e.getMessage(), e);
        }
    }

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams, String realm, Alarm.Status status, String assetId, String assigneeId) {
        String filterRealm = TextUtil.isNullOrEmpty(realm) ? getAuthenticatedRealm().getName() : realm;
        if (!isRealmActiveAndAccessible(filterRealm) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + filterRealm + "' is not active or inaccessible");
        }
        return mapExceptions(() -> alarmService.getAlarms(filterRealm, status, assetId, assigneeId).toArray(new SentAlarm[0]));
    }

    @Override
    public void removeAlarms(RequestParams requestParams, List<Long> alarmIds) {
        if (!isRealmAccessibleByUser(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        mapExceptions(() -> alarmService.removeAlarms(alarmIds));
    }

    @Override
    public SentAlarm getAlarm(RequestParams requestParams, Long alarmId) {
        if (!isRealmAccessibleByUser(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        return mapExceptions(() -> alarmService.getAlarm(alarmId));
    }

    @Override
    public void removeAlarm(RequestParams requestParams, Long alarmId) {
        if (!isRealmAccessibleByUser(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        mapExceptions(() -> alarmService.removeAlarm(alarmId));
    }

    @Override
    public SentAlarm createAlarm(RequestParams requestParams, Alarm alarm, List<String> assetIds) {
        if (getUserId() != null) {
            alarm.setSource(MANUAL);
            alarm.setSourceId(getUserId());
        } else if (getClientId() != null) {
            alarm.setSource(CLIENT);
            alarm.setSourceId(getClientId());
        }
        if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        return mapExceptions(() -> alarmService.sendAlarm(alarm, assetIds));
    }

    @Override
    public void updateAlarm(RequestParams requestParams, Long alarmId, SentAlarm alarm) {
        if (!isRealmAccessibleByUser(alarm.getRealm()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        mapExceptions(() -> alarmService.updateAlarm(alarmId, alarm));
    }

    @Override
    public List<AlarmAssetLink> getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        if (!isRealmAccessibleByUser(realm) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        return mapExceptions(() -> alarmService.getAssetLinks(alarmId, realm));
    }

    @Override
    public void setAssetLinks(RequestParams requestParams, List<AlarmAssetLink> links) {
        if (!isRealmAccessibleByUser(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new ForbiddenException("Realm '" + getAuthenticatedRealmName() + "' is not active or inaccessible");
        }
        mapExceptions(() -> alarmService.linkAssets(links));
    }

}
