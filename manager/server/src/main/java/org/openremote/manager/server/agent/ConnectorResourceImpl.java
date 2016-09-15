/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.server.agent;

import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.connector.Connector;
import org.openremote.manager.shared.connector.ConnectorComponent;
import org.openremote.manager.shared.connector.ConnectorResource;
import org.openremote.manager.shared.http.RequestParams;

import javax.ws.rs.BeanParam;
import java.util.Collection;

public class ConnectorResourceImpl extends WebResource implements ConnectorResource {

    protected final ConnectorService connectorService;

    public ConnectorResourceImpl(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @Override
    public Connector[] getConnectors(@BeanParam RequestParams requestParams) {
        Collection<ConnectorComponent> connectorComponents = connectorService.getConnectorComponents().values();
        return connectorComponents
            .stream()
            .map(c -> new Connector(
                c.getDisplayName(),
                c.getType(),
                c.isSupportingDiscoveryTrigger(),
                c.getConnectorSettings()
            ))
            .toArray(Connector[]::new);
    }

}
