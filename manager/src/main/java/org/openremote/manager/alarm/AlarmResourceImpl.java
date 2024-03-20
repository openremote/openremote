package org.openremote.manager.alarm;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.AlarmResource;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.http.RequestParams;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AlarmResourceImpl extends WebResource implements AlarmResource {
    private static final Logger LOG = Logger.getLogger(AlarmResourceImpl.class.getName());

    protected final AlarmService alarmService;
    protected final MessageBrokerService messageBrokerService;
    protected final AssetStorageService assetStorageService;
    protected final String invalidCriteria;
    protected final String missingId;
    protected final String missingRealm;

    final ManagerIdentityService managerIdentityService;

    public AlarmResourceImpl(AlarmService alarmService,
                             MessageBrokerService messageBrokerService,
                             AssetStorageService assetStorageService,
                             ManagerIdentityService managerIdentityService) {
        this.alarmService = alarmService;
        this.messageBrokerService = messageBrokerService;
        this.assetStorageService = assetStorageService;
        this.managerIdentityService = managerIdentityService;
        this.invalidCriteria = "Invalid criteria set";
        this.missingId = "Missing alarm ID";
        this.missingRealm = "Missing realm";
    }

    @Override
    public SentAlarm[] getAlarms(RequestParams requestParams) {
        try{
            return alarmService.getAlarms().toArray(new SentAlarm[0]);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(this.invalidCriteria, Status.BAD_REQUEST);
        }
    }

    @Override
    public void removeAlarms(RequestParams requestParams, List<Long> ids) {
        try{
            alarmService.removeAlarms(ids, getAuthenticatedRealmName());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(this.invalidCriteria, Status.BAD_REQUEST);
        }
    }

    @Override
    public void removeAlarm(RequestParams requestParams, Long alarmId) {
        if (alarmId == null) {
            throw new WebApplicationException(this.missingId, Status.BAD_REQUEST);
        }
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
        if (alarmId == null) {
            throw new WebApplicationException(this.missingId, Status.BAD_REQUEST);
        }
        this.verifyAccess(alarm);
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
        try{
            return alarmService.getOpenAlarms();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(this.invalidCriteria, Status.BAD_REQUEST);
        }
    }

    @Override
    public List<AlarmAssetLink> getAssetLinks(RequestParams requestParams, Long alarmId, String realm) {
        if (alarmId == null) {
            throw new WebApplicationException(this.missingId, Status.BAD_REQUEST);
        }
        if (realm == null) {
            throw new WebApplicationException(this.missingRealm, Status.BAD_REQUEST);
        }
        return alarmService.getAssetLinks(alarmId, realm);
    }

    @Override
    public void setAssetLinks(RequestParams requestParams, ArrayList<AlarmAssetLink> links) {
        if(links.isEmpty()){
            throw new WebApplicationException("Missing links", Status.BAD_REQUEST);
        }
        alarmService.linkAssets(links);
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
