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
package org.openremote.manager.predicted;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.http.RequestParams;
import org.openremote.model.predicted.AssetPredictedDatapointResource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class AssetPredictedDatapointResourceImpl extends ManagerWebResource implements AssetPredictedDatapointResource {

    private static final Logger LOG = Logger.getLogger(AssetPredictedDatapointResourceImpl.class.getName());

    protected final AssetStorageService assetStorageService;
    protected final AssetPredictedDatapointService assetPredictedDatapointService;

    public AssetPredictedDatapointResourceImpl(TimerService timerService,
                                               ManagerIdentityService identityService,
                                               AssetStorageService assetStorageService,
                                               AssetPredictedDatapointService assetPredictedDatapointService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.assetPredictedDatapointService = assetPredictedDatapointService;
    }

    @Override
    public ValueDatapoint[] getPredictedDatapoints(@BeanParam RequestParams requestParams,
                                          String assetId,
                                          String attributeName,
                                          long fromTimestamp,
                                          long toTimestamp) {
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

            return assetPredictedDatapointService.getValueDatapoints(
                attribute,
                toTimestamp,
                fromTimestamp
            );
        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }
}
