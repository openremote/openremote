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

import com.squareup.okhttp.ResponseBody;
import org.openremote.android.util.JsonUtil;
import org.openremote.shared.event.client.ConsoleRefreshEvent;
import org.openremote.shared.event.client.ShellReadyEvent;
import org.openremote.shared.event.client.ShowFailureEvent;
import org.openremote.shared.flow.Flow;
import org.openremote.shared.inventory.ClientPresetVariant;

import java.io.IOException;
import java.util.logging.Logger;

public class InitialFlowListener extends AbstractEventListener<ShellReadyEvent> {

    private static final Logger LOG = Logger.getLogger(InitialFlowListener.class.getName());

    public InitialFlowListener(String controllerUrl) {
        super(controllerUrl);
    }

    @Override
    public void on(ShellReadyEvent event) {

        ClientPresetVariant clientPresetVariant = event.getClientPresetVariant();

        enqueue(
            request(
                resource("flow", "preset")
                    .addParameter("agent", clientPresetVariant.getUserAgent())
                    .addParameter("width", Integer.toString(clientPresetVariant.getWidthPixels()))
                    .addParameter("height", Integer.toString(clientPresetVariant.getHeightPixels()))
            ).get(),

            new ResponseCallback() {

                @Override
                protected void onSuccess(Integer responseCode, String responseMessage, ResponseBody body) {

                    try {
                        Flow flow = JsonUtil.JSON.readValue(body.string(), Flow.class);
                        dispatch(new ConsoleRefreshEvent(flow));
                    } catch (IOException ex) {
                        dispatch(
                            new ShowFailureEvent("Loading initial flow panel failed. Can't parse JSON. " + ex)
                        );
                    }
                }

                @Override
                protected void onFailure(Integer responseCode, String responseMessage, IOException ex) {
                    if (responseCode != null && responseCode == 404) {
                        dispatch(
                            new ShowFailureEvent("Controller doesn't have initial flow panel for this client.")
                        );
                    } else {
                        dispatch(
                            new ShowFailureEvent("Loading initial flow panel failed. " + responseMessage)
                        );
                    }
                }

            }
        );
    }
}
