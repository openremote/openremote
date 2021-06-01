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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapointResource;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.http.RequestParams;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

import static org.openremote.model.value.Values.JSON;

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
    public ValueDatapoint<?>[] getDatapoints(@BeanParam RequestParams requestParams,
                                             String assetId,
                                             String attributeName,
                                             DatapointInterval interval,
                                             Integer stepSize,
                                             long fromTimestamp,
                                             long toTimestamp) {
        try {

            if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Asset<?> asset = assetStorageService.find(assetId, true);

            if (asset == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(asset.getRealm())) {
                LOG.info("Forbidden access for user '" + getUsername() + "': " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Attribute<?> attribute = asset.getAttribute(attributeName).orElseThrow(() ->
                    new WebApplicationException(Response.Status.NOT_FOUND)
            );

            return assetDatapointService.getValueDatapoints(assetId,
                    attribute,
                    interval,
                    stepSize,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(fromTimestamp), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(toTimestamp), ZoneId.systemDefault()));
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ex);
        } catch (UnsupportedOperationException ex) {
            throw new NotSupportedException(ex);
        }
    }

    @Override
    public DatapointPeriod getDatapointPeriod(RequestParams requestParams, String assetId, String attributeName) {
        try {
            if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), assetId)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Asset<?> asset = assetStorageService.find(assetId, true);

            if (asset == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            if (!isTenantActiveAndAccessible(asset.getRealm())) {
                LOG.info("Forbidden access for user '" + getUsername() + "': " + asset);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

            Attribute<?> attribute = asset.getAttribute(attributeName).orElseThrow(() ->
                    new WebApplicationException(Response.Status.NOT_FOUND)
            );

            return assetDatapointService.getDatapointPeriod(assetId, attributeName);
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ex);
        } catch (UnsupportedOperationException ex) {
            throw new NotSupportedException(ex);
        }
    }

    @Override
    public void getDatapointExport(RequestParams requestParams, String attributeRefsString, long fromTimestamp, long toTimestamp) {
        try {
            AttributeRef[] attributeRefs = JSON.readValue(attributeRefsString, AttributeRef[].class);

            for (AttributeRef attributeRef : attributeRefs
            ) {
                if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), attributeRef.getId())) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }

                Asset<?> asset = assetStorageService.find(attributeRef.getId(), true);

                if (asset == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }

                if (!isTenantActiveAndAccessible(asset.getRealm())) {
                    LOG.info("Forbidden access for user '" + getUsername() + "': " + asset);
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }

                Attribute<?> attribute = asset.getAttribute(attributeRef.getName()).orElseThrow(() ->
                        new WebApplicationException(Response.Status.NOT_FOUND)
                );
            }

            File file = assetDatapointService.exportDatapoints(attributeRefs, fromTimestamp, toTimestamp);

            response.setContentType("text/csv");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

            OutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(file);
            IOUtils.copy(in, out);

            out.close();
            in.close();
            file.delete();

        } catch (JsonProcessingException ex) {
            throw new BadRequestException(ex);
        } catch (IOException ex) {
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
