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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    public SentAlert[] getAlerts(RequestParams requestParams, Long id, String severity, String status) {
        try{
            return alertService.getAlerts(
                    id != null ? Collections.singletonList(id) : null,
                    severity,
                    status
            ).toArray(new SentAlert[0]);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public void removeAlerts(RequestParams requestParams, Long id, String severity, String status) {
        try{
            alertService.removeAlerts(
                    id != null ? Collections.singletonList(id) : null,
                    severity,
                    status
            );
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid criteria set", BAD_REQUEST);
        }
    }

    @Override
    public  void removeAlert(RequestParams requestParams, Long alertId) {
        if (alertId == null) {
            throw new WebApplicationException("Missing alert ID", BAD_REQUEST);
        }
        alertService.removeAlert(alertId);
    }

    @Override
    public void createAlert(RequestParams requestParams, Alert alert) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Alert.HEADER_TRIGGER, CLIENT);

        if (isAuthenticated()){
            headers.put(Constants.AUTH_CONTEXT, getAuthContext());
        }

        boolean success = messageBrokerService.getFluentProducerTemplate()
                .withBody(alert)
                .withHeaders(headers)
                .to(AlertService.ALERT_QUEUE)
                .request(Boolean.class);

        if (!success) {
            throw new WebApplicationException(BAD_REQUEST);
        }
    }

    @Override
    public void setAlertStatus(RequestParams requestParams, String status, Long alertId) {
        if (alertId == null) {
            throw new WebApplicationException("Missing alert ID", BAD_REQUEST);
        }
//        SentAlert sentAlert = alertService.getSentAlert(alertId);
        //TODO: fetch the userId who changed the status
        alertService.setAlertStatus(alertId, status, "");
    }

    protected void verifyAccess(SentAlert sentAlert, String targetId) {
        if (sentAlert == null) {
            LOG.fine("DENIED: Alert not found");
            throw new WebApplicationException(NOT_FOUND);
        }

        if (isSuperUser()) {
            LOG.finest("ALLOWED: Request from super user so allowing");
            return;
        }

        if (!isAuthenticated()) {
            LOG.fine("DENIED: Anonymous request are forbidden");
            throw new WebApplicationException(FORBIDDEN);
        }

//        if (!isAuthenticated()) {
//            if (sentAlert)
//        } else {
//
//            boolean isResrictedUser = managerIdentityService.getIdentityProvider().isRestrictedUser(getAuthContext());
//            switch (sentAlert)
//        }
    }
}
