/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.agent.protocol;

import org.apache.camel.Exchange;
import org.apache.camel.processor.StopProcessor;
import org.openremote.container.ContainerService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.model.Constants;
import org.openremote.model.security.ClientRole;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.apache.camel.builder.Builder.header;

/**
 * Interface for {@link Protocol}s to add/remove Camel {@link Exchange} interceptors for specific clients and to
 * also add/remove clients.
 *
 */
public interface ProtocolClientEventService extends ContainerService {

    String HEADER_ACCESS_RESTRICTED = ProtocolClientEventService.class.getName() + ".HEADER_ACCESS_RESTRICTED";
    String HEADER_CONNECTION_TYPE = ProtocolClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE";
    String HEADER_CONNECTION_TYPE_WEBSOCKET = ProtocolClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE_WEBSOCKET";
    String HEADER_CONNECTION_TYPE_MQTT = ProtocolClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE_MQTT";
    String HEADER_REQUEST_RESPONSE_MESSAGE_ID = ProtocolClientEventService.class.getName() + ".HEADER_REQUEST_RESPONSE_MESSAGE_ID";

    class ClientCredentials {
        String realm;
        ClientRole[] roles;
        String clientId;
        String secret;

        public ClientCredentials(String realm, ClientRole[] roles, String clientId, String secret) {
            this.realm = realm;
            this.roles = roles;
            this.clientId = clientId;
            this.secret = secret;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public ClientRole[] getRoles() {
            return roles;
        }

        public void setRoles(ClientRole[] roles) {
            this.roles = roles;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        @Override
        public String toString() {
            return ClientCredentials.class.getSimpleName() + "{" +
                "realm='" + realm + '\'' +
                ", roles=" + Arrays.toString(roles) +
                ", clientId='" + clientId + '\'' +
                ", secret=" + secret +
                '}';
        }
    }

    void addExchangeInterceptor(Consumer<Exchange> exchangeInterceptor) throws RuntimeException;

    void removeExchangeInterceptor(Consumer<Exchange> exchangeInterceptor);

    void closeSession(String sessionKey);

    void sendToSession(String sessionKey, Object data);

    void addClientCredentials(ClientCredentials clientCredentials) throws RuntimeException;

    void removeClientCredentials(String realm, String  clientId);

    /**
     * Method to stop further processing of the exchange
     */
    static void stopMessage(Exchange exchange) {
        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
    }

    static boolean isInbound(Exchange exchange) {
        return header(HEADER_CONNECTION_TYPE).isNotNull().matches(exchange);
    }

    static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(ConnectionConstants.SESSION_KEY, String.class);
    }

    static String getClientId(Exchange exchange) {
        AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
        if(authContext != null) {
            return authContext.getClientId();
        }
        return null;
    }
}
