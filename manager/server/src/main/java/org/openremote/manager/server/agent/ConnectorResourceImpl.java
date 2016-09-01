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

import org.openremote.container.util.IdentifierUtil;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.attribute.Attributes;
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
    public AssetInfo[] getConnectors(@BeanParam RequestParams requestParams) {
        Collection<ConnectorComponent> connectors = connectorService.getConnectors().values();
        return connectors
                .stream()
                .map(c -> new AssetInfo(IdentifierUtil.getEncodedHash(c.getType().getBytes()), c.getDisplayName(), c.getType(), null))
                .toArray(size -> new AssetInfo[size]);
    }


    @Override
    public AssetInfo getConnector(@BeanParam RequestParams requestParams, String connectorId) {
        Collection<ConnectorComponent> connectors = connectorService.getConnectors().values();
        ConnectorComponent connector = connectors
                .stream()
                .filter(c -> IdentifierUtil.getEncodedHash(c.getType().getBytes()).equals(connectorId))
                .findFirst()
                .orElse(null);
        return connector == null ? null : new AssetInfo(connectorId, connector.getDisplayName(), connector.getType(), null);
    }

    @Override
    public AssetInfo[] getChildren(@BeanParam RequestParams requestParams, String parentId) {
        // TODO: Implement Connector Resource getChildren
        return new AssetInfo[0];
    }

    @Override
    public Attributes getAssetSettings(@BeanParam RequestParams requestParams, String parentId) {
        // TODO: Implement Connector Resource getAssetSettings
        return null;
    }

    @Override
    public Attributes getAssetDiscoverySettings(@BeanParam RequestParams requestParams, String parentId) {
        // TODO: Implement Connector Resource getAssetDiscoverySettings
        return null;
    }
}
