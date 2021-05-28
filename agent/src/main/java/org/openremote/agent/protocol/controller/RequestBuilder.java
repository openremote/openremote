/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.controller;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.agent.protocol.controller.command.ControllerCommandBasic;
import org.openremote.agent.protocol.controller.command.ControllerCommandMapped;
import org.openremote.agent.protocol.http.HTTPProtocol;
import org.openremote.model.attribute.AttributeEvent;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * Request's builder created for {@link ControllerProtocol}
 */
public class RequestBuilder {

    /**
     * Default header used for every request.
     * We're working with JSON object in response
     */
    private static MultivaluedMap<String, Object> getDefaultHeaders() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        headers.addAll("Accept", MediaType.APPLICATION_JSON);

        return headers;
    }

    public static HTTPProtocol.HttpClientRequest buildCommandRequest(ControllerCommand controllerCommand, AttributeEvent event, ResteasyWebTarget webTarget) {
        MultivaluedMap<String, String> queryParam = new MultivaluedHashMap<>();

        //This part could be put in a Wrapper
        if(controllerCommand instanceof ControllerCommandBasic) {
            String commandName = ((ControllerCommandBasic) controllerCommand).getCommandName();
            queryParam.add("name", commandName);


        } else {
            Map<String, String> actionCommandLink = ((ControllerCommandMapped) controllerCommand).getActionCommandLink();
            String attributeValue = event.getValue().map(Object::toString).orElse(null);
            String correspondingCommandName = actionCommandLink.get(attributeValue);
            queryParam.add("name", correspondingCommandName);
        }

        return new HTTPProtocol.HttpClientRequest(
                webTarget,
                "/rest/devices/" + controllerCommand.getDeviceName() + "/commands",
                "POST",
                getDefaultHeaders(),
                queryParam,
                false,
                MediaType.APPLICATION_JSON
        );
    }

    public static HTTPProtocol.HttpClientRequest buildStatusPollingRequest(String deviceName, List<String> sensorsName, String deviceId, ResteasyWebTarget webTarget) {
        MultivaluedMap<String, String> queryParam = new MultivaluedHashMap<>();

        queryParam.addAll("name", sensorsName);

        return new HTTPProtocol.HttpClientRequest(
                webTarget,
                "/rest/devices/" + deviceName + "/polling/" + deviceId,
                "GET",
                getDefaultHeaders(),
                queryParam,
                false,
                MediaType.APPLICATION_JSON
        );
    }

    public static HTTPProtocol.HttpClientRequest buildStatusRequest(String deviceName, List<String> sensorsName, ResteasyWebTarget webTarget) {
        MultivaluedMap<String, String> queryParam = new MultivaluedHashMap<>();

        queryParam.addAll("name", sensorsName);

        return new HTTPProtocol.HttpClientRequest(
                webTarget,
                "/rest/devices/" + deviceName + "/status",
                "GET",
                getDefaultHeaders(),
                queryParam,
                false,
                MediaType.APPLICATION_JSON
        );
    }

    public static HTTPProtocol.HttpClientRequest buildCheckRequest(ResteasyWebTarget webTarget) {
        return new HTTPProtocol.HttpClientRequest(
                webTarget,
                "",
                "GET",
                null,
                null,
                false,
                null
        );
    }
}
