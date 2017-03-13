package org.openremote.manager.server.notification;

import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.security.UserConfiguration;
import org.openremote.manager.shared.http.BadRequestException;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.notification.NotificationResource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import java.util.logging.Logger;

public class NotificationResourceImpl extends WebResource implements NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResourceImpl.class.getName());

    private ManagerIdentityService managerIdentityService = null;

    public NotificationResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }


    @Override
    @Path("notification_token")
    public void setNotificationToken(@BeanParam RequestParams requestParams, @FormParam("token") String token, @FormParam("device_id") String deviceId) {
        if (token == null || deviceId == null) {
            throw new BadRequestException();
        }
        UserConfiguration userConfiguration = managerIdentityService.getUserConfiguration(getRealm(), getAccessToken().getSubject());
        userConfiguration.getNotificationTokenForDeviceId().put(deviceId, token);
        managerIdentityService.mergeUserConfiguration(userConfiguration);
    }
}
