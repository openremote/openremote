package org.openremote.components.client.rest;

import elemental.client.Browser;
import elemental.html.Location;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;
import org.openremote.model.http.Request;

import java.util.logging.Level;
import java.util.logging.Logger;

@JsType(isNative = true, namespace = "openremote")
public class REST {

    @JsOverlay
    private static final Logger LOG = Logger.getLogger(REST.class.getName());

    public static class Configuration {
        public static void setDefaults(String realm) {
            Location location = Browser.getWindow().getLocation();
            REST.apiURL = "//" + location.getHostname() + ":" + location.getPort() + "/" + realm;
            REST.loglevel = LOG.isLoggable(Level.FINE) ? 1 : 0;
        }
    }

    public static String apiURL;

    public static boolean debug;

    public static boolean antiBrowserCache;

    public static int loglevel;

    // Enable debug to fill this value
    public static Request.XMLHttpRequest lastRequest;
}
