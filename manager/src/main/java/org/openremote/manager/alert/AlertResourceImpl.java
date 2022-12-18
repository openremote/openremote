package org.openremote.manager.alert;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.alert.AlertService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.alert.Alert;
import org.openremote.model.alert.AlertResource;
import org.openremote.model.alert.SentAlert;
import org.openremote.model.http.RequestParams;
import org.openremote.model.Constants;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.*;
import static org.openremote.model.alert.Alert.Trigger.CLIENT;

public class AlertResourceImpl extends WebResource implements AlertResource {

    private static final Logger LOG = Logger.getLogger(AlertResourceImpl.class.getName());

    final protected AlertService alertService;
    final protected MessageBrokerService messageBrokerService;
    final protected AssetStorageService assetStorageService;

    final ManagerIdentityService managerIdentityService;

    public AlertResourceImpl(AlertService alertService,
                             MessageBrokerService messageBrokerService,
                             AssetStorageService assetStorageService,
                             ManagerIdentityService managerIdentityService) {
        this.alertService = alertService;
        this.messageBrokerService = messageBrokerService;
        this.assetStorageService = assetStorageService;
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public SentAlert[] getAlerts(RequestParams requestParams, Long id, String severity) {
        try{
            return null;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public void removeAlerts(RequestParams requestParams, Long id, String severity) {
        try{

        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public  void removeAlert(RequestParams requestParams, Long alertId) {
        if (alertId == null) {
            throw new WebApplicationException("Missing alert ID", BAD_REQUEST);
        }

    }

    @Override
    public void createAlert(RequestParams requestParams, Alert alert) {

    }

    protected void verifyAccess(SentAlert sentAlert, String targetId){}
}
