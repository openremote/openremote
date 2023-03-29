package org.openremote.manager.alarm;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.AlarmResource;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.Alarm.Source;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.http.RequestParams;

import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AlarmResourceImpl extends WebResource implements AlarmResource {
    private static final Logger LOG = Logger.getLogger(AlarmResourceImpl.class.getName());

    final protected AlarmService alarmService;
    final protected MessageBrokerService messageBrokerService;
    final protected AssetStorageService assetStorageService;

    final ManagerIdentityService managerIdentityService;

    public AlarmResourceImpl(AlarmService alarmService,
                             MessageBrokerService messageBrokerService,
                             AssetStorageService assetStorageService,
                             ManagerIdentityService managerIdentityService) {
        this.alarmService = alarmService;
        this.messageBrokerService = messageBrokerService;
        this.assetStorageService = assetStorageService;
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams) {
        try{
            return alarmService.getAlarms().toArray(new SentAlarm[0]);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", Status.BAD_REQUEST);
        }
    }

//    @Override
//    public void removeAlerts(RequestParams requestParams, Long id, String severity, String status) {
//        try{
//            alertService.removeAlerts(
//                    id != null ? Collections.singletonList(id) : null,
//                    severity,
//                    status
//            );
//        } catch (IllegalArgumentException e) {
//            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
//        }
//    }
//
//    @Override
//    public  void removeAlert(RequestParams requestParams, Long alertId) {
//        if (alertId == null) {
//            throw new WebApplicationException("Missing alert ID", BAD_REQUEST);
//        }
//        alertService.removeAlert(alertId);
//    }

    @Override
    public void createAlarm(RequestParams requestParams, Alarm alarm) {
        SentAlarm success = alarmService.sendAlarm(alarm);

        if (success.getId() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    @Override
    public void updateAlarm(RequestParams requestParams, Long alarmId, Alarm alarm) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        alarmService.updateAlarm(alarmId, alarm);
    }

    @Override
    public void setAlarmAcknowledged(RequestParams requestParams, String alarmId) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        alarmService.setAlarmAcknowledged(alarmId);
    }

    @Override
    public void setAlarmStatus(RequestParams requestParams, String alarmId, String status) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        alarmService.updateAlarmStatus(alarmId, status);
    }

    @Override
    public void assignUser(RequestParams requestParams, String alarmId, String userId, String realm) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        if (userId == null) {
            throw new WebApplicationException("Missing user ID", Status.BAD_REQUEST);
        }
        if (realm == null) {
            throw new WebApplicationException("Missing realm", Status.BAD_REQUEST);
        }
        alarmService.assignUser(alarmId, userId, realm);
    }

    @Override
    public void setAssetLink(RequestParams requestParams, String alarmId, String assetId, String realm) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        if (assetId == null) {
            throw new WebApplicationException("Missing asset ID", Status.BAD_REQUEST);
        }
        if (realm == null) {
            throw new WebApplicationException("Missing realm", Status.BAD_REQUEST);
        }
        //alarmService.setAssetLink(alarmId, assetId, realm);
    }
    
    @Override
    public AlarmAssetLink[] getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        if (alarmId == null) {
            throw new WebApplicationException("Missing alarm ID", Status.BAD_REQUEST);
        }
        if (realm == null) {
            throw new WebApplicationException("Missing realm", Status.BAD_REQUEST);
        }
        return alarmService.getAssetLinks(alarmId, realm).toArray(new AlarmAssetLink[0]);
    }
    
    protected void verifyAccess(SentAlarm sentAlarm) {
        if (sentAlarm == null) {
            LOG.fine("DENIED: Alarm not found");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (isSuperUser()) {
            LOG.finest("ALLOWED: Request from super user");
            return;
        }

        if (!isAuthenticated()) {
            LOG.fine("DENIED: Anonymous request are forbidden");
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    
}
