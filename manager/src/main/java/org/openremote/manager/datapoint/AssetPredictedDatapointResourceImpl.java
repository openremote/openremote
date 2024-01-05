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
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.datapoint.AssetPredictedDatapointResource;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.http.RequestParams;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.model.security.ClientRole;
import org.openremote.model.value.MetaItemType;

import java.util.Arrays;
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
    public ValueDatapoint<?>[] getPredictedDatapoints(@BeanParam RequestParams requestParams,
                                                      String assetId,
                                                      String attributeName,
                                                      AssetDatapointQuery query) {
        try {

            if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Asset<?> asset = assetStorageService.find(assetId, true);

            if (asset == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            // Realm should be accessible
            if(!isRealmActiveAndAccessible(asset.getRealm())) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // If not logged in, asset should be PUBLIC READ
            if(!isAuthenticated() && !asset.isAccessPublicRead()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // If logged in, user should have READ ASSETS role
            if(isAuthenticated() && !hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                LOG.info("Forbidden access for user '" + getUsername() + "': " + asset.getRealm());
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Attribute<?> attribute = asset.getAttribute(attributeName).orElseThrow(() ->
                new WebApplicationException(Response.Status.NOT_FOUND)
            );

            // If restricted, the attribute should also be restricted
            if(isRestrictedUser()) {
                attribute.getMeta().getValue(MetaItemType.ACCESS_RESTRICTED_READ).ifPresentOrElse((v) -> {
                    if(!v) { throw new WebApplicationException(Response.Status.FORBIDDEN); }
                }, () -> {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                });
            }

            // If not logged in, attribute should be PUBLIC READ
            if(!isAuthenticated()) {
                attribute.getMeta().getValue(MetaItemType.ACCESS_PUBLIC_READ).ifPresentOrElse((v) -> {
                    if(!v) { throw new WebApplicationException(Response.Status.FORBIDDEN); }
                }, () -> {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                });
            }
            return assetPredictedDatapointService.queryDatapoints(assetId, attribute, query).toArray(ValueDatapoint[]::new);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void writePredictedDatapoints(RequestParams requestParams, String assetId, String attributeName, ValueDatapoint<?>[] predictedDatapoints) {
        try {

            if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Asset<?> asset = assetStorageService.find(assetId, true);

            if (asset == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            // Realm should be accessible
            if(!isRealmActiveAndAccessible(asset.getRealm())) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // If not logged in, asset should be PUBLIC READ
            if(!isAuthenticated() && !asset.isAccessPublicRead()) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            // If logged in, user should have READ ASSETS role
            if(isAuthenticated() && !hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                LOG.info("Forbidden access for user '" + getUsername() + "': " + asset.getRealm());
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Attribute<?> attribute = asset.getAttribute(attributeName).orElseThrow(() ->
                new WebApplicationException(Response.Status.NOT_FOUND)
            );

            // If restricted, the attribute should also be restricted
            if(isRestrictedUser()) {
                attribute.getMeta().getValue(MetaItemType.ACCESS_RESTRICTED_READ).ifPresentOrElse((v) -> {
                    if(!v) { throw new WebApplicationException(Response.Status.FORBIDDEN); }
                }, () -> {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                });
            }

            // If not logged in, attribute should be PUBLIC READ
            if(!isAuthenticated()) {
                attribute.getMeta().getValue(MetaItemType.ACCESS_PUBLIC_READ).ifPresentOrElse((v) -> {
                    if(!v) { throw new WebApplicationException(Response.Status.FORBIDDEN); }
                }, () -> {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                });
            }
            assetPredictedDatapointService.updateValues(assetId, attributeName, Arrays.asList(predictedDatapoints));

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
    }
}
