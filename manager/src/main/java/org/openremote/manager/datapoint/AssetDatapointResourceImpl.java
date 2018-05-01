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
package org.openremote.manager.datapoint;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.datapoint.AssetDatapointResource;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.datapoint.Datapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.http.RequestParams;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class AssetDatapointResourceImpl extends ManagerWebResource implements AssetDatapointResource {

    private static final Logger LOG = Logger.getLogger(AssetDatapointResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;
    protected final AssetDatapointService assetDatapointService;

    public AssetDatapointResourceImpl(TimerService timerService,
                                      ManagerIdentityService identityService,
                                      AssetStorageService assetStorageService,
                                      AssetDatapointService assetDatapointService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.assetDatapointService = assetDatapointService;
    }

    @Override
    public NumberDatapoint[] getNumberDatapoints(@BeanParam RequestParams requestParams,
                                                 String assetId,
                                                 String attributeName,
                                                 DatapointInterval interval,
                                                 long timestamp) {
        try {

            if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Asset asset = assetStorageService.find(assetId, true);

            if (asset == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(asset)) {
                LOG.fine("Forbidden access for user '" + getUsername() + "': " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            AssetAttribute attribute = asset.getAttribute(attributeName).orElseThrow(() ->
                new WebApplicationException(Response.Status.NOT_FOUND)
            );

            if (!Datapoint.isDatapointsCapable(attribute) || !attribute.isStoreDatapoints()) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            return assetDatapointService.aggregateDatapoints(
                attribute,
                interval,
                timestamp
            );
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

}
