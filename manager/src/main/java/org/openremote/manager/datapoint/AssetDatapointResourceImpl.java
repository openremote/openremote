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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ConnectionCallback;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapointResource;
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.MetaItemType;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.openremote.model.syslog.SyslogCategory.DATA;
import static org.openremote.model.util.ValueUtil.JSON;

public class AssetDatapointResourceImpl extends ManagerWebResource implements AssetDatapointResource {

    private static final Logger LOG = Logger.getLogger(AssetDatapointResourceImpl.class.getName());
    private static final Logger DATA_EXPORT_LOG = SyslogCategory.getLogger(DATA, AssetDatapointResourceImpl.class);

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
            if (query != null) {
                return assetDatapointService.queryDatapoints(assetId, attribute, query).toArray(ValueDatapoint[]::new);
            }

            return assetDatapointService.getDatapoints(new AttributeRef(assetId, attributeName)).toArray(ValueDatapoint[]::new);
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

            if (!isRealmActiveAndAccessible(asset.getRealm())) {
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
    public void getDatapointExport(AsyncResponse asyncResponse, String attributeRefsString, long fromTimestamp, long toTimestamp) {
        try {
            AttributeRef[] attributeRefs = JSON.readValue(attributeRefsString, AttributeRef[].class);

            for (AttributeRef attributeRef : attributeRefs) {
                if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), attributeRef.getId())) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }

                Asset<?> asset = assetStorageService.find(attributeRef.getId(), true);

                if (asset == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }

                if (!isRealmActiveAndAccessible(asset.getRealm())) {
                    DATA_EXPORT_LOG.info("Forbidden access for user '" + getUsername() + "': " + asset);
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }

                asset.getAttribute(attributeRef.getName()).orElseThrow(() ->
                        new WebApplicationException(Response.Status.NOT_FOUND)
                );
            }

            DATA_EXPORT_LOG.info("User '" + getUsername() +  "' started data export for " + attributeRefsString + " from " + fromTimestamp + " to " + toTimestamp);

            ScheduledFuture<File> exportFuture = assetDatapointService.exportDatapoints(attributeRefs, fromTimestamp, toTimestamp);

            asyncResponse.register((ConnectionCallback) disconnected -> exportFuture.cancel(true));

            File exportFile = null;

            try {
                exportFile = exportFuture.get();

                ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
                FileInputStream fin = new FileInputStream(exportFile);
                ZipEntry zipEntry = new ZipEntry(exportFile.getName());
                zipOut.putNextEntry(zipEntry);
                IOUtils.copy(fin, zipOut);
                zipOut.closeEntry();
                zipOut.close();
                fin.close();

                response.setContentType("application/zip");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dataexport.zip\"");

                asyncResponse.resume(
                    response
                );
            } catch (Exception ex) {
                exportFuture.cancel(true);
                asyncResponse.resume(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR));
                DATA_EXPORT_LOG.log(Level.SEVERE, "Exception in ScheduledFuture: ", ex);
            } finally {
                if (exportFile != null && exportFile.exists()) {
                    try {
                        exportFile.delete();
                    } catch (Exception e) {
                        DATA_EXPORT_LOG.log(Level.SEVERE, "Failed to delete temporary export file: " + exportFile.getPath(), e);
                    }
                }
            }
        } catch (JsonProcessingException ex) {
            asyncResponse.resume(new BadRequestException(ex));
        }
    }
}
