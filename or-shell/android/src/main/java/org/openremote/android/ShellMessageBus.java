/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.android;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openremote.shared.event.Event;
import org.openremote.shared.event.bus.EventBus;
import org.openremote.shared.event.bus.EventListener;
import org.openremote.shared.event.bus.EventRegistration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.android.util.JsonUtil.JSON;

/**
 * Receive and send JSON-serialized instances of {@link Event} from and to WebView.
 */
public class ShellMessageBus extends EventRegistration  {

    private static final Logger LOG = Logger.getLogger(ShellMessageBus.class.getName());

    @SuppressWarnings("unchecked")
    public ShellMessageBus(final Activity activity, final WebView webView) {
        super(false, new EventListener() {
            @Override
            public void on(Event event) {
                LOG.fine("Publishing shell event: " + event.getType());
                final String eventPayload;
                try {
                    eventPayload = JSON.writeValueAsString(event);
                } catch (JsonProcessingException ex) {
                    LOG.log(Level.WARNING, "Can't convert event to JSON string", ex);
                    return;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String escapedMsg = StringEscapeUtils.escapeEcmaScript(eventPayload);
                        webView.loadUrl(
                            "javascript:publishShellEvent('" + escapedMsg + "')"
                        );
                    }
                });
            }
        });
    }

    @JavascriptInterface
    public void onShellEvent(String msg) {
        Event event;
        try {
            event = JSON.readValue(msg, Event.class);
            LOG.fine("Received shell event: " + event.getType());
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Can't convert JSON string to event", ex);
            return;
        }
        EventBus.dispatch(event, this);
    }

}