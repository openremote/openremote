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
package org.openremote.manager.server.datapoint;

import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebResource;
import org.openremote.manager.shared.datapoint.AssetDatapointResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.datapoint.NumberDatapoint;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AssetDatapointResourceImpl extends ManagerWebResource implements AssetDatapointResource {

    private static final Logger LOG = Logger.getLogger(AssetDatapointResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;
    protected final AssetDatapointService assetDatapointService;

    public AssetDatapointResourceImpl(ManagerIdentityService identityService,
                                      AssetStorageService assetStorageService,
                                      AssetDatapointService assetDatapointService) {
        super(identityService);
        this.assetStorageService = assetStorageService;
        this.assetDatapointService = assetDatapointService;
    }

    @Override
    public NumberDatapoint[] getNumberDatapoints(@BeanParam RequestParams requestParams, String assetId, String attributeName) {
        try {
            List<NumberDatapoint> result = new ArrayList<>();

            result.add(new NumberDatapoint("A", 1));
            result.add(new NumberDatapoint("B", 2));
            result.add(new NumberDatapoint("C", 0));
            result.add(new NumberDatapoint("D", 4));
            result.add(new NumberDatapoint("E", 3));
            result.add(new NumberDatapoint("F", 2));
            result.add(new NumberDatapoint("G", 4));

            return result.toArray(new NumberDatapoint[result.size()]);
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

}
