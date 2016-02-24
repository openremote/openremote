package org.openremote.manager.client.service;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.util.Base64Utils;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Richard on 10/02/2016.
 */
public class SecurityServiceImpl implements SecurityService {
    private static Logger logger = Logger.getLogger("");

    private CookieService cookieService;
    private EventBus eventBus;

    @Inject
    public SecurityServiceImpl(CookieService cookieService,
                               EventBus eventBus) {
        this.cookieService = cookieService;
        this.eventBus = eventBus;
    }

    private String getToken() {
        return cookieService.getCookie("access_token");
    }

    private JsonObject decodeToken() {
        JsonObject decodedToken = Json.createObject();

        String token = getToken();
        if (token != null) {
            String[] parts = token.split("\\.");

            if (parts.length == 3) {
                try {
                    String decoded = new String(Base64Utils.fromBase64(parts[1]), "UTF-8");
                    decodedToken = Json.parse(decoded);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            }
        }

        return decodedToken;
    }


    @Override
    public String getUsername() {
        JsonObject decoded = decodeToken();
        String name = null;

        if (decoded.hasKey("name")) {
            name = decoded.getString("name");
        }

        return name;
    }

    @Override
    public boolean isTokenExpired() {
        Date expiryDate = getTokenExpirationDate();
        if (expiryDate == null) {
            return true;
        }

        return expiryDate.before(new Date());
    }

    @Override
    public boolean hasToken() {
        return getToken() != null;
    }

    @Override
    public Date getTokenExpirationDate() {
        JsonObject decoded = decodeToken();
        Date expiryDate = null;

        if (decoded.hasKey("exp")) {
            // This is seconds since the EPOCH
            String expiryDateStr = decoded.getString("exp");
            expiryDate = new Date();
            // Can't use calendar on client side
            expiryDate.setSeconds(Integer.parseInt(expiryDateStr));
        }

        return expiryDate;
    }

    @Override
    public String getXsrfToken() {
        return cookieService.getCookie("XSRF-TOKEN");
    }

    @Override
    public void logout() {
        // Destroy the token and XSRF cookies
        cookieService.removeCookie("access_token");
        cookieService.removeCookie("XSRF-TOKEN");
        eventBus.fireEvent(new UserChangeEvent(null));
    }
}
